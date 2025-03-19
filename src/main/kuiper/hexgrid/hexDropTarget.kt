package hexgrid

import godot.annotation.*
import godot.api.Marker2D
import godot.core.Color

/**
 * A HexDropTarget is the drop-target for a hexagon.
 */
@Tool
@RegisterClass
class HexDropTarget : Marker2D() {

    var hex: Hex? = null

    @RegisterProperty
    var isSelected = false

    @Export
    @RegisterProperty
    var nameSectors = false

    // for debug
    @Export
    @RegisterProperty
    var numbered = false

    private var unlockedColor = Color(1.0, 1.0, 1.0, 1.0)
    private var lockedColor = Color(0.2, 0.2, 0.2, 1.0)
    private var highlightColor = Color(1.0, 0.8, 0.8, 1.0)
    var colour: Color = lockedColor // default to dark grey

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
        colour = highlightColor
        isSelected = true
        queueRedraw()
    }

    @RegisterFunction
    fun unhighlight() {
        colour = if (hex?.hexUnlocked == true) unlockedColor else lockedColor
        isSelected = false
        queueRedraw()
    }
}
