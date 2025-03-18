package hexgrid

import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.api.*
import godot.core.*
import godot.extension.getNodeAs
import godot.global.GD
import state.Location
import kotlin.math.cos
import kotlin.math.sin

/**
 * A Hex represents a location in the world/region map.
 * A hex has six internal triangles, each of which represents a site where a facility can be built.
 * A facility may span more than one site/triangle
 * There are two special Hexes - the company HQ, and the space launch centre
 */
@RegisterClass
class Hex : Node2D() {

    @Export
    @RegisterProperty
    var id: Int = 0

    @Export
    @RegisterProperty
    var hexUnlocked: Boolean = false

    @Export
    @RegisterProperty
    var newRadius: Double = 75.0

    var triangles = Array(6) { it }
    var points = VariantArray<Vector2>()

    // UI elements
    private lateinit var locationLabel: Label

    lateinit var marker: HexDropTarget
    lateinit var location: Location

    private val sectorScene = ResourceLoader.load("res://src/main/kuiper/hexgrid/sector_segment.tscn") as PackedScene
    private val triangleScene = ResourceLoader.load("res://src/main/kuiper/hexgrid/triangle.tscn") as PackedScene
    private val newTriangles = Array<Triangle?>(6) { null }

    private lateinit var textEdit: TextEdit
    private lateinit var pointSet: Map<Int, Triple<Vector2, Vector2, Vector2>>

    @RegisterFunction
    override fun _ready() {
        locationLabel = getNodeAs("%LocationLabel")!!
        textEdit = getNodeAs("TextEdit")!!

        pointSet = calculateVerticesForHex(radius = 125.0f)

        pointSet.forEach { (index, triangle) ->
            val polygon = Polygon2D()
            polygon.setName("Triangle$index")
            polygon.color = Color(GD.randf(), GD.randf(), GD.randf())
            polygon.polygon = PackedVector2Array(triangle.toList().toVariantArray())

            addChild(polygon)
        }
    }

    @RegisterFunction
    override fun _process(delta: Double) {
        /*newTriangles.forEachIndexed { index, triangle ->
            val angleDeg = 60 * index - 30
            val angleRad = Math.PI / 180 * angleDeg
            val newX = newRadius * cos(angleRad)
            val newY = newRadius * sin(angleRad)
            triangle?.positionMutate {
                x = newX
                y = newY
            }
        }*/
    }

    @RegisterFunction
    fun _on_TextEdit_text_changed() {
        val newText = textEdit.text
        newRadius = newText.toDoubleOrNull() ?: 75.0
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
            GD.print("Triangle $i: $p1, $p2, $p3")
            p1 = p2
        }
        return pointSet.toMap()
    }

    @Deprecated("Use calculateVerticesForHex instead")
    private fun hexThroughTriangles() {
        val triangleSideLength = 75.0 // Replace with your triangle's side length
        val radius = (triangleSideLength * kotlin.math.sqrt(3.0)) / 3.0

        for (i in 0 until 6) {
            val triangle = triangleScene.instantiate() as Triangle
            triangle.setName("Triangle$i")

            // Calculate the position for each triangle
            val angleDeg = 60 * i - 30
            val angleRad = Math.PI / 180 * angleDeg
            val x = radius * cos(angleRad)
            val y = radius * sin(angleRad)

            triangle.position = Vector2(x, y)
            triangle.rotationDegrees = (60.0 * i).toFloat()
            triangle.scale = Vector2(1.0, 1.0)
            triangle.zIndex = 1
            triangle.color = Color(1.0 / (i + 1), 1.0, 1.0, 1.0)
            newTriangles[i] = triangle
            addChild(triangle)
        }
    }
    // possible functions:
    // - buildFacility(size: Int, site: Int)
    // - removeFacility(size: Int, site: Int)
}
