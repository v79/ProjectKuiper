package actions

import godot.*
import godot.annotation.*
import godot.core.*
import godot.extensions.getNodeAs
import godot.global.GD

@RegisterClass
class ActionCard : Node2D() {

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
    private var dragging = false
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
    val isDraggingCard by signal1<Int>("card_id")

    @RegisterSignal
    val draggingStopped by signal1<Int>("card_id")


    @RegisterFunction
    override fun _ready() {
        startPosition = position
        cardNameLabel.text = cardName
    }

    @RegisterFunction
    override fun _enterTree() {
        // tell the fan that I am here?
    }


    private val topLeftLimit = Vector2(100f, 100f)
    private val widthLimit = 1200f

    @RegisterFunction
    override fun _process(delta: Double) {
        if (isDraggable && !disabled) {
            if (Input.isActionJustPressed("mouse_left_click".asStringName())) {
                offset = getGlobalMousePosition() - globalPosition
            }
            if (Input.isActionPressed("mouse_left_click".asStringName())) {
                dragging = true
                isDraggingCard.emitSignal(this.cardId)
                // limit drags to bounding box
                val newPosition = getGlobalMousePosition() - offset
                if (newPosition.x < topLeftLimit.x || newPosition.x > topLeftLimit.x + widthLimit) {
                    newPosition.x = if (newPosition.x < topLeftLimit.x) topLeftLimit.x else topLeftLimit.x + widthLimit
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
                draggingStopped.emitSignal(this.cardId)
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
