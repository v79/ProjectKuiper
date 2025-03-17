package utils

import actions.ResourceType
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.Button
import godot.api.Control
import godot.api.FileAccess
import godot.api.Label
import godot.core.Color
import godot.extension.getNodeAs
import godot.global.GD
import kotlinx.serialization.json.Json
import state.Sponsor
import technology.Science

@RegisterClass
class SponsorDataGenerator : Control() {

    private val sponsorList = listOf(
        Sponsor(
            id = 1,
            name = "Europe",
            colour = Color.blue,
            startingResources = mapOf(
                ResourceType.GOLD to 100,
                ResourceType.INFLUENCE to 10,
                ResourceType.CONSTRUCTION_MATERIALS to 250
            ),
            baseScienceRate = mapOf(
                Science.ASTRONOMY to 10.0f,
                Science.PHYSICS to 10.0f,
                Science.BIOCHEMISTRY to 10.0f,
                Science.ENGINEERING to 10.0f,
                Science.MATHEMATICS to 10.0f,
                Science.PSYCHOLOGY to 10.0f,
                Science.ENGINEERING to 10.0f
            ),
            introText = "[b]Welcome to Europe![/b]"
        ), Sponsor(
            id = 2,
            name = "North America",
            colour = Color.red,
            startingResources = mapOf(
                ResourceType.GOLD to 150,
                ResourceType.INFLUENCE to 15,
                ResourceType.CONSTRUCTION_MATERIALS to 200
            ),
            baseScienceRate = mapOf(
                Science.ASTRONOMY to 10.0f,
                Science.PHYSICS to 10.0f,
                Science.BIOCHEMISTRY to 10.0f,
                Science.ENGINEERING to 10.0f,
                Science.MATHEMATICS to 10.0f,
                Science.PSYCHOLOGY to 10.0f,
                Science.ENGINEERING to 10.0f
            ),
            introText = "[b]Welcome to North America![/b]\nAlthough the space launch centre was destroyed by the asteroid, you have the technology and political will to rebuild the NASA space programme.."
        ), Sponsor(
            id = 3,
            name = "South America",
            colour = Color.yellow,
            startingResources = mapOf(
                ResourceType.GOLD to 120,
                ResourceType.INFLUENCE to 12,
                ResourceType.CONSTRUCTION_MATERIALS to 150
            ),
            baseScienceRate = mapOf(
                Science.ASTRONOMY to 10.0f,
                Science.PHYSICS to 10.0f,
                Science.BIOCHEMISTRY to 10.0f,
                Science.ENGINEERING to 10.0f,
                Science.MATHEMATICS to 10.0f,
                Science.PSYCHOLOGY to 10.0f,
                Science.ENGINEERING to 10.0f
            ),
            introText = "[b]Welcome to South America![/b]"
        ), Sponsor(
            id = 4,
            name = "Asia",
            colour = Color.tan,
            startingResources = mapOf(
                ResourceType.GOLD to 200,
                ResourceType.INFLUENCE to 12,
                ResourceType.CONSTRUCTION_MATERIALS to 350
            ),
            baseScienceRate = mapOf(
                Science.ASTRONOMY to 10.0f,
                Science.PHYSICS to 10.0f,
                Science.BIOCHEMISTRY to 10.0f,
                Science.ENGINEERING to 10.0f,
                Science.MATHEMATICS to 10.0f,
                Science.PSYCHOLOGY to 10.0f,
                Science.ENGINEERING to 10.0f
            ),
            introText = "[b]Welcome to Asia![/b]"
        ), Sponsor(
            id = 5,
            name = "Africa",
            colour = Color.green,
            startingResources = mapOf(
                ResourceType.GOLD to 80,
                ResourceType.INFLUENCE to 8,
                ResourceType.CONSTRUCTION_MATERIALS to 250
            ),
            baseScienceRate = mapOf(
                Science.ASTRONOMY to 10.0f,
                Science.PHYSICS to 10.0f,
                Science.BIOCHEMISTRY to 10.0f,
                Science.ENGINEERING to 10.0f,
                Science.MATHEMATICS to 10.0f,
                Science.PSYCHOLOGY to 10.0f,
                Science.ENGINEERING to 10.0f
            ),
            introText = "[b]Welcome to Africa![/b]"
        ), Sponsor(
            id = 6,
            name = "Oceania",
            colour = Color.cyan,
            startingResources = mapOf(
                ResourceType.GOLD to 90,
                ResourceType.INFLUENCE to 9,
                ResourceType.CONSTRUCTION_MATERIALS to 150
            ),
            baseScienceRate = mapOf(
                Science.ASTRONOMY to 10.0f,
                Science.PHYSICS to 10.0f,
                Science.BIOCHEMISTRY to 10.0f,
                Science.ENGINEERING to 10.0f,
                Science.MATHEMATICS to 10.0f,
                Science.PSYCHOLOGY to 10.0f,
                Science.ENGINEERING to 10.0f
            ),
            introText = "[b]Welcome to Oceania![/b]"
        ), Sponsor(
            id = 7,
            name = "Antarctica",
            colour = Color.white,
            startingResources = mapOf(
                ResourceType.GOLD to 50,
                ResourceType.INFLUENCE to 5,
                ResourceType.CONSTRUCTION_MATERIALS to 50
            ),
            baseScienceRate = mapOf(
                Science.ASTRONOMY to 10.0f,
                Science.PHYSICS to 10.0f,
                Science.BIOCHEMISTRY to 10.0f,
                Science.ENGINEERING to 10.0f,
                Science.MATHEMATICS to 10.0f,
                Science.PSYCHOLOGY to 10.0f,
                Science.ENGINEERING to 10.0f
            ),
            introText = "[b]Welcome to Antarctica![/b]\nThis will be your hardest challenge."
        )
    )

    @RegisterFunction
    override fun _ready() {
        val json = Json { prettyPrint = true }
        val sponsorListJson = json.encodeToString(sponsorList)
        GD.print(sponsorListJson)
        val saveFile = FileAccess.open("res://assets/data/sponsors.json", FileAccess.ModeFlags.WRITE)
        if (saveFile == null) {
            GD.printErr("Failed to open sponsors.json file for writing")
            return
        }
        saveFile.storeString(sponsorListJson)
        saveFile.close()

        val detailsLabel = getNodeAs<Label>("%Details")!!
        detailsLabel.text = sponsorListJson
        val quitButton = getNodeAs<Button>("%QuitButton")!!
        quitButton.disabled = false
    }

    @RegisterFunction
    fun _on_QuitButton_pressed() {
        GD.print("Quit button pressed")
        getTree()?.quit()
    }

}
