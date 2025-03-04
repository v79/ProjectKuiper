package technology.editor

import godot.Control
import godot.Label
import godot.RichTextLabel
import godot.Slider
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.extensions.getNodeAs

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
	private lateinit var minLabel: Label
	private lateinit var maxLabel: Label

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
	fun setLabel(richText: String) {
		label.text = richText
	}
}
