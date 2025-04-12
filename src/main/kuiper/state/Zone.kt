package state

import LogInterface
import confirm_action.SectorPlacementStatus
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
    val hexes: MutableList<Location> = mutableListOf()

    /**
     * Find HexDatas that neighbor the given location.
     * This function takes three different approaches to finding neighbors:
     * - Same row, adjacent column
     * - Same column, adjacent row
     * - Distance of 1 in both row and column
     */
    fun getNeighbors(location: Location): List<Location> {
        val neighbors = mutableListOf<Location>()
        for (h in hexes) {
            if (h.row == location.row && h.column == location.column) {
                continue
            }
            // (COL,ROW)
            // FRANCE(2,2), GERMANY(3,1), SPAIN(1,2), ITALY(2,3), UK(0,1)
            val dx = h.row - location.row // GERMANY(1-2=-1), SPAIN(2-2=0), ITALY(3-2=1), UK(1-2=-1)
            val dy = h.column - location.column  // GERMANY(3-2=1), SPAIN(1-2=-1), ITALY(2-2=0), UK(0-2=-2)

            if (h.row == location.row && (dy * dy == 1)) {   // GERMANY(FALSE,TRUE), SPAIN(TRUE,TRUE), ITALY(TRUE,FALSE), UK(FALSE,FALSE)
                hexes.find { it.row == h.row && it.column == h.column }?.let { neighbors.add(it) }
            }
            if (h.column == location.column && (dx * dx == 1)) {  // GERMANY(FALSE,TRUE), SPAIN(FALSE,FALSE), ITALY(FALSE,TRUE), UK(FALSE,FALSE)
                hexes.find { it.row == h.row && it.column == h.column }?.let { neighbors.add(it) }
            }
            if ((dx * dx) == 1 && (dy * dy) == 1) { // GERMANY(TRUE,TRUE), SPAIN(FALSE,TRUE), ITALY(TRUE,FALSE), UK(TRUE,FALSE)
                hexes.find { it.row == h.row && it.column == h.column }?.let { neighbors.add(it) }
            }

        }
        return neighbors
    }

    /**
     * Check if a building can be placed in the given location
     * If all the sectors are empty, then the building can be placed
     * If any of the sectors contain the HQ, then the building cannot be placed
     * Otherwise, the building is 'blocked', which means it can be placed but will destroy the existing building(s)
     * There may be additional considerations for the building type
     */
    fun checkBuildingPlacement(
        location: Location,
        building: Building,
        segmentList: List<Int>
    ): SectorPlacementStatus {
        var status = SectorPlacementStatus.INVALID
        segmentList.forEach { segment ->
            val sector = location.sectors[segment]
            status = when (sector.status) {
                SectorStatus.EMPTY -> {
                    SectorPlacementStatus.OK
                }

                SectorStatus.CONSTRUCTING -> {
                    SectorPlacementStatus.UNDER_CONSTRUCTION
                }

                SectorStatus.BUILT -> {
                    SectorPlacementStatus.BLOCKED
                }

                SectorStatus.DESTROYED -> {
                    SectorPlacementStatus.OK
                }
            }
            // Cannot build or overbuild the HQ
            location.getBuilding(segment)?.let { existing ->
                if (existing is Building.HQ) {
                    status = SectorPlacementStatus.INVALID
                }
            }
            if (status != SectorPlacementStatus.OK) {
                return status
            }
        }
        return status
    }

}
