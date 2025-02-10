package screens.gameSetup

import godot.*
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterSignal
import godot.core.*
import godot.extensions.getNodeAs
import godot.global.GD
import state.GameState
import state.Sponsor
import state.Zone
import technology.Science

@RegisterClass
class GameSetup : Node() {

    @RegisterSignal
    val countrySelectSignal by signal1<Int>("countryId")

    @RegisterSignal
    val startGameSignal by signal0()

    private var selectedCountry: Int = -1

    private val sponsorList = listOf(
        Sponsor(1, "Europe", Color.blue), Sponsor(2, "North America", Color.red),
        Sponsor(3, "South America", Color.yellow),
        Sponsor(4, "Asia", Color.tan),
        Sponsor(5, "Africa", Color.green),
        Sponsor(6, "Oceania", Color.cyan),
        Sponsor(7, "Antarctica", Color.white)
    )

    lateinit var companyNamePanel: PanelContainer
    lateinit var startGameButton: Button
    lateinit var companyNameEdit: TextEdit

    // Called when the node enters the scene tree for the first time.
    @RegisterFunction
    override fun _ready() {
        val hqSponsorListPanel =
            getNodeAs<ItemList>("%HQSponsorList".asNodePath())
        hqSponsorListPanel?.let { list ->
            list.clear()
            sponsorList.forEach { sponsor ->
                list.addItem(sponsor.name)
            }
        }

        companyNamePanel = getNodeAs("CompanyNamePanel".asNodePath())!!
        startGameButton =
            getNodeAs("CompanyNamePanel/MarginContainer/HBoxContainer/VBoxContainer/StartGame_Button".asNodePath())!!
        companyNameEdit = getNodeAs("CompanyNamePanel/MarginContainer/HBoxContainer/CompanyNameEdit".asNodePath())!!
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
        GD.print("Selected country: ${sponsorList[index].name}")
        selectedCountry = sponsorList[index].id
        getNodeAs<ColorRect>("MarginContainer/VBoxContainer/MarginContainer/GridContainer/LocationMap".asNodePath())?.let { map ->
            map.color = sponsorList[index].colour
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
        GD.print("Got game state: ${gameState.stateToString()}, changing year to 1965")
        gameState.year = 1965
        gameState.sponsor = sponsor
        gameState.company.name = companyNameEdit.text
        gameState.company.sciences = generateStartingScienceRates().toMutableMap()
        gameState.zones = setupZones(sponsor)
        GD.print("Starting game for country ${sponsorList[selectedCountry - 1].name}")
        GD.print("Company name: ${gameState.company.name}")
        GD.print("Science rates: ${gameState.company.sciences}")
        getTree()?.changeSceneToFile("res://src/main/kuiper/screens/kuiper/game.tscn")
    }

    private fun setupZones(sponsor: Sponsor): List<Zone> {
        return listOf(
            Zone(sponsor.name, true),
            Zone("Near Earth Orbit"),
            Zone("Mars"),
            Zone("Asteroid Belt"),
            Zone("Jupiter"),
            Zone("Kuiper Belt")
        )
    }

    @RegisterFunction
    fun _on_next_button_pressed() {
        companyNamePanel.visible = true
    }

    @RegisterFunction
    fun _on_company_name_text_changed() {
        if (companyNameEdit.text.length > 10) {
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
