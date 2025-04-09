package hexgrid

import LogInterface
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
import state.Building
import state.GameState
import state.Location
import state.SectorStatus

@RegisterClass
class HexGrid : Control(), LogInterface {

    override var logEnabled: Boolean = true

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

        gameState.company.zones[0].hexes.forEachIndexed { index, location ->
            if (location.name.isNotEmpty()) {
                createHex(index, location)
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

        signalBus.updateHex.connect { buildingActionWrapper ->
            if (buildingActionWrapper.location != null && buildingActionWrapper.building != null && buildingActionWrapper.sectorIds != null && buildingActionWrapper.sectorStatus != null) {
                updateHex(
                    buildingActionWrapper.location!!,
                    buildingActionWrapper.building!!,
                    buildingActionWrapper.sectorIds!!,
                    buildingActionWrapper.sectorStatus!!
                )
            } else {
                logError("HexGrid: updateHex signal received null data")
            }
        }
    }

    /**
     * Create a hexagon and add it to the grid
     * Assign the hexagon's drop target to the group "hexDropTargets"
     */
    private fun createHex(i: Int, location: Location) {
        val hex = hexScene.instantiateAs<Hex>()!!
        hex.signalBus = signalBus
        hex.id = i
        hex.location = location
        hex.hexUnlocked = location.unlocked
        hex.row = location.row
        hex.col = location.column
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
        boxContainer.setPosition(location.position)
        hexGridContainer.addChild(boxContainer)
        hexes.add(hex)
    }

    @RegisterFunction
    override fun _process(delta: Double) {
        card?.let { card ->
            if (card.status == CardStatus.DRAGGING) {
                for (dropTarget in dropTargets) {
                    if (dropTarget.hex != null) {
                        if (dropTarget.hex!!.hexUnlocked) {
                            if (card.globalPosition.distanceTo(dropTarget.globalPosition) <= card.clickRadius / 2) {
                                dropTarget.hex?.highlight()
                                signalBus.cardOnHex.emit(dropTarget.hex!!)
                                // if we're on a hex, stop looping because an adjacent/overlapping hexes trigger the offHex signal too
                                break
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

    @RegisterFunction
    fun updateHex(newLocationData: Location, building: Building, sectorIds: IntArray, sectorStatus: SectorStatus) {
        log("Updating hex with new data: $newLocationData")
        val hexToUpdate =
            hexes.firstOrNull { it.row == newLocationData.row && it.col == newLocationData.column }
        if (hexToUpdate == null) {
            logError("HexGrid: No hex found at row ${newLocationData.row} and column ${newLocationData.column} in hexes")
            return
        }
        hexToUpdate.location = newLocationData
        sectorIds.forEach { sectorId ->
            log("Updating sector $sectorId with building ${building.name}")
            hexToUpdate.segments[sectorId].status = sectorStatus
            hexToUpdate.segments[sectorId].updateUI(
                sectorId, hexToUpdate.segments[sectorId].polygon, building
            )
        }
    }

    // Calculate the position of the grid
    private fun getGridPlacement(): Vector2 {
        val gridWidth = GRID_COLUMNS * HEX_WIDTH
        val xOffset = (signalBus.screenWidth - gridWidth) / 2
        val yOffset = 150
        return Vector2(xOffset, yOffset)
    }
}
