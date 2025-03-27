package state

import hexgrid.map.editor.HexData
import kotlinx.serialization.Serializable

/**
 * A Zone represents a region of space that the player can explore and exploit
 * Each zone contains a number of buildable locations, which are visually represented as Hexes
 * The Zones will be listed in the Tabbar at the top of the screen
 */
@Serializable
class Zone(val id: Int, val name: String, var active: Boolean = false) {
    var description = "Zone $name must be unlocked by researching the appropriate technology"

    // I am not sure that I want this to be a flat list, but it will do for now
    // Locations ideally would appear placed on an actual world map, with relationships between them
    // But for now, we will just have a list of locations
    val hexes: MutableList<HexData> = mutableListOf()
}
