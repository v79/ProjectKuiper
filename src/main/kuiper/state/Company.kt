package state

import LogInterface
import actions.*
import hexgrid.Hex
import kotlinx.serialization.Serializable
import notifications.Notification
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
     * Notification history/memory to ensure we don't sent the same notification multiple times.
     * This can be pruned regularly
     */
    val notificationHistory: MutableSet<Int> = mutableSetOf()

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


    fun doActions(): List<Action> {
        val completed: MutableList<Action> = mutableListOf()
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
        // clean up any expired actions
        completed.addAll(activeActions.filter { it.turnsRemaining == 0 })
        activeActions.removeIf { it.turnsRemaining == 0 }
        return completed
    }

    /**
     * Do the research. For each of the sciences, spend the points on the unlocked technologies.
     * The list of techs will be shuffled to avoid biasing the research towards the first techs in the list
     */
    fun doResearch(): List<Notification> {
        val notifications: MutableList<Notification> = mutableListOf()
        sciences.forEach { science ->
            if (science.value == 0.0f) {
                logWarning("Science doesn't have value: $science")
                notifications.add(
                    Notification.NoScienceWarning(
                        science.key,
                        "There are no ${science.key.displayName} points available to spend this turn"
                    )
                )
            }
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
                    // I wanted this to be a one-off event, but of course it will fire every turn that it applies
                    val notification = Notification.ResearchProgress(
                        technology, "Researching ${technology.title} now 50% complete"
                    )
                    if (!notificationHistory.contains(notification.technology.id)) {
                        notifications.add(
                            notification
                        )
                        notificationHistory.add(notification.technology.id)
                    }
                    log("Company: Technology ${technology.title} now ${technology.progressPct}% complete")
                }
                if (technology.progressPct >= 100.0) {
                    logWarning("Company: Technology ${technology.title} is complete!")
                    // prune any progress notifications for this tech
//                    notificationHistory.removeIf { it == technology.id && technology.status == TechStatus.RESEARCHED }
                    if (technology.status != TechStatus.RESEARCHED) {
                        val notification = Notification.ResearchComplete(
                            technology, "Research complete: ${technology.title}"
                        )
                        notifications.add(
                            notification
                        )
                        notificationHistory.add(notification.technology.id)
                    }
                    technology.status = TechStatus.RESEARCHED
                }
            }
        }
        return notifications
    }

    /**
     * Clear all research points
     */
    fun clearResearch(): List<Notification> {
        sciences.replaceAll { _, _ -> 0f }
        return emptyList()
    }

    /**
     * For every building in every sector in every zone, calculate production, costs and sciences
     */
    fun processBuildings(zones: List<Zone>): List<Notification> {
        val notifications: MutableList<Notification> = mutableListOf()
        log("Processing buildings:")
        resources.forEach {
            log("${it.key.name} = ${it.value}")
        }
        zones.forEach { zone ->
            zone.locations.forEach { loc ->
                loc.buildings.forEach { building ->
                    log("Calculating income and costs for building ${building.key.name}")
                    when (building.key) {
                        is Building.HQ -> {
                            val hq = building.key as Building.HQ
                            log("\t${hq.name} produces")
                            log("\tsciences: ${hq.sciencesProduced}")
                            log("\tcosts: ${hq.runningCosts}")
                            log("\tgenerates: ${hq.resourceGeneration}")
                            hq.sciencesProduced.forEach { science ->
                                sciences[science.key] = sciences[science.key]!! + science.value
                            }
                            hq.resourceGeneration.forEach { generation ->
                                resources[generation.key] = resources[generation.key]!! + generation.value
                            }
                            hq.runningCosts.forEach { runningCost ->
                                resources[runningCost.key] = resources[runningCost.key]!! - runningCost.value
                            }
                        }

                        is Building.ScienceLab -> {
                            val lab = building.key as Building.ScienceLab
                            lab.sciencesProduced.forEach { science ->
                                sciences[science.key] = sciences[science.key]!! + science.value
                            }
                            lab.resourceGeneration.forEach { generation ->
                                resources[generation.key] = resources[generation.key]!! + generation.value
                            }
                            lab.runningCosts.forEach { runningCost ->
                                resources[runningCost.key] = resources[runningCost.key]!! - runningCost.value
                            }
                        }
                    }
                }
            }
        }
        log("Processing complete")
        resources.forEach {
            log("${it.key.name} = ${it.value}")
        }


        return notifications
    }
}
