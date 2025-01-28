package state

import actions.Action
import actions.MutationType
import actions.ResourceMutation
import actions.ScienceMutation
import kotlinx.serialization.Serializable
import technology.Science

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

    /**
     * Update the given resource by the given amount
     * Use a negative amount to subtract
     */
    fun addResource(type: ResourceType, amount: Int) {
        resources.merge(type, amount, Int::plus)
    }

    /**
     * Set the resource to the given amount
     */
    fun setResource(type: ResourceType, amount: Int) {
        resources[type] = amount
    }

    /**
     * Multiply the resource by the given amount. Useful for sciences.
     */
    fun multiplyResource(type: ResourceType, multiplier: Float) {
        resources[type] = (resources[type]!! * multiplier).toInt()
    }

    /**
     * Perform all active actions, update company resources and sciences,
     * spend science on research, and so on
     */
    fun nextTurn() {

        // spend science amongst technologies, i.e. perform research
        // for now, just reset to zero!
//        sciences.replaceAll { _, _ -> 0.0f }

        // perform actions
        // perform ongoing action (if any)
        activeActions.forEach act@{ action ->
            val mutations = action.getMutations()
            mutations.forEach { mutation ->
                when (mutation) {
                    is ResourceMutation -> {
                        if (mutation.amountPerYear != 0) {
                            println("Executing mutation: ${action.id} $mutation")
                            when (mutation.type) {
                                MutationType.ADD -> addResource(mutation.resource, mutation.amountPerYear)
                                MutationType.SET -> setResource(mutation.resource, mutation.amountPerYear)
                                MutationType.RATE_MULTIPLY -> multiplyResource(mutation.resource, mutation.amountPerYear.toFloat())
                            }
                        }
                    }

                    is ScienceMutation -> {
                        if (mutation.amount != 0.0f) {
                            println("Executing science mutation: ${action.id} $mutation")
                            when (mutation.type) {
                                MutationType.ADD -> sciences.merge(
                                    mutation.science,
                                    mutation.amount,
                                    Float::plus
                                )
                                MutationType.SET -> sciences[mutation.science] = mutation.amount
                                MutationType.RATE_MULTIPLY -> sciences.merge(
                                    mutation.science,
                                    mutation.amount,
                                    Float::times
                                )
                            }
                        }
                    }
                }

            }
            action.turnsRemaining--
            // perform completion mutations, which happen when the action expires
            if (action.turnsRemaining == 0) {
                mutations.forEach mut@{ mutation ->
                    when (mutation) {
                        is ResourceMutation -> {
                            if (mutation.completionAmount == null) {
                                return@mut
                            }
                            println("Executing completion mutation: ${action.id} $mutation")
                            when (mutation.type) {
                                MutationType.ADD -> addResource(mutation.resource, mutation.completionAmount)
                                MutationType.SET -> setResource(mutation.resource, mutation.completionAmount)
                                MutationType.RATE_MULTIPLY -> TODO()
                            }
                        }
                        is ScienceMutation -> {
                            // it doesn't make sense to have a completion mutation for science
                            println("Error: completion mutation for science doesn't make sense: $mutation")
                        }
                    }
                }
            }
        }
        // clean up any expired actions
        activeActions.removeIf { it.turnsRemaining == 0 }

        // recalculate science rates

        // update company resources
    }
}

enum class ResourceType {
    GOLD, INFLUENCE, CONSTRUCTION_MATERIALS
}