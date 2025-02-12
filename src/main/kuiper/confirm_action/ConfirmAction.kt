package confirm_action

import SignalBus
import actions.ActionCard
import godot.AnimationPlayer
import godot.Control
import godot.Label
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.core.asStringName
import godot.core.connect
import godot.extensions.getNodeAs
import hexgrid.Hex

@RegisterClass
class ConfirmAction : Control() {

	// Globals
	private lateinit var signalBus: SignalBus

	lateinit var hex: Hex
	lateinit var card: ActionCard

	// UI elements
	private lateinit var titleLabel: Label
	private lateinit var animationPlayer: AnimationPlayer
	private lateinit var actionCardDetails: ActionCardDetails

	@RegisterFunction
	override fun _ready() {
		hide()
		signalBus = getNodeAs("/root/SignalBus")!!
		titleLabel = getNodeAs("%ConfirmActionTitle")!!
		animationPlayer = getNodeAs("AnimationPlayer")!!
		actionCardDetails = getNodeAs("%ActionCardDetails")!!

		signalBus.showActionConfirmation.connect { h, c ->
			hex = h
			card = c
			titleLabel.text = "Are you sure you want to play ${card.cardName}?"
		}
	}

	@RegisterFunction
	fun updateUI() {
		titleLabel.text = "Are you sure you want to play ${card.cardName}?"
	}

	@RegisterFunction
	override fun _process(delta: Double) {

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
