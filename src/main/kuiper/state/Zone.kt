package state

import kotlinx.serialization.Serializable

/**
 * A Zone represents a region of space that the player can explore and exploit
 * Each zone contains a number of buildable locations, which are visually represented as Hexes
 * The Zones will be listed in the Tabbar at the top of the screen
 */
@Serializable
class Zone(val name: String, var active: Boolean = false) {
    var description = "Zone $name must be unlocked by researching the appropriate technology"
}