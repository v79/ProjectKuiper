package confirm_action

import LogInterface
import SignalBus
import actions.*
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
import state.BuildingStatus
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
    private lateinit var placementMessage: RichTextLabel

    // Packed scenes
    private val hexScene = ResourceLoader.load("res://src/main/kuiper/hexgrid/Hex.tscn") as PackedScene

    // Data
    lateinit var hexNode: Hex
    lateinit var card: ActionCard
    lateinit var location: Location
    private var action: Action? = null
    private var confirmEnabled = true
    private var placementStatus: SectorPlacementStatus = SectorPlacementStatus.NONE

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
        placementMessage = getNodeAs("%PlacementMessage")!!
        placementMessage.text = ""
        chooseSectorsContainer.visible = false

        signalBus.showActionConfirmation.connect { h, c ->
            hexNode = h
            card = c
            location = h.location ?: Location(h.row, h.col, "**Unknown**", h.position, false)
            hexNode.row = h.row
            hexNode.col = h.col
            updateUI()
        }

        signalBus.segmentClicked.connect { segmentId, leftMouse ->
            placeBuilding(segmentId, leftMouse)
        }
    }

    @RegisterFunction
    fun updateUI() {
        val plus = "[b][color=WEB_GREEN]+[/color][/b]"
        val minus = "[b][color=WEB_MAROON]-[/color][/b]"
        resetUI()
        renderHex(hexNode)
        titleLabel.text = "Play ${card.cardName} at ${hexNode.location?.name}?"
        action = card.action
        action?.let { act ->
            actionCardDetails.updateCard(card)
            act.initialCosts.forEach { (resourceType, amount) ->
                costsList.appendText(minus)
                costsList.appendText(resourceType.bbCodeIcon(32))
                costsList.appendText("$amount [b]${resourceType.displayName}[b]\n")
            }
            ResourceType.entries.forEach { resourceType ->
                val cost = act.getCost(resourceType)
                if (cost.second != null) {
                    costsPerTurnList.appendText(minus)
                    costsPerTurnList.appendText(resourceType.bbCodeIcon(32))
                    costsPerTurnList.appendText("${cost.second} ${resourceType.displayName}\n")
                }
                val benefits = act.getBenefits(resourceType)
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
                val benefit = act.getScienceBenefit(science)
                if (benefit != null && benefit > 0f) {
                    benefitsList.appendText(
                        "$plus ${science.bbCodeIcon(32)}[b]$benefit ${science.displayName}[/b] per turn\n"
                    )
                }
            }
            if (act.type == ActionType.BUILD) {
                if (act.buildingToConstruct == null) {
                    logError("A build action must have a building to construct: ${act.id}->${act.actionName}")
                } else {
                    // Valid building, so show the building details
                    confirmEnabled = false
                    chooseSectorsContainer.visible = true
                    buildingSummary.visible = true

                    act.buildingToConstruct?.let b@{ building ->
                        when (building) {
                            is Building.HQ -> {
                                logError("Cannot build an HQ, should already exist! ${act.id}->${act.actionName}")
                                return@b
                            }

                            is Building.ScienceLab -> {
                                buildingSummary.appendText("  New ${building.name} at ${hexNode.location?.name}")

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
                                buildingSummary.appendText("  New ${building.name} at ${hexNode.location?.name}")
                                building.runningCosts.forEach { (resourceType, amount) ->
                                    costsPerTurnList.appendText(
                                        "$minus $amount ${resourceType.bbCodeIcon(32)} ${resourceType.displayName} per turn\n"
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
     * Attempt to place the building on the SectorSegment
     * @param segmentId the id of the segment to place the building on. If the building has multiple segments, then this is the first
     * @param leftMouse true if the left mouse button was clicked, false if the right mouse button was clicked: left for place, right for clear
     * Updates [placementStatus]
     * Emits [SignalBus.placeBuilding] if the left mouse button was clicked
     * Emits [SignalBus.clearBuilding] if the right mouse button was clicked
     * Disables the confirm button if the building cannot be placed
     */
    @RegisterFunction
    fun placeBuilding(segmentId: Int, leftMouse: Boolean) {
        if (action != null) {
            action?.let {
                if (it.type == ActionType.BUILD) {
                    it.buildingToConstruct?.let { building ->
                        val segmentCount = building.sectors
                        var segmentToHighlight = segmentId
                        val placementSegments = mutableListOf<Int>()
                        if (leftMouse) {
                            if (placementStatus == SectorPlacementStatus.NONE) {
                                for (i in 0 until segmentCount) {
                                    signalBus.placeBuilding.emit(segmentToHighlight, location.name)
                                    placementSegments.add(segmentToHighlight)
                                    segmentToHighlight++
                                    if (segmentToHighlight == 6) {
                                        segmentToHighlight = 0
                                    }
                                }
                            }
                            it.sectorIds = placementSegments.toIntArray()
                        } else {
                            // Just clear everything
                            building.status = BuildingStatus.NONE
                            for (i in 0 until 6) {
                                signalBus.clearBuilding.emit(i, location.name)
                                placementSegments.clear()
                            }
                        }
                        // Now check if the building is valid
                        if (leftMouse) {
                            if (placementStatus == SectorPlacementStatus.NONE) {
                                placementStatus = gameState.company.zones[0].checkBuildingPlacement(
                                    hexNode.location!!,
                                    building,
                                    placementSegments.toList()
                                )
                            }
                            when (placementStatus) {
                                SectorPlacementStatus.OK, SectorPlacementStatus.NONE -> {
                                    placementMessage.text = ""
                                    confirmButton.disabled = false
                                    building.status = BuildingStatus.PLACED
                                }

                                SectorPlacementStatus.BLOCKED -> {
                                    placementMessage.text =
                                        "[color=yellow]Building this here will require destroying existing buildings, which will cost an additional +1 ${
                                            ResourceType.INFLUENCE.bbCodeIcon(32)
                                        } and take +1 turns to complete.[/color]"
                                    confirmButton.disabled = false
                                    // TODO: Store the sectors that will be demolished/overbuilt into demolitionSectorIds
//                                    it.demolitionSectorIds
                                }

                                SectorPlacementStatus.INVALID -> {
                                    placementMessage.text =
                                        "[color=red]Cannot place building here as it would require destroying the HQ.[/color]"
                                    confirmButton.disabled = true
                                }
                            }
                        } else {
                            placementStatus = SectorPlacementStatus.NONE
                            placementMessage.text = ""
                            confirmButton.disabled = true
                        }
                    }
                }
            }
        }


    }

    /**
     * Render a larger version of the given hexagon
     * But I actually want to render its neighbours too
     */
    private fun renderHex(hex: Hex) {
        log("renderHex: ${hex.location?.name}")
        val scaleFactor = 2.0
        // find neighbours
        val neighbours = if (hex.location != null) {
            log(hex.location.toString())
            gameState.company.zones[0].getNeighbors(hex.location!!)
        } else {
            emptyList()
        }

        if (neighbours.isEmpty()) {
            log("No neighbours found for ${hex.location?.name}")
        }

        val mainHex = hexScene.instantiateAs<Hex>()!!
        // I need to get information from the confirmation action dialogue to the hex, and hence to the sector segment
        // I need:
        // 1. The building size (sector count)
        // 2. The building type
        // 3. The building sprite
        // And in return I need to know:
        // 1. If the sector is occupied, and with what
        mainHex.signalBus = signalBus
        mainHex.id = hex.id
        mainHex.location = hex.location
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
            // and rescale
            neighbourPosition.x *= scaleFactor
            neighbourPosition.y *= scaleFactor

            neighbourHex.colour = Color.dimGray
            neighbourHex.id = index
            neighbourHex.location = neighbour
            neighbourHex.isConfirmationDialog = true
            neighbourHex.hexUnlocked = true
            neighbourHex.setPosition(neighbourPosition)
            neighbourHex.setName("${neighbour.name.replace(' ', '_')}_${index}")
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
        action?.let { act ->
            when (act.type) {
                ActionType.BUILD -> {
                    act.location = hexNode.location
                    logWarning("ConfirmAction: confirmAction(${act.buildingToConstruct}): $act")
                    signalBus.confirmAction.emit(hexNode, ActionWrapper(act))
                }

                ActionType.BOOST, ActionType.INVEST, ActionType.EXPLORE -> {
                    signalBus.confirmAction.emit(hexNode, ActionWrapper(card.action!!))
                }

                ActionType.NONE -> {
                    logError("Tried to confirm an action with no type: ${act.id}->${act.actionName}")
                }
            }
        }

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

/**
 * Status of the attempt to place the building
 * Only INVALID is an error. BLOCKED is a warning, and OK is success
 */
enum class SectorPlacementStatus {
    OK,
    BLOCKED,
    INVALID,
    NONE
}