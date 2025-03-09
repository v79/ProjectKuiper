package actions.generator

import actions.Action
import actions.ActionType
import actions.MutationType
import kotlinx.serialization.json.Json
import state.Building
import actions.ResourceType
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
    val buildResearchLab: Action = createAction(
        id++, "Build Research Lab", "Construct a research lab to increase your science production", 3, ActionType.BUILD
    )
    // add costs
    buildResearchLab.addInitialCost(ResourceType.GOLD, 100)
    buildResearchLab.addInitialCost(ResourceType.INFLUENCE, 2)
    // add building
    val lab = Building.ScienceLab("Research Lab", "A basic research lab", 1, true)
    lab.sciencesProduced = Science.all().associateWith { 5f }
    lab.runningCosts[ResourceType.GOLD] = 10
    buildResearchLab.constructBuilding(lab)

    val basicTelescope = createAction(
        id++, "Build Basic Telescope", "Construct a telescope to increase your astronomy science production", 4, ActionType.BUILD
    )
    basicTelescope.addInitialCost(ResourceType.GOLD, 200)
    basicTelescope.addInitialCost(ResourceType.INFLUENCE, 1)
    basicTelescope.addInitialCost(ResourceType.CONSTRUCTION_MATERIALS, 25)
    // add the building
    val telescope = Building.ScienceLab("Basic Telescope", "A basic telescope", 1, true)
    telescope.sciencesProduced = mapOf(Science.ASTRONOMY to 10f, Science.PHYSICS to 5f)
    telescope.runningCosts[ResourceType.GOLD] = 5
    basicTelescope.constructBuilding(telescope)

    val investCash = createAction(
        id++, "Invest Cash", "Invest in the stock market for a potential return", 5, ActionType.INVEST
    )
    investCash.addInitialCost(ResourceType.GOLD, 100)
    investCash.addInitialCost(ResourceType.INFLUENCE, 1)
    investCash.addMutation(ResourceType.GOLD, MutationType.ADD, 0, 150)

    // really expensive, costs a lot per turn
    // but doing so does add some science per turn
    val buildSpaceship = createAction(
        id++, "Build Spaceship", "Construct a spaceship to explore the galaxy", 10, ActionType.EXPLORE
    )
    buildSpaceship.addInitialCost(ResourceType.GOLD, 1000)
    buildSpaceship.addInitialCost(ResourceType.INFLUENCE, 5)
    buildSpaceship.addInitialCost(ResourceType.CONSTRUCTION_MATERIALS, 100)
    buildSpaceship.addMutation(ResourceType.GOLD, MutationType.SUBTRACT, 100)
    buildSpaceship.addMutation(ResourceType.CONSTRUCTION_MATERIALS, MutationType.SUBTRACT, 25)
    buildSpaceship.addScienceMutation(Science.ASTRONOMY, MutationType.ADD, 5f)
    buildSpaceship.addScienceMutation(Science.ENGINEERING, MutationType.ADD, 5f)


    // serialize the actions to a file
    val json = Json { prettyPrint = false }
    println()
    println("Serializing actions...")
    val allActions = listOf(buildResearchLab, basicTelescope, investCash, buildSpaceship)
    allActions.forEach { action ->
        println("Action: ${action.type} ${action.id} ${action.actionName} takes ${action.turns} turns")
        action.initialCosts.forEach { (type, cost) ->
            println("\tInitial cost: $type $cost")
        }
        action.getMutations().forEach { mutation ->
            println("\tMutation: $mutation")
        }
    }
    val actionJson = json.encodeToString(allActions)
    println(actionJson)
    // write to file
    println("Writing to actions.json...")
    File("assets/data/actions/actions.json").writeText(actionJson)
}

fun createAction(id: Int, name: String, description: String, duration: Int, type: ActionType): Action {
    val action = Action(id, name, description, duration, type)

    return action
}