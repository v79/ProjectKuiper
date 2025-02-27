package technology.web

import godot.GraphNode
import godot.Label
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.extensions.getNodeAs
import technology.Technology

@RegisterClass
class TechNode : GraphNode() {

	var technology: Technology = Technology.EMPTY

	// UI elements
	private lateinit var idLabel: Label
	private lateinit var tierLabel: Label

	@RegisterFunction
	override fun _ready() {
		idLabel = getNodeAs("%idLabel")!!
		tierLabel = getNodeAs("%tierLabel")!!

		updateUI()
	}

	@RegisterFunction
	override fun _process(delta: Double) {

	}

	private fun updateUI() {
		setTitle(technology.title)
		idLabel.text = technology.id.toString()
		tierLabel.text = technology.tier.name
	}
}
