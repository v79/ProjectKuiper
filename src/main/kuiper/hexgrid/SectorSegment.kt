package hexgrid

import LogInterface
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.Area2D
import godot.api.CollisionPolygon2D
import godot.api.Polygon2D
import godot.core.Color
import godot.core.PackedVector2Array
import godot.extension.getNodeAs

/**
 * A SectorSegment represents a triangle in a hexagon, or in game terms a sector within a hex that can be built on.
 */
@RegisterClass
class SectorSegment : Polygon2D(), LogInterface {

	override var logEnabled = true

	// UI elements
	lateinit var area2D: Area2D
	lateinit var collisionPolygon: CollisionPolygon2D

	// Data
	private var sectorId: Int = -1

	@RegisterFunction
	override fun _ready() {
	}

	@RegisterFunction
	override fun _process(delta: Double) {

	}

	@RegisterFunction
	fun create(id: Int, polygonPoints: PackedVector2Array, filled: Boolean = false) {
		collisionPolygon = getNodeAs("%CollisionPolygon2D")!!
		sectorId = id
		collisionPolygon.polygon = polygonPoints
		polygon = polygonPoints
		if (filled) {
			color = Color.white
		} else {
			color = Color.black
		}
	}

	@RegisterFunction
	fun mouseEntered() {
		log("Mouse entered sector $sectorId")
	}
}
