package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.TextConverter.TextConverterContext
import dev.turingcomplete.intellijdevelopertoolsplugin.i18n.I18nUtils
import org.apache.commons.text.StringEscapeUtils

// -- Properties ---------------------------------------------------------------------------------------------------- //
// -- Exposed Methods ----------------------------------------------------------------------------------------------- //

internal fun createEscapeUnescapeContext(title: String) = TextConverterContext(
  convertActionTitle = I18nUtils.message("EscapeUnescapeContext.convertActionTitle"),
  revertActionTitle = I18nUtils.message("EscapeUnescapeContext.revertActionTitle"),
  sourceTitle = I18nUtils.message("EscapeUnescapeContext.sourceTitle"),
  targetTitle = I18nUtils.message("EscapeUnescapeContext.targetTitle"),
  diffSupport = TextConverter.DiffSupport(
    title = title
  )
)

// -- Private Methods ----------------------------------------------------------------------------------------------- //
// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class HtmlEntitiesEscape(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?
) :
  TextConverter(
    textConverterContext = createEscapeUnescapeContext(I18nUtils.message("HtmlEntitiesEscape.contentTitle")),
    configuration = configuration,
    parentDisposable = parentDisposable,
    context = context,
    project = project
  ) {

  override fun toTarget(text: String) {
    targetText.set(StringEscapeUtils.escapeHtml4(text))
  }

  override fun toSource(text: String) {
    sourceText.set(StringEscapeUtils.unescapeHtml4(text))
  }

  class Factory : DeveloperUiToolFactory<HtmlEntitiesEscape> {

    override fun getDeveloperUiToolPresentation() = DeveloperUiToolPresentation(
      menuTitle = I18nUtils.message("HtmlEntitiesEscape.menuTitle"),
      groupedMenuTitle = "HTML Entities",
      contentTitle = I18nUtils.message("HtmlEntitiesEscape.contentTitle"),
      description = DeveloperUiToolPresentation.contextHelp(I18nUtils.message("HtmlEntitiesEscape.description"))
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext
    ): ((DeveloperToolConfiguration) -> HtmlEntitiesEscape) =
      { configuration -> HtmlEntitiesEscape(configuration, parentDisposable, context, project) }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class JavaStringEscape(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?
) :
  TextConverter(
    textConverterContext = createEscapeUnescapeContext(I18nUtils.message("JavaStringEscape.contentTitle")),
    configuration = configuration,
    parentDisposable = parentDisposable,
    context = context,
    project = project
  ) {

  override fun toTarget(text: String) {
    targetText.set(StringEscapeUtils.escapeJava(text))
  }

  override fun toSource(text: String) {
    sourceText.set(StringEscapeUtils.unescapeJava(text))
  }

  class Factory : DeveloperUiToolFactory<JavaStringEscape> {

    override fun getDeveloperUiToolPresentation() = DeveloperUiToolPresentation(
      menuTitle = I18nUtils.message("JavaStringEscape.menuTitle"),
      groupedMenuTitle = "Java String",
      contentTitle = I18nUtils.message("JavaStringEscape.contentTitle"),
      description = DeveloperUiToolPresentation.contextHelp(I18nUtils.message("JavaStringEscape.description"))
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext
    ): ((DeveloperToolConfiguration) -> JavaStringEscape) =
      { configuration -> JavaStringEscape(configuration, parentDisposable, context, project) }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class JsonTextEscape(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?
) :
  TextConverter(
    textConverterContext = createEscapeUnescapeContext(I18nUtils.message("JsonTextEscape.contentTitle")),
    configuration = configuration,
    parentDisposable = parentDisposable,
    context = context,
    project = project
  ) {

  override fun toTarget(text: String) {
    targetText.set(StringEscapeUtils.escapeJson(text))
  }

  override fun toSource(text: String) {
    sourceText.set(StringEscapeUtils.unescapeJson(text))
  }

  class Factory : DeveloperUiToolFactory<JsonTextEscape> {

    override fun getDeveloperUiToolPresentation() = DeveloperUiToolPresentation(
      menuTitle = I18nUtils.message("JsonTextEscape.menuTitle"),
      groupedMenuTitle = "JSON Text",
      contentTitle = I18nUtils.message("JsonTextEscape.contentTitle"),
      description = DeveloperUiToolPresentation.contextHelp(I18nUtils.message("JsonTextEscape.description"))
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext
    ): ((DeveloperToolConfiguration) -> JsonTextEscape) =
      { configuration -> JsonTextEscape(configuration, parentDisposable, context, project) }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class CsvTextEscape(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?
) :
  TextConverter(
    textConverterContext = createEscapeUnescapeContext(I18nUtils.message("CsvTextEscape.contentTitle")),
    configuration = configuration,
    parentDisposable = parentDisposable,
    context = context,
    project = project
  ) {

  override fun toTarget(text: String) {
    targetText.set(StringEscapeUtils.escapeCsv(text))
  }

  override fun toSource(text: String) {
    sourceText.set(StringEscapeUtils.unescapeCsv(text))
  }

  class Factory : DeveloperUiToolFactory<CsvTextEscape> {

    override fun getDeveloperUiToolPresentation() = DeveloperUiToolPresentation(
      menuTitle = I18nUtils.message("CsvTextEscape.menuTitle"),
      groupedMenuTitle = "CSV Text",
      contentTitle = I18nUtils.message("CsvTextEscape.contentTitle"),
      description = DeveloperUiToolPresentation.contextHelp(I18nUtils.message("CsvTextEscape.description"))
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext
    ): ((DeveloperToolConfiguration) -> CsvTextEscape) =
      { configuration -> CsvTextEscape(configuration, parentDisposable, context, project) }
  }
}

// -- Type ---------------------------------------------------------------------------------------------------------- //

internal class XmlTextEscape(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  context: DeveloperUiToolContext,
  project: Project?
) :
  TextConverter(
    textConverterContext = createEscapeUnescapeContext(I18nUtils.message("XmlTextEscape.contentTitle")),
    configuration = configuration,
    parentDisposable = parentDisposable,
    context = context,
    project = project
  ) {

  override fun toTarget(text: String) {
    targetText.set(StringEscapeUtils.escapeXml11(text))
  }

  override fun toSource(text: String) {
    sourceText.set(StringEscapeUtils.unescapeXml(text))
  }

  class Factory : DeveloperUiToolFactory<XmlTextEscape> {

    override fun getDeveloperUiToolPresentation() = DeveloperUiToolPresentation(
      menuTitle = I18nUtils.message("XmlTextEscape.menuTitle"),
      contentTitle = I18nUtils.message("XmlTextEscape.contentTitle"),
      description = DeveloperUiToolPresentation.contextHelp(I18nUtils.message("XmlTextEscape.description"))
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext
    ): ((DeveloperToolConfiguration) -> XmlTextEscape) =
      { configuration -> XmlTextEscape(configuration, parentDisposable, context, project) }
  }
}