package hexgrid

import godot.Marker2D
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * A Hex represents a location in the world/region map.
 * A hex has six internal triangles, each of which represents a site where a facility can be built.
 * A facility may span more than one site/triangle
 * There are two special Hexes - the company HQ, and the space launch centre
 */
@Serializable
class Hex {

	// link to the Node that represents this hex. This is not serialised
	@Transient
	var node: Marker2D? = null


	var hexName: String = ""
	var sites: Array<Site> = arrayOf()


	// possible functions:
	// - buildFacility(size: Int, site: Int)
	// - removeFacility(size: Int, site: Int)
}

@Serializable
class Site(var empty: Boolean = true)

class Facility(var size: Int = 1)
