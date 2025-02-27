package technology.web

import LogInterface
import godot.GraphEdit
import godot.PackedScene
import godot.ResourceLoader
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.core.Vector2
import godot.extensions.instantiateAs
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

	private val techNodes: MutableList<TechNode> = mutableListOf()

	// packed scenes
	private val techNodeScene =
		ResourceLoader.load("res://src/main/kuiper/technology/web/tech_node.tscn") as PackedScene

	// UI elements

	private var centrePoint = Vector2(0, 0)
	private var nodeHeight = 236
	private var nodeWidth = 236

	// GraphEdit doesn't know its dimensions until the first frame is complete
	// So we need to wait until the first frame is complete before we can calculate the layout
	private var firstFrameComplete = false

	@RegisterFunction
	override fun _ready() {

	}

	@RegisterFunction
	override fun _process(delta: Double) {
		if (!firstFrameComplete) {
			centrePoint = getRect().size / 2
			layoutNodes()
			firstFrameComplete = true
		}
	}

	@RegisterFunction
	fun layoutNodes() {
		val nodesPerTier: Map<TechTier, Int> = techNodes.groupBy { it.technology.tier }.mapValues { it.value.size }

		val tierRingCoords: Map<TechTier, List<Vector2>> = nodesPerTier.map { (tier, count) ->
			val ringCoordinates =
				calculateRingCoordinates(centrePoint.x, centrePoint.y, (radius * tier.ordinal), count, tier.ordinal)
			tier to ringCoordinates
		}.toMap()
		tierRingCoords.forEach {
			log("Tier ${it.key} has ${it.value.size} nodes at positions: ${it.value}")
		}

		val centreNode = techNodes.find { it.technology.tier == TechTier.TIER_0 }
		centreNode?.setPositionOffset(Vector2(centrePoint.x, centrePoint.y))

		// Loop through, one tier at a time, and assign positions to nodes
		TechTier.entries.forEach { tier ->
			if (tier == TechTier.TIER_0) return@forEach
			techNodes.filter { it.technology.tier == tier }.forEachIndexed { index, techNode ->
				log("Looking for a position for node ${techNode.technology.id}")

				val pos = tierRingCoords[techNode.technology.tier]?.get(index)
				if (pos == null) {
					logError("No position found for node ${techNode.technology.id}")
				} else {
					log("Setting position offset for node ${techNode.technology.id} to $pos")
					techNode.setPositionOffset(pos)
				}
			}
		}


	}

	@RegisterFunction
	fun nodeAdded(techW: TechWrapper) {
		log("Adding node: ${techW.technology.title}")
		val newNode = techNodeScene.instantiateAs<TechNode>()!!
		newNode.technology = techW.technology
		newNode.setName("Tech_${techW.technology.id}_${techW.technology.title}")
		addChild(newNode)
		techNodes.add(newNode)
		layoutNodes()
	}

	private fun calculateRingCoordinates(
		centerX: Double, centerY: Double, radius: Double, itemCount: Int, tier: Int
	): List<Vector2> {
		val coordinates = mutableListOf<Vector2>()
		val angleIncrement = 2 * Math.PI / itemCount
		val offset = 100 * (tier - 1) // -1 because the centre tier 0 is handled separately
		for (i in 0 until itemCount) {
			val angle = i * angleIncrement
			log("\tAngle: $angle; cos(): ${cos(angle)}; sin(): ${sin(angle)}")
			val x = centerX + radius * cos(angle)
			val y = centerY + radius * sin(angle)
			// the offset needs to change based on the angle. I think.
			coordinates.add(Vector2(x + (cos(angle) * offset), y + (sin(angle) * offset)))
		}

		log("Calculated ring coordinates for $itemCount nodes: $coordinates")
		return coordinates
	}
}
