package dev.turingcomplete.intellijdevelopertoolsplugin._internal.common

import com.intellij.openapi.ui.naturalSorted
import java.util.*

/**
 * A wrapper class for a [Locale]. This is required because the search function
 * of the `comboBox` in the UI DSL does work on the human-readable the display
 * name. It will use the `toString()` (e.g., `de_DE`) representation of the [Locale]
 * instead.
 */
data class LocaleContainer(val locale: Locale) {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun toString(): String = locale.displayName

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    val ALL_AVAILABLE_LOCALES = Locale.getAvailableLocales()
      .filter { it.displayName.isNotBlank() }
      .map { LocaleContainer(it) }
      .naturalSorted()
  }
}