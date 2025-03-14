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
     * Zones are areas of the solar system. Each zone has a list of locations
     */
    val zones: MutableList<Zone> = mutableListOf()

    /**
     * Notification history/memory to ensure we don't sent the same notification multiple times.
     * This can be pruned regularly
     */
    private val notificationHistory: MutableSet<Int> = mutableSetOf()

    /**
     * Activate the given action, adding it to the list of active actions, and spend any initial costs
     */
    fun activateAction(hex: Hex, action: Action) {
        action.turnsRemaining = action.turns
        activeActions.add(action)
        action.initialCosts.forEach { cost ->
            resources.merge(cost.key, cost.value, Int::minus)
        }
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
     * Execute all the active actions, decrementing the turns remaining and applying the mutations
     */
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
                            // it doesn't make sense to have a completion mutation for science, but no need to warn
//                            logWarning("Error: completion mutation for science doesn't make sense: $mutation")
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
                        science.key, "There are no ${science.key.displayName} points available to spend this turn"
                    )
                )
            }
            technologies.filter { it.status == TechStatus.UNLOCKED }.shuffled().forEach { technology ->
                // only research techs that have all their requirements met
                if (getRequiredTechsFor(technology).all { it.status == TechStatus.RESEARCHED }) {
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
        }
        technologies.forEach { technology ->
            if (technology.tier != TechTier.TIER_0) {
                // send a notification if the tech research is 50% complete
                if (technology.progressPct > 50.0f) {
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

                // unlock any techs that require this one if all of its requirements are at least 50% researched
                technologies.filter { tech -> tech.requires.contains(technology.id) }.forEach {
                    if (getRequiredTechsFor(it).all { reqTech -> reqTech.progressPct > 50.0 }) {
                        it.status = TechStatus.UNLOCKED
                        val notification = Notification.TechUnlocked(
                            it, "Technology ${it.title} is now unlocked for research"
                        )
                        if (!notificationHistory.contains(notification.technology.id)) {
                            notifications.add(notification)
                        }
                        notificationHistory.add(notification.technology.id)
                        log("Unlocking technology ${it.title}")
                    }
                }

                if (technology.progressPct >= 100.0) {
                    logWarning("Company: Technology ${technology.title} is complete!")
                    // prune any progress notifications for this tech
//                    notificationHistory.removeIf { it == technology.id && technology.status == TechStatus.RESEARCHED }
                    if (technology.status != TechStatus.RESEARCHED) {
                        val notification = Notification.ResearchComplete(
                            technology, "Research complete: ${technology.title}"
                        )
                        // TODO: this doesn't work either, there will have been a previous notification for this ID!
                        if (!notificationHistory.contains(notification.technology.id)) {
                            notifications.add(notification)
                        }
                        notificationHistory.add(notification.technology.id)
                        // Show a debug error if a tech is researched before all its requirements are
                        getRequiredTechsFor(technology).forEach {
                            if (it.status != TechStatus.RESEARCHED) {
                                logError("Technology ${technology.title} is researched before all requirements are met")
                            }
                        }
                    }
                    technology.status = TechStatus.RESEARCHED
                }
            }
        }
        return notifications
    }

    /**
     * Get the list of technologies that are required for the given technology
     */
    private fun getRequiredTechsFor(technology: Technology) = technologies.filter { it.id in technology.requires }

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
    fun processBuildings(): List<Notification> {
        val notifications: MutableList<Notification> = mutableListOf()
        log("Processing buildings:")
        zones.forEach { zone ->
            zone.locations.forEach { loc ->
                loc.buildings.forEach { building ->
                    when (building.key) {
                        is Building.HQ -> {
                            val hq = building.key as Building.HQ
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

        return notifications
    }

    /**
     * For the given resource type, return a summary string in the format
     * <amount> from <action> OR
     * <amount> from <building>
     */
    fun getCostsPerTurnSummary(resourceType: ResourceType): String {
        val sBuilder = StringBuilder()
        activeActions.forEach { action ->
            action.getCostsPerTurn().filter { it.key == resourceType }.forEach { cost ->
                sBuilder.append("[color=red]-")
                sBuilder.append(cost.value)
                sBuilder.append("[/color] from ")
                sBuilder.append(action.actionName)
                sBuilder.appendLine()
            }
        }
        zones.forEach { zone ->
            zone.locations.forEach { loc ->
                loc.buildings.forEach { building ->
                    building.key.runningCosts.filter { it.key == resourceType }.forEach { cost ->
                        sBuilder.append("[color=red]-")
                        sBuilder.append(cost.value)
                        sBuilder.append("[/color] from ")
                        sBuilder.append(building.key.name)
                        sBuilder.appendLine()
                    }
                    building.key.resourceGeneration.filter { it.key == resourceType }.forEach { cost ->
                        sBuilder.append("[color=green]+")
                        sBuilder.append(cost.value)
                        sBuilder.append("[/color] from ")
                        sBuilder.append(building.key.name)
                        sBuilder.appendLine()
                    }
                }
            }
        }

        return sBuilder.toString()
    }

    /**
     * Sum up all the resource costs per turn for all the active actions
     */
    fun getCostsPerTurnSummary(): Map<ResourceType, Int> {
        val costsPerTurn = mutableMapOf<ResourceType, Int>()

        activeActions.forEach { action ->
            action.getCostsPerTurn().forEach { (resourceType, cost) ->
                costsPerTurn.merge(resourceType, cost, Int::plus)
            }
        }
        return costsPerTurn
    }
}
