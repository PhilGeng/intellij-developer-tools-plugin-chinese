@file:Suppress("UnstableApiUsage")

package dev.turingcomplete.intellijdevelopertoolsplugin._internal.common

import com.intellij.application.options.CodeStyle
import com.intellij.icons.AllIcons
import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.application.ex.ClipboardUtil
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.FocusChangeListener
import com.intellij.openapi.editor.impl.EditorFactoryImpl
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.ui.AnActionButton
import com.intellij.ui.ColorUtil
import com.intellij.ui.components.JBLabel
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration.PropertyType.CONFIGURATION
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.UiUtils.actionsPopup
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.UiUtils.dumbAwareAction
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.settings.DeveloperToolsApplicationSettings
import dev.turingcomplete.intellijdevelopertoolsplugin.i18n.I18nUtils
import java.awt.datatransfer.StringSelection
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.swing.JComponent
import javax.swing.ScrollPaneConstants
import kotlin.math.max

internal class DeveloperToolEditor(
  private val id: String,
  private val context: DeveloperUiToolContext,
  private val configuration: DeveloperToolConfiguration,
  private val project: Project?,
  private val title: String? = null,
  private val editorMode: EditorMode,
  private val parentDisposable: Disposable,
  private val textProperty: ValueProperty<String> = ValueProperty(""),
  private val initialLanguage: Language = PlainTextLanguage.INSTANCE,
  private val diffSupport: DiffSupport? = null,
  private val supportsExpand: Boolean = true,
  private val minimumSizeHeight: Int = DEFAULT_MINIMUM_SIZE_HEIGHT,
) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var softWraps = configuration.register("${context.id}-${id}-softWraps", DeveloperToolsApplicationSettings.instance.editorSoftWraps, CONFIGURATION)
  private var showSpecialCharacters =
    configuration.register("${context.id}-${id}-showSpecialCharacters", DeveloperToolsApplicationSettings.instance.editorShowSpecialCharacters, CONFIGURATION)
  private var showWhitespaces =
    configuration.register("${context.id}-${id}-showWhitespaces", DeveloperToolsApplicationSettings.instance.editorShowWhitespaces, CONFIGURATION)

  private var onTextChangeFromUi = mutableListOf<((String) -> Unit)>()
  private var onFocusGained: (() -> Unit)? = null
  private var onFocusLost: (() -> Unit)? = null
  private var rangeHighlighters = mutableMapOf<String, MutableList<RangeHighlighter>>()

  private val editor: EditorEx by lazy { createEditor() }

  var text: String
    set(value) = textProperty.set(value)
    get() = textProperty.get()

  var isDisposed: Boolean = false
    private set
    get() = editor.isDisposed

  var language: Language = initialLanguage
    set(value) {
      editor.setLanguage(value)
    }

  val component: JComponent by lazy { createComponent() }

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    textProperty.afterChangeConsumeEvent(parentDisposable) { event ->
      if (event.id != TEXT_CHANGE_FROM_DOCUMENT_LISTENER) {
        runWriteAction { editor.document.setText(event.newValue) }
      }
    }

    softWraps.afterChangeConsumeEvent(parentDisposable) {
      if (it.oldValue != it.newValue) {
        editor.settings.apply {
          isUseSoftWraps = it.newValue
          isPaintSoftWraps = it.newValue
          editor.component.repaint()
        }
      }
    }

    showSpecialCharacters.afterChangeConsumeEvent(parentDisposable) {
      if (it.oldValue != it.newValue) {
        editor.settings.apply {
          isShowingSpecialChars = it.newValue
          editor.component.repaint()
        }
      }
    }

    showWhitespaces.afterChangeConsumeEvent(parentDisposable) {
      if (it.oldValue != it.newValue) {
        editor.settings.apply {
          isWhitespacesShown = it.newValue
          editor.component.repaint()
        }
      }
    }
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun onTextChangeFromUi(changeListener: ((String) -> Unit)): DeveloperToolEditor {
    onTextChangeFromUi.add(changeListener)
    return this
  }

  fun onFocusGained(changeListener: () -> Unit): DeveloperToolEditor {
    onFocusGained = changeListener
    return this
  }

  @Suppress("unused")
  fun onFocusLost(changeListener: () -> Unit): DeveloperToolEditor {
    onFocusLost = changeListener
    return this
  }

  private fun createComponent(): JComponent =
    object : BorderLayoutPanel(0, UIUtil.DEFAULT_VGAP), DataProvider {
      init {
        title?.let { addToTop(JBLabel("$it ${editorMode.title}:")) }

        editor.component.border = ValidationResultBorder(editor.component, editor.contentComponent)
        val editorComponent = editor.component.wrapWithToolBar(DeveloperToolEditor::class.java.simpleName, createActions(), ToolBarPlace.RIGHT)
        addToCenter(editorComponent)
        // This prevents the `Editor` from increasing the size of the dialog if
        // the text in the editor is larger than the preferred height on the
        // screen.
        preferredSize = JBUI.size(0, max(minimumSizeHeight, DEFAULT_PREFFERED_SIZE_HEIGHT))
        minimumSize = JBUI.size(0, minimumSizeHeight)
      }

      override fun getData(dataId: String): Any? = when {
        CommonDataKeys.EDITOR.`is`(dataId) -> editor
        else -> null
      }
    }

  fun <T> bindValidator(validation: ValidationInfoBuilder.(T) -> ValidationInfo?): ValidationInfoBuilder.(T) -> ValidationInfo? = {
    validation(it)?.forComponent(editor.component)
  }

  fun removeAllTextRangeHighlighters() {
    editor.markupModel.removeAllHighlighters()
    rangeHighlighters.clear()
  }

  fun removeTextRangeHighlighters(groupId: String) {
    rangeHighlighters[groupId]?.forEach { editor.markupModel.removeHighlighter(it) }
    rangeHighlighters.remove(groupId)
  }

  fun highlightTextRange(
    textRange: TextRange,
    layer: Int,
    textAttributes: TextAttributes?,
    groupId: String = "other",
    gutterIconRenderer: GutterIconRenderer? = null,
  ) {
    val rangeHighlighter = editor.markupModel.addRangeHighlighter(
      textRange.startOffset,
      textRange.endOffset,
      layer,
      textAttributes,
      HighlighterTargetArea.EXACT_RANGE
    )
    rangeHighlighter.gutterIconRenderer = gutterIconRenderer
    rangeHighlighters.computeIfAbsent(groupId) { mutableListOf() }.add(rangeHighlighter)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun createActions(): ActionGroup = DefaultActionGroup().apply {
    add(CopyContentAction())
    if (editorMode.editable) {
      add(ClearContentAction())
    }

    addSeparator()

    val settingsActions = mutableListOf<AnAction>().apply {
      add(
        SimpleToggleAction(
        text = I18nUtils.message("editor.soft_wrap"),
        icon = AllIcons.Actions.ToggleSoftWrap,
        isSelected = { softWraps.get() },
        setSelected = { softWraps.set(it) }
      ))
      add(
        SimpleToggleAction(
        text = I18nUtils.message("editor.show_special_characters"),
        icon = null,
        isSelected = { showSpecialCharacters.get() },
        setSelected = { showSpecialCharacters.set(it) }
      ))
      add(
        SimpleToggleAction(
        text = I18nUtils.message("editor.show_whitespaces"),
        icon = null,
        isSelected = { showWhitespaces.get() },
        setSelected = { showWhitespaces.set(it) }
      ))
    }
    add(
      actionsPopup(
        title = I18nUtils.message("editor.settings"),
        icon = AllIcons.General.Settings,
        actions = settingsActions
      )
    )

    addSeparator()

    val additionalActions = mutableListOf<AnAction>().apply {
      addAll(createDiffAction())
      add(Separator.getInstance())
      add(SaveContentToFileAction())
      if (editorMode.editable) {
        add(OpenContentFromFileAction())
      }
      if (supportsExpand) {
        add(Separator.getInstance())
        add(ExpandEditorAction(this@DeveloperToolEditor))
      }
    }
    add(
      actionsPopup(
        title = I18nUtils.message("editor.additional_actions"),
        icon = AllIcons.Actions.MoreHorizontal,
        actions = additionalActions
      )
    )
  }

  private fun createDiffAction(): List<AnAction> {
    val actions = mutableListOf<AnAction>()

    val firstTitle = title ?: "Content"

    actions.add(dumbAwareAction(I18nUtils.message("editor.more_show_diff_clipboard"), AllIcons.Actions.DiffWithClipboard) { e ->
      val editor = e.getData(CommonDataKeys.EDITOR) ?: error("snh: Editor not found")
      val firstText = runReadAction { editor.document.text }
      UiUtils.showDiffDialog(
        title = I18nUtils.message("editor.more_show_diff_clipboard"),
        firstTitle = firstTitle,
        secondTitle = I18nUtils.message("editor.more_show_diff_clipboard_title"),
        firstText = firstText,
        secondText = ClipboardUtil.getTextInClipboard() ?: ""
      )
    })

    diffSupport?.let {
      actions.add(dumbAwareAction(I18nUtils.message("editor.more_show_diff_other", it.secondTitle), AllIcons.Actions.Diff) { e ->
        val editor = e.getData(CommonDataKeys.EDITOR) ?: error("snh: Editor not found")
        val firstText = runReadAction { editor.document.text }
        UiUtils.showDiffDialog(
          title = I18nUtils.message("editor.more_show_diff_other", it.secondTitle),
          firstTitle = firstTitle,
          secondTitle = it.secondTitle,
          firstText = firstText,
          secondText = it.secondText()
        )
      })
    }

    return actions
  }

  private fun createEditor(): EditorEx {
    val editorFactory = EditorFactory.getInstance()
    val document = (editorFactory as EditorFactoryImpl).createDocument(textProperty.get(), true, false)
    val editor = if (editorMode.editable) {
      editorFactory.createEditor(document, project) as EditorEx
    }
    else {
      editorFactory.createViewer(document, project) as EditorEx
    }
    editor.putUserData(editorActiveKey, UIUtil.hasFocus(editor.contentComponent))
    Disposer.register(parentDisposable) { EditorFactory.getInstance().releaseEditor(editor) }

    return editor.apply {
      document.addDocumentListener(TextChangeListener(), parentDisposable)
      addFocusListener(FocusListener(), parentDisposable)

      syncEditorColors()

      setBorder(JBUI.Borders.empty())
      setCaretVisible(true)
      scrollPane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
      scrollPane.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS

      setLanguage(language)

      settings.apply {
        isLineMarkerAreaShown = true
        isIndentGuidesShown = true
        isLineNumbersShown = true
        isFoldingOutlineShown = false
        isUseSoftWraps = softWraps.get()
        isPaintSoftWraps = softWraps.get()
        isShowingSpecialChars = showSpecialCharacters.get()
        isWhitespacesShown = showWhitespaces.get()
        isBlinkCaret = editorMode.editable
        additionalLinesCount = 0
        project?.let {
          setTabSize(CodeStyle.getIndentOptions(it, document).TAB_SIZE)
        }
      }
    }
  }

  private fun EditorEx.syncEditorColors() {
    setBackgroundColor(null) // To use background from set color scheme

    val isLaFDark = ColorUtil.isDark(UIUtil.getPanelBackground())
    val isEditorDark = EditorColorsManager.getInstance().isDarkEditor
    colorsScheme = if (isLaFDark == isEditorDark) {
      EditorColorsManager.getInstance().globalScheme
    }
    else {
      EditorColorsManager.getInstance().schemeForCurrentUITheme
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private inner class TextChangeListener : DocumentListener {

    override fun documentChanged(event: DocumentEvent) {
      val currentText = event.document.text

      textProperty.set(value = currentText, changeId = TEXT_CHANGE_FROM_DOCUMENT_LISTENER)

      if (editor.getUserData(editorActiveKey)!!) {
        onTextChangeFromUi.forEach { it(currentText) }
      }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private inner class FocusListener : FocusChangeListener {

    override fun focusGained(editor: Editor) {
      editor.putUserData(editorActiveKey, true)
      editor.component.repaint()
      onFocusGained?.invoke()
    }

    override fun focusLost(editor: Editor) {
      editor.putUserData(editorActiveKey, false)
      editor.component.repaint()
      onFocusLost?.invoke()
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class ClearContentAction
    : DumbAwareAction(I18nUtils.message("ClearContentAction.title"), I18nUtils.message("ClearContentAction.description"), AllIcons.Actions.GC) {

    override fun actionPerformed(e: AnActionEvent) {
      val editor = e.getData(CommonDataKeys.EDITOR) ?: error("snh: Editor not found")
      editor.contentComponent.grabFocus()
      runWriteAction {
        editor.putUserData(editorActiveKey, true)
        editor.document.setText("")
      }
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class CopyContentAction
    : DumbAwareAction(I18nUtils.message("CopyContentAction.title"), I18nUtils.message("CopyContentAction.description"), AllIcons.Actions.Copy) {

    override fun actionPerformed(e: AnActionEvent) {
      val editor = e.getData(CommonDataKeys.EDITOR) ?: error("snh: Editor not found")
      val content = runReadAction { editor.document.text }
      CopyPasteManager.getInstance().setContents(StringSelection(content))
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class SaveContentToFileAction
    : DumbAwareAction(I18nUtils.message("SaveContentToFileAction.title"), I18nUtils.message("SaveContentToFileAction.description"), AllIcons.Actions.MenuSaveall) {

    override fun actionPerformed(e: AnActionEvent) {
      val editor = e.getData(CommonDataKeys.EDITOR) ?: error("snh: Editor not found")
      val fileSaverDescriptor = FileSaverDescriptor("Save Content As", "")
      val timeStamp = LocalDateTime.now().format(timestampFormat)
      val defaultFilename = "$timeStamp.txt"
      FileChooserFactory.getInstance()
        .createSaveFileDialog(fileSaverDescriptor, e.project)
        .save(defaultFilename)?.file?.toPath()?.let {
          val content = runReadAction { editor.document.text }
          Files.writeString(it, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        }
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class OpenContentFromFileAction
    : DumbAwareAction(I18nUtils.message("OpenContentFromFileAction.title"), I18nUtils.message("OpenContentFromFileAction.description"), AllIcons.Actions.MenuOpen) {

    override fun actionPerformed(e: AnActionEvent) {
      val editor = e.getData(CommonDataKeys.EDITOR) ?: error("snh: Editor not found")
      val fileChooserDescriptor = FileChooserDescriptor(true, true, false, false, false, false)
      FileChooserFactory.getInstance()
        .createFileChooser(fileChooserDescriptor, e.project, editor.component)
        .choose(e.project).firstOrNull()?.let {
          runWriteAction {
            editor.putUserData(editorActiveKey, true)
            editor.document.setText(Files.readString(it.toNioPath()))
          }
          editor.contentComponent.grabFocus()
        }
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class ExpandEditorAction(private val originalEditor: DeveloperToolEditor) :
    AnActionButton(I18nUtils.message("ExpandEditorAction.title"), null, AllIcons.Actions.MoveToWindow), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
      val expandedText = ValueProperty(originalEditor.text)
      val expandedEditor = createExpandedEditor(originalEditor, expandedText)
      val apply = object : DialogWrapper(originalEditor.component, true) {

        init {
          setSize(700, 550)
          init()

          setOKButtonText(I18nUtils.message("ExpandEditorAction.apply"))
        }

        override fun createCenterPanel(): JComponent = expandedEditor.component

        override fun getDimensionServiceKey(): String? = ExpandEditorAction::class.java.name
      }.showAndGet()

      if (apply) {
        runWriteAction {
          originalEditor.editor.putUserData(editorActiveKey, true)
          originalEditor.editor.document.setText(expandedText.get())
        }
      }
    }

    private fun createExpandedEditor(originalEditor: DeveloperToolEditor, textProperty: ValueProperty<String>) =
      DeveloperToolEditor(
        id = "${originalEditor.id}-expanded",
        context = originalEditor.context,
        configuration = originalEditor.configuration,
        project = originalEditor.project,
        title = originalEditor.title,
        editorMode = originalEditor.editorMode,
        parentDisposable = originalEditor.parentDisposable,
        textProperty = textProperty,
        initialLanguage = originalEditor.initialLanguage,
        diffSupport = originalEditor.diffSupport,
        supportsExpand = false
      )

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
  }
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  data class DiffSupport(
    val title: String,
    val secondTitle: String,
    val secondText: () -> String
  )

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  enum class EditorMode(val title: String, val editable: Boolean) {

    INPUT(I18nUtils.message("editor.input"), true),
    OUTPUT(I18nUtils.message("editor.output"), false),
    INPUT_OUTPUT(I18nUtils.message("editor.input_output"), true),
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private const val TEXT_CHANGE_FROM_DOCUMENT_LISTENER = "documentChangeListener"

    private const val DEFAULT_PREFFERED_SIZE_HEIGHT = 120
    private const val DEFAULT_MINIMUM_SIZE_HEIGHT = 50

    private val editorActiveKey = Key<Boolean>("editorActive")
    private val timestampFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-SS")
  }
}