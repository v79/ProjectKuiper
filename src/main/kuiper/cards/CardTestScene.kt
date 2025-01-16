package cards

import godot.*
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.core.VariantArray
import godot.core.asCachedStringName
import godot.extensions.getNodeAs
import godot.global.GD
import src.main.kuiper.cards.DropTarget

@RegisterClass
class CardTestScene : CanvasLayer() {

	// ideally these would be Node2D or even Marker2D but getNodesInGroup() returns a VariantArray<Node>
	private lateinit var dropTargets: VariantArray<Node>
	private lateinit var card: Card

	@RegisterFunction
	override fun _ready() {
		card = getNodeAs<Card>("Card")!!
		dropTargets =
			getTree()?.getNodesInGroup("dropTargets".asCachedStringName()) ?: VariantArray<Node>()
	}

	@RegisterFunction
	override fun _process(delta: Double) {
		if (card.dragging) {
			dropTargets.forEach {
				if (it is Node2D) {
					if (card.position.distanceTo(it.position) < 150) {
						(it as DropTarget).select()
						// If the player stops dragging, then I want to change the start position of the card to this location
					} else {
						(it as DropTarget).deselect()
					}
				}
			}
		} else {
			dropTargets.forEach {
				if (it is Node2D) {
					(it as DropTarget).deselect()
				}
			}
		}
	}
}
