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
    private val lineNodes = mutableMapOf<Pair<TechnologyNode, TechnologyNode>, Line2D>()

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
        showConnections(node)
        updateSummaryPanel((node as TechnologyNode).technology)
    }

    /**
     * Show the connections between the selected node and all other nodes
     */
    private fun showConnections(node: Node) {
        lineNodes.filter { it.key.first == node || it.key.second == node }
            .filter { it.key.first.technology.tier != TechTier.TIER_0 && it.key.second.technology.tier != TechTier.TIER_0 }
            .filter { it.key.first.technology.progressPct > 33 }
            .forEach { line -> line.value.visible = true }
    }

    @RegisterFunction
    fun nodeDeselected(node: Node) {
        summaryPanel.visible = false
        lineNodes.values.map { it.visible = false }
    }

    fun setTechnologyTree(technologies: List<Technology>) {
        techNodes.clear()
        clearConnections()
        for (tech in technologies) {
            addTechnology(tech)
        }
        for (tech in technologies) {
            addConnections()
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

    /**
     * Add connections between nodes.
     * Skips connections to the Tier Zero starting tech.
     */
    private fun addConnections() {
        for (node in techNodes) {
            for (requirement in node.technology.requires) {
                // skip the tier zero/id zero starting tech
                if (requirement == 0) {
                    continue
                } else {
                    val unlockingNode = techNodes.find { it.technology.id == requirement }
                    if (unlockingNode != null) {
                        // check if the unlocking node has already been added
                        if (unlockingNode.slotConnected(node.technology.id)) {
                            continue
                        } else {
                            unlockingNode.addUnlocks(node.technology.id)
                            val unlockingSlot =
                                unlockingNode.unlockPorts.filter { it.value.techId == node.technology.id }.values.first()
                            val requiringSlot =
                                node.requirePorts.filter { it.value.techId == unlockingNode.technology.id }.values.first()

                            connectNode(unlockingNode.name, unlockingSlot.port, node.name, requiringSlot.port)
                            getLine2DNode(unlockingNode, node)
                        }
                    }
                }
            }
        }
    }

    /**
     * Get the line2D node that represents the connection between two nodes and add it to the lineNodes map
     * It also hides the line2D node by default
     */
    private fun getLine2DNode(unlockingNode: TechnologyNode, newNode: TechnologyNode): Line2D {
        // Get the Line2D node that been created by this connection and store it in a map
        val line2DNode = connectionLayer.getChildren().back() as Line2D
        line2DNode.visible = false
        lineNodes[unlockingNode to newNode] = line2DNode
        return line2DNode
    }

    private fun updateSummaryPanel(technology: Technology) {
        summaryPanel.updateSummary(TechWrapper().apply { this.technology = technology })
    }
}
