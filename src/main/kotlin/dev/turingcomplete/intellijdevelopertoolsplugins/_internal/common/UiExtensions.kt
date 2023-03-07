package dev.turingcomplete.intellijdevelopertoolsplugins._internal.common

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.UIBundle
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.Font
import java.awt.event.ItemEvent
import javax.swing.BorderFactory
import javax.swing.JComponent

// -- Properties ---------------------------------------------------------------------------------------------------- //
// -- Exposed Methods ----------------------------------------------------------------------------------------------- //

/**
 * The UI DSL only verifies an `intTextField` on input.
 */
fun Cell<JBTextField>.validateIntValue(range: IntRange? = null) = this.apply {
  validation {
    if (this@validateIntValue.component.isEnabled) {
      val value = this@validateIntValue.component.text.toIntOrNull()
      when {
        value == null -> error(UIBundle.message("please.enter.a.number"))
        range != null && value !in range -> error(UIBundle.message("please.enter.a.number.from.0.to.1", range.first, range.last))
        else -> null
      }
    }
    else {
      null
    }
  }
}

fun validateMinMaxValueRelation(side: ValidateMinIntValueSide, getOppositeValue: () -> Int):
        ValidationInfoBuilder.(JBTextField) -> ValidationInfo? = {
  if (this.component.isEnabled) {
    it.text?.toIntOrNull()?.let { thisValue ->
      when {
        side == ValidateMinIntValueSide.MIN && thisValue > getOppositeValue() -> {
          ValidationInfo("Minimum must be smaller than or equal to maximum")
        }

        side == ValidateMinIntValueSide.MAX && thisValue < getOppositeValue() ->
          ValidationInfo("Maximum must be larger than or equal to minimum")

        else -> null
      }
    }
  }
  else {
    null
  }
}

fun JBRadioButton.onSelected(selectListener: () -> Unit) = this.apply {
  this.addItemListener { event ->
    if (event.stateChange == ItemEvent.SELECTED) {
      selectListener()
    }
  }
}

fun <T> ComboBox<T>.onChanged(changeListener: (T) -> Unit) {
  this.addItemListener { event ->
    if (event.stateChange == ItemEvent.SELECTED) {
      @Suppress("UNCHECKED_CAST")
      changeListener(selectedItem as T)
    }
  }
}

fun JBLabel.copyable() = this.apply { setCopyable(true) }

fun JComponent.wrapWithToolBar(actionEventPlace: String, actions: ActionGroup, toolBarPlace: ToolBarPlace, withBorder: Boolean = true): JComponent {
  return BorderLayoutPanel().apply {
    val actionToolbar = ActionManager.getInstance().createActionToolbar(actionEventPlace, actions, toolBarPlace.horizontal)
    actionToolbar.targetComponent = this@wrapWithToolBar

    val component = this@wrapWithToolBar.apply {
      if (withBorder) {
        border = BorderFactory.createLineBorder(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground())
      }
    }
    when (toolBarPlace) {
      ToolBarPlace.LEFT -> {
        addToLeft(actionToolbar.component)
        addToCenter(component)
      }

      ToolBarPlace.RIGHT, ToolBarPlace.APPEND -> {
        addToCenter(component)
        addToRight(actionToolbar.component)
      }
    }
  }
}

fun JBFont.toMonospace(): JBFont = JBFont.create(Font(Font.MONOSPACED, this.style, this.size))

// -- Private Methods ----------------------------------------------------------------------------------------------- //
// -- Type ---------------------------------------------------------------------------------------------------------- //

enum class ValidateMinIntValueSide { MIN, MAX }

// -- Type ---------------------------------------------------------------------------------------------------------- //

enum class ToolBarPlace(val horizontal: Boolean) {

  LEFT(false),
  RIGHT(false),
  APPEND(true)
}