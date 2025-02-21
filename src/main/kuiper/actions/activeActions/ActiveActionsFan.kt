package actions.activeActions

import SignalBus
import actions.Action
import godot.Control
import godot.PackedScene
import godot.ResourceLoader
import godot.VBoxContainer
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
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
	}

	@RegisterFunction
	override fun _process(delta: Double) {

	}


	private fun addOngoingAction(action: Action) {
		GD.print("ActiveActionsFan: Adding ongoing action $action")
		val ongoingAction = ongoingActionScene.instantiate() as OngoingAction
		ongoingAction.apply {
			setName("OngoingAction_${action.id}")
			actionId = action.id
			turnsRemaining = action.turnsRemaining
		}
		ongoingActions.add(action)
		ongoingActionsContainer.addChild(ongoingAction)
	}
}
