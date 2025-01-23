package screens.kuiper

import godot.*
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterSignal
import godot.core.Signal0
import godot.core.Vector2
import godot.core.asCachedStringName
import godot.core.signal0
import godot.extensions.getNodeAs
import godot.global.GD
import state.GameState
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

	// UI flags/states
	// Esc menu visibility trigger
	var escMenuVisible by Delegates.observable(false) { _, _, newValue ->
		getNodeAs<Control>("EscMenu")?.visible = newValue
	}
	var sciencePanelVisible by Delegates.observable(false) { _, _, newValue ->
		scienceSummaryPanel.visible = newValue
	}

	// UI elements
	lateinit var yearLbl: Label
	lateinit var eraTabBar: TabBar
	lateinit var companyNameHeader: Label
	lateinit var sciencePanel: HBoxContainer
	lateinit var scienceSummaryPanel: PanelContainer
	private var scienceSummaryPanelMovement: Int = 0 // -1 closing, 0 stationary, 1 opening

	// Called when the node enters the scene tree for the first time.
	@RegisterFunction
	override fun _ready() {
		GD.print("KuiperGame: Loading game state")
		gameState = getTree()?.root?.getChild(0) as GameState

		// Get UI elements
		yearLbl = getNodeAs("Background/AspectRatioContainer/VBoxContainer/TopRow_hbox/PanelContainer2/Year_lbl")!!
		eraTabBar = getNodeAs("Background/AspectRatioContainer/VBoxContainer/TabBar")!!
		eraTabBar.setTabTitle(0, gameState.country?.name ?: "No country selected")
		companyNameHeader =
			getNodeAs("Background/AspectRatioContainer/VBoxContainer/TopRow_hbox/ProjectKuiperHeading")!!
		companyNameHeader.text = "Project Kuiper - ${gameState.company.name}"
		sciencePanel =
			getNodeAs("Background/AspectRatioContainer/VBoxContainer/TopRow_hbox/SciencePanel")!!
		scienceSummaryPanel = getNodeAs("ScienceSummaryPanel")!!

		// populate science panel
		val sspVbox = scienceSummaryPanel.getNodeAs<VBoxContainer>("VBoxContainer")!!
		gameState.company.sciences.forEach { (science, rate) ->
			val sciencePanelItem =
				ResourceLoader.load("res://src/main/kuiper/screens/kuiper/science_rates.tscn") as PackedScene
			val item = sciencePanelItem.instantiate() as ScienceRate
			item.rateLabel = "%.2f".format(gameState.company.sciences[science])
			item.colour = science.color
			item.description = science.label
			item.setMouseFilter(Control.MouseFilter.MOUSE_FILTER_PASS)
			sciencePanel.addChild(item)
			sspVbox.addChild(Label().apply {
				text = "${science.label}: ${gameState.company.sciences[science]}"
			})
		}

	}

	// Called every frame. 'delta' is the elapsed time since the previous frame.
	@RegisterFunction
	override fun _process(delta: Double) {
		yearLbl.text = "Year: ${gameState.year}"
		// this is all very clumsy and I should move it out into a scene.
		// ideal behaviour is to have the panel slide up and down on a click, or be draggable
		if (scienceSummaryPanelMovement != 0) {
			val panelDestination = Vector2(sciencePanel.position.x, scienceSummaryPanel.getRect().size.length())
			val currentPos = scienceSummaryPanel.position
			GD.print("Trying to move panel from ${currentPos.x}, ${currentPos.y}")
			scienceSummaryPanel.setPosition(
				currentPos.lerp(
					Vector2(
						panelDestination.x,
						if (scienceSummaryPanelMovement == 1) 0f else -scienceSummaryPanel.getRect().size.y
					), 0.1
				)
			)
			GD.print("To ${panelDestination.x}, ${panelDestination.y}")
			GD.print("Result: ${scienceSummaryPanel.getRect().position.x}, ${scienceSummaryPanel.getRect().position.y}")

		}

	}

	@RegisterFunction
	override fun _input(event: InputEvent?) {
		if (event != null) {
			if (event.isActionPressed("ui_cancel".asCachedStringName())) {
				if (sciencePanelVisible) {
					sciencePanelVisible = false
				} else {
					on_escape_menu()
				}
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

	@RegisterFunction
	fun _on_science_panel_clicked(event: InputEvent?) {
		// or should this just be in the _input function?
		if (event != null && event is InputEventMouseButton) {
			if (event.buttonIndex == MouseButton.MOUSE_BUTTON_LEFT && event.pressed) {
				scienceSummaryPanelMovement = if (sciencePanelVisible) 0 else 1
				sciencePanelVisible = !sciencePanelVisible
			}
		}
	}

	private fun hideEscapeMenu() {
		escMenuVisible = !escMenuVisible
	}
}
