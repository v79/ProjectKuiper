package screens.mainMenu

import LogInterface
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterSignal
import godot.api.Button
import godot.api.FileAccess
import godot.api.FileDialog
import godot.api.Node
import godot.core.Signal0
import godot.core.signal0
import godot.extension.getNodeAs
import kotlinx.serialization.json.Json
import state.GameConfig
import state.GameState

@RegisterClass
class MainMenu : Node(), LogInterface {

    override var logEnabled: Boolean = true

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
//        signalBus = getNodeAs("%SignalBus")!!

        loadButton = getNodeAs("VBoxContainer/VBoxContainer/Load")!!
        continueButton = getNodeAs("VBoxContainer/VBoxContainer/Continue")!!
        loadFileDialog = getNodeAs("LoadFileDialog")!!
        loadFileDialog.let {
            it.setCurrentPath("user://saves/")
            it.clearFilters()
            it.addFilter("*.json", "Saved JSON games")
        }
        gameConfig = loadConfiguration()
        if (gameConfig.lastSaveFile != null) {
            log("Last save file: ${gameConfig.lastSaveFile}")
            continueButton.let {
                it.disabled = false
                it.tooltipText = "Continue game from ${gameConfig.lastSaveFile}"
            }
            loadButton.disabled = false
        } else {
            logWarning("No save file found")
        }
    }

    // Called every frame. 'delta' is the elapsed time since the previous frame.
    @RegisterFunction
    override fun _process(delta: Double) {
    }

    @RegisterFunction
    fun _on_new_game() {
        getTree()?.changeSceneToFile("res://src/main/kuiper/screens/gameSetup/game_setup.tscn")
    }

    /**
     * Quit the game
     * **signal**: quitGameSignal
     */
    @RegisterFunction
    fun _on_quit_game() {
        log("Quitting game")
        getTree()?.quit()
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
    fun onTechTreeEditorPressed() {
        getTree()?.changeSceneToFile("res://src/main/kuiper/technology/editor/tech_web_editor.tscn")
    }

    @RegisterFunction
    fun onSponsorEditorPressed() {
        getTree()?.changeSceneToFile("res://src/main/kuiper/hexgrid/map/editor/hex_map_editor.tscn")
    }

    @RegisterFunction
    fun loadGame(gameSave: String) {
        if (FileAccess.fileExists(gameSave)) {
            // Load game
            val json = Json {
                prettyPrint = true
                encodeDefaults = true
                allowStructuredMapKeys = true
            }
            val jsonString = FileAccess.open(gameSave, FileAccess.ModeFlags.READ)!!.getAsText()
            val gameData = json.decodeFromString(GameState.serializer(), jsonString)
            log("Loaded $gameSave : ${gameData.stateToString()}")
            val gameState = getTree()?.root?.getChild(0) as GameState
            gameState.deepCopy(gameData)
            getTree()?.changeSceneToFile("res://src/main/kuiper/screens/kuiper/game.tscn")
        } else {
            logError("Save file not found: $gameSave")
        }
        loadButton.disabled = true
        continueButton.disabled = true
        continueButton.tooltipText = ""
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
}
