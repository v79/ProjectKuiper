package technology.web

import LogInterface
import godot.Control
import godot.FileAccess
import godot.annotation.*
import godot.core.signal1
import godot.global.GD
import kotlinx.serialization.json.Json
import technology.TechStatus
import technology.TechTier
import technology.Technology

@RegisterClass
class TechWebEditor : Control(), LogInterface {

	@RegisterProperty
	@Export
	override var logEnabled: Boolean = true

	private val technologies: MutableList<Technology> = mutableListOf()
	private val techWebJsonPath = "res://assets/data/technologies/techweb.json"

	// Signals
	@RegisterSignal
	val nodeAdded by signal1<TechWrapper>("technology_added")

	@RegisterFunction
	override fun _ready() {

	}

	@RegisterFunction
	fun loadTechWeb() {
		if (FileAccess.fileExists(techWebJsonPath)) {
			val file = FileAccess.open(techWebJsonPath, FileAccess.ModeFlags.READ)
			val jsonString = file?.getAsText()
			if (jsonString != null) {
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
	override fun _process(delta: Double) {

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
