package dev.turingcomplete.intellijdevelopertoolsplugins._internal.dialog

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBFont
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.DeveloperToolsPluginService

class ConfigurationContentPanel {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  val panel: DialogPanel = panel {
    row {
      label("Configuration")
        .applyToComponent { font = JBFont.label().asBold() }
        .align(Align.FILL)
        .resizableColumn()

      link("Reset") {
        DeveloperToolsPluginService.saveConfiguration = DeveloperToolsPluginService.SAVE_CONFIGURATION_DEFAULT
        DeveloperToolsPluginService.saveInputs = DeveloperToolsPluginService.SAVE_INPUTS_DEFAULT
        DeveloperToolsPluginService.saveSecrets = DeveloperToolsPluginService.SAVE_SECRETS_DEFAULT
        DeveloperToolsPluginService.loadExamples = DeveloperToolsPluginService.LOAD_EXAMPLES_DEFAULT
      }
    }

    indent {
      row {
        checkBox("Remember configuration")
          .bindSelected(DeveloperToolsPluginService.instance.saveConfiguration)
      }
      row {
        checkBox("Remember inputs")
          .bindSelected(DeveloperToolsPluginService.instance.saveInputs)
      }
      row {
        checkBox("Remember secrets")
          .comment("Secrets are stored in the <a>system keychain</a>.") {
            BrowserUtil.browse("https://plugins.jetbrains.com/docs/intellij/persisting-sensitive-data.html#storage")
          }
          .bindSelected(DeveloperToolsPluginService.instance.saveSecrets)
      }
      row {
        checkBox("Load examples")
          .bindSelected(DeveloperToolsPluginService.instance.loadExamples)
          .comment("Changes to the load examples behaviour only take effect the next time the dialog is opened.")
      }
    }
  }.apply { border = JBEmptyBorder(0, 8, 0, 8) }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}