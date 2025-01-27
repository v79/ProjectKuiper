package actions

import kotlinx.serialization.Serializable
import state.ResourceType
import technology.Science

/**
 * An Action is a task that can be performed by the player.
 * Actions have a cost in gold, influence, and possibly science.
 * They also have a duration, which is the number of turns it takes to complete the action.
 */
@Serializable
class Action(val id: Int, val name: String, val description: String, var duration: Int = 1) {

    var turnsRemaining: Int = duration

    // an action will usually be performed on an entity, such as a hex on the map
    // or a character in a game
    // but for now, the only 'entity' would be the company HQ
    // var entity: Entity? = null

    // the cost of performing this action
    // this cost is expended when the action is played
    // if a player cannot afford the cost, they cannot perform the action
    // if the cost is per turn, then it is recorded as an additional mutation
    private val actionCosts: MutableMap<ResourceType, Int> = mutableMapOf(
        ResourceType.GOLD to 0, ResourceType.INFLUENCE to 0, ResourceType.CONSTRUCTION_MATERIALS to 0
    )

    // some actions may consume science, reducing the amount of science available for technology research
    var scienceCost: Science? = null

    // actions have prerequisites, such as having researched a certain technology
    // but maybe I don't need to capture this in the Action class?

    // actions can be deprecated, meaning they are no longer available to the player
    // does this need to be captured in the Action class?

    // actions have an effect, such as increasing a science research rate, or increasing the company's influence
    // actions can have effect once completed, or they have an effect for each turn that they are active
    // but how do I encode this in the Action class?
    // Better if the action takes place on the target, e.g. on the ScienceRate object?

    // actions either happen each turn, or at expiry


    // but how do I encode all this in text/json format?
    private var propertyToMutate: ResourceType =
        ResourceType.GOLD
    private var mutationEffect: MutationType = MutationType.ADD

    // amountPerYear should only be used with ADD mutations
    private var amountPerYear: Int = 0

    // completionAmount should only be used with CHANGE mutations, and is the final value of the property
    private var completionAmount: Int? = null

    class Mutation(
        val property: ResourceType, val effect: MutationType, val amountPerYear: Int, val completionAmount: Int? = null
    ) {
        override fun toString(): String {
            return "$effect $property by ${amountPerYear}, to completion amount $completionAmount"
        }
    }

    /**
     * Actions perform mutations on the company state
     * There is a primary mutation, which is the main effect of the action
     * There may be additional mutations, such as costs
     */
    fun getMutations(): Set<Mutation> {
        val costs = actionCosts.filter { it.value != 0 }.map { (resourceType, amount) ->
            Mutation(resourceType, MutationType.ADD, -amount, 0)
        }
        return setOf(
            Mutation(propertyToMutate, mutationEffect, amountPerYear, completionAmount)
        ).plus(costs)
    }

    /**
     * Builder function to add a mutation to the action
     */
    fun addMutation(
        resourceType: ResourceType, mutationType: MutationType, amountPerYear: Int, completionAmount: Int? = null
    ) {
        propertyToMutate = resourceType
        mutationEffect = mutationType
        this.amountPerYear = amountPerYear
        this.completionAmount = completionAmount
    }

    /**
     * Builder function to add a cost to the action
     */
    fun addCost(resourceType: ResourceType, amount: Int) {
        actionCosts[resourceType] = amount
    }


    override fun toString(): String {
        return "Action(id=$id, name='$name', description='$description', duration=$duration, turnsRemaining=$turnsRemaining, actionCosts=$actionCosts, scienceCost=$scienceCost)"
    }

}

enum class MutationType {
    ADD, CHANGE
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