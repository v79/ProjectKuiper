package org.liamjd.kuiper

import godot.ColorRect
import godot.Input
import godot.ItemList
import godot.Node
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterSignal
import godot.core.Color
import godot.core.asCachedStringName
import godot.core.asNodePath
import godot.core.signal1
import godot.extensions.getNodeAs
import godot.global.GD

data class Country(val id: Int, val name: String, val colour: Color)

@RegisterClass
class GameSetup : Node() {

    @RegisterSignal
    val countrySelectSignal by signal1<Int>("countryId")

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
        getNodeAs<ColorRect>("MarginContainer/VBoxContainer/MarginContainer/GridContainer/LocationMap".asNodePath())?.let { map ->
            map.color = countryList[index].colour
        }
    }
}
