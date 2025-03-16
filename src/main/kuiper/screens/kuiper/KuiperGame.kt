package screens.kuiper

import LogInterface
import SignalBus
import actions.CardDeck
import actions.activeActions.ActiveActionsFan
import confirm_action.ConfirmAction
import godot.annotation.*
import godot.api.*
import godot.core.*
import godot.extension.getNodeAs
import godot.global.GD
import science.ResourcePanel
import science.SciencePanel
import state.GameState
import technology.Science
import technology.tree.TechTree
import kotlin.properties.Delegates

/**
 * The primary game scene class, the board on which the player plays
 */
@RegisterClass
class KuiperGame : PanelContainer(), LogInterface {

    @RegisterProperty
    @Export
    override var logEnabled: Boolean = true

    // Global state
    private lateinit var gameState: GameState
    private lateinit var signalBus: SignalBus

    // Signals - signals need to be valid Variant types https://docs.godotengine.org/en/stable/contributing/development/core_and_modules/variant_class.html
    @RegisterSignal
    val endTurnSignal: Signal0 by signal0()

    @RegisterSignal
    val escMenuSignal: Signal0 by signal0()

    @RegisterSignal("card_id")
    val cardAdded by signal1<Int>()

    @RegisterSignal("width", "height")
    val screenResized by signal2<Int, Int>()

    @RegisterProperty
    @Export
    var groupCardsEnabled: Boolean = true

    // UI flags/states
    // Esc menu visibility trigger
    var escMenuVisible by Delegates.observable(false) { _, _, newValue ->
        getNodeAs<Control>("%EscMenu")?.visible = newValue
    }

    // UI elements
    private lateinit var yearLbl: Label
    private lateinit var zoneTabBar: TabBar
    private lateinit var companyNameHeader: Label
    private lateinit var sciencePanel: SciencePanel
    private lateinit var resourcePanel: ResourcePanel
    private lateinit var confirmAction: ConfirmAction
    private lateinit var cardDeck: CardDeck
    private lateinit var activeActionsFan: ActiveActionsFan
    private lateinit var techTree: TechTree

    // Called when the node enters the scene tree for the first time.
    @RegisterFunction
    override fun _ready() {
        gameState = getNodeAs("/root/GameState")!!
        signalBus = getNodeAs("/root/SignalBus")!!

        // Get UI elements
        yearLbl = getNodeAs("%Year_lbl")!!
        zoneTabBar = getNodeAs("%EraTabBar")!!
        companyNameHeader = getNodeAs("%ProjectKuiperHeading")!!
        companyNameHeader.text = "Project Kuiper - ${gameState.company.name}"
        sciencePanel = getNodeAs("%SciencePanel")!!
        resourcePanel = getNodeAs("%ResourcePanel")!!
        confirmAction = getNodeAs("%ConfirmActionScene")!!
        confirmAction.cancelAction()
        cardDeck = getNodeAs("%CardDeck")!!
        activeActionsFan = getNodeAs("%ActiveActionsFan")!!
        techTree = getNodeAs("%TechTree")!!

        // populate pulldown panels and resource displays
        populateZoneTabBar()
        populateResourcePanel()
        populateSciencePanel()
        populateTechTree()

        // create cards from actions
        gameState.availableActions.forEach {
            cardAdded.emit(it.id)
        }

        signalBus.showActionConfirmation.connect { hex, card ->
            confirmAction.hex = hex
            confirmAction.card = card
            confirmAction.fadeIn()
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
        gameState.company.sciences.forEach { (science, rate) ->
            signalBus.updateScience.emit(science.name, rate)
        }
        gameState.company.activeActions.forEach { action ->
            signalBus.updateOngoingAction.emit(action.id, action.turnsRemaining)
        }
        gameState.company.resources.forEach { resource ->
            signalBus.updateResource.emit(
                resource.key.name,
                resource.value.toFloat()
            )
        }
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
            if (event.isActionPressed("show_tech_tree".asCachedStringName())) {
                toggleTechTree()
            }
            if (event.isActionPressed("end_turn".asCachedStringName())) {
                on_end_turn()
            }
        }
    }

    @RegisterFunction
    fun on_escape_menu() {
        hideEscapeMenu()
    }

    @RegisterFunction
    fun on_end_turn() {
        gameState.nextTurn()
        updateUIOnTurn()
    }

    @RegisterFunction
    fun on_esc_save_game() {
        GD.print(gameState.stateToString())
        escMenuVisible = false
        gameState.save()
    }

    /**
     * Toggle the processing mode of the given group name, between DISABLED and INHERIT
     * This has the effect of disabling processing (input, etc.) of all nodes and their children in the group.
     * Or re-enabling processing.
     */
    private fun toggleGroupProcessing(groupName: StringName, enabled: Boolean) {
        getTree()?.getNodesInGroup(groupName)?.forEach { node ->
            if (enabled) {
                node.processMode = ProcessMode.PROCESS_MODE_INHERIT
            } else {
                node.processMode = ProcessMode.PROCESS_MODE_DISABLED
            }
        }
    }

    /**
     * Show or hide the tech tree, (dis)abling all nodes in the Cards group if visible
     */
    private fun toggleTechTree() {
        techTree.visible = !techTree.visible
        if (techTree.visible) {
            toggleGroupProcessing("Cards".asCachedStringName(), false)
        } else {
            toggleGroupProcessing("Cards".asCachedStringName(), true)
        }
    }

    private fun populateZoneTabBar() {
        gameState.company.zones.forEachIndexed { index, zone ->
            zoneTabBar.addTab(zone.name)
            zoneTabBar.setTabDisabled(index, !zone.active)
            if (!zone.active) {
                zoneTabBar.setTabTooltip(index, zone.description)
            }
        }
    }

    private fun populateSciencePanel() {
        gameState.company.sciences.forEach { (science, rate) ->
            if (science != Science.EUREKA) {
                sciencePanel.addScience(science, rate)
            }
        }
    }

    private fun populateResourcePanel() {
        gameState.company.resources.forEach { (type, value) ->
            resourcePanel.addResource(type, value)
        }
    }

    private fun populateTechTree() {
        techTree.setTechnologyTree(gameState.company.technologies)
    }

    private fun hideEscapeMenu() {
        escMenuVisible = !escMenuVisible
    }
}
