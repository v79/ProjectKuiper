package hexgrid

import LogInterface
import SignalBus
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.api.*
import godot.core.*
import godot.extension.getNodeAs
import hexgrid.map.editor.MapEditorSignalBus
import state.Location
import state.SectorStatus
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * A Hex is a Godot node that represents a location in the world/region map.
 * See [Location] for the game representation of the same concept, not tied to the Godot Node
 * A hex has six internal triangles, each of which represents a site where a facility can be built.
 * A facility may span more than one sector/triangle
 * There are two special Locations - the company HQ, and the space launch centre
 */
@RegisterClass
class Hex : Node2D(), LogInterface {

    override var logEnabled: Boolean = true

    @Export
    @RegisterProperty
    var id: Int = 0

    @RegisterProperty
    var hexMode: HexMode = HexMode.LOCKED

    @RegisterProperty
    @Export
    var editorSignalBus: MapEditorSignalBus? = null

    @RegisterProperty
    var signalBus: SignalBus? = null

    // UI elements
    private val collisionShape2D: CollisionPolygon2D by lazy { getNodeAs("%CollisionShape2D")!! }
    lateinit var marker: HexDropTarget

    // Packed scenes
    private val sectorScene = ResourceLoader.load("res://src/main/kuiper/hexgrid/sector_segment.tscn") as PackedScene

    // Data
    var row: Int = 0
    var col: Int = 0

    private lateinit var pointSet: Map<Int, Triple<Vector2, Vector2, Vector2>>
    var location: Location? = null
    var isConfirmationDialog: Boolean = false
    val fillTriangles: BooleanArray = BooleanArray(6) { false }
    private val _segments: MutableList<SectorSegment> = mutableListOf()
    val segments: List<SectorSegment>
        get() = _segments.toList()

    companion object {
        const val HEX_RADIUS = 100.0
        val HIGHLIGHT_COLOR = Color.pink
    }


    @RegisterFunction
    override fun _ready() {
        pointSet = calculateVerticesForHex(radius = HEX_RADIUS.toFloat())
        val packedArray = PackedVector2Array(
            pointSet.values.flatMap { listOf(it.first, it.second) }.distinct().toVariantArray()
        )
        collisionShape2D.polygon = packedArray
        pointSet.forEach { (index, triangle) ->
            // the editor doesn't need sector segments; only draw if the editor signal bus is null
            if (editorSignalBus == null) {
                // this loop creates one more segment than we need, for some reason; ignore the one with no location
                if (this.location != null) {
                    val segment = sectorScene.instantiate() as SectorSegment
                    segment.signalBus = signalBus
                    segment.setName("Sector${index - 1}")
                    segment.setTextureRepeat(TextureRepeat.DISABLED)
                    segment.location = location
                    segment.isConfirmationDialog = isConfirmationDialog
                    segment.status = location?.sectors?.get(index - 1)?.status ?: SectorStatus.EMPTY
                    addChild(segment)
                    _segments.add(segment)
                    segment.updateUI(
                        index - 1,
                        PackedVector2Array(triangle.toList().toVariantArray()),
                        location?.getBuilding(index - 1)
                    )
                }
            }
        }
    }

    @RegisterFunction
    override fun _process(delta: Double) {
    }

    /**
     * Draw the hexagon outline and the triangles inside it
     */
    @RegisterFunction
    override fun _draw() {
        val drawInternals = true
        val a = 2 * Math.PI / 6

        var p1 = Vector2(HEX_RADIUS, 0.0)
        for (i in 1..6) {
            val p2 = Vector2(HEX_RADIUS * cos(a * i), HEX_RADIUS * sin(a * i))
            drawLine(
                p1, p2, hexMode.color, 2.0f
            )
            if (fillTriangles.isNotEmpty()) {
                if (fillTriangles[i - 1]) {
                    val fillPolys = PackedVector2Array()
                    fillPolys.insert(0, Vector2(0.0, 0.0))
                    fillPolys.insert(1, p1)
                    fillPolys.insert(2, p2)
                    drawColoredPolygon(
                        fillPolys,
                        hexMode.color
                    )
                }
            }
            p1 = p2
            if (drawInternals) {
                drawLine(
                    Vector2(0.0, 0.0), p2, hexMode.color, 1.0f
                )
            }
        }
    }

    @RegisterFunction
    fun highlight() {
        when (hexMode) {
            HexMode.EDITOR_BLANK -> {
                selfModulate = HIGHLIGHT_COLOR
                zIndex += 1
            }

            HexMode.EDITOR_LOCATION_SET -> {
                selfModulate = HIGHLIGHT_COLOR
                zIndex += 1
            }

            HexMode.LOCKED -> {
                // do nothing
            }

            HexMode.ACTIVE -> {
                selfModulate = HIGHLIGHT_COLOR
            }

            else -> {
                // do nothing
            }
        }
    }

    @RegisterFunction
    fun unhighlight() {
        if (hexMode != HexMode.CARD) {
            selfModulate = Color.white
            if (hexMode == HexMode.EDITOR_BLANK || hexMode == HexMode.EDITOR_LOCATION_SET) {
                zIndex = max(zIndex - 1, 0)
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
        if (hexMode == HexMode.EDITOR_BLANK || hexMode == HexMode.EDITOR_LOCATION_SET) {
            highlight()
        }
    }

    @RegisterFunction
    fun onMouseExited() {
        if (hexMode == HexMode.EDITOR_BLANK || hexMode == HexMode.EDITOR_LOCATION_SET) {
            unhighlight()
        }
    }

    /**
     * This is only relevant in the editor mode.
     */
    @RegisterFunction
    fun onGuiInput(viewport: Node, event: InputEvent?, shapeIdx: Int) {
        // check for clicks
        if (hexMode == HexMode.EDITOR_BLANK || hexMode == HexMode.EDITOR_LOCATION_SET) {
            event?.let { e ->
                if (e.isActionPressed("mouse_left_click".asCachedStringName())) {
                    editorSignalBus?.editor_placeHex?.emit(col, row)
                }
                if (e.isActionPressed("mouse_right_click".asCachedStringName())) {
                    editorSignalBus?.editor_clearHex?.emit(col, row)
                }
            }
        }
    }
}

/**
 * The Hex class is used quite differently in the game map and the editor.
 * So this enum is used to differentiate between the different modes, and affects how the hex is drawn and interacted with
 */
enum class HexMode(val color: Color) {
    EDITOR_BLANK(Color(0.3f, 0.3f, 0.3f, 1.0f)),
    EDITOR_LOCATION_SET(Color(1.0f, 1.0f, 1.0f, 0.5f)),
    LOCKED(Color(0.3f, 0.3f, 0.3f, 1.0f)),
    ACTIVE(Color(1.0f, 1.0f, 1.0f, 1.0f)),
    CARD(Color(1.0f, 1.0f, 1.0f, 1.0f)),
}

