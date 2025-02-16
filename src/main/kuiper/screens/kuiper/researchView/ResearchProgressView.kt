package screens.kuiper.researchView

import godot.*
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.core.Vector2
import godot.extensions.getNodeAs
import technology.Science

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
	var progress: Int = 0

	@RegisterProperty
	@Export
	var cost: Int = 0

	@RegisterProperty
	@Export
	var done: Boolean = false

	@RegisterProperty
	@Export
	var sciSummary: String = ""

	@RegisterProperty
	@Export
	var progressPct: Double = 0.0

	// UI elements
	private lateinit var label: Label
	private lateinit var progressBar: ProgressBar
	private lateinit var hBoxContainer: HBoxContainer
	private lateinit var doneLabel: Label
	private lateinit var scienceProgressContainer: HBoxContainer

	private val completedSciences: MutableSet<Science> = mutableSetOf()

	// Called when the node enters the scene tree for the first time.
	@RegisterFunction
	override fun _ready() {
		label = getNodeAs("VBoxContainer/HBoxContainer/Container/Label")!!
		progressBar = getNodeAs("VBoxContainer/HBoxContainer/Container/ProgressBar")!!
		hBoxContainer = getNodeAs("VBoxContainer/HBoxContainer")!!
		doneLabel = getNodeAs("VBoxContainer/HBoxContainer/DoneLabel")!!
		scienceProgressContainer = getNodeAs("VBoxContainer/MarginContainer/ScienceProgressContainer")!!
	}

	// Called every frame. 'delta' is the elapsed time since the previous frame.
	@RegisterFunction
	override fun _process(delta: Double) {
		label.text = title
		progressBar.value = progressPct
		progressBar.tooltipText = "Progress: $progress of $cost"
		if (done) {
			doneLabel.text = "✅"
		}
	}

	@RegisterFunction
	fun addCompletedScience(science: Science) {
		if (completedSciences.add(science)) {
			val colorRect = ColorRect()
			colorRect.setName(science.name)
			colorRect.color = science.color()
			colorRect.setCustomMinimumSize(Vector2(20.0, 20.0))
			colorRect.setSize(Vector2(20.0, 20.0))
			colorRect.tooltipText = science.displayName
			colorRect.resetSize()
			scienceProgressContainer.addChild(colorRect)
		}
	}
}
