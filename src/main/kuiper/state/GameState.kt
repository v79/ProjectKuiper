package state

import LogInterface
import SignalBus
import actions.Action
import actions.ActionWrapper
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.api.DirAccess
import godot.api.FileAccess
import godot.api.Node
import godot.api.ProjectSettings
import godot.extension.getNodeAs
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import notifications.Notification
import notifications.NotificationWrapper
import java.util.*

/**
 * Complete game state which is persisted between scenes and will be serialized to disk to save the game
 */
@RegisterClass
@Serializable
class GameState : Node(), LogInterface {

    @RegisterProperty
    @Export
    override var logEnabled: Boolean = true

    // Globals
    @Transient
    private lateinit var signalBus: SignalBus

    @RegisterProperty
    @Export
    var year: Int = 1980

    // Data
    var company: Company = Company("Kuiper")
    var sponsor: Sponsor? = null
    var availableActions: MutableList<Action> = mutableListOf()

    // TODO: Could this be better as a Set? Or a Map of <Notification, Boolean> to track if they have been dismissed?
    // That might allow me to delete the notificationHistory on the Company
    val notifications: MutableList<Notification> = mutableListOf()

    @RegisterFunction
    override fun _ready() {
        signalBus = getNodeAs("/root/SignalBus")!!
    }

    /**
     * On turn end, we need to update the game state
     */
    fun nextTurn() {
        // Clear notifications; exiting persistent notifications will remain in the tree until dismissed
        notifications.clear()
        signalBus.nextTurn.emit()
        log("GameState: Next turn")
        // Do research, spending all research points
        notifications.addAll(company.doResearch())
        notifications.addAll(company.clearResearch())
        val completed = company.doActions()
        notifications.addAll(company.processBuildings())
        notifications.addAll(completed.map { Notification.ActionComplete(it, "Action completed: ${it.actionName}") })
        // signal completed actions to expire
        completed.forEach { action ->
            signalBus.actionCompleted.emitSignal(ActionWrapper(action))
        }
        notifications.forEach { notification ->
            signalBus.notify.emit(NotificationWrapper(notification))
        }
        // increment the year
        year++
    }

    fun stateToString(): String {
        return "GameState(sponsor=${sponsor?.name}, ${company.name}, ${company.sciences.size} sciences, year=$year)"
    }

    fun reset() {
        log("GameState: Resetting game state")
        year = 1980
        company = Company("Kuiper")
        sponsor = null
        availableActions.clear()
        notifications.clear()
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
    }

    @RegisterFunction
    fun save() {
        log("GameState: Saving game state")
        val jsonString = json.encodeToString(serializer(), this)
        logInfo(jsonString)
        if (!DirAccess.dirExistsAbsolute("user://saves")) {
            DirAccess.makeDirRecursiveAbsolute("user://saves")
        }
        // TODO: Need to be a lot more careful with the file name
        val fileName = this.company.name.replace(" ", "_").lowercase(Locale.getDefault())
        val saveFile = FileAccess.open("user://saves/savegame-$fileName-kuiper.json", FileAccess.ModeFlags.WRITE)
        // In windows, this will be saved to C:\Users\<username>\AppData\Roaming\Godot\app_userdata\<project_name>\saves\savegame-<filename>-kuiper.json
        log("Saving file ${ProjectSettings.globalizePath(saveFile?.getPath() ?: "null")}")
        saveFile?.let {
            it.storeString(jsonString)
            it.close()
            // Update project configuration
            val configFile = FileAccess.open("user://config.json", FileAccess.ModeFlags.READ_WRITE)
            if (configFile != null) {
                log("GameState: Updating last save file in config")
                val configJson = configFile.getAsText()
                val config = if (configFile.getLength() > 0) {
                    // TODO: make this more robust especially as the game state will change a lot during development
                    json.decodeFromString(GameConfig.serializer(), configJson)
                } else {
                    GameConfig()
                }
                config.updateConfigFile(saveFile.getPath(), configFile)
            } else {
                logWarning("GameState: Could not open config file; creating new")
                val newConfigFile = FileAccess.open("user://config.json", FileAccess.ModeFlags.WRITE)
                if (newConfigFile != null) {
                    GameConfig(lastSaveFile = saveFile.getPath()).updateConfigFile(
                        saveFile.getPath(), newConfigFile
                    )
                } else {
                    logError("GameState: Could not create config file")
                }
            }
        }
    }
}
