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
class ResourceDisplay : Control() {

	// Globals
	private lateinit var signalBus: SignalBus

	@RegisterProperty
	@Export
	var resourceName: String = ""

	@RegisterProperty
	@Export
	lateinit var icon: Resource

	@RegisterProperty
	@Export
	var isScience: Boolean = true

	// Data
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
		iconTexture = getNodeAs("%ResourceIcon")!!

		val png = GD.load<CompressedTexture2D>(icon.resourcePath)
		iconTexture.texture = png

		setTooltipText("${resourceName}: $value")
	}

	@RegisterFunction
	fun updateValue(newValue: Float) {
		this.value = newValue
		label.setText("%.1f".format(this.value))
		setTooltipText("$resourceName: %.2f".format(this.value))
	}

}
