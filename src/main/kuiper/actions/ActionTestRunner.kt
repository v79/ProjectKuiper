package actions

import state.Company
import state.ResourceType
import technology.Science


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
    scienceActionTests(company) // should finish with 1.2 physics science

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

    val physicsTimes1Point2 = Action(100, "Physics x 1.2", "Increase physics science by 20%", duration = 1)
    physicsTimes1Point2.addScienceMutation(Science.PHYSICS, MutationType.RATE_MULTIPLY, 1.2f)

    val geologyTimes1Point5CostsGold = Action(101, "Geology x 1.5", "Increase geology science by 50%", duration = 1)
    geologyTimes1Point5CostsGold.addScienceMutation(Science.GEOLOGY, MutationType.RATE_MULTIPLY, 1.5f)
    geologyTimes1Point5CostsGold.addCost(ResourceType.GOLD, 5)

    val cutMathsFundingForTwoTurns = Action(102, "Cut maths funding", "Reduce maths science by 50%", duration = 2)
    cutMathsFundingForTwoTurns.addScienceMutation(Science.MATHEMATICS, MutationType.ADD, -0.25f)
    cutMathsFundingForTwoTurns.addCost(ResourceType.GOLD, -5)

    company.activeActions.add(physicsTimes1Point2)
    company.activeActions.add(geologyTimes1Point5CostsGold)
    company.activeActions.add(cutMathsFundingForTwoTurns)
}

private fun resourceActionTests(company: Company) {
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
    action_nuke_gold_after_two_turns.addMutation(ResourceType.GOLD, MutationType.SET, 0, 0)

    val action_invest_gold_for_five_turns =
        Action(5, "Invest gold", "Spend 5 gold for 5 turns, receive 50 gold reward", duration = 5)
    action_invest_gold_for_five_turns.addMutation(ResourceType.GOLD, MutationType.ADD, -5, 50)

    val actions = setOf(
        action_add_gold_10,
        action_add_conMats_cost_infl,
        action_add_inf_two_turns,
        action_nuke_gold_after_two_turns, action_invest_gold_for_five_turns
    )

    println("Adding actions to company")
    actions.forEach {
        println(it)
        company.activeActions.add(it)
    }
}