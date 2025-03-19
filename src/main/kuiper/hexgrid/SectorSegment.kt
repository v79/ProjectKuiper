package hexgrid

import LogInterface
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.CollisionPolygon2D
import godot.api.Polygon2D
import godot.core.Color
import godot.core.PackedVector2Array
import godot.extension.getNodeAs
import state.SectorStatus

/**
 * A SectorSegment represents a triangle in a hexagon, or in game terms a sector within a hex that can be built on.
 */
@RegisterClass
class SectorSegment : Polygon2D(), LogInterface {

	override var logEnabled = true

	// UI elements
	private lateinit var collisionPolygon: CollisionPolygon2D

	// Data
	private var sectorId: Int = -1

	@RegisterFunction
	override fun _ready() {
	}

	@RegisterFunction
	override fun _process(delta: Double) {

	}

	@RegisterFunction
	fun create(id: Int, polygonPoints: PackedVector2Array, status: SectorStatus) {
		collisionPolygon = getNodeAs("%CollisionPolygon2D")!!
		sectorId = id
		collisionPolygon.polygon = polygonPoints
		polygon = polygonPoints
		color = when (status) {
			SectorStatus.BUILT -> {
				Color(1.0, 1.0, 1.0, 1.0)
			}

			SectorStatus.EMPTY -> {
				Color(0.2, 0.2, 0.2, 0.2)
			}

			SectorStatus.DESTROYED -> {
				Color(1.0, 0.2, 0.2, 1.0)
			}

			SectorStatus.CONSTRUCTING -> {
				Color(8.0, 0.6, 0.2, 1.0)
			}
		}
	}

	@RegisterFunction
	fun mouseEntered() {
		log("Mouse entered sector $sectorId")
	}
}
