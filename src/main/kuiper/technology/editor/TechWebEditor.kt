package technology.editor

import LogInterface
import godot.annotation.*
import godot.api.Control
import godot.api.FileAccess
import godot.core.connect
import godot.core.signal0
import godot.core.signal1
import godot.extension.getNodeAs
import godot.global.GD
import kotlinx.serialization.json.Json
import technology.TechStatus
import technology.TechTier
import technology.Technology

@RegisterClass
class TechWebEditor : Control(), LogInterface {

    // Globals
    private val signalBus: EditorSignalBus by lazy { getNodeAs("/root/TechWebEditor/EditorSignalBus")!! }

    @RegisterProperty
    @Export
    override var logEnabled: Boolean = true

    private val technologies: MutableList<Technology> = mutableListOf()
    private val techWebJsonPath = "res://assets/data/technologies/techweb.json"

    // Signals
    @RegisterSignal("technology_added")
    val nodeAdded by signal1<TechWrapper>()

    @RegisterSignal("technology_saved")
    val nodeSaved by signal1<TechWrapper>()

    @RegisterSignal
    val techsCleared by signal0()

    @RegisterFunction
    override fun _ready() {

        signalBus.editor_deleteTech.connect { techW ->
            technologies.remove(techW.technology)
        }

        loadTechWeb()
    }

    @RegisterFunction
    fun loadTechWeb() {
        if (FileAccess.fileExists(techWebJsonPath)) {
            val file = FileAccess.open(techWebJsonPath, FileAccess.ModeFlags.READ)
            val jsonString = file?.getAsText()
            if (jsonString != null) {
                technologies.clear()
                technologies.addAll(Json.decodeFromString<List<Technology>>(jsonString))
                log("Loaded ${technologies.size} technologies")
                technologies.forEach { tech ->
                    nodeAdded.emit(TechWrapper().apply { technology = tech })
                }
            } else {
                logError("TechWebEditor: Could not read techweb.json at $techWebJsonPath")
            }
        } else {
            logError("TechWebEditor: Could not find techweb.json at $techWebJsonPath")
        }
    }

    @RegisterFunction
    fun saveTechWeb() {
        log("Saving updated technologies: ${technologies.size} to $techWebJsonPath")
        val json = Json {
            encodeDefaults = true
            prettyPrint = true
        }
        var newJson: String = ""
        try {
            newJson = json.encodeToString(technologies)
        } catch (e: Exception) {
            logError("TechWebEditor: Could not serialize technologies JSON: $e")
            return
        }
        if (FileAccess.fileExists(techWebJsonPath)) {
            val file = FileAccess.open(techWebJsonPath, FileAccess.ModeFlags.WRITE)
            file?.let {
                it.storeString(newJson)
                it.close()
            }
        } else {
            logError("TechWebEditor: Could not find techweb.json at $techWebJsonPath")
        }

    }

    @RegisterFunction
    fun _onAddButtonClicked() {
        GD.print("Add button clicked")
        val newTech =
            Technology(technologies.size, "New Tech", "A new technology", TechTier.TIER_1, TechStatus.UNLOCKED)
        technologies.add(newTech)
        nodeAdded.emit(TechWrapper().apply { technology = newTech })
    }
}
