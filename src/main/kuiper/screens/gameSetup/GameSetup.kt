package screens.gameSetup

import LogInterface
import SignalBus
import actions.ResourceType
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterSignal
import godot.api.*
import godot.core.asCachedStringName
import godot.core.signal0
import godot.core.signal1
import godot.extension.getNodeAs
import loaders.DataLoader
import state.*
import technology.Technology

@RegisterClass
class GameSetup : Node(), LogInterface {

    override var logEnabled: Boolean = true

    // Globals
    private lateinit var dataLoader: DataLoader
    private lateinit var signalBus: SignalBus

    @RegisterSignal("countryId")
    val countrySelectSignal by signal1<Int>()

    @RegisterSignal
    val startGameSignal by signal0()

    // Data
    private var selectedSponsor: Int = -1
    private val sponsorList = mutableListOf<Sponsor>()
    private val technologies = mutableListOf<Technology>()

    // UI elements
    private lateinit var companyNamePanel: PanelContainer
    private lateinit var startGameButton: Button
    private lateinit var nextButton: Button
    private lateinit var companyNameEdit: LineEdit
    private lateinit var locationMap: ColorRect
    private lateinit var hqSponsorListPanel: ItemList
    private lateinit var sponsorDescription: RichTextLabel
    private lateinit var baseResources: RichTextLabel
    private lateinit var baseSciences: RichTextLabel

    // Called when the node enters the scene tree for the first time.
    @RegisterFunction
    override fun _ready() {
        dataLoader = getNodeAs("/root/DataLoader")!!
        signalBus = getNodeAs("/root/SignalBus")!!

        companyNamePanel = getNodeAs("CompanyNamePanel")!!
        startGameButton = getNodeAs("%StartGame_Button")!!
        companyNameEdit = getNodeAs("%CompanyNameEdit")!!
        locationMap = getNodeAs("%LocationMap")!!
        hqSponsorListPanel = getNodeAs("%HQSponsorList")!!
        sponsorDescription = getNodeAs("%SponsorDescription")!!
        baseResources = getNodeAs("%BaseResources")!!
        baseSciences = getNodeAs("%BaseSciences")!!
        nextButton = getNodeAs("%NextButton")!!

        sponsorList.addAll(dataLoader.loadSponsorData())
        technologies.addAll(dataLoader.loadTechWeb())

        hqSponsorListPanel.let { list ->
            list.clear()
            sponsorList.forEach { sponsor ->
                list.addItem(sponsor.name)
            }
        }
    }

    @RegisterFunction
    override fun _process(delta: Double) {

    }

    @RegisterFunction
    override fun _input(event: InputEvent?) {
        if (event != null) {
            if (event.isActionPressed("ui_cancel".asCachedStringName())) {
                getTree()?.changeSceneToFile("res://src/main/kuiper/screens/mainMenu/main_menu.tscn")
            }
        }
    }

    @RegisterFunction
    fun _onLocationListItemSelected(index: Int) {
        selectedSponsor = sponsorList[index].id
        locationMap.color = sponsorList[index].colour
        companyNameEdit.text = "${sponsorList[index].name} Space Agency"
        sponsorDescription.clear()
        sponsorDescription.appendText("Here we put some narrative text about the selected location. Some background information, lore, and hints about the challenges and benefits of the location.\n")
        sponsorDescription.appendText("\n")
        sponsorDescription.appendText(sponsorList[index].introText)

        baseResources.clear()
        baseResources.appendText("[b]Starting resources:[/b]\n")
        sponsorList[index].startingResources.forEach { (resource, amount) ->
            baseResources.appendText("[img=32]${resource.spritePath}[/img] ${resource.displayName}: $amount\n")
        }
        baseSciences.clear()
        baseSciences.appendText("[b]Base science rates:[/b]\n")
        sponsorList[index].baseScienceRate.forEach { (science, rate) ->
            baseSciences.appendText("[img=32]${science.spritePath}[/img] ${science.displayName}: $rate\n")
        }
        nextButton.disabled = false
    }

    @RegisterFunction
    fun _onStartGameButtonPressed() {
        log("Setting up and starting game")
        if (selectedSponsor == -1) {
            logError("No sponsor selected!")
            return
        }

        // globals are added to the tree first, so will be the first child
        val gameState = getTree()?.root?.getChild(0) as GameState
        val sponsor = sponsorList[selectedSponsor - 1]
        log("Randomising technology costs")
        technologies.forEach { technology -> technology.randomiseCosts() }
        gameState.let { gS ->
            gS.year = 1965
            gS.sponsor = sponsor
            gS.company.zones.addAll(setupZones(sponsor))
            gS.company.let { company ->
                company.name = companyNameEdit.text
                company.sciences.putAll(sponsor.baseScienceRate)
                company.resources.putAll(sponsor.startingResources)
                company.technologies.addAll(technologies)
            }
        }
        getTree()?.changeSceneToFile("res://src/main/kuiper/screens/kuiper/game.tscn")
    }

    private fun setupZones(sponsor: Sponsor): List<Zone> {
        val zoneList = listOf(
            Zone(1, sponsor.name, true),
            Zone(2, "Near Earth Orbit"),
            Zone(3, "Mars"),
            Zone(4, "Asteroid Belt"),
            Zone(5, "Jupiter"),
            Zone(6, "Kuiper Belt")
        )
        zoneList[0].apply {
            description = "Your home zone, where your headquarters is located. HQ can be moved in the future."
            locations.add(Location("${sponsor.name} HQ", true))
            locations[0].apply {
                val hq = Building.HQ()
                hq.sciencesProduced.putAll(sponsor.baseScienceRate)
                hq.resourceGeneration[ResourceType.INFLUENCE] = 1
                hq.resourceGeneration[ResourceType.GOLD] = 25
                addBuilding(hq, intArrayOf(1, 2, 4), true)
            }
            for (i in 1..9) {
                locations.add(Location("Location $i"))
            }
        }
        return zoneList
    }

    @RegisterFunction
    fun _on_next_button_pressed() {
        companyNamePanel.visible = true
    }

    @RegisterFunction
    fun _on_company_name_text_changed(newText: String) {
        startGameButton.disabled = newText.length <= 10
    }

    @RegisterFunction
    fun _on_company_panel_back_pressed() {
        companyNamePanel.visible = false
    }

}
