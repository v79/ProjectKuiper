package actions

enum class ResourceType(val displayName: String, val spritePath: String = "") {
    GOLD(
        "Gold", "res://assets/textures/icons/icon-coins-128x128.png"
    ),
    INFLUENCE(
        "Influence", "res://assets/textures/icons/icon-influence-128x128.png"
    ),
    CONSTRUCTION_MATERIALS("Construction materials", "res://assets/textures/icons/icon-conmats-128x128.png"),
    NONE("")
}