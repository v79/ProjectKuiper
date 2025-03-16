package technology.editor

import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.api.Control
import godot.api.LineEdit
import godot.api.RichTextLabel
import godot.api.Slider
import godot.extension.getNodeAs

@RegisterClass
class ScienceRangeEdit : Control() {

    @RegisterProperty
    var min = 0

    @RegisterProperty
    var max = 0

    // UI elements
    private lateinit var label: RichTextLabel
    private lateinit var minSlider: Slider
    private lateinit var maxSlider: Slider
    private lateinit var minLabel: LineEdit
    private lateinit var maxLabel: LineEdit

    @RegisterFunction
    override fun _ready() {
        label = getNodeAs("%ScienceLabel")!!
        minSlider = getNodeAs("%MinimumSlider")!!
        maxSlider = getNodeAs("%MaximumSlider")!!
        minLabel = getNodeAs("%MinimumCostLabel")!!
        maxLabel = getNodeAs("%MaximumCostLabel")!!
    }

    @RegisterFunction
    override fun _process(delta: Double) {

    }

    @RegisterFunction
    fun onMinSliderValueChanged(value: Float) {
        min = value.toInt()
        if (min > max) {
            max = min
            maxSlider.value = max.toDouble()
        }
        minLabel.text = min.toString()
    }

    @RegisterFunction
    fun onMaxSliderValueChanged(value: Float) {
        max = value.toInt()
        if (max < min) {
            min = max
            minSlider.value = min.toDouble()
        }
        maxLabel.text = max.toString()
    }

    @RegisterFunction
    fun onMinLabelValueChanged(value: String) {
        min = value.toIntOrNull() ?: 0
        if (min > max) {
            max = min
        }
        minSlider.value = min.toDouble()
    }

    @RegisterFunction
    fun onMaxLabelValueChanged(value: String) {
        max = value.toIntOrNull() ?: 0
        if (max < min) {
            min = max
        }
        minSlider.value = min.toDouble()
    }

    @RegisterFunction
    fun setLabel(richText: String) {
        label.text = richText
    }

    fun setDefault(min: Int, max: Int) {
        this.min = min
        this.max = max
        minSlider.value = min.toDouble()
        maxSlider.value = max.toDouble()
        minLabel.text = min.toString()
        maxLabel.text = max.toString()
    }

    fun getRange() = Pair(min, max)
    fun setRange(range: Pair<Int, Int>) {
        min = range.first
        max = range.second
        minLabel.text = min.toString()
        maxLabel.text = max.toString()
        minSlider.value = min.toDouble()
        maxSlider.value = max.toDouble()
    }
}
