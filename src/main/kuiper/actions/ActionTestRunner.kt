package actions

import state.Company
import state.ResourceType


fun main() {
    println("Setting up company")
    val company = Company("Kuiper")
    company.addResource(ResourceType.GOLD, 100)
    company.addResource(ResourceType.INFLUENCE, 10)
    company.addResource(ResourceType.CONSTRUCTION_MATERIALS, 50)
    println("Company initialized with...")
    ResourceType.entries.forEach {
        println("\t${it}: ${company.resources[it]}")
    }
    println("Setting up some actions")

    val action_add_gold_10 = Action(1, "Add 10 gold", "One time action to add 10 gold")
    action_add_gold_10.addMutation(ResourceType.GOLD, MutationType.ADD, 0, 10)

    val action_add_conMats_cost_infl =
        Action(
            2,
            "Add 10 construction materials",
            "One time action to add 10 construction materials costing 5 influence"
        )
    action_add_conMats_cost_infl.addMutation(ResourceType.CONSTRUCTION_MATERIALS, MutationType.ADD, 0, 10)
    action_add_conMats_cost_infl.addCost(ResourceType.INFLUENCE, 5)

    val action_add_inf_two_turns =
        Action(
            3,
            "Add 1 influence over 2 turns",
            "For next 2 turns, add 1 influence, then remove all gold",
            duration = 2
        )
    action_add_inf_two_turns.addMutation(ResourceType.INFLUENCE, MutationType.ADD, 1, 0)

    val action_nuke_gold_after_two_turns =
        Action(4, "Nuke gold after 2 turns", "Remove all gold after 2 turns", duration = 2)
    action_nuke_gold_after_two_turns.addMutation(ResourceType.GOLD, MutationType.CHANGE, 0, 0)

    val actions = setOf(
        action_add_gold_10,
        action_add_conMats_cost_infl,
        action_add_inf_two_turns,
        action_nuke_gold_after_two_turns
    )

    println("Adding actions to company")
    actions.forEach {
        println(it)
        company.activeActions.add(it)
    }

    var turn = 1

    println("*** Calling nextTurn($turn), actions active: ${company.activeActions.size}")
    company.nextTurn()
    println("Company resources after $turn turns")
    ResourceType.entries.forEach {
        println("\t${it}: ${company.resources[it]}")
    }
    turn++
    println("*** Calling nextTurn($turn), actions active: ${company.activeActions.size}")
    company.nextTurn()

    println("Company resources after $turn turns")
    ResourceType.entries.forEach {
        println("\t${it}: ${company.resources[it]}")
    }

}