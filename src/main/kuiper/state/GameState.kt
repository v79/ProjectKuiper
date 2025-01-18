package state

import godot.DirAccess
import godot.FileAccess
import godot.Node
import godot.ProjectSettings
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.global.GD
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json

/**
 * Complete game state which is persisted between scenes and will be serialized to disk to save the game
 */
@RegisterClass
@Serializable
class GameState : Node() {

    @RegisterProperty
    @Export
    var year: Int = 1980

    var country: Country? = null

    fun nextTurn() {
        GD.print("GameState: Next turn")
        year++
    }

    fun stateToString(): String {
        return "GameState(country=${country?.name}, year=$year)"
    }

    @Transient
    private val json = Json { prettyPrint = true }

    @RegisterFunction
    fun save() {
        GD.print("GameState: Saving game state")
        val jsonString = json.encodeToString(serializer(), this)
        if (!DirAccess.dirExistsAbsolute("user://saves")) {
            DirAccess.makeDirRecursiveAbsolute("user://saves")
        }
        val saveFile = FileAccess.open("user://saves/savegame-kuiper.json", FileAccess.ModeFlags.WRITE)
        // In windows, this will be saved to C:\Users\<username>\AppData\Roaming\Godot\app_userdata\<project_name>\saves\savegame-kuiper.json
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
