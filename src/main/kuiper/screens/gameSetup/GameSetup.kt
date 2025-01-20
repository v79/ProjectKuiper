package screens.gameSetup

import godot.*
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterSignal
import godot.core.*
import godot.extensions.getNodeAs
import godot.global.GD
import state.Country
import state.GameState

@RegisterClass
class GameSetup : Node() {

	@RegisterSignal
	val countrySelectSignal by signal1<Int>("countryId")

	@RegisterSignal
	val startGameSignal by signal0()

	private var selectedCountry: Int = -1

	private val countryList = listOf(
		Country(1, "Europe", Color.blue), Country(2, "North America", Color.red),
		Country(3, "South America", Color.yellow),
		Country(4, "Asia", Color.tan),
		Country(5, "Africa", Color.green),
		Country(6, "Oceania", Color.cyan),
		Country(7, "Antarctica", Color.white)
	)

	lateinit var companyNamePanel: PanelContainer
	lateinit var startGameButton: Button
	lateinit var companyNameEdit: TextEdit

	// Called when the node enters the scene tree for the first time.
	@RegisterFunction
	override fun _ready() {
		val locationList =
			getNodeAs<ItemList>("MarginContainer/VBoxContainer/MarginContainer/GridContainer/LocationList".asNodePath())
		locationList?.let { list ->
			GD.print("Clearing and populating location list")
			list.clear()
			countryList.forEach { country ->
				list.addItem(country.name)
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
		GD.print("Selected country: ${countryList[index].name}")
		selectedCountry = countryList[index].id
		getNodeAs<ColorRect>("MarginContainer/VBoxContainer/MarginContainer/GridContainer/LocationMap".asNodePath())?.let { map ->
			map.color = countryList[index].colour
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
		GD.print("Got game state: ${gameState.stateToString()}, changing year to 1965")
		gameState.year = 1965
		gameState.country = countryList[selectedCountry - 1]
		gameState.companyName = companyNameEdit.text
		GD.print("Starting game for country ${countryList[selectedCountry - 1].name}")
		getTree()?.changeSceneToFile("res://src/main/kuiper/screens/kuiper/game.tscn")
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
}
