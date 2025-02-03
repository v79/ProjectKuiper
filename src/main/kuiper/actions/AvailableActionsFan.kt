package actions

import SignalBus
import godot.*
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.core.connect
import godot.extensions.getNodeAs
import godot.global.GD

@RegisterClass
class AvailableActionsFan : Node2D() {

    // Globals
    private lateinit var signalBus: SignalBus

    private var actionCards: MutableMap<Int, ActionCard> = mutableMapOf()

    private val cardCount: Int
        get() = actionCards.size
    private var cardIdCounter: Int = 0

    // Card curve properties
    @RegisterProperty
    @Export
    var placementCurve: Curve = Curve()

    @RegisterProperty
    @Export
    var rotationCurve: Curve = Curve()

    @RegisterProperty
    @Export
    var xSep: Float = 10f

    @RegisterProperty
    @Export
    var yMin: Float = 70f

    @RegisterProperty
    @Export
    var yMax: Float = -70f

    @RegisterProperty
    @Export
    var maxRotationDegrees: Float = 10f

    private var maxWidth = 1200f
    private val cardWidth = 200f

    // UI elements
    private val fanContainer: CenterContainer by lazy { getNodeAs("HBoxContainer/FanContainer")!! }
    private val dropZoneBounds: Control by lazy { getNodeAs("DropZoneBounds")!! }

    // packed scenes
    private val actionCardScene = ResourceLoader.load("res://src/main/kuiper/actions/action_card.tscn") as PackedScene

    @RegisterFunction
    override fun _ready() {
        signalBus = getNodeAs("/root/SignalBus")!!
        signalBus.onScreenResized.connect { width, height ->
            screenResized(width, height)
        }
    }

    @RegisterFunction
    override fun _process(delta: Double) {
    }

    // temp function to create new cards on demand for testing
    @RegisterFunction
    fun addCard() {
        addActionCard(++cardIdCounter)
    }

    // temp function to remove a random card for testing
    @RegisterFunction
    fun removeRandomCard() {
        if (actionCards.isNotEmpty()) {
            val index = (0 until actionCards.size).random()
            val card = fanContainer.getChild(index) as ActionCard?
            GD.print("Removing card: idx: $index - ${card?.cardId}")
            if (card != null) {
                fanContainer.removeChild(card)
                actionCards.remove(card.cardId)
                updateCardPlacements()
            }
        }
    }

    /**
     * Card fan placement algorithm from https://github.com/guladam/godot_2d_card_fanning/blob/main/hand.gd
     */
    @RegisterFunction
    fun updateCardPlacements() {
        var allCardsSize = cardWidth * cardCount + xSep * (cardCount - 1)
        var finalXSep = xSep

        if (allCardsSize > maxWidth) {
            finalXSep = (maxWidth - cardWidth * cardCount) / (cardCount - 1)
            allCardsSize = maxWidth
        }

        val offset = (maxWidth - allCardsSize) / 2

        actionCards.forEach { (id, card) ->
            val index = getIndexForCardId(id)
            var yMultiplier = placementCurve.sample((1.0 / (cardCount - 1) * index).toFloat())
            var rotMultiplier = rotationCurve.sample((1.0 / (cardCount - 1) * index).toFloat())

            if (cardCount == 1) {
                yMultiplier = 0.0f
                rotMultiplier = 0.0f
            }

            val finalX = offset + cardWidth * index + finalXSep * index
            val finalY = yMin + yMax * yMultiplier

            card.positionMutate {
                x = finalX.toDouble()
                y = finalY.toDouble()
            }
            card.startPosition = card.position
            card.rotationDegrees = maxRotationDegrees * rotMultiplier
            card.startRotation = card.rotationDegrees
        }
    }

    /**
     * Responding to the SignalBus.onScreenResized signal
     * Redraw the card fan based on the new screen size
     */
    @RegisterFunction
    fun screenResized(width: Int, height: Int) {
        maxWidth = width.toFloat() - 400f
        GD.print("Screen resized; max width now $maxWidth")
        updateCardPlacements()
    }

    /**
     * Scan the fan for a card with a given ID
     */
    fun getCardNodeById(id: Int): ActionCard? {
        fanContainer.getChildren().forEach { child ->
            if (child is ActionCard) {
                if (child.cardId == id) {
                    return child
                }
            }
        }
        return null
    }

    /**
     * Get the index of a card in the fan
     */
    private fun getIndexForCardId(id: Int): Int {
        var index = 0
        fanContainer.getChildren().forEach {
            if (it is ActionCard) {
                if (it.cardId == id) {
                    return index
                }
            }
            index++
        }
        GD.printErr("While searching for card $id, it was not found. Returning -1")
        return -1
    }

    @RegisterFunction
    fun cardEnteredHandler(id: Int) {
        actionCards[id]?.highlight()
    }

    @RegisterFunction
    fun cardExitHandler(id: Int) {
        actionCards[id]?.unhighlight()
    }

    @RegisterFunction
    fun disableOtherCards(id: Int) {
        actionCards.filterNot { it.key == id }.forEach { (_, card) ->
            card.disabled = true
        }
    }

    @RegisterFunction
    fun enableAllCards() {
        actionCards.forEach { (_, card) ->
            card.disabled = false
        }
    }


    /**
     * Add a new action card to the fan
     * @param id The ID of the action card, to be fetched from the data store
     */
    @RegisterFunction
    fun addActionCard(id: Int) {
        GD.print("Adding card: $id")
        val card = actionCardScene.instantiate() as ActionCard
        // In reality, I'd be loading the action from a data store
        card.cardId = id
        card.cardName = "Card $id"

        actionCards[id] = card
        fanContainer.addChild(card)

        // connect signals
        card.mouseEntered.connect { c ->
            cardEnteredHandler(c)
        }
        card.mouseExited.connect { c ->
            cardExitHandler(c)
        }
        card.isDraggingCard.connect { c ->
            disableOtherCards(c)
        }
        card.draggingStopped.connect {
            enableAllCards()
        }

        // draw the fan
        updateCardPlacements()
    }
}
