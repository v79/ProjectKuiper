package screens.mainMenu

import godot.Node
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterSignal
import godot.core.Signal0
import godot.core.signal0
import godot.global.GD

@RegisterClass
class MainMenu : Node() {

	@RegisterSignal
	val quitGameSignal: Signal0 by signal0()

	@RegisterSignal
	val newGameSignal: Signal0 by signal0()

	// Called when the node enters the scene tree for the first time.
	@RegisterFunction
	override fun _ready() {
	}

	// Called every frame. 'delta' is the elapsed time since the previous frame.
	@RegisterFunction
	override fun _process(delta: Double) {
	}

	@RegisterFunction
	fun _on_new_game() {
		GD.print("Setting uo new game")
		getTree()?.changeSceneToFile("res://src/main/kuiper/screens/gameSetup/game_setup.tscn")
	}
	/**
	 * Quit the game
	 * **signal**: quitGameSignal
	 */
	@RegisterFunction
	fun _on_quit_game() {
		GD.print("Quitting game")
		getTree()?.quit()
	}


}
