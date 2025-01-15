package screens.Kuiper

import godot.Label
import godot.Node
import godot.TabBar
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterSignal
import godot.core.Signal0
import godot.core.signal0
import godot.extensions.getNodeAs
import godot.global.GD
import state.GameState

/**
 * The primary game scene class, the board on which the player plays
 */
@RegisterClass
class KuiperGame : Node() {

	// Global state
	private lateinit var gameState: GameState

	@RegisterSignal
	val endTurnSignal: Signal0 by signal0()

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

	@RegisterFunction
	fun _on_quit_game() {
		GD.print("KuiperGame Quitting game")
		getTree()?.quit()
	}

}
