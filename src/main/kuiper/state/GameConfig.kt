package state

import godot.api.FileAccess
import godot.global.GD
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json

@Serializable
data class GameConfig(val lastSaveFile: String? = null) {
    @Transient
    val json = Json { prettyPrint = true }

    fun updateConfigFile(saveFileName: String, configFile: FileAccess) {
        val newConfig = this.copy(lastSaveFile = saveFileName)
        val newConfigJson = json.encodeToString(serializer(), newConfig)
        GD.print("GameState: New config: $newConfigJson")
        configFile.storeString(newConfigJson)
        configFile.close()
    }
}
