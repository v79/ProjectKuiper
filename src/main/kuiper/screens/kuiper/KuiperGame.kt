package screens.kuiper

import godot.*
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterSignal
import godot.core.Signal0
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

    // UI elements
    lateinit var yearLbl: Label
    lateinit var eraTabBar: TabBar
    lateinit var companyNameHeader: Label

    // Called when the node enters the scene tree for the first time.
    @RegisterFunction
    override fun _ready() {
        GD.print("KuiperGame: Loading game state")
        gameState = getTree()?.root?.getChild(0) as GameState
        GD.print(gameState.stateToString())
        // Get UI elements
        yearLbl = getNodeAs("Background/AspectRatioContainer/VBoxContainer/TopRow_hbox/PanelContainer2/Year_lbl")!!
        eraTabBar = getNodeAs("Background/AspectRatioContainer/VBoxContainer/TabBar")!!
        eraTabBar.setTabTitle(0, gameState.country?.name ?: "No country selected")
        companyNameHeader =
            getNodeAs("Background/AspectRatioContainer/VBoxContainer/TopRow_hbox/ProjectKuiperHeading")!!
        companyNameHeader.text = "Project Kuiper - ${gameState.companyName}"
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
        escMenuVisible = false
        gameState.save()
    }

    private fun hideEscapeMenu() {
        escMenuVisible = !escMenuVisible
    }
}
