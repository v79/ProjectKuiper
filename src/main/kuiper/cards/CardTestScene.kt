package cards

import godot.CanvasLayer
import godot.InputEvent
import godot.InputEventMouseButton
import godot.Label
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.core.VariantArray
import godot.core.asCachedStringName
import godot.core.toVariantArray
import godot.extensions.getNodeAs
import src.main.kuiper.cards.DropTarget

@RegisterClass
class CardTestScene : CanvasLayer() {

	// ideally these would be Node2D or even Marker2D but getNodesInGroup() returns a VariantArray<Node>
	private lateinit var dropTargets: VariantArray<DropTarget>
	private lateinit var card: Card
	private lateinit var label: Label

	@RegisterFunction
	override fun _ready() {
		card = getNodeAs("Card")!!
		label = getNodeAs("Label")!!
		dropTargets =
			getTree()?.getNodesInGroup("dropTargets".asCachedStringName())?.map { it as DropTarget }?.toVariantArray()
				?: VariantArray<DropTarget>()
	}

	@RegisterFunction
	override fun _input(event: InputEvent?) {
		if (event != null) {
			if (event is InputEventMouseButton && !event.isPressed()) {
				dropTargets.forEach {
					if (it.isSelected) {
						// dropped card over a target. It now lives here
						// emit a signal to say that the card has been applied to the target
						card.startPosition = it.position
						card.dragging = false
					}
				}
			}
		}
	}

	@RegisterFunction
	override fun _process(delta: Double) {
		label.text = "Selected targets: ${dropTargets.filter { it.isSelected }.joinToString { it.name.toString() }}"
		if (card.dragging) {
			dropTargets.forEach {
				if (card.position.distanceTo(it.position) < 150) {
					it.apply {
						select()
					}
				} else {
					it.apply {
						deselect()
					}
				}
			}
		} else {
			dropTargets.forEach {
				it.deselect()
			}
		}
	}
}
