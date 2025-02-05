package hexgrid

import godot.FontVariation
import godot.Marker2D
import godot.annotation.*
import godot.core.Color
import godot.core.PackedVector2Array
import godot.core.VariantArray
import godot.core.Vector2
import godot.global.GD
import kotlin.math.cos
import kotlin.math.sin

/**
 * A HexDropTarget is the drop-target for a hexagon.
 * It currently renders the Hexagon but this will change later.
 */
@Tool
@RegisterClass
class HexDropTarget : Marker2D() {

	@RegisterProperty
	var isSelected = false

	@Export
	@RegisterProperty
	var drawInternals = false

	// for debug
	@Export
	@RegisterProperty
	var numbered = false

	@Export
	@RegisterProperty
	var fillTriangles: VariantArray<Boolean> = VariantArray()

	@Export
	@RegisterProperty
	var colour: Color = Color(1.0, 1.0, 1.0, 1.0)
	private var originalColor: Color = colour

	var hex = Hex()

	companion object {
		const val HEX_RADIUS = 50.0
	}

	/**
	 * Draw the hexagon using draw calls. To be replaced with artwork later.
	 */
	@RegisterFunction
	override fun _draw() {
		val a = 2 * Math.PI / 6
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
			if (numbered) {
				drawChar(font, Vector2((0.0 + p1.x + p2.x) / 3, (0.0 + p1.y + p2.y) / 3), i.toString(), 22, Color.black)
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
		GD.print("Highlighting dropTarget")
		colour = Color(1.0, 0.8, 0.8, 1.0)
		queueRedraw()
		isSelected = true
	}

	@RegisterFunction
	fun unhighlight() {
		colour = originalColor
		queueRedraw()
		isSelected = false
	}
}
