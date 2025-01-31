package actions

import godot.CenterContainer
import godot.Node2D
import godot.PackedScene
import godot.ResourceLoader
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

	private val maxWidth = 500f
	private val cardWidth = 200f

	// UI elements
	private val fanContainer: CenterContainer by lazy { getNodeAs("HBoxContainer/FanContainer")!! }

	// packed scenes
	private val actionCardScene = ResourceLoader.load("res://src/main/kuiper/actions/action_card.tscn") as PackedScene

	@RegisterFunction
	override fun _ready() {

	}

	@RegisterFunction
	override fun _process(delta: Double) {
		placeCards()
	}

	@RegisterFunction
	fun addNewCard() {
		addActionString("New Card ${cardCount + 1}")
	}

	// TODO: I really want to pass the whole Action here, but that is not possible with Godot/JVM
	@RegisterFunction
	fun addActionString(w: String) {
		GD.print("Adding card: $w")
		// the cards seem to merge when I drag them, as if they share state
		val card = actionCardScene.instantiate() as ActionCard
		card.cardName = w
		actionCardIds.add(cardCount + 1) // should actually be the card ID
		fanContainer.callDeferred(::addChild, card)
	}

	@RegisterFunction
	fun placeCards() {
		actionCardIds.forEachIndexed { index, _ ->
			val card = fanContainer.getChild(index) as ActionCard
			card.positionMutate {
				x = index * 300.0
				card.startPosition.x = x
			}
		}
	}

}
