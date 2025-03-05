package technology

import actions.Action
import kotlinx.serialization.Serializable
import kotlin.random.Random

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
        get() = if (researched) 100.0 else ((100.0 / totalCost) * progress)

    val researched: Boolean
        get() = researchProgress.values.all { it.progress >= it.cost }

    /**
     * The ranges of science points that unlock this technology. The first number is the minimum, the second is the maximum.
     * These values will be used during game setup to determine the actual cost of the technology
     */
    val unlockRanges: MutableMap<Science, Pair<Int, Int>> = mutableMapOf()

    /**
     * Higher tier techs will have higher cost multipliers. This allows us to convert a range from, say, Physics(60,75) to a cost of 140 physics points with a multiplier of 2.0
     */
    var multiplier: Double = 1.0

    /**
     * A map of each science and the progress towards unlocking the technology
     */
    val researchProgress: MutableMap<Science, ResearchProgress> = mutableMapOf()

    val totalCost: Int
        get() = researchProgress.values.sumOf { it.cost }
    val progress: Int
        get() = researchProgress.values.sumOf { it.progress }


    /**
     * Randomise the costs of the technology based on the ranges provided
     */
    fun randomiseCosts() {
        unlockRanges.forEach { unlock ->
            val randomValue = if (unlock.value.first >= unlock.value.second) unlock.value.first else Random.nextInt(
                unlock.value.first,
                unlock.value.second
            )
            researchProgress[unlock.key] = ResearchProgress(
                randomValue, 0
            )
        }
    }

    /**
     * Add progress to a specific science, capping at the cost of the technology for that science
     */
    fun addProgress(science: Science, amount: Int) {
        val reqs = researchProgress[science]
        if (reqs != null) {
            if (reqs.progress + amount >= reqs.cost) {
                reqs.progress = reqs.cost
            } else {
                reqs.progress += amount
            }
            researchProgress[science] = reqs
        }
    }

    var requires: MutableList<Int> = mutableListOf()
    var actionsUnlocked: MutableList<Action> = mutableListOf()
    var actionsDeprecated: MutableList<Action> = mutableListOf()

    override fun toString(): String {
        return "Technology(id=$id, title='$title', tier=$tier, desc=${description}; progress=$progress, totalCost=$totalCost, researched=$researched)"
    }

    fun scienceProgressComplete(science: Science): Boolean {
        return researchProgress[science]?.progress == researchProgress[science]?.cost
    }

    fun setUnlockRange(science: Science, range: Pair<Int, Int>) {
        unlockRanges[science] = range
    }

    companion object {
        val EMPTY = Technology(-1, "Empty", "Empty", TechTier.TIER_1, TechStatus.UNLOCKED)
    }

}

/**
 * Tech tiers will be represented through circles in the UI
 */
enum class TechTier(val description: String) {
    TIER_0("Starting technologies"), TIER_1("Basic technologies"), TIER_2("Intermediate technologies"), TIER_3("Advanced technologies"), TIER_4(
        "Expert technologies"
    ),
    TIER_5("Future technologies")
}

enum class TechStatus {
    LOCKED, UNLOCKED, RESEARCHING, RESEARCHED
}

@Serializable
data class ResearchProgress(val cost: Int, var progress: Int = 0) {
    override fun toString() = "$progress / $cost"
}