package science

import SignalBus
import actions.ResourceType
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.Control
import godot.api.HBoxContainer
import godot.api.RichTextLabel
import godot.api.VBoxContainer
import godot.core.Vector2
import godot.core.connect
import godot.extension.getNodeAs
import screens.kuiper.pullDownPanel.PullDownPanel
import state.GameState

@RegisterClass
class ResourcePanel : Control() {

    // Globals
    private lateinit var signalBus: SignalBus
    private lateinit var gameState: GameState

    // UI elements
    private lateinit var panelContents: VBoxContainer
    private lateinit var iconRow: HBoxContainer
    private lateinit var pulldownControl: PullDownPanel

    private var resourceType: ResourceType = ResourceType.GOLD

    @RegisterFunction
    override fun _ready() {
        signalBus = getNodeAs("/root/SignalBus")!!
        gameState = getNodeAs("/root/GameState")!!

        iconRow = getNodeAs("%ResourcePanel")!!
        panelContents = getNodeAs("%PanelContents")!!
        pulldownControl = getNodeAs("%PulldownPanel")!!

        signalBus.updateResource.connect { resourceName, value ->
            resourceType = ResourceType.valueOf(resourceName.uppercase())
            iconRow.getNodeAs<ResourceDisplay>(resourceType.name)?.apply {
                updateValue(value)
                val summary = gameState.company.getCostsPerTurnSummary(resourceType)
                panelContents.getNodeAs<RichTextLabel>("${resourceType}_summary")?.apply {
                    val headline =
                        "[img=25]${resourceType.spritePath}[/img] [b]${resourceType.displayName}:[/b] %.2f".format(value)
                    text = if (summary.isNotEmpty()) {
                        "$headline\n\t$summary"
                    } else {
                        headline
                    }
                    setFitContent(true)
                }
            }
            signalBus.recalcPulldownPanelSignal.emit(panelContents)
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
        panelContents.addChild(label)
        panelContents.resetSize()
    }
}
