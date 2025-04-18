package actions

import SignalBus
import godot.annotation.*
import godot.api.*
import godot.common.util.toRealT
import godot.core.*
import godot.extension.getNodeAs
import godot.global.GD
import hexgrid.Hex
import state.Building

@RegisterClass
class ActionCard : Node2D() {

    private val signalBus: SignalBus by lazy {
        getNodeAs("/root/Kuiper/SignalBus")!!
    }

    @RegisterProperty
    @Export
    var cardName = "Card"

    @RegisterProperty
    @Export
    var cardId: Int = 0

    @RegisterProperty
    @Export
    var influenceCost = 3

    @RegisterProperty
    @Export
    var clickRadius = 200

    companion object {
        const val CARD_WIDTH = 150f
        const val CARD_HEIGHT = 200f
    }

    // The action associated with this card
    var action: Action? = null
        private set

    private var isDraggable = false
    private var placedOnHex: Hex? = null

    // properties set during placement in the fan
    var status = CardStatus.IN_FAN
    private var offset = Vector2()
    var startPosition = Vector2()
    var startRotation: Float = 0.0f

    // UI elements
    private val cardNameLabel: Label by lazy { getNodeAs("%ActionName")!! }
    private val influenceCostLabel: Label by lazy { getNodeAs("%InfluenceCost")!! }
    private val goldCostLabel: Label by lazy { getNodeAs("%GoldCost")!! }
    private val conMatsCostLabel: Label by lazy { getNodeAs("%ConMatsCost")!! }
    private val turnsLabel: Label by lazy { getNodeAs("%Turns")!! }
    private val cardImage: PanelContainer by lazy { getNodeAs("PanelContainer")!! }
    private val sectorSizeLabel: Label by lazy { getNodeAs("%SectorSize")!! }
    private val iconTexture: TextureRect by lazy { getNodeAs("%IconTexture")!! }

    // signals
    @RegisterSignal("card_id")
    val mouseEntered by signal1<Int>()

    @RegisterSignal("card_id")
    val mouseExited by signal1<Int>()

    @RegisterSignal("card")
    val isDraggingCard by signal1<ActionCard>()

    @RegisterSignal("card")
    val draggingStopped by signal1<ActionCard>()

    private var topLeftLimit = Vector2(100f, 100f)
    private var widthLimit = 1200f

    @RegisterFunction
    override fun _ready() {
        startPosition = position
        cardNameLabel.text = cardName

        // when screen is resized, update the width limit to constrain dragging
        calcWidthLimit(signalBus.screenWidth)
        signalBus.onScreenResized.connect { width, _ ->
            calcWidthLimit(width)
        }

        // if the card is placed on a hex, disable dragging and ... do stuff?
        signalBus.cardOnHex.connect { h ->
            placedOnHex = h
        }

        signalBus.cardOffHex.connect {
            placedOnHex = null
        }


        // when the action is confirmed, return the card to the fan
        signalBus.cancelActionConfirmation.connect {
            if (isInsideTree()) {
                returnCardToFan()
                this.show()
                draggingStopped.emitSignal(this)
            }
        }
    }


    @RegisterFunction
    override fun _process(delta: Double) {
        if (isDraggable && status != CardStatus.DISABLED) {
            if (Input.isActionJustPressed("mouse_left_click".asCachedStringName())) {
                offset = getGlobalMousePosition() - globalPosition
            }
            if (Input.isActionPressed("mouse_left_click".asCachedStringName())) {
                status = CardStatus.DRAGGING
                isDraggingCard.emitSignal(this)
                // limit drags to bounding box
                val newPosition = getGlobalMousePosition() - offset
                if (newPosition.x < topLeftLimit.x) {
                    newPosition.x = topLeftLimit.x
                }
                if (newPosition.x > widthLimit) {
                    newPosition.x = widthLimit.toRealT()
                }
                if (newPosition.y < topLeftLimit.y) {
                    newPosition.y = topLeftLimit.y
                }
                globalPosition = newPosition

                // clear rotation when dragging but revert when released
                getTree()!!.createTween()?.tweenProperty(this, "rotation".asNodePath(), GD.degToRad(0.0f), 0.5)
            }
            if (Input.isActionJustReleased("mouse_left_click".asCachedStringName())) {
                if (placedOnHex != null) {
                    status = CardStatus.PLACED_ON_HEX
                    // now we trigger the confirmation dialog and other cool stuff by emitting a signal
                    this.hide()
                    signalBus.showActionConfirmation.emitSignal(placedOnHex, this)
                } else {
                    status = CardStatus.IN_FAN
                    draggingStopped.emitSignal(this)
                    returnCardToFan()
                }
            }
        }
    }

    /**
     *  Disable the card from being dragged
     */
    @RegisterFunction
    fun disableProcessing() {
        setProcessInput(false)
        setProcess(false)
        setProcessShortcutInput(false)
        status = CardStatus.DISABLED
    }

    /**
     *  Enable the card to be dragged
     */
    @RegisterFunction
    fun enableProcessing() {
        setProcessInput(true)
        setProcess(true)
        setProcessShortcutInput(true)
        status = CardStatus.IN_FAN
    }

    @RegisterFunction
    fun _on_area_2d_mouse_entered() {
        mouseEntered.emitSignal(this.cardId)
        if (status != CardStatus.DRAGGING) {
            isDraggable = true
        }
    }

    @RegisterFunction
    fun _on_area_2d_mouse_exited() {
        mouseExited.emitSignal(this.cardId)
        if (status != CardStatus.DRAGGING) {
            isDraggable = false
        }
    }

    /**
     *  Return the card to its original position in the fan
     */
    private fun returnCardToFan() {
        getTree()!!.createTween()?.tweenProperty(this, "position".asNodePath(), startPosition, 0.5)
        getTree()!!.createTween()?.tweenProperty(this, "rotation".asNodePath(), GD.degToRad(startRotation), 0.5)
        status = CardStatus.IN_FAN
    }

    fun highlight() {
        cardImage.modulate = Color(1.0, 0.8, 0.8, 1.0)
    }

    fun unhighlight() {
        cardImage.modulate = Color(1.0, 1.0, 1.0, 1.0)
    }

    /**
     *  Calculate the right limit for dragging
     */
    private fun calcWidthLimit(width: Int) {
        widthLimit = (width - 100f - offset.x - CARD_WIDTH).toFloat()
    }

    /**
     *  Set the action associated with this card
     *  Add all the appropriate costs and effects
     */
    fun setAction(action: Action) {
        this.action = action
        this.cardId = action.id
        this.cardName = action.actionName
        this.turnsLabel.text = action.turns.toString()
        val building: Building? = action.buildingToConstruct
        iconTexture.setTexture(null)

        // set the texture based on the Action type
        when (action.type) {
            ActionType.BUILD -> {
                setThemeVariation("BuildCard".asCachedStringName())
                sectorSizeLabel.text = building?.sectors.toString()
                if (building != null) {
                    building.spritePath?.let { sPath ->
                        val texture = ResourceLoader.load(sPath, "Texture2D") as Texture2D
                        iconTexture.setTexture(texture)
                    }
                }
            }

            ActionType.INVEST -> {
                setThemeVariation("InvestCard".asCachedStringName())
            }

            else -> {
                // stay with default black card
            }
        }
        sectorSizeLabel.visible = action.type == ActionType.BUILD

        val tooltipStringBuilder = StringBuilder()
        tooltipStringBuilder.appendLine(action.description)
        if (action.turns > 0) {
            tooltipStringBuilder.appendLine("Turns to complete: ${action.turns}")
        } else {
            tooltipStringBuilder.appendLine("Instant action")
        }
        // set the cost labels
        if (action.initialCosts.isNotEmpty()) {
            tooltipStringBuilder.append("Initial Costs: ")
        }
        action.initialCosts.forEach { (type, cost) ->
            when (type) {
                ResourceType.INFLUENCE -> if (cost != 0) {
                    influenceCostLabel.text = cost.toString()
                    tooltipStringBuilder.append("Influence: $cost ")
                } else {
                    influenceCostLabel.hide()
                }

                ResourceType.GOLD -> if (cost != 0) {
                    goldCostLabel.text = cost.toString()
                    tooltipStringBuilder.append("Gold: $cost ")
                } else {
                    goldCostLabel.hide()
                }

                ResourceType.CONSTRUCTION_MATERIALS -> if (cost != 0) {
                    conMatsCostLabel.text = cost.toString()
                    tooltipStringBuilder.append("Construction Materials: $cost ")
                } else {
                    conMatsCostLabel.hide()
                }

                ResourceType.NONE -> {
                    // do nothing
                }
            }
        }
        // Add mutation results to the tooltip
        if (action.getMutations().isNotEmpty()) {
            tooltipStringBuilder.appendLine()
            tooltipStringBuilder.appendLine("Effects: ")
            action.getMutations().forEach {
                tooltipStringBuilder.appendLine(it.toString())
            }
        }
        cardImage.tooltipText = tooltipStringBuilder.toString()
    }

    private fun setThemeVariation(variation: StringName = "".asStringName()) {
        cardImage.setThemeTypeVariation(variation)
        cardNameLabel.setThemeTypeVariation(variation)
        turnsLabel.setThemeTypeVariation(variation)
        influenceCostLabel.setThemeTypeVariation(variation)
        conMatsCostLabel.setThemeTypeVariation(variation)
        sectorSizeLabel.setThemeTypeVariation(variation)
        goldCostLabel.setThemeTypeVariation(variation)
    }
}

enum class CardStatus {
    IN_FAN, DRAGGING, PLACED_ON_HEX, DISABLED
}
