package actions

import SignalBus
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.api.*
import godot.core.connect
import godot.extension.getNodeAs
import godot.global.GD

@RegisterClass
class AvailableActionsFan : Node2D() {

    // Globals
    private lateinit var signalBus: SignalBus

    private var actionCards: MutableMap<Int, ActionCard> = mutableMapOf()

    private val cardCount: Int
        get() = actionCards.size

    private var maxWidth = 1200f

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

    // UI elements
    private lateinit var fanContainer: CenterContainer
    private lateinit var delayTimer: Timer

    // packed scenes
    private val actionCardScene = ResourceLoader.load("res://src/main/kuiper/actions/action_card.tscn") as PackedScene

    @RegisterFunction
    override fun _ready() {
        signalBus = getNodeAs("/root/SignalBus")!!
        fanContainer = getNodeAs("HBoxContainer/FanContainer")!!
        delayTimer = getNodeAs("%DelayActivationTimer")!!

        GD.print("AvailableActionsFan ready - connecting signals")
        // connect signals
        signalBus.dealCardFromDeck.connect { actionWrapper ->
            addActionCard(actionWrapper.action)
        }
        signalBus.onScreenResized.connect { width, height ->
            screenResized(width, height)
        }
        signalBus.confirmAction.connect { _, actionWrapper ->
            actionWrapper.action?.let { action ->
                removeCard(action.id)
            }
            // when an action is confirmed, the mouse click immediately activates the card under the mouse
            // I don't want this to happen, so I'm adding a delay to re-enable the cards after a short time
            delayTimer.start()
            delayTimer.timeout.connect {
                actionCards.forEach { (_, card) -> card.enableProcessing() }
            }

        }
        signalBus.cancelActionConfirmation.connect {
            delayTimer.start()
            delayTimer.timeout.connect {
                actionCards.forEach { (_, card) -> card.enableProcessing() }
            }
        }
        signalBus.showActionConfirmation.connect { _, c ->
            // disable all the other cards to stop double-emit when confirming/cancelling action
            actionCards.forEach { (_, card) ->
                if (card != c) {
                    card.disableProcessing()
                }
            }
        }
    }

    @RegisterFunction
    override fun _process(delta: Double) {
    }

    // temp function to remove a random card for testing
    @RegisterFunction
    fun removeRandomCard() {
        if (actionCards.isNotEmpty()) {
            val index = (0 until actionCards.size).random()
            val card = fanContainer.getChild(index) as ActionCard?
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
        var allCardsSize = ActionCard.CARD_WIDTH * cardCount + xSep * (cardCount - 1)
        var finalXSep = xSep

        if (allCardsSize > maxWidth) {
            finalXSep = (maxWidth - ActionCard.CARD_WIDTH * cardCount) / (cardCount - 1)
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

            val finalX = offset + ActionCard.CARD_WIDTH * index + finalXSep * index
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

    /**
     * Add a new action card to the fan
     * @param action The action to add, from the card deck
     */
    @RegisterFunction
    fun addActionCard(action: Action?) {
        if (action == null) {
            GD.printErr("Tried to add a null action card")
            return
        }
        val card = actionCardScene.instantiate() as ActionCard
        card.setAction(action)

        actionCards[action.id] = card
        fanContainer.addChild(card)

        // connect signals
        card.mouseEntered.connect { c ->
            cardEnteredHandler(c)
        }
        card.mouseExited.connect { c ->
            cardExitHandler(c)
        }
        card.isDraggingCard.connect { c ->
            disableOtherCards(c.cardId)
            signalBus.draggingCard.emitSignal(c)
        }
        card.draggingStopped.connect { c ->
            enableAllCards()
            signalBus.droppedCard.emitSignal(c)
        }

        // draw the fan
        updateCardPlacements()
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
            card.status = CardStatus.DISABLED
        }
    }

    @RegisterFunction
    fun enableAllCards() {
        actionCards.forEach { (_, card) ->
            card.status = CardStatus.IN_FAN
        }
    }

    @RegisterFunction
    fun removeCard(id: Int) {
        val card = getCardNodeById(id)
        if (card != null) {
            fanContainer.removeChild(card)
            actionCards.remove(id)
            updateCardPlacements()
            enableAllCards()
            card.queueFree()
        } else {
            GD.printErr("Tried to remove card $id, but it was not found")
        }
    }
}
