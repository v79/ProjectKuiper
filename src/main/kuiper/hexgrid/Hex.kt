package hexgrid

import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.api.Label
import godot.api.Node2D
import godot.extension.getNodeAs
import state.Location

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

    var triangles = Array(6) { it }

    // UI elements
    private lateinit var locationLabel: Label

    lateinit var marker: HexDropTarget
    lateinit var location: Location

    @RegisterFunction
    override fun _ready() {
        locationLabel = getNodeAs("%LocationLabel")!!
    }

    @RegisterFunction
    override fun _process(delta: Double) {
    }

    // possible functions:
    // - buildFacility(size: Int, site: Int)
    // - removeFacility(size: Int, site: Int)
}
