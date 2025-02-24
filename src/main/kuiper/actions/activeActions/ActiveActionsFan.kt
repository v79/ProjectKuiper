package actions.activeActions

import SignalBus
import actions.Action
import godot.Control
import godot.PackedScene
import godot.ResourceLoader
import godot.VBoxContainer
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.core.Vector2
import godot.core.connect
import godot.extensions.getNodeAs
import godot.global.GD
import state.GameState

@RegisterClass
class ActiveActionsFan : Control() {

	// Globals
	private lateinit var signalBus: SignalBus
	private lateinit var gameState: GameState

	// packed scenes
	private val ongoingActionScene =
		ResourceLoader.load("res://src/main/kuiper/actions/activeActions/ongoing_action.tscn") as PackedScene

	// UI elements
	private lateinit var ongoingActionsContainer: VBoxContainer

	// list of going actions as an ordered list
	private val ongoingActions: MutableList<Action> = mutableListOf()

	private val cardHeight: Double = 50.0

	@RegisterFunction
	override fun _ready() {
		signalBus = getNodeAs("/root/SignalBus")!!
		gameState = getNodeAs("/root/GameState")!!
		ongoingActionsContainer = getNodeAs("%OngoingActionsContainer")!!

		// populate existing ongoing actions
		gameState.company.activeActions.forEach { addOngoingAction(it) }

		// connect to signals
		signalBus.confirmAction.connect { hex, actionWrapper ->
			if (actionWrapper.action == null) {
				GD.printErr("ConfirmAction received a null action $actionWrapper")
				return@connect
			}
			gameState.company.activateAction(hex, actionWrapper.action!!)
			addOngoingAction(actionWrapper.action!!)
		}

		signalBus.actionCompleted.connect { actionWrapper ->
			if (actionWrapper.action == null) {
				GD.printErr("ConfirmAction received a null action $actionWrapper")
				return@connect
			}
			ongoingActions.removeIf { it.id == actionWrapper.action?.id }
		}
	}

	@RegisterFunction
	override fun _process(delta: Double) {

	}

	/**
	 * Add an ongoing action to the UI
	 */
	private fun addOngoingAction(action: Action) {
		val ongoingAction = ongoingActionScene.instantiate() as OngoingAction
		GD.print(ongoingAction)
		ongoingAction.apply {
			setName("OngoingAction_${action.id}")
			actionId = action.id
			turnsRemaining = action.turnsRemaining
			setAction(action)
		}
		ongoingActions.add(action)
		ongoingActions.sortBy { it.turnsRemaining }
		ongoingActionsContainer.addChild(ongoingAction)
		placeActions()
	}


	/**
	 * Place the ongoing actions in the correct order, sorted by time remaining, shortest first
	 */
	private fun placeActions() {
		var yPos = 0.0
		ongoingActions.forEachIndexed { index, action ->
			val ongoingAction = ongoingActionsContainer.getNodeAs<OngoingAction>("OngoingAction_${action.id}")
			if (ongoingAction != null) {
				ongoingAction.setPosition(Vector2(0.0, yPos))
				yPos += cardHeight
			} else {
				GD.printErr("Got null when looking for OngoingAction_${action.id}")
			}
		}
	}
}
