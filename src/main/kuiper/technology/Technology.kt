package technology

import actions.Action
import kotlinx.serialization.Serializable

@Deprecated("Newer version coming soon")
class TechWeb {
    val technologies = ArrayList<Technology>(100)

    /**
     * Find the list of technologies that are unlocked by this technology
     */
    fun unlockedBy(technology: Technology): List<Technology> {
        return technologies.filter { it.requires.contains(technology.id) }
    }
}

/**
 * Technologies are the main way to unlock new actions and features in the game
 * They are researched not through deliberate player action, but through the accumulation of science points
 */
@Serializable
class Technology(val id: Int, var title: String, var description: String, var tier: TechTier, var status: TechStatus) {

    val progressPct: Double
        get() = if (researched) 1.0 else ((100.0 / totalCost) * progress)

    val researched: Boolean
        get() = unlockRequirements.values.all { it.progress >= it.cost }

    val unlockRequirements: MutableMap<Science, ResearchProgress> = mutableMapOf()

    val totalCost: Int
        get() = unlockRequirements.values.sumOf { it.cost }
    val progress: Int
        get() = unlockRequirements.values.sumOf { it.progress }

    fun addProgress(science: Science, amount: Int) {
        val reqs = unlockRequirements[science]
        if (reqs != null) {
            if (reqs.progress + amount >= reqs.cost) {
                reqs.progress = reqs.cost
            } else {
                reqs.progress += amount
            }
        }

    }

    var requires: MutableList<Int> = mutableListOf()
    var actionsUnlocked: MutableList<Action> = mutableListOf()
    var actionsDeprecated: MutableList<Action> = mutableListOf()

    override fun toString(): String {
        return "Technology(id=$id, title='$title', tier=$tier. progress=$progress, totalCost=$totalCost, researched=$researched)"
    }

    fun scienceProgressComplete(science: Science): Boolean {
        return unlockRequirements[science]?.progress == unlockRequirements[science]?.cost
    }

    companion object {
        val EMPTY = Technology(-1, "Empty", "Empty", TechTier.TIER_1, TechStatus.UNLOCKED)
    }

}

/**
 * Tech tiers will be represented through circles in the UI
 */
enum class TechTier(val description: String) {
    TIER_0("Starting technologies"),
    TIER_1("Basic technologies"),
    TIER_2("Intermediate technologies"),
    TIER_3("Advanced technologies"),
    TIER_4("Expert technologies"),
    TIER_5("Future technologies")
}

enum class TechStatus {
    LOCKED,
    UNLOCKED,
    RESEARCHING,
    RESEARCHED
}

@Serializable
data class ResearchProgress(val cost: Int, var progress: Int = 0)