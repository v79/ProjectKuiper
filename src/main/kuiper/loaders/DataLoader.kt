package loaders

import actions.Action
import godot.FileAccess
import godot.Node
import godot.annotation.RegisterClass
import godot.global.GD
import kotlinx.serialization.json.Json

/**
 * Autoload 'singleton' class to load data files
 */
@RegisterClass
class DataLoader : Node() {

	// file paths
	private val actionsJsonPath = "res://assets/data/actions/actions.json"

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

}
