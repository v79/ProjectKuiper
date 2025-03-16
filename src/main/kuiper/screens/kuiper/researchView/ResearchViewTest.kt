package screens.kuiper.researchView

import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.*
import godot.extension.getNodeAs
import godot.global.GD
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import technology.ResearchProgress
import technology.Science
import technology.Technology

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
        val researchView =
            ResourceLoader.load("res://src/main/kuiper/screens/kuiper/researchView/research_progress_view.tscn") as PackedScene
        techList.forEach { tech ->
            GD.print("Populating tech ${tech.title}")
            Science.entries.forEach { science ->
                val req = GD.randiRange(10, 100)
                val progress = 0
                tech.researchProgress[science] = ResearchProgress(req, progress)
            }
            val techItem = researchView.instantiate() as ResearchProgressView
            techItem.title = tech.title
            techItem.progressPct = tech.progressPct
            techItem.cost = tech.totalCost
            techItem.id = tech.id

            techItemList.add(techItem)
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
        techList.forEach { tech ->
            val researchView = techItemList.find { it.id == tech.id }
            researchView?.sciSummary = ""
            Science.entries.forEach { science ->
                val growth = GD.randiRange(0, 10)
                tech.addProgress(science, growth)
                if (tech.totalCost > 0) {
                    if (tech.researchProgress[science]?.progress == tech.researchProgress[science]?.cost) {
                        researchView?.addCompletedScience(science)
                    }
                    researchView?.progressPct = if (tech.progressPct == 1.0) 100.0 else tech.progressPct
                }
            }
            if (tech.researched) {
                msgText = "Researched ${tech.title}!"
            }
        }
    }
}
