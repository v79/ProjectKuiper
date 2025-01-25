package screens.kuiper.researchView

import godot.Control
import godot.HBoxContainer
import godot.Label
import godot.ProgressBar
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.extensions.getNodeAs

@RegisterClass
class ResearchProgressView : Control() {

	@RegisterProperty
	@Export
	var id: Int = -1
	
	@RegisterProperty
	@Export
	var title: String = ""

	@RegisterProperty
	@Export
	var progress: Float = 0.0f

	@RegisterProperty
	@Export
	var cost: Float = 0.0f

	@RegisterProperty
	@Export
	var done: Boolean = false

	// UI elements
	private lateinit var label: Label
	private lateinit var progressBar: ProgressBar
	private lateinit var hBoxContainer: HBoxContainer
	private lateinit var doneLabel: Label

	// Called when the node enters the scene tree for the first time.
	@RegisterFunction
	override fun _ready() {
		label = getNodeAs("VBoxContainer/HBoxContainer/Container/Label")!!
		progressBar = getNodeAs("VBoxContainer/HBoxContainer/Container/ProgressBar")!!
		hBoxContainer = getNodeAs("VBoxContainer/HBoxContainer")!!
		doneLabel = getNodeAs("VBoxContainer/HBoxContainer/DoneLabel")!!

	}

	// Called every frame. 'delta' is the elapsed time since the previous frame.
	@RegisterFunction
	override fun _process(delta: Double) {
		label.text = title
		progressBar.value = progress.toDouble()
		progressBar.tooltipText = "Progress: $progress of $cost"
		if(done) {
			doneLabel.text = "✅"
		}
	}
}
