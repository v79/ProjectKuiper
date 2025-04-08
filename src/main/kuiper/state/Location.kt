package state

import kotlinx.serialization.Serializable

/**
 * A Location represents a single buildable hex within a Zone
 */
@Serializable
data class Location(val name: String, var unlocked: Boolean = false) {
    // locations may have some interesting properties in the future, such as base production rates

    // a location is divided into 6 sectors, each of which can be built on
    // buildings know which sectors they are built on
    val buildings: Map<Building, IntArray>
        get() = _buildings
    private val _buildings: MutableMap<Building, IntArray> = mutableMapOf()

    val sectors = List(6) { Sector(it) }

    /**
     * Add a building to this location
     * @param building the building to add
     * @param sectorIds the sector ids that the building will occupy
     * @param alreadyBuilt whether the building is already built or not, e.g. will it be "Built" or "Constructing"
     */
    fun addBuilding(building: Building, sectorIds: IntArray, alreadyBuilt: Boolean = false) {
        _buildings[building] = sectorIds
        sectorIds.forEach { sectors[it].status = if (alreadyBuilt) SectorStatus.BUILT else SectorStatus.CONSTRUCTING }
    }

    /**
     * Get the building that occupies the given sector
     */
    fun getBuilding(sectorId: Int): Building? {
        return _buildings.entries.find { it.value.contains(sectorId) }?.key
    }

    fun getSectorStatus(sectorId: Int): SectorStatus {
        return sectors[sectorId].status
    }
}

/**
 * A Sector represents a single buildable sector within a Location, and is represented by a triangle within a Hex
 */
@Serializable
class Sector(val id: Int, var status: SectorStatus = SectorStatus.EMPTY)

/**
 * The status of a Sector
 */
enum class SectorStatus {
    EMPTY,
    CONSTRUCTING,
    BUILT,
    DESTROYED
}
