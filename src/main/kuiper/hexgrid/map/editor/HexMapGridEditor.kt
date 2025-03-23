package hexgrid.map.editor

import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.api.GridContainer
import godot.api.Label
import godot.api.PackedScene
import godot.api.ResourceLoader
import godot.core.Vector2
import godot.extension.getNodeAs
import hexgrid.Hex
import state.Location
import kotlin.math.sqrt

@RegisterClass
class HexMapGridEditor : GridContainer() {

    @RegisterProperty
    @Export
    var dimension: Int = 6
    
    // Packed scenes
    private val hexScene = ResourceLoader.load("res://src/main/kuiper/hexgrid/Hex.tscn") as PackedScene

    // UI elements

    // Data
    private var grid = Array(dimension) { Array(dimension) { Vector2.ZERO } }

    @RegisterFunction
    override fun _ready() {

        // set up the grid
        grid = calculateGridCoordinates(dimension, dimension)
        grid.forEachIndexed { i, row ->
            row.forEachIndexed { j, vector2 ->
                val hex = hexScene.instantiate() as Hex
                hex.location = Location("location $i $j")
                hex.setName("Hex_${i}_$j")
                hex.colour = godot.core.Color.mediumPurple
                hex.setPosition(vector2)
                hex.getNodeAs<Label>("%LocationLabel")!!.visible = false
                addChild(hex)
            }
        }

        /*//2 * (75.0 * sin(Math.PI / 2))
                var xSpacing = 225.0
                for (i in 0..5) {
                    for (j in 0..5) {
                        val rowStagger = if (j % 2 == 0) {
                            112.5
                        } else {
                            0.0
                        }
                        val colStagger = if (j % 2 == 0) {
                            0.0
                        } else {
                            -75.0
                        }
                        val hex = hexScene.instantiate() as Hex
                        val diameter = hex.newRadius * 2 * cos(Math.PI / 6)
                        hex.location = Location("location $i $j")
                        hex.setName("Hex_${i}_$j")
                        hex.colour = Color.mediumPurple
                        hex.setPosition(Vector2((i * xSpacing) + rowStagger, (j * diameter) + colStagger))
                        hex.getNodeAs<Label>("%LocationLabel")!!.visible = false
                        addChild(hex)
                    }
                }*/

    }

    @RegisterFunction
    override fun _process(delta: Double) {

    }

    private fun calculateGridCoordinates(xCount: Int, yCount: Int): Array<Array<Vector2>> {
        // flat topped, evenq orientation
        val hexCoords = Array(xCount) { Array<Vector2>(yCount) { Vector2.ZERO } }
        val radius = 75.0
        val diameter = radius * 2
        val width = diameter
        val height = sqrt(3.0) * radius
        val horizDistance = 3.0 / 2.0 * radius

        // we are calculating the coordinates of the centre of each hex
        for (i in 0 until xCount) {
            for (j in 0 until yCount) {
                val x = i * horizDistance
                val y = j * height + (i % 2) * (height / 2)
                hexCoords[i][j] = Vector2(x, y)
            }
        }
        return hexCoords
    }

    /*  fun axial_to_oddq(hex: Hex) {
          val col = hex.q
          val row = hex.r + (hex.q - (hex.q&1)) / 2
          return OffsetCoord(col, row)
      }

      fun oddq_to_axial(hex: Hex) {
          val q = hex.col
          val r = hex.row - (hex.col - (hex.col&1)) / 2
          return Hex(q, r)
      }*/
}
