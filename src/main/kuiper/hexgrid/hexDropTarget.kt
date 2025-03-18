package hexgrid

import godot.annotation.*
import godot.api.Marker2D
import godot.core.Color
import godot.core.VariantArray

/**
 * A HexDropTarget is the drop-target for a hexagon.
 * It currently renders the Hexagon but this will change later.
 */
@Tool
@RegisterClass
class HexDropTarget : Marker2D() {

    var hex: Hex? = null

    @RegisterProperty
    var isSelected = false

    @Export
    @RegisterProperty
    var drawInternals = false

    @Export
    @RegisterProperty
    var nameSectors = false

    // for debug
    @Export
    @RegisterProperty
    var numbered = false

    var fillTriangles: VariantArray<Boolean> = VariantArray()

    private var unlockedColor = Color(1.0, 1.0, 1.0, 1.0)
    private var lockedColor = Color(0.2, 0.2, 0.2, 1.0)
    private var highlightColor = Color(1.0, 0.8, 0.8, 1.0)
    var colour: Color = lockedColor // default to dark grey

    /**
     * Draw the hexagon using draw calls. To be replaced with artwork later.
     */
    @RegisterFunction
    override fun _draw() {
        /*    val a = 2 * Math.PI / 6
            val r = 100.0
            var p1 = Vector2(100.0, 0.0)
            val font = FontVariation()
            for (i in 1..6) {
                val p2 = Vector2(r * cos(a * i), r * sin(a * i))
                drawLine(
                    p1,
                    p2,
                    colour,
                    2.0f
                )
                if (fillTriangles[translateHexIdToSectorId(i - 1)]) {
                    val fillPolys = PackedVector2Array()
                    fillPolys.insert(0, Vector2(0.0, 0.0))
                    fillPolys.insert(1, p1)
                    fillPolys.insert(2, p2)
                    drawColoredPolygon(
                        fillPolys,
                        colour
                    )
                }
                if (nameSectors) {
                    drawChar(
                        font,
                        Vector2((0.0 + p1.x + p2.x) / 3, (0.0 + p1.y + p2.y) / 3),
                        translateHexIdToSectorId(i).toString(),
                        22,
                        Color.green
                    )
                }
                if (numbered) {
                    drawChar(
                        font,
                        Vector2((0.0 + p1.x + p2.x) / 3, (0.0 + p1.y + p2.y) / 3),
                        i.toString(),
                        22,
                        Color.green
                    )
                    // this draws 0 to 5, but we want 1 to 6
                    drawChar(
                        font,
                        Vector2((24.0 + p1.x + p2.x) / 3, (0.0 + p1.y + p2.y) / 3),
                        translateHexIdToSectorId(i).toString(),
                        22,
                        Color.red
                    )
                }
                p1 = p2
                if (drawInternals) {
                    drawLine(
                        Vector2(0.0, 0.0),
                        p2,
                        colour,
                        1.0f
                    )
                }
            }

         */
    }

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
        fillTriangles.resize(6)
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
