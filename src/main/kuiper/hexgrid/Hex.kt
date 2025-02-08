package hexgrid

import godot.Node2D
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.extensions.getNodeAs
import kotlinx.serialization.Serializable

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
	var locationName: String = ""

	@Export
	@RegisterProperty
	var hexUnlocked: Boolean = false

	lateinit var marker: HexDropTarget

	var sites: Array<Site> = arrayOf()

	@RegisterFunction
	override fun _ready() {
//		marker.hex = this
	}

	// possible functions:
	// - buildFacility(size: Int, site: Int)
	// - removeFacility(size: Int, site: Int)
}

@Serializable
class Site(var empty: Boolean = true)

class Facility(var size: Int = 1)
