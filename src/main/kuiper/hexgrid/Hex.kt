package hexgrid

import LogInterface
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.api.*
import godot.core.*
import godot.extension.getNodeAs
import hexgrid.map.editor.HexData
import hexgrid.map.editor.MapEditorSignalBus
import state.SectorStatus
import kotlin.math.cos
import kotlin.math.sin

/**
 * A Hex is a Godot node that represents a location in the world/region map.
 * See [HexData] for the game representation of the same concept, not tied to the Godot Node
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
    var hexRadius: Double = 75.0

    @RegisterProperty
    var hexMode: HexMode = HexMode.NORMAL

    @RegisterProperty
    @Export
    var editorSignalBus: MapEditorSignalBus? = null

    // UI elements
    private val collisionShape2D: CollisionPolygon2D by lazy { getNodeAs("%CollisionShape2D")!! }
    lateinit var marker: HexDropTarget
    var hexData: HexData? = null

    // Packed scenes
    private val sectorScene = ResourceLoader.load("res://src/main/kuiper/hexgrid/sector_segment.tscn") as PackedScene

    // Data
    var row: Int = 0
    var col: Int = 0

    private lateinit var pointSet: Map<Int, Triple<Vector2, Vector2, Vector2>>
    private var unlockedColor = Color(1.0, 1.0, 1.0, 1.0)
    private var lockedColor = Color(0.2, 0.2, 0.2, 1.0)
    private var highlightColor = Color(1.0, 0.8, 0.8, 1.0)
    var colour: Color = unlockedColor
    var isConfirmationDialog: Boolean = false
    val fillTriangles: BooleanArray = BooleanArray(6) { false }

    @RegisterFunction
    override fun _ready() {
        if (!hexUnlocked) {
            colour = lockedColor
        }
        pointSet = calculateVerticesForHex(radius = hexRadius.toFloat())
        val packedArray = PackedVector2Array(
            pointSet.values.flatMap { listOf(it.first, it.second) }.distinct().toVariantArray()
        )
        collisionShape2D.polygon = packedArray
        pointSet.forEach { (index, triangle) ->
            val segment = sectorScene.instantiate() as SectorSegment
            segment.setName("Sector${index - 1}")
            segment.setTextureRepeat(CanvasItem.TextureRepeat.TEXTURE_REPEAT_DISABLED)
            segment.location = hexData?.location
            segment.isConfirmationDialog = isConfirmationDialog
            segment.status = hexData?.location?.sectors?.get(index - 1)?.status ?: SectorStatus.EMPTY
            addChild(segment)
            segment.updateUI(
                index - 1,
                PackedVector2Array(triangle.toList().toVariantArray()),
                hexData?.location?.getBuilding(index - 1)
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

        var p1 = Vector2(hexRadius, 0.0)
        for (i in 1..6) {
            val p2 = Vector2(hexRadius * cos(a * i), hexRadius * sin(a * i))
            drawLine(
                p1, p2, colour, 2.0f
            )
            if (fillTriangles.isNotEmpty()) {
                if (fillTriangles[i - 1]) {
                    val fillPolys = PackedVector2Array()
                    fillPolys.insert(0, Vector2(0.0, 0.0))
                    fillPolys.insert(1, p1)
                    fillPolys.insert(2, p2)
                    drawColoredPolygon(
                        fillPolys,
                        colour
                    )
                }
            }
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
        if (hexMode != HexMode.CARD) {
            colour = highlightColor
            if (hexMode == HexMode.EDITOR) {
                zIndex += 1
            }
            queueRedraw()
        }
    }

    @RegisterFunction
    fun unhighlight() {
        if (hexMode != HexMode.CARD) {
            colour = if (hexUnlocked) unlockedColor else lockedColor
            if (hexMode == HexMode.EDITOR) {
                zIndex -= 1
            }
            queueRedraw()
        }
    }

    @RegisterFunction
    fun redraw() {
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
        if (hexMode == HexMode.EDITOR) {
            highlight()
        }
    }

    @RegisterFunction
    fun onMouseExited() {
        if (hexMode == HexMode.EDITOR) {
            unhighlight()
        }
    }

    /**
     * This is only relevant in the editor mode.
     */
    @RegisterFunction
    fun onGuiInput(viewport: Node, event: InputEvent?, shapeIdx: Int) {
        // check for clicks
        if (hexMode == HexMode.EDITOR) {
            event?.let { e ->
                if (e.isActionPressed("mouse_left_click".asCachedStringName())) {
                    editorSignalBus?.editor_placeHex?.emit(row, col)
                }
                if (e.isActionPressed("mouse_right_click".asCachedStringName())) {
                    editorSignalBus?.editor_clearHex?.emit(row, col)
                }
            }
        }
    }
}

/**
 * The Hex class is used quite differently in the game map and the editor.
 * So this enum is used to differentiate between the two modes.
 */
enum class HexMode {
    NORMAL,
    EDITOR,
    CARD
}

