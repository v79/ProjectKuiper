package hexgrid

import SignalBus
import actions.ActionCard
import actions.CardStatus
import godot.*
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.core.VariantArray
import godot.core.Vector2
import godot.core.asCachedStringName
import godot.core.connect
import godot.extensions.getNodeAs
import godot.extensions.instantiateAs
import godot.global.GD

@RegisterClass
class HexGrid : Control() {

	// Globals
	private lateinit var signalBus: SignalBus

	// UI elements
	private var dropTargets: VariantArray<HexDropTarget> = VariantArray()
	private lateinit var hexGridContainer: GridContainer
	private lateinit var gridPlacementContainer: HBoxContainer

	// Packed scenes
	private val hexScene =
		ResourceLoader.load("res://src/main/kuiper/hexgrid/Hex.tscn") as PackedScene

	private val hexes: MutableList<Hex> = mutableListOf()
	private var card: ActionCard? = null

	private val MAX_HEXES = 10
	private val GRID_COLUMNS = 5
	private val HEX_WIDTH = 200

	@RegisterFunction
	override fun _ready() {
		signalBus = getNodeAs("/root/SignalBus")!!
		hexGridContainer = getNodeAs("%HexGridContainer")!!
		gridPlacementContainer = getNodeAs("%GridPlacementContainer")!!

		// calculate grid placement
		val gridPlacement = getGridPlacement()
		gridPlacementContainer.setPosition(gridPlacement)

		for (i in 0 until MAX_HEXES) {
			GD.print("Creating hex $i")
			createHex(i)
		}
		// set up the HQ hex, which is unlocked and has a label
		assert(hexes.size >= 3) {
			"Not enough hexes created"
		}
		val hqHex = hexes[2]
		hqHex.hexUnlocked = true
		hqHex.locationName = "HQ"
		hqHex.marker.queueRedraw()

		// connect to card dragging signals
		signalBus.draggingCard.connect { card ->
			this.card = card
		}
		signalBus.droppedCard.connect {
			this.card = null
		}
		signalBus.onScreenResized.connect { _, _ ->
			// recalculate grid placement
			val newPlacement = getGridPlacement()
			gridPlacementContainer.setPosition(newPlacement)
		}
	}

	/**
	 * Create a hexagon and add it to the grid
	 * Assign the hexagon's drop target to the group "hexDropTargets"
	 */
	private fun createHex(i: Int, unlocked: Boolean = false) {
		val hex = hexScene.instantiateAs<Hex>()!!
		hex.id = i
		hex.locationName = "Hex $i"
		hex.hexUnlocked = unlocked
		val dropTarget = hex.getNodeAs<HexDropTarget>("%HexDropTarget")!!
		dropTarget.addToGroup("hexDropTargets".asCachedStringName())
		dropTarget.setName("HexDropTarget$i")
		dropTarget.hex = hex
		dropTargets.add(dropTarget)
		hex.marker = dropTarget
		hex.setName("Hex$i")
		val label = hex.getNodeAs<Label>("%LocationLabel")!!
		val boxContainer = BoxContainer()
		boxContainer.addChild(hex)
		label.text = hex.locationName
		hexGridContainer.addChild(boxContainer)
		hexes.add(hex)
	}

	// Calculate the position of the grid
	private fun getGridPlacement(): Vector2 {
		val gridWidth = GRID_COLUMNS * HEX_WIDTH
		val xOffset = (signalBus.screenWidth - gridWidth) / 2
		val yOffset = 150
		return Vector2(xOffset, yOffset)
	}

	@RegisterFunction
	override fun _process(delta: Double) {
		card?.let { card ->
			if (card.status == CardStatus.DRAGGING) {
				for (dropTarget in dropTargets) {
					if (dropTarget.hex != null) {
						if (dropTarget.hex!!.hexUnlocked) {
							if (card.globalPosition.distanceTo(dropTarget.globalPosition) < card.clickRadius / 2) {
								dropTarget.highlight()
								signalBus.cardOnHex.emit(dropTarget.hex!!)
							} else {
								signalBus.cardOffHex.emit(dropTarget.hex!!)
								dropTarget.unhighlight()
							}
						}
					}
				}
			}
		} ?: run {
			for (dropTarget in dropTargets) {
				// This redraws every frame, which is inefficient, so only do it for unlocked hexes. Tiny saving.
				dropTarget.hex?.let {
					if (it.hexUnlocked) {
						dropTarget.unhighlight()
					}
				}
			}
		}
	}
}
