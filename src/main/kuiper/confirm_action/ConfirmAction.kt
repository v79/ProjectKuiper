package confirm_action

import LogInterface
import SignalBus
import actions.ActionCard
import actions.ActionType
import actions.ActionWrapper
import actions.ResourceType
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.api.*
import godot.core.asStringName
import godot.core.connect
import godot.extension.getNodeAs
import godot.extension.instantiateAs
import hexgrid.Hex
import state.Building
import state.Location
import technology.Science
import utils.clearChildren

@RegisterClass
class ConfirmAction : Control(), LogInterface {

    @RegisterProperty
    @Export
    override var logEnabled: Boolean = false

    // Globals
    private lateinit var signalBus: SignalBus

    lateinit var hex: Hex
    lateinit var card: ActionCard
    lateinit var location: Location

    // UI elements
    private lateinit var titleLabel: Label
    private lateinit var animationPlayer: AnimationPlayer
    private lateinit var actionCardDetails: ActionCardDetails
    private lateinit var costsList: RichTextLabel
    private lateinit var costsPerTurnList: RichTextLabel
    private lateinit var benefitsList: RichTextLabel
    private lateinit var buildingsList: RichTextLabel
    private lateinit var buildingHeading: Label
    private lateinit var buildingSummary: RichTextLabel
    private lateinit var sectorCountLabel: RichTextLabel
    private lateinit var hexBoxContainer: CenterContainer
    private lateinit var hexLocationLabel: Label
    private lateinit var confirmButton: Button
    private lateinit var chooseSectorsContainer: HBoxContainer

    // Packed scenes
    private val hexScene = ResourceLoader.load("res://src/main/kuiper/hexgrid/Hex.tscn") as PackedScene

    private var confirmEnabled = true

    @RegisterFunction
    override fun _ready() {
        hide()
        signalBus = getNodeAs("/root/SignalBus")!!
        titleLabel = getNodeAs("%ConfirmActionTitle")!!
        animationPlayer = getNodeAs("AnimationPlayer")!!
        actionCardDetails = getNodeAs("%ActionCardDetails")!!
        costsList = getNodeAs("%CostsList")!!
        benefitsList = getNodeAs("%BenefitsList")!!
        costsPerTurnList = getNodeAs("%CostsPerTurnList")!!
        buildingsList = getNodeAs("%BuildingList")!!
        buildingHeading = getNodeAs("%Building_")!!
        hexBoxContainer = getNodeAs("%HexBoxContainer")!!
        hexLocationLabel = getNodeAs("%HexLocationLabel")!!
        confirmButton = getNodeAs("%ConfirmButton")!!
        buildingSummary = getNodeAs("%BuildingSummary_")!!
        sectorCountLabel = getNodeAs("%SectorCountLabel_")!!
        chooseSectorsContainer = getNodeAs("%ConfirmSectorContainer")!!
        chooseSectorsContainer.visible = false

        signalBus.showActionConfirmation.connect { h, c ->
            hex = h
            card = c
            location = h.location
            updateUI()
        }
    }

    @RegisterFunction
    fun updateUI() {
        val plus = "[b][color=WEB_GREEN]+[/color][/b]"
        val minus = "[b][color=WEB_MAROON]-[/color][/b]"
        resetUI()
        renderHex(hex, location)
        titleLabel.text = "Play ${card.cardName}?"
        card.action?.let { action ->
            actionCardDetails.updateCard(card)
            action.initialCosts.forEach { (resourceType, amount) ->
                costsList.appendText(minus)
                costsList.appendText(resourceType.bbCodeIcon(32))
                costsList.appendText("$amount [b]${resourceType.displayName}[b]\n")
            }
            ResourceType.entries.forEach { resourceType ->
                val cost = action.getCost(resourceType)
                if (cost.second != null) {
                    costsPerTurnList.appendText(minus)
                    costsPerTurnList.appendText(resourceType.bbCodeIcon(32))
                    costsPerTurnList.appendText("${cost.second} ${resourceType.displayName}\n")
                }
                val benefits = action.getBenefits(resourceType)
                benefits.let { (perTurn, completion) ->
                    val sBuilder = StringBuilder()
                    if (perTurn != null && perTurn > 0) {
                        sBuilder.append("$plus $perTurn ${resourceType.displayName} per turn\n")
                    }
                    if (completion != null) {
                        if (sBuilder.isNotEmpty()) {
                            sBuilder.append(" and ")
                        }
                        sBuilder.append("Sets [b]${resourceType.displayName}[/b] to $completion when completed\n")
                    }
                    if (sBuilder.isNotEmpty()) {
                        benefitsList.appendText(sBuilder.toString())
                    }
                }
            }
            Science.entries.forEach { science ->
                val benefit = action.getScienceBenefit(science)
                if (benefit != null && benefit > 0f) {
                    benefitsList.appendText(
                        "${science.bbCodeIcon(32)}[b]$benefit ${science.displayName}[/b] per turn\n"
                    )
                }
            }/* if (costsPerTurnList.getChildCount() == 0) {
                 val costLabel = Label()
                 costLabel.setName("CostLabel_NONE")
                 costLabel.text = "  -- None --" // Would be nice if this were in italics
                 costsPerTurnList.addChild(costLabel)
             }*/
            if (action.type == ActionType.BUILD) {
                if (action.buildingToConstruct == null) {
                    logError("A build action must have a building to construct: ${action.id}->${action.actionName}")
                } else {
                    // Valid building, so show the building details
                    confirmEnabled = false
                    chooseSectorsContainer.visible = true
                    buildingSummary.visible = true

                    action.buildingToConstruct?.let b@{ building ->
                        when (building) {
                            is Building.HQ -> {
                                logError("Cannot build an HQ, should already exist! ${action.id}->${action.actionName}")
                                return@b
                            }

                            is Building.ScienceLab -> {
                                buildingSummary.appendText("  New ${building.name} at ${hex.location.name}")

                                building.runningCosts.forEach { (resourceType, amount) ->
                                    costsPerTurnList.appendText(
                                        "$minus ${resourceType.bbCodeIcon(32)} $amount ${resourceType.displayName} per turn\n"
                                    )
                                }
                                building.sciencesProduced.forEach { (science, amount) ->
                                    buildingsList.appendText(if (amount > 0) plus else minus)
                                    buildingsList.appendText(
                                        "${science.bbCodeIcon(32)}[b]$amount ${science.displayName}[/b] per turn\n"
                                    )
                                }
                            }
                        }

                        val contiguous = if (building.sectorsMustBeContiguous) " contiguous" else ""
                        val s = if (building.sectors == 1) "" else "s"
                        sectorCountLabel.appendText("  Requires ${building.sectors}${contiguous} sector$s")
                        if (buildingsList.text.isNotEmpty()) {
                            buildingHeading.text = "Once construction complete:"
                        } else {
                            buildingHeading.visible = false
                        }
                    }
                }
            } else {
                confirmEnabled = true
            }

            confirmButton.disabled = !confirmEnabled
        }
    }

    /**
     * Render a larger version of the given hexagon
     */
    private fun renderHex(hex: Hex, location: Location) {
        hexBoxContainer.clearChildren()
        val hexToRender = hexScene.instantiateAs<Hex>()!!
        // Would be better if I had a deep copy function?
        hexToRender.id = hex.id
        hexToRender.location = location
        hexToRender.hexUnlocked = true
        hexToRender.sectors = location.sectors.toMutableList()
        hexToRender.setName("ConfirmHex${hex.id}")
        // hide the location label because I've got better one down below
        hexToRender.getNodeAs<Label>("LocationLabel")!!.visible = false
        // make it big
        hexToRender.scaleMutate {
            x = 2.0
            y = 2.0
        }
        val boxContainer = BoxContainer()
        boxContainer.setName("ConfirmHex_BoxContainer")
        boxContainer.addChild(hexToRender)
        hexBoxContainer.addChild(boxContainer)
        hexLocationLabel.text = location.name
    }

    @RegisterFunction
    fun resetUI() {
        titleLabel.text = ""
        costsList.clear()
        costsPerTurnList.clear()
        benefitsList.clear()
        buildingsList.clear()
        buildingHeading.visible = false
        sectorCountLabel.clear()
        sectorCountLabel.visible = false
    }

    @RegisterFunction
    override fun _process(delta: Double) {

    }

    @RegisterFunction
    fun fadeIn() {
        animationPlayer.play("show_panel".asStringName())
    }

    @RegisterFunction
    fun confirmAction() {
        logWarning("ConfirmAction: confirmAction(): Confirming action ${card.cardName}")
        hide()
        signalBus.confirmAction.emit(hex, ActionWrapper(card.action!!))
    }

    @RegisterFunction
    fun cancelAction() {
        hide()
        signalBus.cancelActionConfirmation.emit()
    }

    @RegisterFunction
    fun enableConfirmButton() {
        confirmButton.disabled = false
    }

}
