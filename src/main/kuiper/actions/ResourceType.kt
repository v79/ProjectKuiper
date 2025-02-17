package actions

enum class ResourceType(val displayName: String, val spritePath: String = "") {
    GOLD(
        "gold", "res://assets/textures/icons/icon-coins-128x128.png"
    ),
    INFLUENCE(
        "influence", "res://assets/textures/icons/icon-influence-128x128.png"
    ),
    CONSTRUCTION_MATERIALS("construction materials", "res://assets/textures/icons/icon-conmats-128x128.png"),
    NONE("")
}