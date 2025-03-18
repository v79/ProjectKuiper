package hexgrid

import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.api.*
import godot.core.Color
import godot.core.VariantArray
import godot.core.Vector2
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

    @RegisterFunction
    override fun _ready() {
        locationLabel = getNodeAs("%LocationLabel")!!
        textEdit = getNodeAs("TextEdit")!!

        // Arrange these six triangles in a hexagon
// r = (s * sqrt(3)) / 3
// r = 125 * sqrt(3) / 3
// r = 125 * 1.732 / 3
// r = 72.168
//        val radius = 150 * 0.577

        val triangleSideLength = 75.0 // Replace with your triangle's side length
        val radius = (triangleSideLength * kotlin.math.sqrt(3.0)) / 3.0
//        val radius = 72.168

        for (i in 0 until 6) {
            /*
                        val segment = sectorScene.instantiate() as SectorSegment
                        segment.setName("SectorSegment$i")
                        val triangle = segment.getNodeAs<Triangle>("%Triangle")!!
                        triangle.setName("Triangle$i")
            */
            val triangle = triangleScene.instantiate() as Triangle
            triangle.setName("Triangle$i")

            // Calculate the position for each triangle
            val angleDeg = 60 * i - 30
            val angleRad = Math.PI / 180 * angleDeg
            val x = radius * cos(angleRad)
            val y = radius * sin(angleRad)

            GD.print("placing triangle $i at $x, $y")
            // placing triangle 0 at 37.5, -21.650635094610962
            // placing triangle 1 at 37.5, 21.650635094610962
            // placing triangle 2 at 2.651438096812267E-15, 43.30127018922193
            // placing triangle 3 at -37.5, 21.650635094610962
            // placing triangle 4 at -37.5, -21.65063509461097
            // placing triangle 5 at -7.954314290436801E-15, -43.30127018922193

            triangle.position = Vector2(x, y)
            triangle.rotationDegrees = (60.0 * i).toFloat()
            triangle.scale = Vector2(1.0, 1.0)
            triangle.zIndex = 1
            triangle.color = Color(1.0 / (i + 1), 1.0, 1.0, 1.0)
            newTriangles[i] = triangle
            addChild(triangle)
        }

        /* points.resize(6)
         val hexSize = 100.0
         for (i in 0 until 6) {
             val angleDeg = 60 * i - 30
             val angleRad = Math.PI / 180 * angleDeg
             val x = position.x + hexSize * Math.cos(angleRad)
             val y = position.y + hexSize * Math.sin(angleRad)
             points[i] = Vector2(x, y)

             polygon = PackedVector2Array(points)
             color = Color.indianRed
         }*/

    }

    @RegisterFunction
    override fun _process(delta: Double) {
        newTriangles.forEachIndexed { index, triangle ->
            val angleDeg = 60 * index - 30
            val angleRad = Math.PI / 180 * angleDeg
            val newX = newRadius * cos(angleRad)
            val newY = newRadius * sin(angleRad)
            triangle?.positionMutate {
                x = newX
                y = newY
            }
        }
    }

    @RegisterFunction
    fun _on_TextEdit_text_changed() {
        val newText = textEdit.text
        newRadius = newText.toDoubleOrNull() ?: 75.0
    }

    // possible functions:
    // - buildFacility(size: Int, site: Int)
    // - removeFacility(size: Int, site: Int)
}
