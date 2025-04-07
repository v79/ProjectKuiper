package actions.activeActions

import LogInterface
import SignalBus
import actions.Action
import actions.ActionType
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.Control
import godot.api.PackedScene
import godot.api.ResourceLoader
import godot.api.VBoxContainer
import godot.core.Vector2
import godot.core.connect
import godot.extension.getNodeAs
import state.GameState

@RegisterClass
class ActiveActionsFan : Control(), LogInterface {

    override var logEnabled: Boolean = true

    // Globals
    private lateinit var gameState: GameState
    private val signalBus: SignalBus by lazy {
        getNodeAs("/root/Kuiper/SignalBus")!!
    }

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
        gameState = getNodeAs("/root/GameState")!!
        ongoingActionsContainer = getNodeAs("%OngoingActionsContainer")!!

        // populate existing ongoing actions
        gameState.company.activeActions.forEach { addOngoingAction(it) }

        // connect to signals
        signalBus.confirmAction.connect { hex, actionWrapper ->
            if (actionWrapper.action == null) {
                logError("ConfirmAction received a null action $actionWrapper")
                return@connect
            }
            actionWrapper.action?.let {
                gameState.company.activateAction(hex, it)
                addOngoingAction(it)
                // update the resource panel. Company cannot do this as it doesn't have access to the signal bus
                gameState.company.resources.forEach { resource ->
                    signalBus.updateResource.emit(resource.key.name, resource.value.toFloat())
                }
                // if this a building action, update the hex grid with icons and stuff
                if (it.type == ActionType.BUILD) {
                    log("Updating hexgrid for build action")
                    log(it.toString())
                    log("Hex: ${hex.hexData}")
                }
            }
        }

        signalBus.actionCompleted.connect { actionWrapper ->
            if (actionWrapper.action == null) {
                logError("ConfirmAction received a null action $actionWrapper")
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
                logError("Got null when looking for OngoingAction_${action.id}")
            }
        }
    }
}
