package screens.kuiper

import godot.*
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterSignal
import godot.core.*
import godot.extensions.getNodeAs
import godot.global.GD
import state.GameState
import technology.Science
import kotlin.properties.Delegates

/**
 * The primary game scene class, the board on which the player plays
 */
@RegisterClass
class KuiperGame : Node() {

	// Global state
	private lateinit var gameState: GameState

	@RegisterSignal
	val endTurnSignal: Signal0 by signal0()

	@RegisterSignal
	val escMenuSignal: Signal0 by signal0()

	@RegisterSignal
	val recalcPulldownPanelSignal: Signal1<Control> by signal1("panel_name")

	// UI flags/states
	// Esc menu visibility trigger
	var escMenuVisible by Delegates.observable(false) { _, _, newValue ->
		getNodeAs<Control>("EscMenu")?.visible = newValue
	}

	// UI elements
	lateinit var yearLbl: Label
	lateinit var eraTabBar: TabBar
	lateinit var companyNameHeader: Label
	lateinit var sciencePanel: HBoxContainer
	lateinit var scienceSummaryPanel: Control

	// Called when the node enters the scene tree for the first time.
	@RegisterFunction
	override fun _ready() {
		GD.print("KuiperGame: Loading game state")
		gameState = getTree()?.root?.getChild(0) as GameState

		// Get UI elements
		yearLbl = getNodeAs("Background/AspectRatioContainer/VBoxContainer/TopRow_hbox/PanelContainer2/Year_lbl")!!
		eraTabBar = getNodeAs("Background/AspectRatioContainer/VBoxContainer/TabBarContainer/TabBar")!!
		eraTabBar.setTabTitle(0, gameState.country?.name ?: "No country selected")
		companyNameHeader =
			getNodeAs("Background/AspectRatioContainer/VBoxContainer/TopRow_hbox/ProjectKuiperHeading")!!
		companyNameHeader.text = "Project Kuiper - ${gameState.company.name}"
		sciencePanel =
			getNodeAs("Background/AspectRatioContainer/VBoxContainer/TopRow_hbox/Container/SciencePanel")!!
		scienceSummaryPanel =
			getNodeAs("Background/AspectRatioContainer/VBoxContainer/TopRow_hbox/Container/PulldownPanel/ScienceSummaryContents")!!

		// populate science panel
		GD.print("Populating science panel with ${gameState.company.sciences.size} sciences")
		GD.print("ScienceSummaryPanel currently has ${scienceSummaryPanel.getChildCount()} children")
		gameState.company.sciences.forEach { (science, rate) ->
			val sciencePanelItem =
				ResourceLoader.load("res://src/main/kuiper/screens/kuiper/science_rates.tscn") as PackedScene
			val item = sciencePanelItem.instantiate() as ScienceRate
			item.rateLabel = "%.2f".format(gameState.company.sciences[science])
			item.colour = science.color()
			item.description = science.label
			item.setMouseFilter(Control.MouseFilter.MOUSE_FILTER_PASS)
			sciencePanel.addChild(item)
			scienceSummaryPanel.addChild(Label().apply {
				text = "${science.label}: ${gameState.company.sciences[science]}"
			})
		}
		scienceSummaryPanel.resetSize()

		// I need to get the sliding panel which contains this to recalculate its dimensions
		recalcPulldownPanelSignal.emit(scienceSummaryPanel)
	}

	// Called every frame. 'delta' is the elapsed time since the previous frame.
	@RegisterFunction
	override fun _process(delta: Double) {
		yearLbl.text = "Year: ${gameState.year}"
	}


	@RegisterFunction
	override fun _input(event: InputEvent?) {
		if (event != null) {
			if (event.isActionPressed("ui_cancel".asCachedStringName())) {
				on_escape_menu()
			}
			if (event.isActionPressed("game_save".asCachedStringName())) {
				on_esc_save_game()
			}
		}
	}

	@RegisterFunction
	fun on_escape_menu() {
		hideEscapeMenu()
	}

	@RegisterFunction
	fun on_end_turn() {
		GD.print("End turn!")
		gameState.nextTurn()
	}

	@RegisterFunction
	fun on_esc_save_game() {
		GD.print("Game: Save button pressed")
		GD.print(gameState.stateToString())
		escMenuVisible = false
		gameState.save()
	}

	private fun hideEscapeMenu() {
		escMenuVisible = !escMenuVisible
	}
}
