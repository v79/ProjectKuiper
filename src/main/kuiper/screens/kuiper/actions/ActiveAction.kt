package screens.kuiper.actions

import godot.Control
import godot.Label
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.core.signal0
import godot.extensions.getNodeAs

@RegisterClass
class ActiveAction : Control() {

	// UI elements
	private lateinit var actionName: Label
	private lateinit var actionDescription: Label
	private lateinit var turnsRemaining: Label

	@RegisterProperty
	@Export
	var actName: String = ""

	@RegisterProperty
	@Export
	var actDescription: String = ""

	@RegisterProperty
	@Export
	var turnsLeft: String = ""

	val actionComplete by signal0()

	@RegisterFunction
	override fun _ready() {
		actionName = getNodeAs("PanelContainer/MarginContainer/VBoxContainer/HBoxContainer/ActionName")!!
		actionDescription = getNodeAs("PanelContainer/MarginContainer/VBoxContainer/ActionDescription")!!
		turnsRemaining = getNodeAs("PanelContainer/MarginContainer/VBoxContainer/HBoxContainer/TurnsRemaining")!!
	}

	@RegisterFunction
	override fun _process(delta: Double) {
		actionName.text = actName
		actionDescription.text = actDescription
		turnsRemaining.text = turnsLeft
	}
}
