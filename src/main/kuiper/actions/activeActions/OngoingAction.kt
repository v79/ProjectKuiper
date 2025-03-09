package actions.activeActions

import LogInterface
import SignalBus
import actions.Action
import actions.ActionType
import godot.*
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.core.Vector2
import godot.core.asStringName
import godot.core.connect
import godot.extensions.getNodeAs

@RegisterClass
class OngoingAction : Node2D(), LogInterface {

	override var logEnabled: Boolean = true

	// Globals
	private lateinit var signalBus: SignalBus

	// UI elements
	private lateinit var nameLbl: RichTextLabel
	private lateinit var costsPTLabel: RichTextLabel
	private lateinit var turnsLbl: Label
	private lateinit var cardBackground: PanelContainer
	private lateinit var expiryTimer: Timer
	private lateinit var animationPlayer: AnimationPlayer

	@RegisterProperty
	var actionId: Int = 0

	@RegisterProperty
	var turnsRemaining: Int = 99

	private var action: Action = Action()

	@RegisterFunction
	override fun _ready() {
		signalBus = getNodeAs("/root/SignalBus")!!
		nameLbl = getNodeAs("%NameLabel")!!
		cardBackground = getNodeAs("%CardBackground")!!
		turnsLbl = getNodeAs("%TurnsLabel")!!
		costsPTLabel = getNodeAs("%CostsPerTurnLabel")!!
		expiryTimer = getNodeAs("%ExpiryTimer")!!
		animationPlayer = getNodeAs("%AnimationPlayer")!!

		nameLbl.setText("[b]${action.actionName}[/b]")
		turnsLbl.setText(turnsRemaining.toString())

		signalBus.updateOngoingAction.connect { id, turnsLeft ->
			if (id == action.id) {
				turnsRemaining = turnsLeft
				updateUI()
			}
		}

		signalBus.actionCompleted.connect { wrapper ->
			if (action.id == wrapper.action?.id) {
				expireAction()
			}
		}

		updateUI()
	}

	@RegisterFunction
	fun _on_area_2d_mouse_entered() {
		scale = Vector2(1.5, 1.5)
	}

	@RegisterFunction
	fun _on_area_2d_mouse_exited() {
		scale = Vector2(1.0, 1.0)
	}

	fun setAction(act: Action) {
		action = act
	}

	private fun updateUI() {
		val tooltipSBuilder = StringBuilder()
		tooltipSBuilder.appendLine(action.description)
		turnsLbl.setText(turnsRemaining.toString())
		cardBackground.setTooltipText(action.description)
		costsPTLabel.clear()
		if (action.getCostsPerTurn().isNotEmpty()) {
			tooltipSBuilder.appendLine("Per turn costs:")
		}
		action.getCostsPerTurn().forEach { (resource, amount) ->
			costsPTLabel.appendText("$amount ${resource.bbCodeIcon(25)} ")
			tooltipSBuilder.appendLine("$amount ${resource.displayName}")
		}
		cardBackground.setTooltipText(tooltipSBuilder.toString())
		when (action.type) {
			ActionType.BUILD -> {
				cardBackground.setThemeTypeVariation("BuildCard".asStringName())
				turnsLbl.setThemeTypeVariation("BuildCard".asStringName())
				nameLbl.setThemeTypeVariation("BuildCard".asStringName())
				costsPTLabel.setThemeTypeVariation("BuildCard".asStringName())
			}

			ActionType.INVEST -> {
				cardBackground.setThemeTypeVariation("InvestCard".asStringName())
				turnsLbl.setThemeTypeVariation("InvestCard".asStringName())
				nameLbl.setThemeTypeVariation("InvestCard".asStringName())
				costsPTLabel.setThemeTypeVariation("InvestCard".asStringName())
			}

			else -> {
				// stay with default black card
			}
		}
	}


	/**
	 * Do a fancy animation to show that the action has completed, then delete the panel
	 */
	private fun expireAction() {
		animationPlayer.play("remove_panel".asStringName())
		expiryTimer.start()
		expiryTimer.timeout.connect {
			queueFree()
		}
	}
}
