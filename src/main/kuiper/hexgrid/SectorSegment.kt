package hexgrid

import LogInterface
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.*
import godot.core.Color
import godot.core.PackedVector2Array
import godot.core.Vector2
import godot.extension.getNodeAs
import state.Building
import state.Location
import state.SectorStatus

/**
 * A SectorSegment represents a triangle in a hexagon, or in game terms a sector within a hex that can be built on.
 */
@RegisterClass
class SectorSegment : Polygon2D(), LogInterface {

	override var logEnabled = true

	// UI elements
	private lateinit var collisionPolygon: CollisionPolygon2D
	private lateinit var sprite2D: Sprite2D
	private lateinit var area2D: Area2D

	// Data
	private var sectorId: Int = -1
	var location: Location? = null
	var status: SectorStatus = SectorStatus.EMPTY

	@RegisterFunction
	override fun _ready() {
		area2D = getNodeAs("Area2D")!!
		sprite2D = getNodeAs("%Sprite2D")!!
	}

	@RegisterFunction
	fun updateUI(id: Int, polygonPoints: PackedVector2Array, building: Building?) {
		sprite2D = getNodeAs("%Sprite2D")!!
		val spritePath = building?.spritePath
		val sprite = if (spritePath != null) {
			ResourceLoader.load(spritePath, "Texture") as Texture2D
		} else {
			null
		}
		collisionPolygon = getNodeAs("%CollisionPolygon2D")!!
		sectorId = id
		collisionPolygon.polygon = polygonPoints
		polygon = polygonPoints
		when (status) {
			SectorStatus.BUILT -> {
				if (sprite != null) {
					sprite2D.texture = sprite
					sprite2D.centered = false
					sprite2D.setScale(Vector2(0.3, 0.3))
					sprite2D.setOffset(calculateSpriteOffset())
				}
				color = Color(1.0, 1.0, 1.0, 1.0)
			}

			SectorStatus.EMPTY -> {
				color = Color(0.2, 0.2, 0.2, 0.2)
			}

			SectorStatus.DESTROYED -> {
				color = Color(1.0, 0.2, 0.2, 1.0)
			}

			SectorStatus.CONSTRUCTING -> {
				color = Color(8.0, 0.6, 0.2, 1.0)
			}
		}
	}

	@RegisterFunction
	fun mouseEntered() {
		if (location == null) {
			return
		} else if (location!!.unlocked) {
			log("Mouse entered location '${location!!.name}' sector $sectorId")
		}

	}

	@RegisterFunction
	fun mouseExited() {
	}

	/**
	 * Calculate the offset for the sprite based on the sector id
	 * These are hard-coded values for now, but could be calculated based on the triangle centre points in the future
	 *       _
	 *    /3\4/5\
	 *    \2/1\0/
	 */
	private fun calculateSpriteOffset(): Vector2 {
		when (sectorId) {
			0 -> return Vector2(60.0, 10.0)
			1 -> return Vector2(-70.0, 70.0)
			2 -> return Vector2(-190.0, 10.0)
			3 -> return Vector2(-190.0, -140.0)
			4 -> return Vector2(-65.0, -210.0)
			5 -> return Vector2(60, -140.0)
		}
		return Vector2(0.0, 0.0)
	}
}
