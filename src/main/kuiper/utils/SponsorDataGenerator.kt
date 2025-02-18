package utils

import godot.Button
import godot.Control
import godot.FileAccess
import godot.Label
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.core.Color
import godot.extensions.getNodeAs
import godot.global.GD
import kotlinx.serialization.json.Json
import state.Sponsor

@RegisterClass
class SponsorDataGenerator : Control() {

	private val sponsorList = listOf(
		Sponsor(1, "Europe", Color.blue),
		Sponsor(2, "North America", Color.red),
		Sponsor(3, "South America", Color.yellow),
		Sponsor(4, "Asia", Color.tan),
		Sponsor(5, "Africa", Color.green),
		Sponsor(6, "Oceania", Color.cyan),
		Sponsor(7, "Antarctica", Color.white)
	)

	@RegisterFunction
	override fun _ready() {
		val json = Json { prettyPrint = true }
		val sponsorListJson = json.encodeToString(sponsorList)
		val saveFile = FileAccess.open("res://assets/data/sponsors.json", FileAccess.ModeFlags.WRITE)
		if (saveFile == null) {
			GD.printErr("Failed to open sponsors.json file for writing")
			return
		}
		saveFile.storeString(sponsorListJson)
		saveFile.close()

		val detailsLabel = getNodeAs<Label>("%Details")!!
		detailsLabel.text = sponsorListJson
		val quitButton = getNodeAs<Button>("%QuitButton")!!
		quitButton.disabled = false
	}

	@RegisterFunction
	fun _on_QuitButton_pressed() {
		GD.print("Quit button pressed")
		getTree()?.quit()
	}

}
