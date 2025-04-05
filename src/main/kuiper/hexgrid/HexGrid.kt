package hexgrid

import SignalBus
import actions.ActionCard
import actions.CardStatus
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.*
import godot.core.Vector2
import godot.core.asCachedStringName
import godot.core.connect
import godot.extension.getNodeAs
import godot.extension.instantiateAs
import hexgrid.map.editor.HexData
import state.GameState

@RegisterClass
class HexGrid : Control() {

    // Globals
    private val signalBus: SignalBus by lazy { getNodeAs("/root/Kuiper/SignalBus")!! }
    private lateinit var gameState: GameState

    // UI elements
    private var dropTargets: MutableList<HexDropTarget> = mutableListOf()
    private lateinit var hexGridContainer: Container
    private lateinit var gridPlacementContainer: HBoxContainer

    // Packed scenes
    private val hexScene = ResourceLoader.load("res://src/main/kuiper/hexgrid/Hex.tscn") as PackedScene

    private val hexes: MutableList<Hex> = mutableListOf()
    private var card: ActionCard? = null

    private val MAX_HEXES = 10
    private val GRID_COLUMNS = 5
    private val HEX_WIDTH = 200

    @RegisterFunction
    override fun _ready() {
        gameState = getNodeAs("/root/GameState")!!

        hexGridContainer = getNodeAs("%HexGridContainer")!!
        gridPlacementContainer = getNodeAs("%GridPlacementContainer")!!

        // calculate grid placement
        val gridPlacement = getGridPlacement()
        gridPlacementContainer.setPosition(gridPlacement)

        gameState.company.zones[0].hexes.forEachIndexed { index, hexData ->
            if (hexData.location.name.isNotEmpty()) {
                createHex(index, hexData)
            }
        }

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
    private fun createHex(i: Int, hexData: HexData) {
        val hex = hexScene.instantiateAs<Hex>()!!
        hex.id = i
        hex.hexData = hexData
        hex.hexUnlocked = hexData.location.unlocked
        val dropTarget = hex.getNodeAs<HexDropTarget>("%HexDropTarget")!!
        dropTarget.addToGroup("hexDropTargets".asCachedStringName())
        dropTarget.setName("HexDropTarget$i")
        dropTarget.hex = hex
        dropTargets.add(dropTarget)
        hex.marker = dropTarget
        hex.setName("Hex$i")
        val boxContainer = BoxContainer()
        boxContainer.setName("Hex${i}_BoxContainer")
        boxContainer.addChild(hex)
        boxContainer.setPosition(hexData.position)
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
                                dropTarget.hex?.highlight()
                                signalBus.cardOnHex.emit(dropTarget.hex!!)
                            } else {
                                signalBus.cardOffHex.emit(dropTarget.hex!!)
                                dropTarget.hex?.unhighlight()
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
