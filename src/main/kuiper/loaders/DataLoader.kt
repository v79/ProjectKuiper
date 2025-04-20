package loaders

import LogInterface
import actions.Action
import godot.annotation.RegisterClass
import godot.api.DirAccess
import godot.api.FileAccess
import godot.api.Node
import godot.global.GD
import kotlinx.serialization.json.Json
import state.Sponsor
import technology.Technology

/**
 * Autoload 'singleton' class to load data files
 */
@RegisterClass
class DataLoader : Node(), LogInterface {

    override var logEnabled: Boolean = true

    // file paths
    private val actionsJsonPath = "res://assets/data/actions/actions.json"
    private val sponsorDataFolder = "res://assets/data/sponsors"
    private val techTreeJsonPath = "res://assets/data/technologies/techweb.json"

    /**
     * Load actions data from the actions.json file
     */
    fun loadActionsData(): List<Action> {
        GD.print("Loading actions data...")
        val actionsFile = FileAccess.open(actionsJsonPath, FileAccess.ModeFlags.READ)
        if (actionsFile == null) {
            GD.printErr("Failed to load actions data from $actionsJsonPath")
            return emptyList()
        }
        val actionsJson = Json.decodeFromString<List<Action>>(actionsFile.getAsText())
        GD.print("Loaded ${actionsJson.size} actions")
        return actionsJson
    }

    /**
     * Load each of the sponsor data files in the sponsorDataFolder
     */
    fun loadSponsorData(): List<Sponsor> {
        val sponsorList = mutableListOf<Sponsor>()

        if (!DirAccess.dirExistsAbsolute(sponsorDataFolder)) {
            logError("Sponsors data directory does not exist")
            return emptyList()
        }
        val dir = DirAccess.open(sponsorDataFolder)
        var count = 0
        val json = Json {
            allowStructuredMapKeys = true
        }
        dir?.let { dir ->
            dir.listDirBegin()
            var fileName = dir.getNext()
            while (fileName != "") {
                if (fileName.endsWith(".sponsor.json")) {
                    log("Found sponsor file $fileName")
                    val sponsorFile =
                        FileAccess.open("${sponsorDataFolder}/${fileName}", FileAccess.ModeFlags.READ)
                    sponsorFile?.let { file ->
                        val jsonString = file.getAsText()
                        try {
                            val sponsor = json.decodeFromString(Sponsor.serializer(), jsonString)
                            sponsorList.add(sponsor)
                        } catch (e: Exception) {
                            logError("Failed to parse sponsor file $fileName: ${e.message}")
                        }
                        file.close()
                    } ?: run {
                        logError("Failed to open sponsor file $fileName")
                    }
                    count++
                }
                fileName = dir.getNext()
            }
        }
        return sponsorList.toList()
    }

    /**
     * Load tech tree data from the techweb.json file
     */
    fun loadTechWeb(): List<Technology> {
        val techTreeFile = FileAccess.open(techTreeJsonPath, FileAccess.ModeFlags.READ)
        if (techTreeFile == null) {
            GD.printErr("Failed to load tech tree data from $techTreeJsonPath")
            return emptyList()
        }
        val techTreeJson = Json.decodeFromString<List<Technology>>(techTreeFile.getAsText())
        GD.print("Loaded ${techTreeJson.size} technologies")
        return techTreeJson
    }

}
