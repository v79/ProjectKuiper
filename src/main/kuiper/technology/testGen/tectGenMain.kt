package technology.testGen

import kotlinx.serialization.json.Json
import technology.TechTier
import technology.TechWeb
import technology.Technology

fun main() {
    println("Setting up some basic technologies")

    val startingPoint = Technology(
        0,
        "Starting Point",
        "The beginning of all things",
        TechTier.TIER_1
    )
    val integratedCircuits = Technology(
        1,
        "Integrated Circuits",
        "The foundation of modern computing",
        TechTier.TIER_1
    ).apply {
        requires.add(0)
    }
    val lightweightAlloys = Technology(
        2,
        "Lightweight Alloys",
        "Stronger and lighter materials for construction",
        TechTier.TIER_1
    ).apply {
        requires.add(0)
    }
    val multiStageRockets = Technology(
        3,
        "Multi-Stage Rockets",
        "Multiple stages for more efficient space travel",
        TechTier.TIER_1
    ).apply {
        requires.add(0)
    }
    val guidanceSystems = Technology(
        4,
        "Guidance Systems",
        "Precision control for rockets and missiles",
        TechTier.TIER_1
    ).apply {
        requires.add(0)
        requires.add(1)
    }
    val basicLifeSupport = Technology(
        5,
        "Basic Life Support",
        description = "The essentials for keeping astronauts alive",
        tier = TechTier.TIER_1
    ).apply {
        requires.add(0)
    }
    val solidRocketFuels = Technology(
        6,
        "Solid Rocket Fuels",
        "Simple and reliable rocket propulsion",
        TechTier.TIER_1
    ).apply {
        requires.add(3)
        requires.add(2)
    }

    val allTechs = mutableListOf(
        integratedCircuits,
        lightweightAlloys,
        multiStageRockets,
        guidanceSystems,
        basicLifeSupport,
        solidRocketFuels
    )

    val Json = Json { prettyPrint = true; encodeDefaults = true }
    val jsonTechWeb = Json.encodeToString(allTechs)
    println(jsonTechWeb)

    val techWeb = TechWeb()
    techWeb.technologies.addAll(allTechs)

    println("Technology ${startingPoint.title} unlocks:")
    println("  ${techWeb.unlockedBy(startingPoint).joinToString { it.title }}")

}