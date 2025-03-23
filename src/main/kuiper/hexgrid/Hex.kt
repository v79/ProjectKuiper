package hexgrid

import LogInterface
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.api.*
import godot.core.Color
import godot.core.PackedVector2Array
import godot.core.Vector2
import godot.core.toVariantArray
import godot.extension.getNodeAs
import state.Location
import state.SectorStatus
import kotlin.math.cos
import kotlin.math.sin

/**
 * A Hex represents a location in the world/region map.
 * A hex has six internal triangles, each of which represents a site where a facility can be built.
 * A facility may span more than one sector/triangle
 * There are two special Hexes - the company HQ, and the space launch centre
 */
@RegisterClass
class Hex : Node2D(), LogInterface {

    override var logEnabled: Boolean = true

    @Export
    @RegisterProperty
    var id: Int = 0

    @Export
    @RegisterProperty
    var hexUnlocked: Boolean = false

    @Export
    @RegisterProperty
    var newRadius: Double = 75.0

    // UI elements
    private val locationLabel: Label by lazy { getNodeAs("%LocationLabel")!! }
    private val area2D: Area2D by lazy { getNodeAs<Area2D>("%Area2D")!! }
    private val collisionShape2D: CollisionPolygon2D by lazy { getNodeAs("%CollisionShape2D")!! }
    lateinit var marker: HexDropTarget
    var location: Location? = null

    // Packed scenes
    private val sectorScene = ResourceLoader.load("res://src/main/kuiper/hexgrid/sector_segment.tscn") as PackedScene

    // Data
    var q: Int = 0
    var r: Int = 0
    var row: Int = 0
    var col: Int = 0

    private lateinit var pointSet: Map<Int, Triple<Vector2, Vector2, Vector2>>
    private var unlockedColor = Color(1.0, 1.0, 1.0, 1.0)
    private var lockedColor = Color(0.2, 0.2, 0.2, 1.0)
    private var highlightColor = Color(1.0, 0.8, 0.8, 1.0)
    var colour: Color = unlockedColor
    var isConfirmationDialog: Boolean = false

    @RegisterFunction
    override fun _ready() {
        if (!hexUnlocked) {
            colour = lockedColor
        }
        pointSet = calculateVerticesForHex(radius = newRadius.toFloat())
        val packedArray = PackedVector2Array(
            pointSet.values.flatMap { listOf(it.first, it.second) }.distinct().toVariantArray()
        )
        collisionShape2D.polygon = packedArray
        // for math reasons, the vertices are 1-indexed, but the sectors are 0-indexed
        pointSet.forEach { (index, triangle) ->
            val segment = sectorScene.instantiate() as SectorSegment
            segment.setName("Sector${index - 1}")
            segment.setTextureRepeat(CanvasItem.TextureRepeat.TEXTURE_REPEAT_DISABLED)
            segment.location = location
            segment.isConfirmationDialog = isConfirmationDialog
            segment.status = location?.sectors?.get(index - 1)?.status ?: SectorStatus.EMPTY
            addChild(segment)
            segment.updateUI(
                index - 1, PackedVector2Array(triangle.toList().toVariantArray()), location?.getBuilding(index - 1)
            )
        }
    }

    @RegisterFunction
    override fun _process(delta: Double) {
    }

    /**
     * Draw the hexagon outline
     */
    @RegisterFunction
    override fun _draw() {
        val drawInternals = true
        val a = 2 * Math.PI / 6

        var p1 = Vector2(newRadius, 0.0)
        for (i in 1..6) {
            val p2 = Vector2(newRadius * cos(a * i), newRadius * sin(a * i))
            drawLine(
                p1, p2, colour, 2.0f
            )
            p1 = p2
            if (drawInternals) {
                drawLine(
                    Vector2(0.0, 0.0), p2, colour, 1.0f
                )
            }
        }
    }

    @RegisterFunction
    fun highlight() {
        colour = highlightColor
        zIndex += 1
        queueRedraw()
    }

    @RegisterFunction
    fun unhighlight() {
        colour = if (hexUnlocked) unlockedColor else lockedColor
        zIndex -= 1
        queueRedraw()
    }

    /**
     * Calculate the vertices for a hexagon, as a map of triangle index to the three vertices of the triangle
     */
    private fun calculateVerticesForHex(radius: Float): Map<Int, Triple<Vector2, Vector2, Vector2>> {
        val a = 2 * Math.PI / 6
        val pointSet: MutableMap<Int, Triple<Vector2, Vector2, Vector2>> = mutableMapOf()
        var p1 = Vector2(radius, 0)
        val p3 = Vector2(0.0, 0.0)
        for (i in 1..6) {
            val p2 = Vector2(radius * cos(a * i), radius * sin(a * i))
            pointSet[i] = Triple(p1, p2, p3)
            p1 = p2
        }
        return pointSet.toMap()
    }

    @RegisterFunction
    fun onMouseEntered() {
        highlight()
    }

    @RegisterFunction
    fun onMouseExited() {
        unhighlight()
    }

    // possible functions:
    // - buildFacility(size: Int, site: Int)
    // - removeFacility(size: Int, site: Int)
}
