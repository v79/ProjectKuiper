package actions

import godot.*
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.core.VariantArray
import godot.extensions.callDeferred
import godot.extensions.getNodeAs
import godot.global.GD

@RegisterClass
class AvailableActionsFan : Node2D() {

	@Export
	@RegisterProperty
	var actionCardIds: VariantArray<Int> = VariantArray()
	private val cardCount: Int
		get() = actionCardIds.size

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

	private val maxWidth = 1400f
	private val cardWidth = 200f

	// UI elements
	private val fanContainer: CenterContainer by lazy { getNodeAs("HBoxContainer/FanContainer")!! }
	private val dropZoneBounds: Control by lazy { getNodeAs("DropZoneBounds")!! }

	// packed scenes
	private val actionCardScene = ResourceLoader.load("res://src/main/kuiper/actions/action_card.tscn") as PackedScene

	@RegisterFunction
	override fun _ready() {

	}

	@RegisterFunction
	override fun _process(delta: Double) {

	}

	// temp function to create new cards on demand for testing
	@RegisterFunction
	fun addCard() {
		addActionCard(cardCount + 1)
	}

	@RegisterFunction
	fun removeRandomCard() {
		if (actionCardIds.size > 0) {
			val index = (0 until actionCardIds.size).random()
			val card = fanContainer.getChild(index) as ActionCard?
			if (card != null) {
				fanContainer.removeChild(card)
				actionCardIds.remove(index)
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

		actionCardIds.forEachIndexed { index, _ ->
			if (fanContainer.getChild(index) == null) {
				GD.printErr("Failed to get card at index $index from actionCards array")
			} else {
				val card = fanContainer.getChild(index) as ActionCard?
				if (card != null) {
					var yMultiplier = placementCurve.sample((1.0 / (cardCount - 1) * index).toFloat())
					var rotMultiplier = rotationCurve.sample((1.0 / (cardCount - 1) * index).toFloat())

					if (cardCount == 1) {
						yMultiplier = 0.0f
						rotMultiplier = 0.0f
					}

					val finalX = offset + cardWidth * index + finalXSep * index
					val finalY = yMin + yMax * yMultiplier

					GD.print("Placing card $index to position ($finalX, $finalY), rotated ${rotMultiplier * maxRotationDegrees}")
					card.positionMutate {
						x = finalX.toDouble()
						y = finalY.toDouble()
					}
					card.startPosition = card.position
					card.rotationDegrees = maxRotationDegrees * rotMultiplier
					card.startRotation = card.rotationDegrees
				} else {
					GD.printErr("Got a null card for index $index. Somehow.")
				}
			}
		}
	}

	// TODO: I really want to pass the whole Action here, but that is not possible with Godot/JVM
	@RegisterFunction
	fun addActionCard(id: Int) {
		GD.print("Adding card: $id")
		// the cards seem to merge when I drag them, as if they share state
		val card = actionCardScene.instantiate() as ActionCard
		card.cardName = "Card $id"
		actionCardIds.add(id) // I wish this could be the card but it can't
		GD.print("Card count: $cardCount")
		GD.print("Card IDs: ${actionCardIds.size}")
		fanContainer.addChild(card)
		updateCardPlacements()
//		fanContainer.callDeferred(::addChild, card).also {
//			updateCardPlacements()
//		}
	}
}
