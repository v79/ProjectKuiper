package actions.activeActions

import SignalBus
import godot.Control
import godot.Label
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.extensions.getNodeAs

@RegisterClass
class OngoingAction : Control() {

	// Globals
	private lateinit var signalBus: SignalBus

	// UI elements
	private lateinit var turnsRemainingLbl: Label

	@RegisterProperty
	var actionId: Int = 0

	@RegisterProperty
	var turnsRemaining: Int = 99


	@RegisterFunction
	override fun _ready() {
		signalBus = getNodeAs("/root/SignalBus")!!
		turnsRemainingLbl = getNodeAs("%TurnsRemaining")!!

		turnsRemainingLbl.setText(turnsRemaining.toString())
	}

	@RegisterFunction
	override fun _process(delta: Double) {

	}
}
