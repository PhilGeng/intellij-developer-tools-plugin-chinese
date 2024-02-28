package dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.instance.dialog

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.instance.handling.OpenDeveloperToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.ui.instance.handling.OpenDeveloperToolReference
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

@Service(Service.Level.APP)
internal class MainDialogService {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val dialogLock = ReentrantReadWriteLock()
  private val dialog = AtomicReference<MainDialog?>()

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  fun openDialog(project: Project?) {
    dialogLock.write {
      val currentDialog = dialog.get()
      if (currentDialog == null || !currentDialog.isShowing) {
        val mainDialog = MainDialog(project)
        this.dialog.set(mainDialog)
        mainDialog.show()
      }
      else {
        currentDialog.toFront()
      }
    }
  }

  fun closeDialog() {
    dialogLock.write {
      dialog.set(null)
    }
  }

  fun <T : OpenDeveloperToolContext> openTool(project: Project?, context: T, reference: OpenDeveloperToolReference<T>) {
    openDialog(project)
    dialogLock.read {
      dialog.get()?.contentPanelHandler?.openTool(context, reference)
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}