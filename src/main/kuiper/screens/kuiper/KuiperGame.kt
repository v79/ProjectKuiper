package screens.kuiper

import SignalBus
import actions.CardDeck
import confirm_action.ConfirmAction
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

    // Signals - signals need to be valid Variant types https://docs.godotengine.org/en/stable/contributing/development/core_and_modules/variant_class.html
    @RegisterSignal
    val endTurnSignal: Signal0 by signal0()

    @RegisterSignal
    val escMenuSignal: Signal0 by signal0()

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
    private lateinit var zoneTabBar: TabBar
    private lateinit var companyNameHeader: Label
    private lateinit var sciencePanel: HBoxContainer
    private lateinit var scienceSummaryPanel: Control
    private lateinit var activeActionList: Control
    private lateinit var confirmAction: ConfirmAction
    private lateinit var cardDeck: CardDeck

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
        zoneTabBar = getNodeAs("%EraTabBar")!!
        populateZoneTabBar()
        companyNameHeader = getNodeAs("%ProjectKuiperHeading")!!
        companyNameHeader.text = "Project Kuiper - ${gameState.company.name}"
        sciencePanel = getNodeAs("%SciencePanel")!!
        scienceSummaryPanel = getNodeAs("%ScienceSummaryContents")!!
        activeActionList = getNodeAs("ActiveActionList")!!
        confirmAction = getNodeAs("%ConfirmActionScene")!!
        confirmAction.cancelAction()
        cardDeck = getNodeAs("%CardDeck")!!

        // populate science panel
        populateSciencePanel()

        // I need to get the sliding panel which contains this to recalculate its dimensions
        recalcPulldownPanelSignal.emit(scienceSummaryPanel)

        // active actions
        GD.print("Dummy: KuiperGame: Populating active actions")
        val activeActionView = activeActionScene.instantiate() as ActiveAction
        gameState.availableActions.forEach {
            activeActionView.actName = it.actionName
            activeActionView.actDescription = it.description
            activeActionView.turnsLeft = it.turns.toString()
            activeActionList.addChild(activeActionView) // this throws an error
        }

        // create cards from actions
        gameState.availableActions.forEach {
            cardAdded.emit(it.id)
        }

        // Connect signals
        getTree()?.root?.sizeChanged?.connect {
            val size = getTree()?.root?.size
            size?.let {
                signalBus.onScreenResized.emit(size.width, size.height)
            }
        }

        signalBus.showActionConfirmation.connect { hex, card ->
            confirmAction.hex = hex
            confirmAction.card = card
            confirmAction.fadeIn()
        }

        signalBus.confirmAction.connect { hex, card ->
            gameState.company.activateAction(hex, card.action!!)
        }

        // finally, update the UI
        updateUIOnTurn()
    }


    // Called every frame. 'delta' is the elapsed time since the previous frame.
    @RegisterFunction
    override fun _process(delta: Double) {
    }

    @RegisterFunction
    fun updateUIOnTurn() {
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
        updateUIOnTurn()
    }

    @RegisterFunction
    fun on_esc_save_game() {
        GD.print(gameState.stateToString())
        escMenuVisible = false
        gameState.save()
    }

    private fun populateZoneTabBar() {
        gameState.zones.forEachIndexed { index, zone ->
            zoneTabBar.addTab(zone.name)
            zoneTabBar.setTabDisabled(index, !zone.active)
            if (!zone.active) {
                zoneTabBar.setTabTooltip(index, zone.description)
            }
        }
    }

    private fun populateSciencePanel() {
        GD.print("Dummy: KuiperGame: Populating science panel")
        gameState.company.sciences.forEach { (science, rate) ->
            val item = sciencePanelItem.instantiate() as ScienceRate
            item.rateLabel = "%.2f".format(gameState.company.sciences[science])
            item.colour = science.color()
            item.description = science.displayName
            item.setMouseFilter(MouseFilter.MOUSE_FILTER_IGNORE)
            sciencePanel.addChild(item)
            scienceSummaryPanel.addChild(Label().apply {
                text = "${science.displayName}: ${gameState.company.sciences[science]}"
            })
        }
        scienceSummaryPanel.resetSize()
    }

    private fun hideEscapeMenu() {
        escMenuVisible = !escMenuVisible
    }
}
