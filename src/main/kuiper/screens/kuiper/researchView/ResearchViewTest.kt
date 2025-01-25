package screens.kuiper.researchView

import godot.*
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.extensions.getNodeAs
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import technology.Technology
import godot.global.GD
import technology.ResearchProgress
import technology.Science

@RegisterClass
class ResearchViewTest : Control() {

	private val testFile = "res://techs/t1TestTest.json"
	private var techList = mutableListOf<Technology>()
	private var msgText = ""

	// UI elements
	private lateinit var listBox: VBoxContainer
	private lateinit var button: Button
	private lateinit var message: Label
	private val techItemList: MutableList<ResearchProgressView> = mutableListOf()

	@RegisterFunction
	override fun _ready() {
		GD.print("hello")
		if (FileAccess.fileExists(testFile)) {
			val file = FileAccess.open(testFile, FileAccess.ModeFlags.READ)!!.getAsText()
			techList = Json.decodeFromString(ListSerializer(Technology.serializer()), file).toMutableList()
		} else {
			GD.printErr("Unable to find file $testFile")
		}

		// get UI components
		listBox = getNodeAs("VBoxContainer")!!
		button = getNodeAs("Button")!!
		message = getNodeAs("Label")!!

		// randomise tech science requirements and add techs to UI
		val techItemScene =
			ResourceLoader.load("res://src/main/kuiper/screens/kuiper/researchView/research_progress_view.tscn") as PackedScene
		techList.forEach {
			GD.print("Populating tech ${it.title}")
			Science.entries.forEach { science ->
				val req = GD.randfRange(1.0f, 10.0f)
				val progress = GD.randfRange(0.0f, req)
				it.unlockRequirements[science] = ResearchProgress(req, progress)
			}
			val techItem = techItemScene.instantiate() as ResearchProgressView
			techItemList.add(techItem)
			techItem.title = it.title
			techItem.progress = it.progress
			techItem.cost = it.totalCost
			techItem.id = it.id
			listBox.addChild(techItem)
			listBox.resetSize()
		}
	}

	@RegisterFunction
	override fun _process(delta: Double) {
		message.text = msgText
		techList.forEach {
			val techItem = techItemList.find { tech -> tech.id == it.id }
			techItem?.progress = it.progress
			if (it.researched) {
				techItem?.done = true
			}
		}
	}

	@RegisterFunction
	fun _on_button_pressed() {
		msgText = "Researching!"
		techList.forEach {
			Science.entries.forEach { science ->
				it.addProgress(science, GD.randfRange(0.0f, 10.0f))
				GD.print("Tech ${it.title} ${science.label} progress: ${it.progress} / ${it.unlockRequirements[science]?.required}}")
			}
		}
	}
}
