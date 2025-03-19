package hexgrid

import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.annotation.Tool
import godot.api.Marker2D

/**
 * A HexDropTarget is the drop-target for a hexagon.
 * Soon to be @Deprecated?
 */
@Tool
@RegisterClass
class HexDropTarget : Marker2D() {

    var hex: Hex? = null

    @RegisterProperty
    var isSelected = false

    /**
     * Translate the hexagon ID to a sector ID
     * Visually, sector IDs start in the 'top left' triangle and go clockwise
     * But hex IDs start 3 triangles on (roughly at 4 o'clock) and go clockwise
     */
    private fun translateHexIdToSectorId(hexId: Int): Int {
        return (hexId + 3) % 6
    }

    @RegisterFunction
    override fun _ready() {
    }


    @RegisterFunction
    override fun _process(delta: Double) {
    }

    @RegisterFunction
    fun highlight() {
        isSelected = true
    }

    @RegisterFunction
    fun unhighlight() {
        isSelected = false
    }
}
