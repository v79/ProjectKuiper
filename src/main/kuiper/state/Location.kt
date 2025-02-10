package state

import hexgrid.Hex
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * A Location represents a single buildable hex within a Zone
 */
@Serializable
class Location(val name: String, var unlocked: Boolean = false) {
    // locations may have some interesting properties in the future, such as base production rates

    // a location is divided into 6 sectors, each of which can be built on
    val sectors = Array(6) { Sector(it) }

    @Transient
    val hex: Hex? = null
}

@Serializable
class Sector(val id: Int, var status: SectorStatus = SectorStatus.EMPTY)

enum class SectorStatus {
    EMPTY,
    BUILDING,
    BUILT,
    DESTROYED
}
