package screens.kuiper

import SignalBus
import godot.*
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterSignal
import godot.core.*
import godot.extensions.getNodeAs
import godot.global.GD
import screens.kuiper.actions.ActiveAction
import state.GameState
import kotlin.properties.Delegates

/**
 * The primary game scene class, the board on which the player plays
 */
@RegisterClass
class KuiperGame : PanelContainer() {

	// Global state
	private lateinit var gameState: GameState
	private lateinit var signalBus: SignalBus

	@RegisterSignal
	val endTurnSignal: Signal0 by signal0()

	@RegisterSignal
	val escMenuSignal: Signal0 by signal0()

	// Signals - signals need to be valid Variant types https://docs.godotengine.org/en/stable/contributing/development/core_and_modules/variant_class.html
	@RegisterSignal
	val cardAdded by signal1<Int>("card_id")

	@RegisterSignal
	val screenResized by signal2<Int, Int>("width", "height")


	@RegisterSignal
	val recalcPulldownPanelSignal: Signal1<Control> by signal1("panel_name")

	// UI flags/states
	// Esc menu visibility trigger
	var escMenuVisible by Delegates.observable(false) { _, _, newValue ->
		getNodeAs<Control>("EscMenu")?.visible = newValue
	}

	// UI elements
	private lateinit var yearLbl: Label
	private lateinit var eraTabBar: TabBar
	private lateinit var companyNameHeader: Label
	private lateinit var sciencePanel: HBoxContainer
	private lateinit var scienceSummaryPanel: Control
	private lateinit var activeActionList: Control

	// packed scenes
	private val activeActionScene =
		ResourceLoader.load("res://src/main/kuiper/screens/kuiper/actions/active_action.tscn") as PackedScene
	private val sciencePanelItem =
		ResourceLoader.load("res://src/main/kuiper/screens/kuiper/science_rates.tscn") as PackedScene

	// Called when the node enters the scene tree for the first time.
	@RegisterFunction
	override fun _ready() {
		gameState = getNodeAs("/root/GameState")!!
		signalBus = getNodeAs("/root/SignalBus")!!

		// Get UI elements
		yearLbl = getNodeAs("%Year_lbl")!!
		eraTabBar = getNodeAs("Background/AspectRatioContainer/VBoxContainer/TabBarContainer/TabBar")!!
		eraTabBar.setTabTitle(0, gameState.country?.name ?: "No country selected")
		companyNameHeader =
			getNodeAs("Background/AspectRatioContainer/VBoxContainer/TopRow_hbox/ProjectKuiperHeading")!!
		companyNameHeader.text = "Project Kuiper - ${gameState.company.name}"
		sciencePanel =
			getNodeAs("Background/AspectRatioContainer/VBoxContainer/TopRow_hbox/Container/SciencePanel")!!
		scienceSummaryPanel =
			getNodeAs("Background/AspectRatioContainer/VBoxContainer/TopRow_hbox/Container/PulldownPanel/ScienceSummaryContents")!!
		activeActionList =
			getNodeAs("ActiveActionList")!!

		// populate science panel
		GD.print("KuiperGame: Populating science panel")
		gameState.company.sciences.forEach { (science, rate) ->
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


		// active actions
		GD.print("KuiperGame: Populating active actions")
		val activeActionView = activeActionScene.instantiate() as ActiveAction
		gameState.availableActions.forEach {
			activeActionView.actName = it.name
			activeActionView.actDescription = it.description
			activeActionView.turnsLeft = it.duration.toString()
			activeActionList.addChild(activeActionView)
		}

		// Connect signals
		getTree()?.root?.sizeChanged?.connect {
			val size = getTree()?.root?.size
			size?.let {
				signalBus.onScreenResized.emit(size.width, size.height)
			}
		}
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
		GD.print(gameState.stateToString())
		escMenuVisible = false
		gameState.save()
	}

	private fun hideEscapeMenu() {
		escMenuVisible = !escMenuVisible
	}
}
