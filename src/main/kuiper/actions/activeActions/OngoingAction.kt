package actions.activeActions

import SignalBus
import actions.Action
import actions.ActionType
import godot.*
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.core.Vector2
import godot.core.asStringName
import godot.extensions.getNodeAs
import godot.global.GD

@RegisterClass
class OngoingAction : Node2D() {

    // Globals
    private lateinit var signalBus: SignalBus

    // UI elements
    private lateinit var nameLbl: RichTextLabel
    private lateinit var turnsLbl: Label
    private lateinit var cardBackground: PanelContainer

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

        nameLbl.setText("[b]${action.actionName}[/b]")
        turnsLbl.setText(turnsRemaining.toString())
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
        cardBackground.setTooltipText(turnsRemaining.toString())
        when (action.type) {
            ActionType.BUILD -> {
                cardBackground.setThemeTypeVariation("BuildCard".asStringName())
            }

            ActionType.INVEST -> {
                cardBackground.setThemeTypeVariation("InvestCard".asStringName())
            }

            else -> {
                // stay with default black card
            }
        }
    }
}
