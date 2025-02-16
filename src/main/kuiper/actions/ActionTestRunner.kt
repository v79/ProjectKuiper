package actions

import kotlinx.serialization.json.Json
import state.Company
import technology.Science

private val json = Json { prettyPrint = true }

fun main() {
    println("Setting up company")
    val company = Company("Kuiper")
    company.addResource(ResourceType.GOLD, 100)
    company.addResource(ResourceType.INFLUENCE, 10)
    company.addResource(ResourceType.CONSTRUCTION_MATERIALS, 50)
    Science.entries.forEach { science ->
        company.sciences[science] = 1.0f
    }
    println("Company initialized with...")
    ResourceType.entries.forEach {
        println("\t${it}: ${company.resources[it]}")
    }
    Science.entries.forEach { science ->
        println("\t${science}: ${company.sciences[science]}")
    }

    resourceActionTests(company) // should finish with 30 gold, 7 influence, 60 construction materials
//    scienceActionTests(company) // should finish with 1.2 physics science

    var turn = 1
    while (company.activeActions.isNotEmpty()) {
        println("*** Calling nextTurn($turn), actions active: ${company.activeActions.size}")
        company.nextTurn()
        println("Company resources after $turn turns")
        ResourceType.entries.forEach {
            println("\t${it}: ${company.resources[it]}")
        }
        Science.entries.forEach { science ->
            println("\t${science}: ${company.sciences[science]}")
        }
        turn++
    }

}

private fun scienceActionTests(company: Company) {
    println("Setting up some science actions")

    val physicsTimes1Point2 = Action(100, "Physics x 1.2", "Increase physics science by 20%", turns = 1, ActionType.BOOST)
    physicsTimes1Point2.addScienceMutation(Science.PHYSICS, MutationType.RATE_MULTIPLY, 1.2f)

    val geologyTimes1Point5CostsGold = Action(101, "Geology x 1.5", "Increase engineering science by 50%", turns = 1, ActionType.BOOST)
    geologyTimes1Point5CostsGold.addScienceMutation(Science.ENGINEERING, MutationType.RATE_MULTIPLY, 1.5f)
    geologyTimes1Point5CostsGold.addInitialCost(ResourceType.GOLD, 5)

    val cutMathsFundingForTwoTurns = Action(102, "Cut maths funding", "Reduce maths science by 50%", turns = 2, ActionType.BOOST)
    cutMathsFundingForTwoTurns.addScienceMutation(Science.MATHEMATICS, MutationType.ADD, -0.25f)
    cutMathsFundingForTwoTurns.addInitialCost(ResourceType.GOLD, -5)

    company.activeActions.add(physicsTimes1Point2)
    company.activeActions.add(geologyTimes1Point5CostsGold)
    company.activeActions.add(cutMathsFundingForTwoTurns)
}

private fun resourceActionTests(company: Company) {
    println("Setting up some actions")

    val action_add_gold_10 = Action(1, "Add 10 gold", "One time action to add 10 gold", turns = 0, ActionType.BOOST)
    action_add_gold_10.addMutation(ResourceType.GOLD, MutationType.ADD, 0, 10)

    val action_add_conMats_cost_infl =
        Action(
            2,
            "Add 10 construction materials",
            "One time action to add 10 construction materials costing 5 influence",
            turns = 0,
            ActionType.BOOST
        )
    action_add_conMats_cost_infl.addMutation(ResourceType.CONSTRUCTION_MATERIALS, MutationType.ADD, 0, 10)
    action_add_conMats_cost_infl.addInitialCost(ResourceType.INFLUENCE, 5)

    val action_add_inf_two_turns =
        Action(
            3,
            "Add 1 influence over 2 turns",
            "For next 2 turns, add 1 influence, then remove all gold",
            turns = 2,
            ActionType.BOOST
        )
    action_add_inf_two_turns.addMutation(ResourceType.INFLUENCE, MutationType.ADD, 1, 0)

    val action_nuke_gold_after_two_turns =
        Action(4, "Nuke gold after 2 turns", "Remove all gold after 2 turns", turns = 2, ActionType.BOOST)
    action_nuke_gold_after_two_turns.addMutation(ResourceType.GOLD, MutationType.RESET, 0, 0)

    val action_invest_gold_for_five_turns =
        Action(5, "Invest gold", "Spend 5 gold for 5 turns, receive 50 gold reward", turns = 5, ActionType.INVEST)
    action_invest_gold_for_five_turns.addMutation(ResourceType.GOLD, MutationType.ADD, -5, 50)

    val actions = setOf(
        action_add_gold_10,
        action_add_conMats_cost_infl,
        action_add_inf_two_turns,
        action_nuke_gold_after_two_turns, action_invest_gold_for_five_turns
    )

    val actionsString = json.encodeToString(actions)
    println(actionsString)

    println("Adding actions to company")
    actions.forEach {
        println(it.actionName)
        company.activeActions.add(it)
    }

}