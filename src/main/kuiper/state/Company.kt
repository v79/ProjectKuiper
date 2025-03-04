package state

import LogInterface
import actions.*
import hexgrid.Hex
import kotlinx.serialization.Serializable
import technology.Science
import technology.TechStatus
import technology.TechTier
import technology.Technology

/**
 * The company that the player is running
 * It will likely have many other properties over time
 */
@Serializable
class Company(var name: String) : LogInterface {

    override var logEnabled = true

    /**
     * Resources are the primary currency of the game
     */
    val resources: MutableMap<ResourceType, Int> = mutableMapOf(
        ResourceType.GOLD to 0, ResourceType.INFLUENCE to 0, ResourceType.CONSTRUCTION_MATERIALS to 0
    )

    /**
     * Sciences are the primary way to unlock new technologies
     */
    val sciences: MutableMap<Science, Float> = mutableMapOf()

    /**
     * Currently active actions which have a limited duration
     */
    val activeActions: MutableList<Action> = mutableListOf()

    /**
     * The list of technologies that the company has or could research
     */
    val technologies: MutableList<Technology> = mutableListOf()

    /**
     * Activate the given action, adding it to the list of active actions
     */
    fun activateAction(hex: Hex, action: Action) {
        action.turnsRemaining = action.turns
        activeActions.add(action)
        log("Company: Activating action $action")
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
        log("Company: Next turn")

        val completed: MutableList<Action> = mutableListOf()


        // perform actions
        // perform ongoing action (if any)
        log("Company: Executing ${activeActions.size} active actions")
        activeActions.forEach act@{ action ->
            // construct buildings

            // progress projects

            // mutate resources
            val mutations = action.getMutations()
            mutations.forEach { mutation ->
                when (mutation) {
                    is ResourceMutation -> {
                        if (mutation.amountPerYear != 0) {
                            log("Executing mutation: ${action.id} $mutation")
                            when (mutation.type) {
                                MutationType.ADD -> addResource(mutation.resource, mutation.amountPerYear)
                                MutationType.SUBTRACT -> addResource(mutation.resource, -mutation.amountPerYear)
                                MutationType.RESET -> setResource(mutation.resource, mutation.amountPerYear)
                                MutationType.RATE_MULTIPLY -> multiplyResource(
                                    mutation.resource, mutation.amountPerYear.toFloat()
                                )
                            }
                        }
                    }

                    is ScienceMutation -> {
                        if (mutation.amount != 0.0f) {
                            log("Executing science mutation: ${action.id} $mutation")
                            when (mutation.type) {
                                MutationType.ADD -> sciences.merge(
                                    mutation.science, mutation.amount, Float::plus
                                )

                                MutationType.SUBTRACT -> sciences.merge(
                                    mutation.science, mutation.amount, Float::minus
                                )

                                MutationType.RESET -> sciences[mutation.science] = mutation.amount
                                MutationType.RATE_MULTIPLY -> sciences.merge(
                                    mutation.science, mutation.amount, Float::times
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
                            log("Executing completion mutation: ${action.id} $mutation")
                            when (mutation.type) {
                                MutationType.ADD -> addResource(mutation.resource, mutation.completionAmount)
                                MutationType.SUBTRACT -> addResource(mutation.resource, -mutation.completionAmount)
                                MutationType.RESET -> setResource(mutation.resource, mutation.completionAmount)
                                MutationType.RATE_MULTIPLY -> TODO()
                            }
                        }

                        is ScienceMutation -> {
                            // it doesn't make sense to have a completion mutation for science
                            logWarning("Error: completion mutation for science doesn't make sense: $mutation")
                        }
                    }
                }
            }
        }

        // spend the science points on research
        doResearch()

        // clean up any expired actions
        completed.addAll(activeActions.filter { it.turnsRemaining == 0 })
        activeActions.removeIf { it.turnsRemaining == 0 }

        // TODO: recalculate science rates
        sciences.replaceAll { _, rate -> rate + 10.0f }
        // update company resources

        return completed
    }

    /**
     * Do the research. For each of the sciences, spend the points on the unlocked technologies.
     * The list of techs will be shuffled to avoid biasing the research towards the first techs in the list
     */
    private fun doResearch() {
        sciences.forEach { science ->
            technologies.filter { it.status == TechStatus.UNLOCKED }.shuffled().forEach { technology ->
                val cost = technology.researchProgress[science.key]?.cost ?: 0
                val currentProgress = technology.researchProgress[science.key]?.progress ?: 0
                val remaining = cost - currentProgress
                val toSpend = sciences[science.key] ?: 0.0f
                if (toSpend > 0.0f) {
                    if (toSpend >= remaining) {
                        technology.addProgress(science.key, remaining)
                        sciences[science.key] = toSpend - remaining
                    } else {
                        technology.addProgress(science.key, toSpend.toInt())
                        sciences[science.key] = 0.0f
                    }
                }
            }
        }
        technologies.forEach { technology ->
            if (technology.tier != TechTier.TIER_0) {
                if (technology.progressPct > 50.0f) {
                    log("Company: Technology ${technology.title} now ${technology.progressPct}% complete")
                }
                if (technology.progressPct >= 100.0) {
                    logWarning("Company: Technology ${technology.title} is complete!")
                    technology.status = TechStatus.RESEARCHED
                }
            }
        }
    }
}
