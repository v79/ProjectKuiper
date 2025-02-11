package actions

import SignalBus
import godot.*
import godot.annotation.*
import godot.core.*
import godot.extensions.getNodeAs
import godot.global.GD
import godot.util.toRealT
import hexgrid.Hex

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

	@RegisterProperty
	@Export
	var clickRadius = 200

	companion object {
		const val CARD_WIDTH = 150f
		const val CARD_HEIGHT = 200f
	}

	var status = CardStatus.IN_FAN

	//    var dragging = false
	private var isDraggable = false

	//    var disabled = false
	private var placedOnHex: Hex? = null


	// properties set during placement in the fan
	private var offset = Vector2()
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
		signalBus.cancelActionConfirmation.connect { ->
			status = CardStatus.IN_FAN
			this.show()
			draggingStopped.emitSignal(this)
			returnCardToFan()
		}
	}


	@RegisterFunction
	override fun _process(delta: Double) {
		if (isDraggable && status != CardStatus.DISABLED) {
			if (Input.isActionJustPressed("mouse_left_click".asStringName())) {
				offset = getGlobalMousePosition() - globalPosition
			}
			if (Input.isActionPressed("mouse_left_click".asStringName())) {
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
			} else if (Input.isActionJustReleased("mouse_left_click".asStringName())) {
				if (placedOnHex != null) {
					GD.print("Card placed on hex ${placedOnHex?.locationName}")
					status = CardStatus.PLACED_ON_HEX

					// now we trigger the confirmation dialog and other cool stuff by emitting a signal
					this.hide()
					signalBus.showActionConfirmation.emitSignal(placedOnHex!!, this)

				} else {
					status = CardStatus.IN_FAN
					draggingStopped.emitSignal(this)
					returnCardToFan()
				}
			}
		}
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
		getTree()!!.createTween()
			?.tweenProperty(this, "rotation".asNodePath(), GD.degToRad(startRotation), 0.5)
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
}

enum class CardStatus {
	IN_FAN, DRAGGING, PLACED_ON_HEX, DISABLED
}
