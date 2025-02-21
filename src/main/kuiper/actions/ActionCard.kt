package actions

import SignalBus
import godot.*
import godot.annotation.*
import godot.core.*
import godot.extensions.getNodeAs
import godot.global.GD
import godot.util.toRealT
import hexgrid.Hex
import state.Building

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
		signalBus.cancelActionConfirmation.connect {
			if (isInsideTree()) {
				returnCardToFan()
				this.show()
				draggingStopped.emitSignal(this)
				GD.print("Card ${this.cardName} returned to fan")
			}
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

		// set the texture based on the Action type
		when (action.type) {
			ActionType.BUILD -> {
				cardImage.setThemeTypeVariation("BuildCard".asStringName())
				sectorSizeLabel.text = building?.sectors.toString()
			}

			ActionType.INVEST -> {
				cardImage.setThemeTypeVariation("InvestCard".asStringName())
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
}

enum class CardStatus {
	IN_FAN, DRAGGING, PLACED_ON_HEX, DISABLED
}
