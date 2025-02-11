package confirm_action

import SignalBus
import actions.ActionCard
import godot.Control
import godot.Label
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.core.connect
import godot.extensions.getNodeAs

@RegisterClass
class ActionCardDetails : Control() {

	// Globals
	private lateinit var signalBus: SignalBus

	@RegisterProperty
	@Export
	var cardTitle: String = ""

	// UI elements
	private lateinit var titleLabel: Label

	@RegisterFunction
	override fun _ready() {
		signalBus = getNodeAs("/root/SignalBus")!!
		titleLabel = getNodeAs("%CardTitle")!!

		signalBus.showActionConfirmation.connect { h, c ->
//			cardTitle = c.cardName
			updateCard(c)
		}
	}

	@RegisterFunction
	fun updateCard(card: ActionCard) {
		titleLabel.text = card.cardName
	}

	@RegisterFunction
	override fun _process(delta: Double) {
	}
}
