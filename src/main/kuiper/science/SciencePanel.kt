package science

import SignalBus
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.Control
import godot.api.HBoxContainer
import godot.api.RichTextLabel
import godot.core.Vector2
import godot.core.connect
import godot.extension.getNodeAs
import technology.Science

@RegisterClass
class SciencePanel : Control() {

    // Globals
    private lateinit var signalBus: SignalBus

    // UI elements
    private lateinit var iconRow: HBoxContainer

    private var science: Science = Science.PHYSICS

    @RegisterFunction
    override fun _ready() {
        signalBus = getNodeAs("/root/SignalBus")!!

        iconRow = getNodeAs("%SciencePanel")!!

        signalBus.updateScience.connect { scienceName, value ->
            if (scienceName != "EUREKA") {
                science = Science.valueOf(scienceName.uppercase())
                iconRow.getNodeAs<ResourceDisplay>(science.name)?.apply {
                    updateValue(value)
                }
            }
        }
    }

    @RegisterFunction
    fun addScience(science: Science, rate: Float) {
        iconRow.getNodeAs<ResourceDisplay>(science.name)?.apply {
            updateValue(rate)
        }
        val label = RichTextLabel().apply {
            bbcodeEnabled = true
            scrollActive = false
            scrollToLine(0)
            customMinimumSize = Vector2(300.0, 30.0)
            setName("${science.name}_summary")
            text = "[img=25]${science.spritePath}[/img] [b]${science.displayName}:[/b] %.2f".format(rate)
        }
    }

}
