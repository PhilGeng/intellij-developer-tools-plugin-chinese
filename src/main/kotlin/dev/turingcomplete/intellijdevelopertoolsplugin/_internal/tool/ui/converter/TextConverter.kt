package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter

import com.intellij.icons.AllIcons
import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.selected
import com.intellij.ui.layout.not
import com.intellij.util.Alarm
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration.PropertyType.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiTool
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.DeveloperToolEditor
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.DeveloperToolEditor.EditorMode.INPUT_OUTPUT
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.ErrorHolder
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.PropertyComponentPredicate
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.TextConverter.ActiveInput.SOURCE
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.TextConverter.ActiveInput.TARGET
import dev.turingcomplete.intellijdevelopertoolsplugin.i18n.I18nUtils

internal abstract class TextConverter(
  protected val textConverterContext: TextConverterContext,
  protected val configuration: DeveloperToolConfiguration,
  protected val context: DeveloperUiToolContext,
  protected val project: Project?,
  parentDisposable: Disposable
) : DeveloperUiTool(parentDisposable), DeveloperToolConfiguration.ChangeListener {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var liveConversion = configuration.register("liveConversion", true)
  protected var sourceText = configuration.register("sourceText", textConverterContext.defaultSourceText, INPUT)
  protected var targetText = configuration.register("targetText", textConverterContext.defaultTargetText, INPUT)

  private val conversionAlarm by lazy { Alarm(parentDisposable) }

  private var lastActiveInput = AtomicProperty(SOURCE)
  private val toSourceActive = PropertyComponentPredicate(lastActiveInput, TARGET)

  private val sourceEditor by lazy { createSourceEditor() }
  private val targetEditor by lazy { createTargetEditor() }

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    liveConversion.afterChange(parentDisposable) {
      liveTransformToLastActiveInput()
    }
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun Panel.buildUi() {
    buildTopConfigurationUi()

    row {
      resizableRow()
      val sourceEditorCell = cell(sourceEditor.component).align(Align.FILL)
      textConverterContext.sourceErrorHolder?.let { sourceErrorHolder ->
        sourceEditorCell
          .validationOnApply(sourceEditor.bindValidator(sourceErrorHolder.asValidation()))
          .validationRequestor(DUMMY_DIALOG_VALIDATION_REQUESTOR)
      }
    }

    buildMiddleFirstConfigurationUi()
    buildActionsUi()
    buildMiddleSecondConfigurationUi()

    row {
      resizableRow()
      val targetEditorCell = cell(targetEditor.component).align(Align.FILL)
      textConverterContext.targetErrorHolder?.let { targetErrorHolder ->
        targetEditorCell
          .validationOnApply(targetEditor.bindValidator(targetErrorHolder.asValidation()))
          .validationRequestor(DUMMY_DIALOG_VALIDATION_REQUESTOR)
      }
    }
  }

  protected open fun Panel.buildTopConfigurationUi() {
    // Override if needed
  }

  protected open fun Panel.buildMiddleFirstConfigurationUi() {
    // Override if needed
  }

  protected open fun Panel.buildMiddleSecondConfigurationUi() {
    // Override if needed
  }

  protected fun setSourceLanguage(language: Language) {
    sourceEditor.language = language
  }

  protected fun setTargetLanguage(language: Language) {
    targetEditor.language = language
  }

  abstract fun toTarget(text: String)

  abstract fun toSource(text: String)

  fun targetText(): String = targetText.get()

  fun sourceText(): String = sourceText.get()

  override fun configurationChanged(property: ValueProperty<out Any>) {
    liveTransformToLastActiveInput()
  }

  override fun activated() {
    configuration.addChangeListener(parentDisposable, this)
  }

  override fun deactivated() {
    configuration.removeChangeListener(this)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun Panel.buildActionsUi() {
    buttonsGroup {
      row {
        val liveConversionCheckBox = checkBox(I18nUtils.message("editor.live_conversion"))
          .bindSelected(liveConversion)
          .gap(RightGap.SMALL)
        icon(AllIcons.General.ArrowUp)
          .visibleIf(toSourceActive)
          .enabledIf(liveConversion)
          .gap(RightGap.SMALL)
        icon(AllIcons.General.ArrowDown)
          .visibleIf(toSourceActive.not())
          .enabledIf(liveConversion)
          .gap(RightGap.SMALL)

        button("▼ ${textConverterContext.convertActionTitle}") { transformToTarget() }
          .enabledIf(liveConversionCheckBox.selected.not())
          .gap(RightGap.SMALL)
        button("▲ ${textConverterContext.revertActionTitle}") { transformToSource() }
          .enabledIf(liveConversionCheckBox.selected.not())
      }
    }
  }

  private fun transformToSource() {
    doToSource(targetEditor.text)
  }

  private fun transformToTarget() {
    doToTarget(sourceEditor.text)
  }

  private fun doToTarget(text: String) {
    doConversion {
      try {
        toTarget(text)
      } catch (ignore: Exception) {
      }
    }
  }

  private fun doToSource(text: String) {
    doConversion {
      try {
        toSource(text)
      } catch (ignore: Exception) {
      }
    }
  }

  private fun doConversion(conversion: () -> Unit) {
    if (!isDisposed && !conversionAlarm.isDisposed) {
      conversionAlarm.cancelAllRequests()
      conversionAlarm.addRequest(conversion, 0)
    }
  }

  private fun createSourceEditor() =
    DeveloperToolEditor(
      id = "source",
      title = textConverterContext.sourceTitle,
      editorMode = INPUT_OUTPUT,
      parentDisposable = parentDisposable,
      configuration = configuration,
      context = context,
      project = project,
      textProperty = sourceText,
      diffSupport = textConverterContext.diffSupport?.let { diffSupport ->
        DeveloperToolEditor.DiffSupport(
          title = diffSupport.title,
          secondTitle = textConverterContext.targetTitle,
          secondText = { targetText.get() },
        )
      }
    ).apply {
      onFocusGained {
        lastActiveInput.set(SOURCE)
      }
      this.onTextChangeFromUi { text ->
        if (liveConversion.get()) {
          lastActiveInput.set(SOURCE)
          doToTarget(text)
        }
      }
    }

  private fun createTargetEditor(): DeveloperToolEditor {
    return DeveloperToolEditor(
      id = "target",
      title = textConverterContext.targetTitle,
      editorMode = INPUT_OUTPUT,
      parentDisposable = parentDisposable,
      configuration = configuration,
      context = context,
      project = project,
      textProperty = targetText,
      diffSupport = textConverterContext.diffSupport?.let { diffSupport ->
        DeveloperToolEditor.DiffSupport(
          title = diffSupport.title,
          secondTitle = textConverterContext.sourceTitle,
          secondText = { sourceText.get() },
        )
      }
    ).apply {
      onFocusGained {
        lastActiveInput.set(TARGET)
      }
      this.onTextChangeFromUi { text ->
        if (liveConversion.get()) {
          lastActiveInput.set(TARGET)
          doToSource(text)
        }
      }
    }
  }

  private fun liveTransformToLastActiveInput() {
    if (liveConversion.get()) {
      // Trigger a text change. So if the text was changed in manual mode, it
      // will now be converted once during the switch to live mode.
      when (lastActiveInput.get()) {
        SOURCE -> transformToTarget()
        TARGET -> transformToSource()
      }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  enum class ActiveInput {

    SOURCE,
    TARGET
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  data class TextConverterContext(
    val convertActionTitle: String,
    val revertActionTitle: String,
    val sourceTitle: String,
    val targetTitle: String,
    val sourceErrorHolder: ErrorHolder? = null,
    val targetErrorHolder: ErrorHolder? = null,
    val diffSupport: DiffSupport? = null,
    val defaultSourceText: String = "",
    val defaultTargetText: String = ""
  )

  data class DiffSupport(
    val title: String
  )

  // -- Companion Object -------------------------------------------------------------------------------------------- //
}

