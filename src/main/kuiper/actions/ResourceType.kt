package actions

enum class ResourceType(val displayName: String, val spritePath: String = "") {
    GOLD(
        "Gold", "res://assets/textures/icons/icon-coins-128x128.png"
    ),
    INFLUENCE(
        "Influence", "res://assets/textures/icons/icon-influence-128x128.png"
    ),
    CONSTRUCTION_MATERIALS("Construction materials", "res://assets/textures/icons/icon-conmats-128x128.png"),
    NONE("");

    /**
     * Get a BBCode icon for this resource type
     * @param size the size of the icon
     */
    fun bbCodeIcon(size: Int = 32): String {
        return "[img=$size]$spritePath[/img]"
    }
}