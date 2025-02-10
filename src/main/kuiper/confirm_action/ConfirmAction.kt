package confirm_action

import SignalBus
import actions.ActionCard
import godot.AnimationPlayer
import godot.Control
import godot.Label
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.core.asStringName
import godot.extensions.getNodeAs
import hexgrid.Hex

@RegisterClass
class ConfirmAction : Control() {

	// Globals
	private lateinit var signalBus: SignalBus

	var hex: Hex? = null
	var card: ActionCard? = null

	// UI elements
	private lateinit var titleLabel: Label
	private lateinit var animationPlayer: AnimationPlayer

	@RegisterFunction
	override fun _ready() {
		hide()
		signalBus = getNodeAs("/root/SignalBus")!!
		titleLabel = getNodeAs("%ConfirmActionTitle")!!
		animationPlayer = getNodeAs("AnimationPlayer")!!
	}


	@RegisterFunction
	override fun _process(delta: Double) {
		card?.let {
			titleLabel.text = "Are you sure you want to play ${it.cardName}?"
		}

	}

	@RegisterFunction
	fun fadeIn() {
		animationPlayer.play("show_panel".asStringName())
	}

	@RegisterFunction
	fun cancelAction() {
		hide()
		signalBus.cancelActionConfirmation.emit()
	}
}
