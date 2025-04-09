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
    var name: String
    var sectorsMustBeContiguous: Boolean
    var runningCosts: MutableMap<ResourceType, Int>
    var resourceGeneration: MutableMap<ResourceType, Int>
    var status: BuildingStatus

    // spritePath?
    var spritePath: String?

    /**
     * Special building representing the player's headquarters
     */
    @Serializable
    class HQ : Building {
        override var name = "HQ"
        override var sectors = 3
        override var sectorsMustBeContiguous = false
        override var runningCosts: MutableMap<ResourceType, Int> = mutableMapOf()
        override var resourceGeneration: MutableMap<ResourceType, Int> = mutableMapOf()
        override var spritePath: String? = "res://assets/textures/buildings/hq.png"
        override var status: BuildingStatus = BuildingStatus.BUILT
        var baseCostToBuild = 0
        val sciencesProduced: MutableMap<Science, Float> = mutableMapOf()
        var baseInfluenceGenerated = 1
    }

    /**
     * Any building that produces science per turn is a science lab
     * This could be a library, university, cyclotron, etc.
     * Name, cost, and production rate should be customized
     */
    @Serializable
    class ScienceLab() : Building {

        constructor(name: String, description: String, sectors: Int, contiguous: Boolean) : this() {
            this.name = name
            this.labDescription = description
            this.sectors = sectors
            this.sectorsMustBeContiguous = contiguous
        }

        override var name: String = "Lab"
        override var sectors: Int = 1
        override var sectorsMustBeContiguous: Boolean = true
        override var resourceGeneration: MutableMap<ResourceType, Int> = mutableMapOf()
        override var runningCosts: MutableMap<ResourceType, Int> = mutableMapOf()
        override var status: BuildingStatus = BuildingStatus.NONE

        // TODO: This should be part of the constructor, so that different labs can have different icons
        override var spritePath: String? = "res://assets/textures/buildings/basic_lab.png"
        var labDescription: String = ""
        var sciencesProduced: Map<Science, Float> = mapOf()
    }

    @Serializable
    class Factory() : Building {
        constructor(name: String, description: String, sectors: Int, contiguous: Boolean) : this() {
            this.name = name
            this.factoryDescription = description
            this.sectors = sectors
            this.sectorsMustBeContiguous = contiguous
        }

        override var name: String = "Basic Factory"
        override var sectors: Int = 1
        override var sectorsMustBeContiguous: Boolean = true
        override var resourceGeneration: MutableMap<ResourceType, Int> = mutableMapOf()
        override var runningCosts: MutableMap<ResourceType, Int> = mutableMapOf()
        override var spritePath: String? = "res://assets/textures/buildings/basic_factory.png"
        override var status: BuildingStatus = BuildingStatus.NONE
        private var factoryDescription: String = ""
    }

    // I'd really like a fluent API for building construction

}

enum class BuildingStatus {
    NONE,
    PLACED,
    UNDER_CONSTRUCTION,
    BUILT,
    DESTROYED
}