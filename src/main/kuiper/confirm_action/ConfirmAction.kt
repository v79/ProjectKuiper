package confirm_action

import SignalBus
import actions.ActionCard
import actions.ActionType
import actions.ActionWrapper
import actions.ResourceType
import godot.*
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.core.Color
import godot.core.asCachedStringName
import godot.core.asStringName
import godot.core.connect
import godot.extensions.getNodeAs
import godot.extensions.instantiateAs
import godot.global.GD
import hexgrid.Hex
import hexgrid.HexDropTarget
import state.Building
import state.Location
import state.SectorStatus
import technology.Science
import utils.clearChildren
import utils.hasChildren

@RegisterClass
class ConfirmAction : Control() {

    // Globals
    private lateinit var signalBus: SignalBus

    lateinit var hex: Hex
    lateinit var card: ActionCard
    lateinit var location: Location

    // UI elements
    private lateinit var titleLabel: Label
    private lateinit var animationPlayer: AnimationPlayer
    private lateinit var actionCardDetails: ActionCardDetails
    private lateinit var costsListContainer: VBoxContainer
    private lateinit var costsPerTurnContainer: VBoxContainer
    private lateinit var benefitsListContainer: VBoxContainer
    private lateinit var buildingListContainer: VBoxContainer
    private lateinit var buildingHeading: Label
    private lateinit var hexBoxContainer: CenterContainer
    private lateinit var hexLocationLabel: Label

    // Packed scenes
    private val hexScene = ResourceLoader.load("res://src/main/kuiper/hexgrid/Hex.tscn") as PackedScene


    @RegisterFunction
    override fun _ready() {
        hide()
        signalBus = getNodeAs("/root/SignalBus")!!
        titleLabel = getNodeAs("%ConfirmActionTitle")!!
        animationPlayer = getNodeAs("AnimationPlayer")!!
        actionCardDetails = getNodeAs("%ActionCardDetails")!!
        costsListContainer = getNodeAs("%CostsList")!!
        benefitsListContainer = getNodeAs("%BenefitsList")!!
        costsPerTurnContainer = getNodeAs("%CostsPerTurnList")!!
        buildingListContainer = getNodeAs("%BuildingList")!!
        buildingHeading = getNodeAs("%Building_")!!
        hexBoxContainer = getNodeAs("%HexBoxContainer")!!
        hexLocationLabel = getNodeAs("%HexLocationLabel")!!

        signalBus.showActionConfirmation.connect { h, c ->
            hex = h
            card = c
            location = h.location
            updateUI()
        }
    }

    @RegisterFunction
    fun updateUI() {
        resetUI()
        renderHex(hex, location)
        titleLabel.text = "Play ${card.cardName}?"
        card.action?.let { action ->
            actionCardDetails.updateCard(card)
            action.initialCosts.forEach { (resourceType, amount) ->
                val costLabel = Label()
                costLabel.setName("CostLabel_${resourceType.name}")
                costLabel.text = "  $amount ${resourceType.displayName}"
                costsListContainer.addChild(costLabel)
            }
            ResourceType.entries.forEach { resourceType ->
                val cost = action.getCost(resourceType)
                if (cost.second != null) {
                    val costLabel = Label()
                    costLabel.setName("CPTLabel_${resourceType.name}")
                    costLabel.text = "  ${cost.second} ${resourceType.displayName}"
                    costsPerTurnContainer.addChild(costLabel)
                }
                val benefits = action.getBenefits(resourceType)
                benefits.let { (perTurn, completion) ->
                    val benefitLabel = Label()
                    benefitLabel.setName("BenefitLabel_${resourceType.name}")
                    benefitLabel.autowrapMode = TextServer.AutowrapMode.AUTOWRAP_WORD
                    val sBuilder = StringBuilder()
                    if (perTurn != null && perTurn > 0) {
                        sBuilder.append("  $perTurn ${resourceType.displayName} per turn")
                    }
                    if (completion != null) {
                        if (sBuilder.isNotEmpty()) {
                            sBuilder.append(" and ")
                        }
                        sBuilder.append("set ${resourceType.displayName} to $completion when completed")
                    }
                    if (sBuilder.isNotEmpty()) {
                        benefitLabel.text = sBuilder.toString()
                        benefitsListContainer.addChild(benefitLabel)
                    }
                }
            }
            Science.entries.forEach { science ->
                val benefit = action.getScienceBenefit(science)
                if (benefit != null && benefit > 0f) {
                    val benefitLabel = Label()
                    benefitLabel.setName("ScienceBenefitLabel_${science.name}")
                    benefitLabel.text = "  $benefit ${science.displayName} per turn"
                    benefitsListContainer.addChild(benefitLabel)
                }
            }
            if (costsPerTurnContainer.getChildCount() == 0) {
                val costLabel = Label()
                costLabel.setName("CostLabel_NONE")
                costLabel.text = "  -- None --" // Would be nice if this were in italics
                costsPerTurnContainer.addChild(costLabel)
            }
            if (action.type == ActionType.BUILD) {
                if (action.buildingToConstruct == null) {
                    GD.printErr("A Build action must have a building to construct: ${action.id}->${action.actionName}")
                } else {
                    val buildingLabel = Label()
                    action.buildingToConstruct?.let b@{ building ->
                        when (building) {
                            is Building.HQ -> {
                                GD.printErr("Cannot build an HQ, should already exist! ${action.id}->${action.actionName}")
                                return@b
                            }

                            is Building.ScienceLab -> {
                                buildingLabel.setName("Build_ScienceLab_${building.labName}")
                                buildingLabel.text = "  New ${building.labName} at ${hex.location.name}"
                                buildingListContainer.addChild(buildingLabel)
                                building.baseRunningCost.let { (resourceType, amount) ->
                                    val costLabel = Label()
                                    costLabel.setName("Build_ScienceLab_Cost_${building.labName}")
                                    costLabel.text = "  Costing $amount ${resourceType.displayName} per turn"
                                    buildingListContainer.addChild(costLabel)
                                }
                                building.sciencesProduced.forEach { (science, amount) ->
                                    val scienceLabel = Label()
                                    scienceLabel.setName("Build_ScienceLab_Science_${science.name}")
                                    scienceLabel.text = "  Producing $amount ${science.displayName} per turn"
                                    buildingListContainer.addChild(scienceLabel)
                                }
                            }
                        }
                        if (buildingListContainer.hasChildren()) {
                            buildingHeading.text = "Once construction complete:"
                        } else {
                            buildingHeading.visible = false
                        }
                    }
                }
            }
        }
    }

    private fun renderHex(hex: Hex, location: Location) {
        hexBoxContainer.clearChildren()
        val hexToRender = hexScene.instantiateAs<Hex>()!!
        hexToRender.id = hex.id
        hexToRender.hexUnlocked = true
        hexToRender.triangles = hex.triangles
        val dropTarget = hexToRender.getNodeAs<HexDropTarget>("%HexDropTarget")!!
        dropTarget.setName("ConfirmHexDropTarget")
        dropTarget.hex = hexToRender
        dropTarget.fillTriangles.resize(6)
        dropTarget.drawInternals = true
        dropTarget.colour = Color.white
        hex.marker = dropTarget
        location.sectors.forEachIndexed { index, sector ->
            if (sector.status == SectorStatus.BUILT) {
                hex.triangles = Array(6) { 0 }
                hex.triangles[index] = 1
                hex.marker.fillTriangles[index] = true
            }
        }
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
        costsListContainer.clearChildren()
        costsPerTurnContainer.clearChildren()
        benefitsListContainer.clearChildren()
        buildingListContainer.clearChildren()
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
        GD.print("Confirming action ${card.cardName}")
        hide()
        signalBus.confirmAction.emit(hex, ActionWrapper(card.action!!))
    }

    @RegisterFunction
    fun cancelAction() {
        hide()
        signalBus.cancelActionConfirmation.emit()
    }
}
