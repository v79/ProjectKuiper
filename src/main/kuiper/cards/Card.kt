package cards

import godot.*
import godot.annotation.*
import godot.core.Signal0
import godot.core.Vector2
import godot.core.signal0

@RegisterClass
class Card : Node2D() {

	@RegisterProperty
	@Export
	var cardName = "Card"

	@RegisterProperty
	@Export
	var influenceCost = 3

	// mouse handling variables
	@RegisterSignal
	val mouseEntered: Signal0 by signal0()

	@RegisterSignal
	val mouseExited: Signal0 by signal0()

	// drag handling variables
	private var hasMouse = false
	private var clickRadius = 150
	var dragging = false
	var startPosition = Vector2()

	// Called when the node enters the scene tree for the first time.
	@RegisterFunction
	override fun _ready() {
		startPosition = position
	}

	// Called every frame. 'delta' is the elapsed time since the previous frame.
	@RegisterFunction
	override fun _process(delta: Double) {
		if (dragging && Input.isMouseButtonPressed(MouseButton.MOUSE_BUTTON_LEFT)) {
			// While dragging, move the card with the mouse.
			val pos = getGlobalMousePosition()
			position = position.lerp(pos, 4 * delta)
		}
		// Return to start position if dragging has stopped
		if(!dragging && position.distanceTo(startPosition) > 1) {
			position = position.lerp(startPosition,6 * delta)
		}
	}

	@RegisterFunction
	override fun _input(event: InputEvent?) {
		if (event != null) {
			if (event is InputEventMouseButton && event.buttonIndex == MouseButton.MOUSE_BUTTON_LEFT) {
				if ((event.position - this.position).length() < clickRadius) {
					// Start dragging if the click is on the sprite.
					if (!dragging && event.isPressed()) {
						dragging = true;
					}
				}
				// Stop dragging if the button is released.
				if (dragging && !event.isPressed()) {
					// check if we've hit a drag target
					// if not, return to start position
					dragging = false;
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
}
