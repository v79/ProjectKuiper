package state

import LogInterface
import hexgrid.map.editor.HexData
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * A Zone represents a region of space that the player can explore and exploit
 * Each zone contains a number of buildable locations, which are visually represented as Hexes
 * The Zones will be listed in the Tabbar at the top of the screen
 */
@Serializable
class Zone(val id: Int, val name: String, var active: Boolean = false) : LogInterface {

    @Transient
    override var logEnabled: Boolean = true

    var description = "Zone $name must be unlocked by researching the appropriate technology"

    // I am not sure that I want this to be a flat list, but it will do for now
    // Locations ideally would appear placed on an actual world map, with relationships between them
    // But for now, we will just have a list of locations
    val hexes: MutableList<HexData> = mutableListOf()

    /**
     * Find HexDatas that neighbor the given location
     */
    fun getNeighbors(hexData: HexData): List<HexData> {
        val neighbors = mutableListOf<HexData>()
        for (h in hexes) {
            if (h.row == hexData.row && h.column == hexData.column) {
                continue
            }
//            if (h.location.unlocked) {
            val dx = h.row - hexData.row
            val dy = h.column - hexData.column
            if (dx * dx + dy * dy == 1) {
                val neighborHex = hexes.find { it.row == h.row && it.column == h.column }
                if (neighborHex != null) {
                    neighbors.add(neighborHex)
                }
            }
//            }
        }
        return neighbors
    }
}
