package screens.kuiper.escMenu

import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterSignal
import godot.api.Control
import godot.core.Signal0
import godot.core.signal0

@RegisterClass
class EscMenu : Control() {

    // Pure UI elements such as buttons have inherent signals that can be connected to, such as pressed()
    // This will call functions like _on_resume()
    // Those functions can then emit signals that the parent node can connect to
    // Through the registered custom signals on this node
    // The parent node can then connect to these signals and call its own functions
    // Therefore, I will name all inherit signal responding functions with a "_" prefix
    // And all custom responding functions without a prefix

    @RegisterSignal
    val resumeSignal: Signal0 by signal0()

    @RegisterSignal
    val quitSignal: Signal0 by signal0()

    @RegisterSignal
    val returnToMainMenuSignal: Signal0 by signal0()

    @RegisterSignal
    val pleaseSaveSignal: Signal0 by signal0()

    /**
     * Resume the game by closing the escape menu
     */
    @RegisterFunction
    fun _on_resume() {
        resumeSignal.emit()
    }

    @RegisterFunction
    fun _on_quit() {
        // check if there is an unsaved game in progress, or some other such sanity check
        getTree()?.quit()
    }

    @RegisterFunction
    fun _on_return_to_main_menu() {
        getTree()?.changeSceneToFile("res://src/main/kuiper/screens/mainMenu/main_menu.tscn")
    }

    @RegisterFunction
    fun _on_save() {
        pleaseSaveSignal.emit()
    }

}
