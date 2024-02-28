package dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.instance.handling

import kotlin.reflect.KClass

internal interface OpenDeveloperToolReference<T : OpenDeveloperToolContext> {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  val id: String
  val contextClass: KClass<T>

  // -- Exported Methods -------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    fun <T : OpenDeveloperToolContext> of(id: String, contextClass: KClass<T>) =
      object : OpenDeveloperToolReference<T> {
        override val id: String
          get() = id
        override val contextClass: KClass<T>
          get() = contextClass
      }
  }
}