package actions

import godot.annotation.RegisterClass
import kotlinx.serialization.Serializable
import state.Building
import technology.Science

@RegisterClass
class ActionWrapper() : godot.Object() {
    constructor(action: Action) : this() {
        this.action = action
    }

    var action: Action? = null
}

/**
 * An Action is a task that can be performed by the player.
 * Actions have a cost in gold, influence, and possibly science.
 * They also have a duration, which is the number of turns it takes to complete the action.
 */
@Serializable
class Action() {

    constructor(id: Int, name: String, description: String, turns: Int = 1, type: ActionType) : this() {
        this.id = id
        this.actionName = name
        this.description = description
        this.turns = turns
        this.type = type
    }

    var id: Int = -1
    var actionName: String = ""
    var description: String = ""
    var turns: Int = 1
    var turnsRemaining: Int = 1
    var type: ActionType = ActionType.NONE

    // an action will usually be performed on an entity, such as a hex on the map
    // or a character in a game
    // but for now, the only 'entity' would be the company HQ
    // var entity: Entity? = null

    init {
        turnsRemaining = turns
    }


    val initialCosts: MutableMap<ResourceType, Int> = mutableMapOf()
    private val mutations: MutableSet<ResourceMutation> = mutableSetOf()
    private val scienceMutations: MutableSet<ScienceMutation> = mutableSetOf()
    var buildingToConstruct: Building? = null

    fun addInitialCost(resourceType: ResourceType, amount: Int) {
        initialCosts[resourceType] = amount
    }

    fun addMutation(
        resourceType: ResourceType,
        mutationType: MutationType,
        amountPerYear: Int,
        completionAmount: Int? = null
    ) {
        val mutation = ResourceMutation(resourceType, mutationType, amountPerYear, completionAmount)
        mutations.add(mutation)
    }

    fun addScienceMutation(science: Science, mutationType: MutationType, amount: Float) {
        val mutation = ScienceMutation(science, mutationType, amount)
        scienceMutations.add(mutation)
    }

    fun constructBuilding(building: Building) {
        buildingToConstruct = building
    }

    fun getMutations(): Set<Mutation> {
        return (mutations + scienceMutations).toSet()
    }

    /**
     * Get the initial cost of the action, and the cost per turn, for the given resource
     * @param resourceType the type of resource to get the cost for
     * @return a Pair of the initial cost and the cost per turn
     */
    fun getCost(resourceType: ResourceType): Pair<Int?, Int?> {
        val costPerTurn = mutations.find { it.resource == resourceType }?.amountPerYear
        return Pair(initialCosts[resourceType], costPerTurn)
    }

    /**
     * Get all the costs of the action, per turn, for all resources
     */
    fun getCostsPerTurn(): Map<ResourceType, Int> {
        return mutations.filter { it.type == MutationType.SUBTRACT }
            .associate { it.resource to it.amountPerYear }
    }

    /**
     * Get all the incomes of the action, per turn, for all resources
     */
    fun getIncomePerTurn(): Map<ResourceType, Int> {
        return mutations.filter { it.type == MutationType.ADD }
            .associate { it.resource to it.amountPerYear }
    }

    /**
     * Get the benefits of the action, for the given resource
     * A benefit is defined as a positive change in the resource - ADD or RATE_MULTIPLY mutations
     * @param resourceType the type of resource to get the benefit for
     * @return a Pair of the benefit per turn and the completion benefit
     */
    fun getBenefits(resourceType: ResourceType): Pair<Int?, Int?> {
        val benefitTypes = setOf(MutationType.ADD, MutationType.RATE_MULTIPLY)
        val benefitPerTurn =
            mutations.find { it.resource == resourceType && benefitTypes.contains(it.type) }?.amountPerYear
        val completionBenefit =
            mutations.find { it.resource == resourceType && it.completionAmount != null }?.completionAmount
        return Pair(benefitPerTurn, completionBenefit)
    }

    fun getScienceBenefit(science: Science): Float? {
        val benefitPerTurn = scienceMutations.find { it.science == science }?.amount
        if (benefitPerTurn != null && benefitPerTurn > 0f) {
            return benefitPerTurn
        }
        return null
    }

    // some actions may consume science, reducing the amount of science available for technology research
    // var scienceCost: Science? = null

    // actions have prerequisites, such as having researched a certain technology
    // but maybe I don't need to capture this in the Action class?

    // actions can be deprecated, meaning they are no longer available to the player
    // does this need to be captured in the Action class?

    // ✅ actions have an effect, such as increasing a science research rate, or increasing the company's influence
    // ✅ actions can have effect once completed, or they have an effect for each turn that they are active
    // Better if the action takes place on the target, e.g. on the ScienceRate object?
    // actions can alter a Location/Hex or sector, e.g. by building a structure
    // actions either happen each turn, or at expiry

    override fun toString(): String {
        return "Action(id=$id, actionName='$actionName', turns=$turns, type=$type, initialCosts=$initialCosts, mutations=${mutations.size}, scienceMutations=${scienceMutations.size}, buildingToConstruct=${buildingToConstruct})"
    }

}

enum class ActionType {
    NONE, BUILD, BOOST, INVEST, EXPLORE
}


/**
 * Action behaviours that could be encoded as functions
 * // FLAT increases. For science, this value is added to the pool of science to be spent researching at the end of this turn
 * add_science(science: Science, amount: Int) -> flat increase of a science, as a boost (negative to debuff)
 * add_production(prod: Int) -> flat increase/decrease of production
 * add_influence(inf: Int)
 * add_gold(gold: Int)
 *
 * // RATE increases, changes to the base yearly rates, the 'income'
 * change_science_rate(science: Science, amount: Int)
 * change_production/influence/gold(amount: Int)
 */


