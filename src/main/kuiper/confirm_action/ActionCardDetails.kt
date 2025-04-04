package confirm_action

import SignalBus
import actions.ActionCard
import actions.ActionType
import actions.ResourceType
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.api.*
import godot.core.StringName
import godot.core.Vector2
import godot.core.asStringName
import godot.core.connect
import godot.extension.getNodeAs
import godot.global.GD
import hexgrid.Hex
import state.Building

@RegisterClass
class ActionCardDetails : Control() {

    // Globals
    private val signalBus: SignalBus by lazy { getNodeAs("/root/Kuiper/SignalBus")!! }

    @RegisterProperty
    @Export
    var cardTitle: String = ""

    // UI elements
    private val panelContainer: PanelContainer by lazy { getNodeAs("PanelContainer")!! }
    private val titleLabel: Label by lazy { getNodeAs("%CardTitle")!! }
    private val turnsLabel: Label by lazy { getNodeAs("%Turns")!! }
    private val goldLabel: Label by lazy { getNodeAs("%GoldCost")!! }
    private val influenceLabel: Label by lazy { getNodeAs("%InfluenceCost")!! }
    private val conMatsLabel: Label by lazy { getNodeAs("%ConMatsCost")!! }
    private val descLabel: Label by lazy { getNodeAs("%CardDescription")!! }
    private val sectorSizeLabel: Label by lazy { getNodeAs("%SectorSize")!! }
    private val iconTexture: TextureRect by lazy { getNodeAs("%IconTexture")!! }
    private val hexContainer: BoxContainer by lazy { getNodeAs("%HexContainer")!! }
    private val hex: Hex by lazy { getNodeAs("%Hex")!! }

    @RegisterFunction
    override fun _ready() {
        hex.setScale(Vector2(0.75, 0.75))

        signalBus.showActionConfirmation.connect { _, c ->
            updateCard(c)
        }
    }

    @RegisterFunction
    fun updateCard(card: ActionCard) {
        titleLabel.text = card.cardName
        iconTexture.setTexture(null)
        hex.setPosition(Vector2(130, 15)) // Hard-coded position, I can't get the hexContainer's size for some reason
        card.action?.let { action ->
            val building: Building? = action.buildingToConstruct
            // set the theme variation to display the correct texture for the card
            when (action.type) {
                ActionType.BUILD -> {
                    setThemeVariation("BuildCard".asStringName())
                    sectorSizeLabel.text = building?.sectors.toString()
                    GD.print("Updating card with building: ${building?.name} and spritePath ${building?.spritePath}")
                    building?.spritePath?.let { sPath ->
                        val texture = ResourceLoader.load(sPath, "Texture2D") as Texture2D
                        iconTexture.setTexture(texture)
                    }
                }

                ActionType.INVEST -> {
                    hex.visible = false
                    setThemeVariation("InvestCard".asStringName())
                }

                else -> {
                    hex.visible = false
                    setThemeVariation()
                }
            }

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
            sectorSizeLabel.visible = action.type == ActionType.BUILD
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

    private fun setThemeVariation(variation: StringName = "".asStringName()) {
        panelContainer.setThemeTypeVariation(variation)
        titleLabel.setThemeTypeVariation(variation)
        turnsLabel.setThemeTypeVariation(variation)
        goldLabel.setThemeTypeVariation(variation)
        influenceLabel.setThemeTypeVariation(variation)
        conMatsLabel.setThemeTypeVariation(variation)
        descLabel.setThemeTypeVariation(variation)
        sectorSizeLabel.setThemeTypeVariation(variation)
    }
}
