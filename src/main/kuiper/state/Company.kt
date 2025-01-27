package state

import actions.Action
import actions.MutationType
import kotlinx.serialization.Serializable
import technology.Science
import kotlin.contracts.ExperimentalContracts

/**
 * The company that the player is running
 * It will likely have many other properties over time
 */
@Serializable
class Company(var name: String) {

    val resources: MutableMap<ResourceType, Int> = mutableMapOf(
        ResourceType.GOLD to 0, ResourceType.INFLUENCE to 0, ResourceType.CONSTRUCTION_MATERIALS to 0
    )

    var sciences: MutableMap<Science, Float> = mutableMapOf()

    // Currently active actions which have a limited duration
    var activeActions: MutableList<Action> = mutableListOf()

    // is this where I put all the mutator functions?
    /**
     * Update the given resource by the given amount
     * Use a negative amount to subtract
     */
    fun addResource(type: ResourceType, amount: Int) {
        resources.merge(type, amount, Int::plus)
    }

    fun setResource(type: ResourceType, amount: Int) {
        resources[type] = amount
    }

    @OptIn(ExperimentalContracts::class)
    fun nextTurn() {
        activeActions.forEach { action ->
            val mutations = action.getMutations()
            mutations.forEach { mutation ->
                if (mutation.amountPerYear != 0) {
                    println("Executing mutation: ${action.id} $mutation")
                    when (mutation.effect) {
                        MutationType.ADD -> addResource(mutation.property, mutation.amountPerYear)
                        MutationType.CHANGE -> setResource(mutation.property, mutation.amountPerYear)
                    }
                }
            }
            action.turnsRemaining--
            // perform ongoing action (if any)
            if (action.turnsRemaining == 0) {
                if (mutations.any { it.completionAmount != null }) {
                    mutations.forEach { mutation ->
                        println("Executing completion mutation: ${action.id} $mutation")
                        when (mutation.effect) {
                            MutationType.ADD -> addResource(mutation.property, mutation.completionAmount!!)
                            MutationType.CHANGE -> setResource(mutation.property, mutation.completionAmount!!)
                        }
                    }
                }
            }
        }
        // clean up any expired actions
        activeActions.removeIf { it.turnsRemaining == 0 }
    }
}

enum class ResourceType {
    GOLD, INFLUENCE, CONSTRUCTION_MATERIALS
}