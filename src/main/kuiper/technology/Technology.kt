package technology

import kotlinx.serialization.Serializable

/**
 * Technologies are the main way to unlock new actions and features in the game
 * They are researched not through deliberate player action, but through the accumulation of science points
 */
@Serializable
class Technology(val id: Int, val title: String, val description: String, val tier: TechTier) {

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
    TIER_5("Master technologies")
}