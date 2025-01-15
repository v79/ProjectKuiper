package screens.Kuiper.EscMenu

import godot.Control
import godot.InputEvent
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterSignal
import godot.core.Signal0
import godot.core.signal0
import screens.Kuiper.KuiperGame

@RegisterClass
class EscMenu : Control() {

    @RegisterSignal
    val resumeSignal: Signal0 by signal0()

    @RegisterSignal
    val quitSignal: Signal0 by signal0()

    // Called when the node enters the scene tree for the first time.
    @RegisterFunction
    override fun _ready() {

    }

    // Called every frame. 'delta' is the elapsed time since the previous frame.
    @RegisterFunction
    override fun _process(delta: Double) {

    }

    @RegisterFunction
    override fun _input(event: InputEvent?) {

    }

    /**
     * Resume the game by closing the escape menu
     */
    @RegisterFunction
    fun _on_resume() {
        // this looks naughty!
        (getParent() as KuiperGame).escMenuVisible = false
    }

    @RegisterFunction
    fun _on_quit() {
        // check if there is an unsaved game in progress, or some other such sanity check
        getTree()?.quit()
    }
}
