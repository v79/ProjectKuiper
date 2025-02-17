package science

import SignalBus
import godot.*
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.core.connect
import godot.extensions.getNodeAs
import godot.global.GD

@RegisterClass
class ScienceDisplay : Control() {

	// Globals
	lateinit var signalBus: SignalBus

	@RegisterProperty
	@Export
	var scienceName: String = ""

	@RegisterProperty
	@Export
	lateinit var icon: Resource

	@RegisterProperty
	@Export
	var value: Float = 0.0f

	// UI elements
	private lateinit var label: Label
	private lateinit var iconTexture: TextureRect

	@RegisterFunction
	override fun _ready() {
		signalBus = getNodeAs("/root/SignalBus")!!

		label = getNodeAs("%RateLabel")!!
		iconTexture = getNodeAs("%ScienceIcon")!!

		val png = GD.load<CompressedTexture2D>(icon.resourcePath)
		iconTexture.texture = png

		setTooltipText("$scienceName: $value")

		signalBus.updateScience.connect { science, value ->
			if (science.lowercase() == scienceName.lowercase()) {
				updateValue(value)
			}
		}
	}

	@RegisterFunction
	fun updateValue(value: Float) {
		this.value = value
		label.text = "%.1f".format(value)
		setTooltipText("$scienceName: %.2f".format(value))
	}

}
