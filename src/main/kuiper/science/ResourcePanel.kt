package science

import SignalBus
import actions.ResourceType
import godot.Control
import godot.HBoxContainer
import godot.RichTextLabel
import godot.VBoxContainer
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.core.Vector2
import godot.core.connect
import godot.extensions.getNodeAs
import screens.kuiper.pullDownPanel.PullDownPanel

@RegisterClass
class ResourcePanel : Control() {

    // Globals
    private lateinit var signalBus: SignalBus

    // UI elements
    private lateinit var panelContents: VBoxContainer
    private lateinit var iconRow: HBoxContainer
    private lateinit var pulldownControl: PullDownPanel

    private var resourceType: ResourceType = ResourceType.GOLD

    @RegisterFunction
    override fun _ready() {
        signalBus = getNodeAs("/root/SignalBus")!!

        iconRow = getNodeAs("%ResourcePanel")!!
        panelContents = getNodeAs("%PanelContents")!!
        pulldownControl = getNodeAs("%PulldownPanel")!!

        signalBus.updateResource.connect { resourceName, value, summary ->
            resourceType = ResourceType.valueOf(resourceName.uppercase())
            iconRow.getNodeAs<ResourceDisplay>(resourceType.name)?.apply {
                updateValue(value)
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
