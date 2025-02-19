package screens.gameSetup

import godot.*
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterSignal
import godot.core.asCachedStringName
import godot.core.signal0
import godot.core.signal1
import godot.extensions.getNodeAs
import godot.global.GD
import loaders.DataLoader
import state.*
import technology.Science

@RegisterClass
class GameSetup : Node() {

    // Global autoloads
    private lateinit var dataLoader: DataLoader

    @RegisterSignal
    val countrySelectSignal by signal1<Int>("countryId")

    @RegisterSignal
    val startGameSignal by signal0()

    private var selectedCountry: Int = -1
    private val sponsorList = mutableListOf<Sponsor>()

    // UI elements
    private lateinit var companyNamePanel: PanelContainer
    private lateinit var startGameButton: Button
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

        companyNamePanel = getNodeAs("CompanyNamePanel")!!
        startGameButton = getNodeAs("%StartGame_Button")!!
        companyNameEdit = getNodeAs("%CompanyNameEdit")!!
        locationMap = getNodeAs("%LocationMap")!!
        hqSponsorListPanel = getNodeAs("%HQSponsorList")!!
        sponsorDescription = getNodeAs("%SponsorDescription")!!
        baseResources = getNodeAs("%BaseResources")!!
        baseSciences = getNodeAs("%BaseSciences")!!

        sponsorList.addAll(dataLoader.loadSponsorData())

        hqSponsorListPanel.let { list ->
            list.clear()
            sponsorList.forEach { sponsor ->
                list.addItem(sponsor.name)
            }
        }
    }

    // Called every frame. 'delta' is the elapsed time since the previous frame.
    @RegisterFunction
    override fun _process(delta: Double) {
        if (Input.isActionPressed("ui_cancel".asCachedStringName())) {
            getTree()?.changeSceneToFile("res://src/main/kuiper/screens/mainMenu/main_menu.tscn")
        }
    }

    @RegisterFunction
    fun _onLocationListItemSelected(index: Int) {
        selectedCountry = sponsorList[index].id
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
    }

    @RegisterFunction
    fun _onStartGameButtonPressed() {
        if (selectedCountry == -1) {
            GD.print("No country selected!")
            return
        }

        // globals are added to the tree first, so will be the first child
        val gameState = getTree()?.root?.getChild(0) as GameState
        val sponsor = sponsorList[selectedCountry - 1]
        gameState.year = 1965
        gameState.sponsor = sponsor
        gameState.company.name = companyNameEdit.text
        gameState.company.sciences.putAll(generateStartingScienceRates())
        gameState.zones = setupZones(sponsor)

        GD.print("Starting game for country ${sponsorList[selectedCountry - 1].name}")
        GD.print("Company name: ${gameState.company.name}")
        GD.print("Science rates: ${gameState.company.sciences}")

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
                addBuilding(Building.HQ(), intArrayOf(1, 2, 4), true)
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
        if (newText.length > 10) {
            startGameButton.disabled = false
        } else {
            startGameButton.disabled = true
        }
    }

    @RegisterFunction
    fun _on_company_panel_back_pressed() {
        companyNamePanel.visible = false
    }

    /**
     * Create some random starting science rates
     * Later these will be based on the country selected and loaded from reference data
     */
    private fun generateStartingScienceRates(): Map<Science, Float> {
        val scienceRates = mutableMapOf<Science, Float>()
        Science.entries.forEach { science ->
            scienceRates[science] = GD.randfRange(1.0f, 10.0f)
        }
        return scienceRates
    }

}
