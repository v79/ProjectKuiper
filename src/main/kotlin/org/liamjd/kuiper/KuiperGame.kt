package org.liamjd.kuiper

import godot.Label
import godot.Node
import godot.TabBar
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.extensions.getNodeAs
import godot.global.GD
import org.liamjd.kuiper.state.GameState

/**
 * The primary game scene class, the board on which the player plays
 */
@RegisterClass
class KuiperGame : Node() {

	private lateinit var gameState: GameState

	// UI elements
	lateinit var yearLbl: Label
	lateinit var eraTabBar: TabBar

	// Called when the node enters the scene tree for the first time.
	@RegisterFunction
	override fun _ready() {
		GD.print("KuiperGame: _Loading game state")
		gameState = getTree()?.root?.getChild(0) as GameState
		GD.print(gameState.stateToString())
		// Get UI elements
		yearLbl = getNodeAs("Background/AspectRatioContainer/VBoxContainer/TopRow_hbox/PanelContainer2/Year_lbl")!!
		eraTabBar = getNodeAs("Background/AspectRatioContainer/VBoxContainer/TabBar")!!
		eraTabBar.setTabTitle(0, gameState.country?.name ?: "No country selected")
	}

	// Called every frame. 'delta' is the elapsed time since the previous frame.
	@RegisterFunction
	override fun _process(delta: Double) {
		yearLbl.text = "Year: ${gameState.year}"
	}
}
