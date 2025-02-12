package actions.generator

import actions.Action
import kotlinx.serialization.json.Json
import state.Building
import state.ResourceType
import technology.Science
import java.io.File

/**
 * A simple script to generate actions for the game
 * I may have to build a UI for this in the future
 */
fun main() {
    println("Generating actions...")
    var id = 1

    // start with the simplest 'build a research lab' action
    val buildResearchLab = createAction(
        id++, "Build Research Lab", "Construct a research lab to increase your science production", 3
    )
    // add costs
    buildResearchLab.addCost(ResourceType.GOLD, 100)
    buildResearchLab.addCost(ResourceType.INFLUENCE, 2)
    // add building
    val lab = Building.ScienceLab("Research Lab", "A basic research lab", 1, true)
    lab.sciencesProduced = Science.all().associateWith { 5f }
    lab.baseRunningCost = ResourceType.GOLD to 10
    buildResearchLab.constructBuilding(lab)

    val basicTelescope = createAction(
        id++, "Build Basic Telescope", "Construct a telescope to increase your astronomy science production", 4
    )
    basicTelescope.addCost(ResourceType.GOLD, 200)
    basicTelescope.addCost(ResourceType.INFLUENCE, 1)
    basicTelescope.addCost(ResourceType.CONSTRUCTION_MATERIALS, 25)
    // add the building
    val telescope = Building.ScienceLab("Basic Telescope", "A basic telescope", 1, true)
    telescope.sciencesProduced = mapOf(Science.ASTRONOMY to 10f, Science.PHYSICS to 5f)
    telescope.baseRunningCost = ResourceType.GOLD to 5
    basicTelescope.constructBuilding(telescope)


    // serialize the actions to a file
    val json = Json { prettyPrint = false }
    println()
    println("Serializing actions...")
    val allActions = listOf(buildResearchLab, basicTelescope)
    val actionJson = json.encodeToString(allActions)
    println(actionJson)
    // write to file
    println("Writing to actions.json...")
    File("assets/data/actions/actions.json").writeText(actionJson)
}

fun createAction(id: Int, name: String, description: String, duration: Int): Action {
    val action = Action(id, name, description, duration)

    return action
}