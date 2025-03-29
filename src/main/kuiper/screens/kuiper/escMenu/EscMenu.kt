package screens.kuiper.escMenu

import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterSignal
import godot.api.Control
import godot.core.Signal0
import godot.core.signal0
import godot.extension.getNodeAs
import state.GameState

@RegisterClass
class EscMenu : Control() {

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
    fun onReturnToMainMenu() {
        // check if there is an unsaved game in progress, or some other such sanity check
        // is there anything I should be clearing before going to the main menu?
        getNodeAs<GameState>("/root/GameState")?.reset()
        getTree()?.changeSceneToFile("res://src/main/kuiper/screens/mainMenu/main_menu.tscn")
    }

    @RegisterFunction
    fun _on_save() {
        pleaseSaveSignal.emit()
    }

}
