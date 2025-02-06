package hexgrid

import SignalBus
import actions.ActionCard
import godot.Control
import godot.Node2D
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.core.VariantArray
import godot.core.asCachedStringName
import godot.core.connect
import godot.core.toVariantArray
import godot.extensions.getNodeAs

@RegisterClass
class HexGrid : Control() {

	private lateinit var signalBus: SignalBus
	private lateinit var dropTargets: VariantArray<HexDropTarget>
	private var card: ActionCard? = null

	@RegisterFunction
	override fun _ready() {
		signalBus = getNodeAs("/root/SignalBus")!!
		val dropNodes =
			getTree()?.getNodesInGroup("hexDropTargets".asCachedStringName())?.map { it as Node2D }?.toVariantArray()
				?: VariantArray<Node2D>()

		// find the dropTarget nodes
		dropTargets = dropNodes.map {
			it.getChild(0) as HexDropTarget
		}.toVariantArray()

		// connect to card dragging signals
		signalBus.draggingCard.connect { card ->
			this.card = card
		}
		signalBus.droppedCard.connect {
			this.card = null
		}
	}

	@RegisterFunction
	override fun _process(delta: Double) {
		card?.let { card ->
			if (card.dragging) {
				for (dropTarget in dropTargets) {
					if (card.globalPosition.distanceTo(dropTarget.globalPosition) < card.clickRadius) {
						dropTarget.highlight()
					} else {
						dropTarget.unhighlight()
					}
				}
			}
		} ?: run {
			for (dropTarget in dropTargets) {
				dropTarget.unhighlight()
			}
		}
	}
}
