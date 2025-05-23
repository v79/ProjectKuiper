package science

import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.api.*
import godot.extension.getNodeAs
import godot.global.GD

@RegisterClass
class ResourceDisplay : Control() {

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
