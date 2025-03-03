package technology.tree

import LogInterface
import SignalBus
import godot.*
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.extensions.getNodeAs
import technology.TechTier
import technology.Technology
import technology.editor.TechWrapper

@RegisterClass
class TechTree : GraphEdit(), LogInterface {

    @RegisterProperty
    @Export
    override var logEnabled: Boolean = true

    // Globals
    private lateinit var signalBus: SignalBus

    // Packed scenes
    private val nodeScene =
        ResourceLoader.load("res://src/main/kuiper/technology/tree/technology_node.tscn") as PackedScene

    // UI elements
    private lateinit var connectionLayer: Control
    private lateinit var summaryPanel: TechSummaryPanel

    // Data
    private val techNodes: MutableList<TechnologyNode> = mutableListOf()

    @RegisterFunction
    override fun _ready() {
        signalBus = getNodeAs("/root/SignalBus")!!

        connectionLayer = getNodeAs("_connection_layer")!!
        summaryPanel = getNodeAs("%TechSummaryPanel")!!

        summaryPanel.visible = false
    }

    @RegisterFunction
    fun layoutNodes() {

    }

    @RegisterFunction
    override fun _process(delta: Double) {

    }

    @RegisterFunction
    fun nodeSelected(node: Node) {
        summaryPanel.visible = true
        updateSummaryPanel((node as TechnologyNode).technology)
    }

    @RegisterFunction
    fun nodeDeselected(node: Node) {
        summaryPanel.visible = false
    }

    fun setTechnologyTree(technologies: List<Technology>) {
        techNodes.clear()
        clearConnections()
        for (tech in technologies) {
            addTechnology(tech)
        }
        arrangeNodes()
    }

    /**
     * Add a technology to the tree, by creating a new TechnologyNode
     * This will set up the incoming requirement slots
     */
    private fun addTechnology(tech: Technology) {
        val node = nodeScene.instantiate() as TechnologyNode
        node.let {
            it.technology = tech
            it.setName("Tech_${tech.id}_${tech.title.replace(" ", "_")}")
            if (tech.tier == TechTier.TIER_0) {
                it.visible = false
            }
        }
        node.technology = tech

        // Set up slots for the requirements and unlocks
        tech.requires.forEach { requirement ->
            node.addRequirement(requirement)
        }

        addChild(node)
        techNodes.add(node)
    }

    private fun updateSummaryPanel(technology: Technology) {
        summaryPanel.updateSummary(TechWrapper().apply { this.technology = technology })
    }
}
