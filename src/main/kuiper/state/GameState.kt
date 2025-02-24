package state

import SignalBus
import actions.Action
import actions.ActionWrapper
import godot.DirAccess
import godot.FileAccess
import godot.Node
import godot.ProjectSettings
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.extensions.getNodeAs
import godot.global.GD
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import java.util.*

/**
 * Complete game state which is persisted between scenes and will be serialized to disk to save the game
 */
@RegisterClass
@Serializable
class GameState : Node() {

    // Globals
    @Transient
    private lateinit var signalBus: SignalBus

    @RegisterProperty
    @Export
    var year: Int = 1980

    var company: Company = Company("Kuiper")
    var sponsor: Sponsor? = null
    var zones: List<Zone> = listOf()

    // actions
    var availableActions: MutableList<Action> = mutableListOf()

    @RegisterFunction
    override fun _ready() {
        signalBus = getNodeAs("/root/SignalBus")!!
    }

    /**
     * On turn end, we need to update the game state
     */
    fun nextTurn() {
        GD.print("GameState: Next turn")
        val completed = company.nextTurn()
        // signal completed actions to expire
        completed.forEach { action ->
            signalBus.actionCompleted.emitSignal(ActionWrapper(action))
        }
        // increment the year
        year++
    }

    fun stateToString(): String {
        return "GameState(sponsor=${sponsor?.name}, ${company.name}, ${company.sciences.size} sciences, year=$year)"
    }

    @Transient
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        allowStructuredMapKeys = true
    }

    /**
     * I can't use a data class for GameState because it extends Node, so I have to manually implement a deep copy method
     * Add each new property to this method to ensure it is loaded correctly
     */
    fun deepCopy(sourceState: GameState) {
        this.year = sourceState.year
        this.sponsor = sourceState.sponsor
        this.company = sourceState.company
        this.zones = sourceState.zones
    }

    @RegisterFunction
    fun save() {
        GD.print("GameState: Saving game state")
        val jsonString = json.encodeToString(serializer(), this)
        GD.print(jsonString)
        if (!DirAccess.dirExistsAbsolute("user://saves")) {
            DirAccess.makeDirRecursiveAbsolute("user://saves")
        }
        // TODO: Need to be a lot more careful with the file name
        val fileName = this.company.name.replace(" ", "_").lowercase(Locale.getDefault())
        val saveFile = FileAccess.open("user://saves/savegame-$fileName-kuiper.json", FileAccess.ModeFlags.WRITE)
        // In windows, this will be saved to C:\Users\<username>\AppData\Roaming\Godot\app_userdata\<project_name>\saves\savegame-<filename>-kuiper.json
        GD.print("Saving file ${ProjectSettings.globalizePath(saveFile?.getPath() ?: "null")}")
        saveFile?.let {
            it.storeString(jsonString)
            it.close()
            // Update project configuration
            val configFile = FileAccess.open("user://config.json", FileAccess.ModeFlags.READ_WRITE)
            if (configFile != null) {
                GD.print("GameState: Updating last save file in config")
                val configJson = configFile.getAsText()
                val config = if (configFile.getLength() > 0) {
                    // TODO: make this more robust especially as the game state will change a lot during development
                    json.decodeFromString(GameConfig.serializer(), configJson)
                } else {
                    GameConfig()
                }
                config.updateConfigFile(saveFile.getPath(), configFile)
            } else {
                GD.print("GameState: Could not open config file; creating new")
                val newConfigFile = FileAccess.open("user://config.json", FileAccess.ModeFlags.WRITE)
                if (newConfigFile != null) {
                    GameConfig(lastSaveFile = saveFile.getPath()).updateConfigFile(
                        saveFile.getPath(),
                        newConfigFile
                    )
                } else {
                    GD.printErr("GameState: Could not create config file")
                }
            }
        }
    }
}
