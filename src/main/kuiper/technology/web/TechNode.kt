package technology.web

import godot.GraphNode
import godot.Label
import godot.LineEdit
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.extensions.getNodeAs
import technology.Technology

@RegisterClass
class TechNode : GraphNode() {

	var technology: Technology = Technology.EMPTY

	// UI elements
	private lateinit var idLabel: Label
	private lateinit var titleLabel: LineEdit

	@RegisterFunction
	override fun _ready() {
		idLabel = getNodeAs("%idLabel")!!
		titleLabel = getNodeAs("%titleLabel")!!

		updateUI()
	}

	@RegisterFunction
	override fun _process(delta: Double) {

	}

	private fun updateUI() {
		idLabel.text = technology.id.toString()
		titleLabel.text = technology.title
	}
}
