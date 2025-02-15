package confirm_action

import SignalBus
import actions.ActionCard
import actions.ResourceType
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
    private val titleLabel: Label by lazy { getNodeAs("%CardTitle")!! }
    private val turnsLabel: Label by lazy { getNodeAs("%Turns")!! }
    private val goldLabel: Label by lazy { getNodeAs("%GoldCost")!! }
    private val influenceLabel: Label by lazy { getNodeAs("%InfluenceCost")!! }
    private val conMatsLabel: Label by lazy { getNodeAs("%ConMatsCost")!! }
    private val descLabel: Label by lazy { getNodeAs("%CardDescription")!! }


    @RegisterFunction
    override fun _ready() {
        signalBus = getNodeAs("/root/SignalBus")!!

        signalBus.showActionConfirmation.connect { _, c ->
            updateCard(c)
        }
    }

    @RegisterFunction
    fun updateCard(card: ActionCard) {
        titleLabel.text = card.cardName
        card.action?.let { action ->
            turnsLabel.text = action.turns.toString()
            descLabel.text = action.description
            val sBuilder = StringBuilder()
            val goldCost = action.getCost(ResourceType.GOLD)
            buildCostString(goldCost, sBuilder)
            goldLabel.text = sBuilder.toString()
            sBuilder.clear()
            val infCost = action.getCost(ResourceType.INFLUENCE)
            buildCostString(infCost, sBuilder)
            influenceLabel.text = sBuilder.toString()
            sBuilder.clear()
            val conMatsCost = action.getCost(ResourceType.CONSTRUCTION_MATERIALS)
            buildCostString(conMatsCost, sBuilder)
            conMatsLabel.text = sBuilder.toString()
        }
    }

    /**
     * Build a string representation of the cost of an action
     * @param resourceCost a pair of integers representing the cost of the action
     * @param sBuilder a StringBuilder to build the string
     */
    private fun buildCostString(resourceCost: Pair<Int?, Int?>, sBuilder: StringBuilder) {
        resourceCost.let { cost ->
            if (cost.first != null) {
                sBuilder.append(cost.first.toString())
            }
            if (cost.second != null) {
                if (cost.first != null) {
                    sBuilder.appendLine(" + ")
                }
                sBuilder.append("${cost.second.toString()}/turn")
            }
        }
    }

    @RegisterFunction
    override fun _process(delta: Double) {
    }
}
