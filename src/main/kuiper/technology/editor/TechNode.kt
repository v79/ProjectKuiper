package technology.editor

import LogInterface
import SignalBus
import godot.*
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.core.Color
import godot.core.Vector2
import godot.core.connect
import godot.extensions.getNodeAs
import technology.Technology

/**
 * This is for the editor, not the game
 * See [technology.tree.TechnologyNode] for the in-game version
 */
@RegisterClass
class TechNode : GraphNode(), LogInterface {

	@Export
	@RegisterProperty
	override var logEnabled: Boolean = false

	// Globals
	private lateinit var signalBus: SignalBus

	var technology: Technology = Technology.EMPTY

	var slots: MutableMap<Int, TechPortConnection> = mutableMapOf()
		private set
	private var slotCounter = 0
	val unlockPorts: Map<Int, TechPortConnection>
		get() = slots.filter { it.value.direction == PortDirection.OUT }
	val requirePorts: Map<Int, TechPortConnection>
		get() = slots.filter { it.value.direction == PortDirection.IN }

	// UI elements
	private lateinit var vBox: VBoxContainer
	private lateinit var addIncoming: Button
	private lateinit var addOutgoing: Button
	private lateinit var editor: TechEditor

	@RegisterFunction
	override fun _ready() {
		signalBus = getNodeAs("/root/SignalBus")!!

		vBox = getNodeAs("%VBox")!!
		addIncoming = getNodeAs("%AddIncomingBtn")!!
		addOutgoing = getNodeAs("%AddOutgoingBtn")!!
		editor = getNodeAs("%TechEditor")!!

		signalBus.editor_techSaved.connect { techW ->
			if (techW.technology.id == technology.id) {
				technology = techW.technology
				updateUI()
			}
		}

		updateUI()
	}

	@RegisterFunction
	fun addRequirement(techId: Int) {
		addSlot(direction = PortDirection.IN)
		slots[slotCounter] =
			TechPortConnection(techId = techId, port = requirePorts.size, direction = PortDirection.IN)
	}

	@RegisterFunction
	fun addUnlocks(techId: Int) {
		addSlot(PortDirection.OUT)
		slots[slotCounter] =
			TechPortConnection(techId = techId, port = unlockPorts.size, direction = PortDirection.OUT)
	}

	/**
	 * Add a new slot and label to the node, with the given direction
	 */
	private fun addSlot(direction: PortDirection) {
		val newLabel: Label = Label().apply {
			text = if (direction == PortDirection.OUT) {
				"Unlocks"
			} else {
				"Requires"
			}
			setName("Unlocks_${slotCounter}")
			if (direction == PortDirection.OUT) {
				setHorizontalAlignment(HorizontalAlignment.HORIZONTAL_ALIGNMENT_RIGHT)
			}
		}
		addChild(newLabel)
		moveChild(newLabel, slotCounter)
		setSlot(
			slotIndex = slotCounter,
			enableLeftPort = (direction == PortDirection.IN),
			typeLeft = 0,
			colorLeft = Color.white,
			enableRightPort = (direction == PortDirection.OUT),
			typeRight = 0,
			colorRight = Color.green,
		)
		slotCounter++
	}

	@RegisterFunction
	fun onAddIncomingPressed() {
		addSlot(direction = PortDirection.IN)
	}

	@RegisterFunction
	fun onAddOutgoingPressed() {
		addSlot(direction = PortDirection.OUT)
	}

	@RegisterFunction
	fun onEditButtonPressed() {
		editor.techWrapper.technology = technology
		editor.visible = true
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

	private fun updateUI() {
		setTitle("${technology.id} ${technology.title} (T${technology.tier.ordinal})")
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
