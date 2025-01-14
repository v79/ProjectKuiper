package org.liamjd.kuiper.screens

import godot.ColorRect
import godot.Input
import godot.ItemList
import godot.Node
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterSignal
import godot.core.*
import godot.extensions.getNodeAs
import godot.global.GD
import org.liamjd.kuiper.state.Country
import org.liamjd.kuiper.state.GameState

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

	// Called when the node enters the scene tree for the first time.
	@RegisterFunction
	override fun _ready() {
		GD.print("GameSetup: _ready()")

		val locationList =
			getNodeAs<ItemList>("MarginContainer/VBoxContainer/MarginContainer/GridContainer/LocationList".asNodePath())
		locationList?.let { list ->
			GD.print("Clearing and populating location list")
			list.clear()
			countryList.forEach { country ->
				list.addItem(country.name)
			}
		}
	}

	// Called every frame. 'delta' is the elapsed time since the previous frame.
	@RegisterFunction
	override fun _process(delta: Double) {
		if (Input.isActionPressed("ui_cancel".asCachedStringName())) {
			getTree()?.changeSceneToFile("res://src/main/scenes/main_menu.tscn")
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
		GD.print("Got gamestate: ${gameState.stateToString()}, changing year to 1965")
		gameState.year = 1965
		gameState.country = countryList[selectedCountry - 1]
		GD.print("Starting game for country ${countryList[selectedCountry - 1].name}")
		getTree()?.changeSceneToFile("res://src/main/scenes/game.tscn")
	}
}
