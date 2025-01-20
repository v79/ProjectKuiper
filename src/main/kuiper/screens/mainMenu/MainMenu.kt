package screens.mainMenu

import godot.Button
import godot.FileAccess
import godot.FileDialog
import godot.Node
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterSignal
import godot.core.Signal0
import godot.core.signal0
import godot.extensions.getNodeAs
import godot.global.GD
import kotlinx.serialization.json.Json
import state.GameConfig
import state.GameState

@RegisterClass
class MainMenu : Node() {

	@RegisterSignal
	val quitGameSignal: Signal0 by signal0()

	@RegisterSignal
	val newGameSignal: Signal0 by signal0()

	private lateinit var gameConfig: GameConfig
	private lateinit var loadButton: Button
	private lateinit var continueButton: Button
	private lateinit var loadFileDialog: FileDialog

	// Called when the node enters the scene tree for the first time.
	@RegisterFunction
	override fun _ready() {
		loadButton = getNodeAs("VBoxContainer/VBoxContainer/Load")!!
		continueButton = getNodeAs("VBoxContainer/VBoxContainer/Continue")!!
		loadFileDialog = getNodeAs("LoadFileDialog")!!
		GD.print("Main Menu ready - got file dialog: $loadFileDialog")
		loadFileDialog.let {
			it.setCurrentPath("user://saves/")
			it.clearFilters()
			it.addFilter("*.json","Saved JSON games")
		}
		GD.print(loadFileDialog.currentPath)
		gameConfig = loadConfiguration()
		if (gameConfig.lastSaveFile != null) {
			GD.print("Last save file: ${gameConfig.lastSaveFile}")
			continueButton.let {
				it.disabled = false
				it.tooltipText = "Continue game from ${gameConfig.lastSaveFile}"
			}
			loadButton.disabled = false
		} else {
			GD.print("No save file found")
		}
	}

	// Called every frame. 'delta' is the elapsed time since the previous frame.
	@RegisterFunction
	override fun _process(delta: Double) {
	}

	@RegisterFunction
	fun _on_new_game() {
		GD.print("Setting up new game")
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

	private fun loadConfiguration(): GameConfig {
		// Load configuration file, if it exists
		val configFile = FileAccess.open("user://config.json", FileAccess.ModeFlags.READ)
		val gameConfig = if (configFile != null) {
			val configString = configFile.getAsText()
			Json.decodeFromString(GameConfig.serializer(), configString)
		} else {
			GameConfig()
		}
		return gameConfig
	}


	@RegisterFunction
	fun _on_continue_game() {
		loadGame(gameConfig.lastSaveFile!!)
	}

	@RegisterFunction
	fun _on_load_button_pressed() {
		loadFileDialog.visible = true
	}

	@RegisterFunction
	fun _on_load_file_selected(file: String) {
		loadGame(file)
	}

	@RegisterFunction
	fun loadGame(gameSave: String) {
		if (FileAccess.fileExists(gameSave)) {
			// Load game
			val jsonString = FileAccess.open(gameSave, FileAccess.ModeFlags.READ)!!.getAsText()
			val gameData = Json.decodeFromString(GameState.serializer(), jsonString)
			GD.print("Loaded $gameSave : ${gameData.stateToString()}")
			val gameState = getTree()?.root?.getChild(0) as GameState
			gameState.deepCopy(gameData)
			getTree()?.changeSceneToFile("res://src/main/kuiper/screens/kuiper/game.tscn")
		} else {
			GD.printErr("Save file not found: $gameSave")
		}
		loadButton.disabled = true
		continueButton.disabled = true
		continueButton.tooltipText = ""
	}

}
