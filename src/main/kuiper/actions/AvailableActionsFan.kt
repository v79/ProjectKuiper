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

	// UI elements
	private val fanContainer: CenterContainer by lazy { getNodeAs("HBoxContainer/FanContainer")!! }

	@Export
	@RegisterProperty
	var actionCardIds: VariantArray<Int> = VariantArray()

	// packed scenes
	private val actionCardScene = ResourceLoader.load("res://src/main/kuiper/actions/action_card.tscn") as PackedScene


	@RegisterFunction
	override fun _ready() {
		actionCardIds.forEach {
			GD.print("onReady: Card ID: $it")
		}
		GD.print("There are ${actionCardIds.size} cards!")
	}

	@RegisterFunction
	override fun _process(delta: Double) {

	}

	@RegisterFunction
	fun addActionString(w: String) {
		GD.print("Adding card: $w")
		val card = actionCardScene.instantiate() as ActionCard
		card.cardName = w
		actionCardIds.add(1) // should actually be the card ID
		fanContainer.callDeferred(::addChild, card)
	}

}
