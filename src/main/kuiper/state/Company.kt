package state

import actions.*
import godot.global.GD
import hexgrid.Hex
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

    val sciences: MutableMap<Science, Float> = mutableMapOf()

    // Currently active actions which have a limited duration
    val activeActions: MutableList<Action> = mutableListOf()

    /**
     * Activate the given action, adding it to the list of active actions
     */
    fun activateAction(hex: Hex, action: Action) {
        action.turnsRemaining = action.turns
        activeActions.add(action)
        GD.print("Company: Activating action $action")
    }

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
     * Return a list of completed actions (turns remaining == 0)
     */
    fun nextTurn(): List<Action> {

        val completed: MutableList<Action> = mutableListOf()
        // spend science amongst technologies, i.e. perform research
        // for now, just reset to zero!
//        sciences.replaceAll { _, _ -> 0.0f }

        // perform actions
        // perform ongoing action (if any)
        activeActions.forEach act@{ action ->
            // construct buildings

            // progress projects

            // mutate resources
            val mutations = action.getMutations()
            mutations.forEach { mutation ->
                when (mutation) {
                    is ResourceMutation -> {
                        if (mutation.amountPerYear != 0) {
                            println("Executing mutation: ${action.id} $mutation")
                            when (mutation.type) {
                                MutationType.ADD -> addResource(mutation.resource, mutation.amountPerYear)
                                MutationType.SUBTRACT -> addResource(mutation.resource, -mutation.amountPerYear)
                                MutationType.RESET -> setResource(mutation.resource, mutation.amountPerYear)
                                MutationType.RATE_MULTIPLY -> multiplyResource(
                                    mutation.resource,
                                    mutation.amountPerYear.toFloat()
                                )
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

                                MutationType.SUBTRACT -> sciences.merge(
                                    mutation.science,
                                    mutation.amount,
                                    Float::minus
                                )

                                MutationType.RESET -> sciences[mutation.science] = mutation.amount
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
                                MutationType.SUBTRACT -> addResource(mutation.resource, -mutation.completionAmount)
                                MutationType.RESET -> setResource(mutation.resource, mutation.completionAmount)
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
        completed.addAll(activeActions.filter { it.turnsRemaining == 0 })
        activeActions.removeIf { it.turnsRemaining == 0 }

        // recalculate science rates
        sciences.replaceAll { _, rate -> rate + 1.0f }
        // update company resources

        return completed
    }
}
