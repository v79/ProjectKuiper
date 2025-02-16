package state

import actions.ResourceType
import kotlinx.serialization.Serializable
import technology.Science

/**
 * A Building represents a structure that can be built on a Location
 * A building takes up one or more sectors on a Location
 * A building usually has a base production rate for a resource or science
 * A building usually has a base cost to build and to run each turn
 */
@Serializable
sealed interface Building {
    // Not sure what will be common to all buildings yet
    var sectors: Int
    var sectorsMustBeContiguous: Boolean
    // spritePath?

    /**
     * Special building representing the player's headquarters
     */
    @Serializable
    class HQ : Building {
        var name = "HQ"
        override var sectors = 3
        override var sectorsMustBeContiguous = false
        var baseCostToBuild = 0
        var baseRunningCost = 0
    }

    /**
     * Any building that produces science per turn is a science lab
     * This could be a library, university, cyclotron, etc.
     * Name, cost, and production rate should be customized
     */
    @Serializable
    class ScienceLab(
        val labName: String,
        val labDescription: String,
        override var sectors: Int,
        override var sectorsMustBeContiguous: Boolean = true
    ) : Building {
        var baseRunningCost = ResourceType.GOLD to 10
        var sciencesProduced: Map<Science, Float> = mapOf()
    }

    // I'd really like a fluent API for building construction

}