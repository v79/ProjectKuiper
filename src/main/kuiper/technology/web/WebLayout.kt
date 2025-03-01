package technology.web

import LogInterface
import godot.Control
import godot.GraphEdit
import godot.PackedScene
import godot.ResourceLoader
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.core.Color
import godot.core.PackedVector2Array
import godot.core.Vector2
import godot.extensions.getNodeAs
import godot.extensions.instantiateAs
import godot.global.GD
import technology.TechTier
import kotlin.math.cos
import kotlin.math.sin

/**
 * To find the actual position of your mouse in the graph edit, considering zoom and scroll_offset, use this:
 * var real_position: Vector2 = (mouse_position + scroll_offset) / zoom
 * Mouse position can be obtained either from get_global_mouse_position() or from connection_x_empty signals which provide a release_position parameter (they are the same).
 * If you want to set a GraphNode's position to this, use its position_offset property.
 */

@RegisterClass
class WebLayout : GraphEdit(), LogInterface {

    @RegisterProperty
    @Export
    override var logEnabled: Boolean = true

    @RegisterProperty
    @Export
    var radius: Double = 350.0

    @RegisterProperty
    @Export
    var showConnections: Boolean = true

    private val techNodes: MutableList<TechNode> = mutableListOf()

    // packed scenes
    private val techNodeScene =
        ResourceLoader.load("res://src/main/kuiper/technology/web/tech_node.tscn") as PackedScene

    // UI elements
    private lateinit var connectionLayer: Control

    private var centrePoint = Vector2(0, 0)
    private var nodeHeight = 236
    private var nodeWidth = 236

    // GraphEdit doesn't know its dimensions until the first frame is complete
    // So we need to wait until the first frame is complete before we can calculate the layout
    private var firstFrameComplete = false

    @RegisterFunction
    override fun _ready() {
        connectionLayer = getNodeAs("_connection_layer")!!
    }

    @RegisterFunction
    override fun _process(delta: Double) {
        if (!firstFrameComplete) {
            centrePoint = getRect().size / 2
            layoutNodes()
            firstFrameComplete = true
        }
        connectionLayer.visible = showConnections
    }

    /**
     * Layout the nodes in the graph, placing them in concentric rings around the centre point
     * The centre point will contain the Tier 0 node
     * The first ring will contain the Tier 1 nodes, the second ring the Tier 2 nodes, etc.
     */
    @RegisterFunction
    fun layoutNodes() {
        val nodesPerTier: Map<TechTier, Int> = techNodes.groupBy { it.technology.tier }.mapValues { it.value.size }

        val tierRingCoords: Map<TechTier, List<Vector2>> = nodesPerTier.map { (tier, count) ->
            val ringCoordinates =
                calculateRingCoordinates(centrePoint.x, centrePoint.y, (radius * tier.ordinal), count, tier.ordinal)
            tier to ringCoordinates
        }.toMap()

        val centreNode = techNodes.find { it.technology.tier == TechTier.TIER_0 }
        centreNode?.setPositionOffset(Vector2(centrePoint.x, centrePoint.y))
        // and make it invisible
        centreNode?.apply {
//			visible = false
        }
        // Loop through, one tier at a time, and assign positions to nodes
        TechTier.entries.forEach { tier ->
            if (tier == TechTier.TIER_0) return@forEach
            techNodes.filter { it.technology.tier == tier }.forEachIndexed { index, techNode ->
                val pos = tierRingCoords[techNode.technology.tier]?.get(index)
                if (pos == null) {
                    logError("No position found for node ${techNode.technology.id}")
                } else {
                    techNode.setPositionOffset(pos)
                }
            }
        }
    }

    @RegisterFunction
    fun nodeAdded(techW: TechWrapper) {
        logWarning("Adding node: ${techW.technology.id} ${techW.technology.title}")
        val newNode = techNodeScene.instantiateAs<TechNode>()!!
        newNode.technology = techW.technology
        newNode.setName("Tech_${techW.technology.id}_${techW.technology.title.replace(' ', '_')}")
        // check requirements and unlocks
        techW.technology.requires.forEach { reqId ->
            log("Adding requirement $reqId to unlock just added node ${techW.technology.title}")
            newNode.addRequirement(reqId)
            val unlockingNode = findNode(reqId)
            if (unlockingNode == null) {
                logError("Could not find node with id $reqId that would unlock ${techW.technology.id}")
            } else {
                log("Adding unlocks from ${unlockingNode.technology.title} (${unlockingNode.technology.id}) to ${techW.technology.title} (${techW.technology.id})")
                unlockingNode.addUnlocks(techW.technology.id)
                if (newNode.getInputPortCount() > 0) {
//                    if (unlockingNode.technology.tier != TechTier.TIER_0) {

                    log("Unlocking node: ${unlockingNode.technology.id} unlockPorts: ${unlockingNode.unlockPorts}")
                    log("This new node: requirePorts: ${newNode.requirePorts}")
                    val unlockingSlot =
                        unlockingNode.unlockPorts.filter { it.value.techId == newNode.technology.id }.values.first()
                    val requiringSlot =
                        newNode.requirePorts.filter { it.value.techId == unlockingNode.technology.id }.values.first()
                    log("Adding connection between #${unlockingNode.technology.id} [$unlockingSlot] -> #${newNode.technology.id} [$requiringSlot]")

                    connectNode(
                        fromNode = unlockingNode.name,
                        fromPort = unlockingSlot.port,
                        toNode = newNode.name,
                        toPort = requiringSlot.port,
                    )
//                    } else {
//                    	log("Not adding Tier 0 connections")
//                    }
                } else {
                    logError("Could not connect ${unlockingNode.technology.title} to ${techW.technology.title} because no input ports were found")
                }
            }
        }
        addChild(newNode)
        techNodes.add(newNode)
        layoutNodes()
    }

    private fun getNodesForTechIds(techIds: List<Int>): List<TechNode> {
        return techNodes.filter { techIds.contains(it.technology.id) }
    }

    override fun _getConnectionLine(fromPosition: Vector2, toPosition: Vector2): PackedVector2Array {
        log("My own special _getConnectionLine function")
        return super.getConnectionLine(fromPosition, toPosition)
    }

    @RegisterFunction
    fun nodeSelected(node: TechNode) {
        // show connection lines to this node

        log("Selected ${node.technology.title}")
        log("-----")
        log("unlockPorts: ${node.unlockPorts}")
        log("\t${node.unlockPorts.values.map { it.techId }}:")
        log("\t\t${getNodesForTechIds(node.unlockPorts.values.map { it.techId })}")
        log("requirePorts: ${node.requirePorts}")
        log("-----")


        val unlockedByThis =
            getNodesForTechIds(node.unlockPorts.values.map { it.techId })
        logWarning("Selected Node: ${node.toSuperString()} unlocks ${unlockedByThis.size} technologies")
        unlockedByThis.forEach { unl ->
            logWarning("\tUnlocks ${unl.toSuperString()}")
            // these port positions are wrong
            log(
                "\t\tLooking for connection line between this node port ${node.getPortForTech(unl.technology.id)} and unlocked node port ${
                    unl.getPortForTech(
                        node.technology.id
                    )
                }"
            )
            val line = getConnectionLine(
                unl.getInputPortPosition(
                    unl.getPortForTech(
                        node.technology.id
                    )
                ),
                node.getOutputPortPosition(node.getPortForTech(unl.technology.id))
            )
            log("\tLine is: ${line}")
            line.forEach {
                log(it.toString())
            }
            // line is just a PackedVector2Array, not a Node
            // and I don't see how to get the specific connecting node
            // the coordinates in the line2D are global but I only get the local coordinates from getConnectionLine()
            // and they have different coordinate points
            connectionLayer.getChildren(includeInternal = true).forEach { cntLine ->
                val line2D = cntLine as godot.Line2D
                val start = line2D.points.first()
                val end = line2D.points.last()
                if (start == line.first() && end == line.last()) {
                    line2D.modulate = Color.red
                }
            }
        }
    }

    @RegisterFunction
    fun nodeDeselected(node: TechNode) {

    }

    /**
     * Find a node by its technology id
     */
    private fun findNode(techId: Int): TechNode? {
        return techNodes.find { it.technology.id == techId }
    }

    /**
     * Calculate the coordinates for a ring of nodes
     * @param centerX the x coordinate of the centre of the ring
     * @param centerY the y coordinate of the centre of the ring
     * @param radius the radius of the ring; different tiers will have different radii
     * @param itemCount the number of nodes in the ring
     * @param tier the tier of the nodes in the ring
     * @return a list of Vector2 coordinates for the nodes in the ring
     */
    private fun calculateRingCoordinates(
        centerX: Double, centerY: Double, radius: Double, itemCount: Int, tier: Int
    ): List<Vector2> {
        val coordinates = mutableListOf<Vector2>()
        val angleIncrement = 2 * Math.PI / itemCount
        val offset = 100 * (tier - 1) // -1 because the centre tier 0 is handled separately
        for (i in 0 until itemCount) {
            val angle = i * angleIncrement
//			log("\tAngle: $angle; cos(): ${cos(angle)}; sin(): ${sin(angle)}")
            val x = centerX + radius * cos(angle)
            val y = centerY + radius * sin(angle)
            // the offset needs to change based on the angle. I think.
            coordinates.add(Vector2(x + (cos(angle) * offset), y + (sin(angle) * offset)))
        }

//		log("Calculated ring coordinates for $itemCount nodes: $coordinates")
        return coordinates
    }
}
