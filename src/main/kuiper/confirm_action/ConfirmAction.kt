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
import godot.core.Color
import godot.core.asStringName
import godot.core.connect
import godot.extension.getNodeAs
import godot.extension.instantiateAs
import hexgrid.Hex
import state.Building
import state.GameState
import state.Location
import technology.Science
import utils.clearChildren
import kotlin.math.sqrt

@RegisterClass
class ConfirmAction : Control(), LogInterface {

    @RegisterProperty
    @Export
    override var logEnabled: Boolean = true

    // Globals
    private val signalBus: SignalBus by lazy { getNodeAs("/root/Kuiper/SignalBus")!! }
    private val gameState: GameState by lazy { getNodeAs("/root/GameState")!! }

    lateinit var hexNode: Hex
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
    private lateinit var confirmButton: Button
    private lateinit var chooseSectorsContainer: HBoxContainer


    // Packed scenes
    private val hexScene = ResourceLoader.load("res://src/main/kuiper/hexgrid/Hex.tscn") as PackedScene

    private var confirmEnabled = true

    @RegisterFunction
    override fun _ready() {
        hide()
        titleLabel = getNodeAs("%ConfirmActionTitle")!!
        animationPlayer = getNodeAs("AnimationPlayer")!!
        actionCardDetails = getNodeAs("%ActionCardDetails")!!
        costsList = getNodeAs("%CostsList")!!
        benefitsList = getNodeAs("%BenefitsList")!!
        costsPerTurnList = getNodeAs("%CostsPerTurnList")!!
        buildingsList = getNodeAs("%BuildingList")!!
        buildingHeading = getNodeAs("%Building_")!!
        hexBoxContainer = getNodeAs("%HexBoxContainer")!!
        confirmButton = getNodeAs("%ConfirmButton")!!
        buildingSummary = getNodeAs("%BuildingSummary_")!!
        sectorCountLabel = getNodeAs("%SectorCountLabel_")!!
        chooseSectorsContainer = getNodeAs("%ConfirmSectorContainer")!!
        chooseSectorsContainer.visible = false

        signalBus.showActionConfirmation.connect { h, c ->
            hexNode = h
            card = c
            location = h.hexData?.location ?: Location("**Unknown**", false)
            hexNode.row = h.hexData?.row ?: -1
            hexNode.col = h.hexData?.column ?: -1
            updateUI()
        }


    }

    @RegisterFunction
    fun updateUI() {
        val plus = "[b][color=WEB_GREEN]+[/color][/b]"
        val minus = "[b][color=WEB_MAROON]-[/color][/b]"
        resetUI()
        renderHex(hexNode)
        titleLabel.text = "Play ${card.cardName} at ${hexNode.hexData?.location?.name}?"
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
                        sBuilder.append("$plus $perTurn ${resourceType.bbCodeIcon(32)} ${resourceType.displayName} per turn\n")
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
                        "$plus ${science.bbCodeIcon(32)}[b]$benefit ${science.displayName}[/b] per turn\n"
                    )
                }
            }
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
                                buildingSummary.appendText("  New ${building.name} at ${hexNode.hexData?.location?.name}")

                                building.runningCosts.forEach { (resourceType, amount) ->
                                    costsPerTurnList.appendText(
                                        "$minus $amount ${resourceType.bbCodeIcon(32)} ${resourceType.displayName} per turn\n"
                                    )
                                }
                                building.sciencesProduced.forEach { (science, amount) ->
                                    buildingsList.appendText(if (amount > 0) plus else minus)
                                    buildingsList.appendText(
                                        "${science.bbCodeIcon(32)}[b]$amount ${science.displayName}[/b] per turn\n"
                                    )
                                }
                            }

                            is Building.Factory -> {
                                buildingSummary.appendText("  New ${building.name} at ${hexNode.hexData?.location?.name}")
                                building.runningCosts.forEach { (resourceType, amount) ->
                                    costsPerTurnList.appendText(
                                        "$minus $amount ${resourceType.bbCodeIcon(32)} ${resourceType.displayName} per turn\n"
                                    )
                                }
                            }

                            else -> {
                                logError("Unknown building type: ${building.name}")
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
     * But I actually want to render its neighbours too
     */
    private fun renderHex(hex: Hex) {
        log("renderHex: ${hex.hexData?.location?.name}")
        val scaleFactor = 2.0
        // find neighbours
        val neighbours = if (hex.hexData != null) {
            log(hex.hexData.toString())
            gameState.company.zones[0].getNeighbors(hex.hexData!!)
        } else {
            emptyList()
        }

        if (neighbours.isEmpty()) {
            log("No neighbours found for ${hex.hexData?.location?.name}")
        } else {
            log("Found ${neighbours.size} neighbours for ${hex.hexData?.location?.name}")
        }
        for (neighbour in neighbours) {
            log("Neighbour: ${neighbour.location.name} at c${neighbour.column},r${neighbour.row}")
        }

        val mainHex = hexScene.instantiateAs<Hex>()!!
        // Would be better if I had a deep copy function?
        mainHex.id = hex.id
        mainHex.hexData = hex.hexData
        mainHex.isConfirmationDialog = true
        mainHex.hexUnlocked = true
        mainHex.col = hex.col
        mainHex.row = hex.row
        mainHex.setName("ConfirmHex${hex.id}")
        // make it big
        mainHex.scaleMutate {
            x = scaleFactor
            y = scaleFactor
        }
        val boxContainer = BoxContainer()
        boxContainer.setName("ConfirmHex_BoxContainer")
        boxContainer.setMouseFilter(Control.MouseFilter.MOUSE_FILTER_PASS)

        hexBoxContainer.addChild(boxContainer)

        val height = sqrt(3.0) * hex.hexRadius
        val horizDistance = 3.0 / 2.0 * hex.hexRadius
        // How on earth do I get the neighbours to render in the right place?
        neighbours.forEachIndexed { index, neighbour ->
            val neighbourHex = hexScene.instantiateAs<Hex>()!!
            val neighbourPosition = mainHex.position
            if (neighbour.column < mainHex.col) {
                neighbourPosition.x = mainHex.position.x - horizDistance
            }
            if (neighbour.column > mainHex.col) {
                neighbourPosition.x = mainHex.position.x + horizDistance
            }
            if (neighbour.row < mainHex.row) {
                neighbourPosition.y = mainHex.position.y - (height / 2.0)
            }
            if (neighbour.row > mainHex.row) {
                neighbourPosition.y = mainHex.position.y + (height / 2.0)
            }
            if (neighbour.row == mainHex.row) {
                neighbourPosition.y += (height / 2.0)
            }
            if (neighbour.column == mainHex.col) {
                neighbourPosition.x += horizDistance
            }
            // and recale
            neighbourPosition.x *= scaleFactor
            neighbourPosition.y *= scaleFactor

            neighbourHex.colour = Color.dimGray
            neighbourHex.id = index
            neighbourHex.hexData = neighbour
            neighbourHex.isConfirmationDialog = true
            neighbourHex.hexUnlocked = true
            neighbourHex.setPosition(neighbourPosition)
            neighbourHex.setName("${neighbour.location.name.replace(' ', '_')}_${index}")
            // make it big
            neighbourHex.scaleMutate {
                x = scaleFactor
                y = scaleFactor
            }
            neighbourHex.setPosition(neighbourPosition)
            boxContainer.addChild(neighbourHex)
        }
        boxContainer.addChild(mainHex)
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
        hexBoxContainer.clearChildren()
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
        signalBus.confirmAction.emit(hexNode, ActionWrapper(card.action!!))
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
