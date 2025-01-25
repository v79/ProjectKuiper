package technology

import kotlinx.serialization.Serializable

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
class Technology(val id: Int, val title: String, val description: String, val tier: TechTier) {

//    val progressPct: Float
//        get() = if (researched) 1f else progress / cost

    val researched: Boolean
        get() = unlockRequirements.values.all { it.progress >= it.required }

    // I think this should be a map instead?
    val unlockRequirements: MutableMap<Science
            , ResearchProgress> = mutableMapOf()

    val totalCost: Float
        get() = unlockRequirements.values.sumOf { it.required.toDouble() }.toFloat()
    val progress: Float
        get() = unlockRequirements.values.sumOf { it.progress.toDouble() }.toFloat()

    fun addProgress(science: Science, amount: Float) {
        unlockRequirements[science]?.let {
            it.progress += amount
        }
    }

    var requires: MutableList<Int> = mutableListOf()
    var actionsUnlocked: MutableList<Action> = mutableListOf()
    var actionsDeprecated: MutableList<Action> = mutableListOf()

}

/**
 * Tech tiers will be represented through circles in the UI
 */
enum class TechTier(val description: String) {
    TIER_1("Basic technologies"),
    TIER_2("Intermediate technologies"),
    TIER_3("Advanced technologies"),
    TIER_4("Expert technologies"),
    TIER_5("Future technologies")
}

@Serializable
class ResearchProgress(val required: Float, var progress: Float = 0.0f)