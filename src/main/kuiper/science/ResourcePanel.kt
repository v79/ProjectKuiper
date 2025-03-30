package science

import SignalBus
import actions.ResourceType
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.Control
import godot.api.HBoxContainer
import godot.api.RichTextLabel
import godot.core.Vector2
import godot.core.connect
import godot.extension.getNodeAs
import state.GameState

@RegisterClass
class ResourcePanel : Control() {

    // Globals
    private lateinit var signalBus: SignalBus
    private lateinit var gameState: GameState

    // UI elements
    private lateinit var iconRow: HBoxContainer

    private var resourceType: ResourceType = ResourceType.GOLD

    @RegisterFunction
    override fun _ready() {
        signalBus = getNodeAs("/root/SignalBus")!!
        gameState = getNodeAs("/root/GameState")!!

        iconRow = getNodeAs("%ResourcePanel")!!

        signalBus.updateResource.connect { resourceName, value ->
            resourceType = ResourceType.valueOf(resourceName.uppercase())
            iconRow.getNodeAs<ResourceDisplay>(resourceType.name)?.apply {
                updateValue(value)
            }
        }
    }

    @RegisterFunction
    fun addResource(res: ResourceType, rate: Int) {
        iconRow.getNodeAs<ResourceDisplay>(res.name)?.apply {
            updateValue(rate.toFloat())
        }
        val label = RichTextLabel().apply {
            bbcodeEnabled = true
            scrollActive = false
            scrollToLine(0)
            customMinimumSize = Vector2(300.0, 30.0)
            setName("${res.name}_summary")
            text = "[img=25]${res.spritePath}[/img] [b]${res.displayName}:[/b] $rate"
        }
    }
}
