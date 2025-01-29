package actions

import godot.*
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.core.Vector2
import godot.extensions.getNodeAs

@RegisterClass
class ActionCard : Node2D() {

    @RegisterProperty
    @Export
    var cardName = "Card"

    @RegisterProperty
    @Export
    var influenceCost = 3

    private var action: Action? = null

    // mouse handling variables
    /*	@RegisterSignal
        val mouseEntered: Signal0 by signal0()

        @RegisterSignal
        val mouseExited: Signal0 by signal0()*/

    // drag handling variables
    private var hasMouse = false

    @RegisterProperty
    @Export
    var clickRadius = 200
    private var dragging = false
    private var startPosition = Vector2()

    // UI elements
    private lateinit var cardNameLabel: Label
    private lateinit var influenceCostLabel: Label


    @RegisterFunction
    override fun _ready() {
        startPosition = position
        cardNameLabel = getNodeAs("PanelContainer/VBoxContainer/HBoxContainer/CardName")!!
    }

    @RegisterFunction
    override fun _process(delta: Double) {

        cardNameLabel.text = cardName

        // While dragging, move the card with the mouse.
        if (dragging && Input.isMouseButtonPressed(MouseButton.MOUSE_BUTTON_LEFT)) {
            val mPos = getLocalMousePosition()
            // trying to find a weighting which feels good; high numbers are jerky, low numbers are disconnected from the mouse position
            position = position.lerp(mPos, 8 * delta)
        }
        // Return to start position if dragging has stopped
        if (!dragging && position.distanceTo(startPosition) > 1) {
            position = position.lerp(startPosition, 6 * delta)
        }
    }

    @RegisterFunction
    override fun _input(event: InputEvent?) {
        if (event != null) {
            if (event is InputEventMouseButton && event.buttonIndex == MouseButton.MOUSE_BUTTON_LEFT) {
                if ((event.position - this.globalPosition).length() < clickRadius) {
                    // Start dragging if the click is on the control node.
                    if (!dragging && event.isPressed()) {
                        dragging = true
                    }
                }
                // Stop dragging if the button is released.
                if (dragging && !event.isPressed()) {
                    // check if we've hit a drag target
                    // if not, return to start position
                    dragging = false
                }
            }
        }
    }


    @RegisterFunction
    fun _on_mouse_entered() {
        hasMouse = true
    }

    @RegisterFunction
    fun _on_mouse_exited() {
        hasMouse = false
    }

    fun setAction(action: Action) {
        this.action = action
    }
}
