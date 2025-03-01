package technology.web

import LogInterface
import godot.*
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.core.Color
import godot.core.Vector2
import godot.extensions.getNodeAs
import technology.Technology

@RegisterClass
class TechNode : GraphNode(), LogInterface {

	@Export
	@RegisterProperty
	override var logEnabled: Boolean = true

	var technology: Technology = Technology.EMPTY

	var slots: MutableMap<Int, TechPortConnection> = mutableMapOf()
		private set
	private var slotCounter = 0
	val unlockPorts: Map<Int, TechPortConnection>
		get() = slots.filter { it.value.direction == PortDirection.OUT }
	val requirePorts: Map<Int, TechPortConnection>
		get() = slots.filter { it.value.direction == PortDirection.IN }

	// UI elements
	private lateinit var tierLabel: Label
	private lateinit var vBox: VBoxContainer
	private lateinit var addIncoming: Button
	private lateinit var addOutgoing: Button

	@RegisterFunction
	override fun _ready() {
		tierLabel = getNodeAs("%tierLabel")!!
		vBox = getNodeAs("%VBox")!!
		addIncoming = getNodeAs("%AddIncomingBtn")!!
		addOutgoing = getNodeAs("%AddOutgoingBtn")!!

		updateUI()
	}

	@RegisterFunction
	fun addRequirement(techId: Int) {
		val newLabel: Label = Label().apply {
			text = "Requires"
			setName("Requires_${slotCounter}")
		}
		addChild(newLabel)
		moveChild(newLabel, 0)
		setSlot(
			slotIndex = slotCounter,
			enableLeftPort = true,
			typeLeft = 0,
			colorLeft = Color.white,
			enableRightPort = false,
			typeRight = 0,
			colorRight = Color.green,
		)
		slots[slotCounter] =
			TechPortConnection(techId = techId, port = requirePorts.size,  direction = PortDirection.IN)
		slotCounter++
	}

	@RegisterFunction
	fun addUnlocks(techId: Int) {
		val newLabel: Label = Label().apply {
			text = "Unlocks"
			setName("Unlocks_${slotCounter}")
			setHorizontalAlignment(HorizontalAlignment.HORIZONTAL_ALIGNMENT_RIGHT)
		}
		addChild(newLabel)
		moveChild(newLabel, slotCounter)
		setSlot(
			slotIndex = slotCounter,
			enableLeftPort = false,
			typeLeft = 0,
			colorLeft = Color.white,
			enableRightPort = true,
			typeRight = 0,
			colorRight = Color.green,
		)
		// port positions are calculated... later? There's a cache
		slots[slotCounter] =
			TechPortConnection(techId = techId, port = unlockPorts.size ,direction = PortDirection.OUT)
		slotCounter++
	}

	/**
	 * Find the port index for the given tech ID. It does not distinguish between port directions.
	 */
	fun getPortForTech(techId: Int): Int {
		val matches = slots.filter { it.value.techId == techId }
		if (matches.size > 1) {
			logError("More than one slot with tech ID $techId found")
		}
		return matches.values.first().port
	}

	fun updateUI() {
		setTitle("${technology.id} ${technology.title}")
		tierLabel.text = technology.tier.name
	}

	fun toSuperString(): String {
		val sBuilder = StringBuilder()
		sBuilder.append("${technology.id} ${technology.title}: ")

		return sBuilder.toString()
	}
}

data class TechPortConnection(
	var port: Int,
	val techId: Int = -1,
	var position: Vector2 = Vector2.ZERO,
	var direction: PortDirection = PortDirection.IN
)

enum class PortDirection {
	IN, OUT
}
