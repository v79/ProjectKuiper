package screens.kuiper

import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.api.ColorRect
import godot.api.Control
import godot.api.Label
import godot.api.LabelSettings
import godot.core.Color
import godot.extension.getNodeAs

// TODO: rename this class, as it's a control node and I need ScienceRate to be a property of the Company
// TODO: move this class, as it's not just for science
@RegisterClass
class ScienceRate : Control() {

    @RegisterProperty
    @Export
    var rateLabel: String = ""

    @RegisterProperty
    @Export
    var colour: Color = Color()

    @RegisterProperty
    @Export
    var description: String = ""

    private lateinit var label: Label
    private lateinit var colorRect: ColorRect
    private var labelSettings = LabelSettings()

    // Called when the node enters the scene tree for the first time.
    @RegisterFunction
    override fun _ready() {
        label = getNodeAs("MarginContainer/HBoxContainer/Rate")!!
        colorRect = getNodeAs("MarginContainer/HBoxContainer/ColorRect")!!
    }

    // Called every frame. 'delta' is the elapsed time since the previous frame.
    // TODO: This shouldn't be called every frame :)
    @RegisterFunction
    override fun _process(delta: Double) {
        label.text = rateLabel
        colorRect.color = colour
        labelSettings.fontColor = colour
        label.setLabelSettings(labelSettings)
        this.tooltipText = "$description: $rateLabel"
    }
}
