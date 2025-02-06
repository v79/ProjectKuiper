package actions

import SignalBus
import godot.*
import godot.annotation.*
import godot.core.*
import godot.extensions.getNodeAs
import godot.global.GD
import godot.util.toRealT

@RegisterClass
class ActionCard : Node2D() {

    private lateinit var signalBus: SignalBus

    @RegisterProperty
    @Export
    var cardName = "Card"

    @RegisterProperty
    @Export
    var cardId: Int = 0

    @RegisterProperty
    @Export
    var influenceCost = 3

    private var action: Action? = null

    @RegisterProperty
    @Export
    var clickRadius = 200

    companion object {
        const val CARD_WIDTH = 150f
        const val CARD_HEIGHT = 200f
    }

    var dragging = false
    private var isDraggable = false
    var disabled = false
    private var offset = Vector2()

    // properties set during placement in the fan
    var startPosition = Vector2()
    var startRotation: Float = 0.0f

    // UI elements
    private val cardNameLabel: Label by lazy { getNodeAs("PanelContainer/VBoxContainer/HBoxContainer/CardName")!! }
    private val cardImage: PanelContainer by lazy { getNodeAs("PanelContainer")!! }
    private lateinit var influenceCostLabel: Label
    private lateinit var parentNode: Node

    // signals
    @RegisterSignal
    val mouseEntered by signal1<Int>("card_id")

    @RegisterSignal
    val mouseExited by signal1<Int>("card_id")

    @RegisterSignal
    val isDraggingCard by signal1<ActionCard>("card")

    @RegisterSignal
    val draggingStopped by signal1<ActionCard>("card")

    private var topLeftLimit = Vector2(100f, 100f)
    private var widthLimit = 1200f

    @RegisterFunction
    override fun _ready() {
        signalBus = getNodeAs("/root/SignalBus")!!
        startPosition = position
        cardNameLabel.text = cardName

        // when screen is resized, update the width limit to constrain dragging
        signalBus.onScreenResized.connect { width, _ ->
            widthLimit = (width - 100f - offset.x - CARD_WIDTH).toFloat()
        }
    }


    @RegisterFunction
    override fun _process(delta: Double) {
        if (isDraggable && !disabled) {
            if (Input.isActionJustPressed("mouse_left_click".asStringName())) {
                offset = getGlobalMousePosition() - globalPosition
            }
            if (Input.isActionPressed("mouse_left_click".asStringName())) {
                dragging = true
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
            } else if (Input.isActionJustReleased("mouse_left_click".asStringName())) {
                getTree()!!.createTween()?.tweenProperty(this, "position".asNodePath(), startPosition, 0.5)
                dragging = false
                draggingStopped.emitSignal(this)
                getTree()!!.createTween()?.tweenProperty(this, "rotation".asNodePath(), GD.degToRad(startRotation), 0.5)
            }
        }
    }

    @RegisterFunction
    fun _on_area_2d_mouse_entered() {
        mouseEntered.emitSignal(this.cardId)
        if (!dragging) {
            isDraggable = true
        }
    }

    @RegisterFunction
    fun _on_area_2d_mouse_exited() {
        mouseExited.emitSignal(this.cardId)
        if (!dragging) {
            isDraggable = false
        }
    }

    @RegisterFunction
    fun _on_area_2d_body_entered(body: StaticBody2D) {

    }

    @RegisterFunction
    fun _on_area_2d_body_exited() {

    }

    fun highlight() {
        cardImage.modulate = Color(1.0, 0.8, 0.8, 1.0)
    }

    fun unhighlight() {
        cardImage.modulate = Color(1.0, 1.0, 1.0, 1.0)
    }
}
