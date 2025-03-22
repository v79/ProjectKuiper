package hexgrid.map.editor

import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.GridContainer
import godot.api.Label
import godot.api.PackedScene
import godot.api.ResourceLoader
import godot.core.Color
import godot.core.Vector2
import godot.extension.getNodeAs
import hexgrid.Hex
import state.Location
import kotlin.math.cos

@RegisterClass
class HexMapGridEditor : GridContainer() {

    // Packed scenes
    private val hexScene = ResourceLoader.load("res://src/main/kuiper/hexgrid/Hex.tscn") as PackedScene

    // UI elements


    @RegisterFunction
    override fun _ready() {

        val startX = 150.0
        val startY = 150.0
//2 * (75.0 * sin(Math.PI / 2))
        var xStagger = 225.0
        var yStagger = 0.0
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
                hex.setPosition(Vector2((i * xStagger) + startX + rowStagger, (j * diameter) + startY + colStagger))
                hex.getNodeAs<Label>("%LocationLabel")!!.visible = false
                addChild(hex)
            }
        }

    }

    @RegisterFunction
    override fun _process(delta: Double) {

    }
}
