package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper
import com.fasterxml.jackson.dataformat.toml.TomlMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindItem
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.ErrorHolder
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.i18n.I18nUtils

internal class CodeFormattingConverter(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?
) : TextConverter(
  textConverterContext = TextConverterContext(
    convertActionTitle = I18nUtils.message("CodeFormattingConverter.convertActionTitle"),
    revertActionTitle = I18nUtils.message("CodeFormattingConverter.revertActionTitle"),
    sourceTitle = I18nUtils.message("CodeFormattingConverter.sourceTitle"),
    targetTitle = I18nUtils.message("CodeFormattingConverter.targetTitle"),
    sourceErrorHolder = ErrorHolder(),
    targetErrorHolder = ErrorHolder(),
    diffSupport = DiffSupport(
      title = I18nUtils.message("CodeFormattingConverter.diffSupport.title")
    )
  ),
  configuration = configuration,
  parentDisposable = parentDisposable,
  context = context,
  project = project
), DeveloperToolConfiguration.ChangeListener {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var firstLanguage = configuration.register("firstLanguage", Language.JSON)
  private var secondLanguage = configuration.register("secondLanguage", Language.YAML)

  private val codeStyles by lazy { LanguageCodeStyleSettingsProvider.EP_NAME.extensionList.associate { it.language.id to it.language } }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun configurationChanged(property: ValueProperty<out Any>) {
    setLanguages()
    super.configurationChanged(property)
  }

  override fun Panel.buildTopConfigurationUi() {
    row {
      comboBox(Language.entries)
        .label(I18nUtils.message("CodeFormattingConverter.First"))
        .bindItem(firstLanguage)
    }
  }

  override fun Panel.buildMiddleSecondConfigurationUi() {
    row {
      comboBox(Language.entries)
        .label(I18nUtils.message("CodeFormattingConverter.Second"))
        .bindItem(secondLanguage)
    }
  }

  override fun toTarget(text: String) {
    covert(textConverterContext.sourceErrorHolder!!) {
      if (text.isBlank()) {
        targetText.set("")
      }
      else {
        targetText.set(secondLanguage.get().asString(firstLanguage.get().parse(text)))
      }
    }
  }

  override fun toSource(text: String) {
    covert(textConverterContext.targetErrorHolder!!) {
      if (text.isBlank()) {
        sourceText.set("")
      }
      else {
        sourceText.set(firstLanguage.get().asString(secondLanguage.get().parse(text)))
      }
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun covert(inputErrorHolder: ErrorHolder, doConvert: () -> Unit) {
    // We have to clear both `ErrorHolder`s here. If he user makes an invalid
    // input in A, which shows an error, and then edits B, the contents of A
    // would be replaced, but the error message is still visible.
    textConverterContext.targetErrorHolder!!.clear()
    textConverterContext.sourceErrorHolder!!.clear()

    try {
      doConvert()
    } catch (e: Exception) {
      inputErrorHolder.add(e)
    }

    // The `validate` in this class is not used as a validation mechanism. We
    // make use of its text field error UI to display the `errorHolder`.
    validate()
  }

  private fun setLanguages() {
    codeStyles[firstLanguage.get().languageId]?.let { setSourceLanguage(it) }
    codeStyles[secondLanguage.get().languageId]?.let { setTargetLanguage(it) }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private enum class Language(val title: String, val languageId: String, val objectMapper: ObjectMapper) {

    JSON("JSON", "JSON", ObjectMapper()),
    YAML("YAML", "YAML", YAMLMapper()),
    XML("XML", "XML", XmlMapper()),
    TOML("TOML", "TOML", TomlMapper()),
    PROPERTIES("Properties", "Properties", JavaPropsMapper());

    override fun toString(): String = title

    fun parse(text: String): JsonNode = objectMapper.readTree(text)

    fun asString(root: JsonNode): String = objectMapper.writeValueAsString(root)
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<CodeFormattingConverter> {

    override fun getDeveloperUiToolPresentation() = DeveloperUiToolPresentation(
      menuTitle = "Text Format",
      contentTitle = "Text Format Converter"
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext
    ): ((DeveloperToolConfiguration) -> CodeFormattingConverter) = { configuration ->
      CodeFormattingConverter(configuration, parentDisposable, context, project)
    }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //
}