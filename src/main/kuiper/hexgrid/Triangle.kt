package hexgrid

import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.Polygon2D
import godot.core.PackedVector2Array
import godot.core.VariantArray
import godot.core.Vector2
import kotlin.math.cos
import kotlin.math.sin

@RegisterClass
class Triangle : Polygon2D() {

	private val radius: Int = 75
	private val points = VariantArray<Vector2>()

	@RegisterFunction
	override fun _ready() {
		points.resize(3)
		for (i in 1..3) {
			// angle of each vertex in radians
			// equates to 120 degrees between each vertex
			val angle = 2 * Math.PI / 3 * i
			val x = radius * cos(angle)
			val y = radius * sin(angle)
			points[i - 1] = Vector2(x, y)
		}
		polygon = PackedVector2Array(points)
//		rotationDegrees = -30.0f
	}
}
