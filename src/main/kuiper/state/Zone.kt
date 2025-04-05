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
     * Find HexDatas that neighbor the given location.
     * This function takes three different approaches to finding neighbors:
     * - Same row, adjacent column
     * - Same column, adjacent row
     * - Distance of 1 in both row and column
     */
    fun getNeighbors(hexData: HexData): List<HexData> {
        val neighbors = mutableListOf<HexData>()
        for (h in hexes) {
            if (h.row == hexData.row && h.column == hexData.column) {
                continue
            }
            // (COL,ROW)
            // FRANCE(2,2), GERMANY(3,1), SPAIN(1,2), ITALY(2,3), UK(0,1)
            val dx = h.row - hexData.row // GERMANY(1-2=-1), SPAIN(2-2=0), ITALY(3-2=1), UK(1-2=-1)
            val dy = h.column - hexData.column  // GERMANY(3-2=1), SPAIN(1-2=-1), ITALY(2-2=0), UK(0-2=-2)

            if (h.row == hexData.row && (dy * dy == 1)) {   // GERMANY(FALSE,TRUE), SPAIN(TRUE,TRUE), ITALY(TRUE,FALSE), UK(FALSE,FALSE)
                hexes.find { it.row == h.row && it.column == h.column }?.let { neighbors.add(it) }
            }
            if (h.column == hexData.column && (dx * dx == 1)) {  // GERMANY(FALSE,TRUE), SPAIN(FALSE,FALSE), ITALY(FALSE,TRUE), UK(FALSE,FALSE)
                hexes.find { it.row == h.row && it.column == h.column }?.let { neighbors.add(it) }
            }
            if ((dx * dx) == 1 && (dy * dy) == 1) { // GERMANY(TRUE,TRUE), SPAIN(FALSE,TRUE), ITALY(TRUE,FALSE), UK(TRUE,FALSE)
                hexes.find { it.row == h.row && it.column == h.column }?.let { neighbors.add(it) }
            }

        }
        return neighbors
    }
}
