package technology.web

import godot.*
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.core.Color
import godot.extensions.getNodeAs
import godot.global.GD
import technology.Technology

@RegisterClass
class TechNode : GraphNode() {

	var technology: Technology = Technology.EMPTY

	// UI elements
	private lateinit var idLabel: Label
	private lateinit var tierLabel: Label
	private lateinit var vBox: VBoxContainer
	private lateinit var addIncoming: Button
	private lateinit var addOutgoing: Button

	private var requiresCount = 0
	private var unlocksCount = 0
	private var connectionCount = 0

	val unlockPorts: ArrayList<Int> = ArrayList()
	val requirePorts: ArrayList<Int> = ArrayList()

	@RegisterFunction
	override fun _ready() {
		idLabel = getNodeAs("%idLabel")!!
		tierLabel = getNodeAs("%tierLabel")!!
		vBox = getNodeAs("%VBox")!!
		addIncoming = getNodeAs("%AddIncomingBtn")!!
		addOutgoing = getNodeAs("%AddOutgoingBtn")!!

		updateUI()
	}

	@RegisterFunction
	override fun _process(delta: Double) {

	}

	@RegisterFunction
	fun addRequirement(id: Int?) {
		GD.print("Adding requirement slot to tech ${this.technology.id} (from $id)")
		val newLabel: Label = Label().apply {
			text = "Requires"
			setName("Requires_$connectionCount")
		}
		addChild(newLabel)
		moveChild(newLabel, 0)
		setSlot(
			slotIndex = connectionCount,
			enableLeftPort = true,
			typeLeft = 0,
			colorLeft = Color.white,
			enableRightPort = false,
			typeRight = 0,
			colorRight = Color.green,
		)
		requiresCount++
		connectionCount++
		if (id != null) {
			requirePorts.add(connectionCount)
			GD.print("Added incoming port slot: ${connectionCount - 1}")
		}
	}

	@RegisterFunction
	fun addUnlocks(id: Int?) {
		val newLabel: Label = Label().apply {
			text = "Unlocks"
			setName("Unlocks_$connectionCount")
			setHorizontalAlignment(HorizontalAlignment.HORIZONTAL_ALIGNMENT_RIGHT)
		}
		addChild(newLabel)

		moveChild(newLabel, if (connectionCount == 0) 0 else connectionCount)
		setSlot(
			slotIndex = connectionCount,
			enableLeftPort = false,
			typeLeft = 0,
			colorLeft = Color.white,
			enableRightPort = true,
			typeRight = 0,
			colorRight = Color.green,
		)
		unlocksCount++
		connectionCount++
		if (id != null) {
			unlockPorts.add(connectionCount)
			GD.print("Added outgoing port slot: ${connectionCount - 1}")
		}
	}

	@RegisterFunction
	fun _on_AddOutgoingBtn_pressed() {
		addUnlocks(null)
	}

	@RegisterFunction
	fun _on_AddIncomingBtn_pressed() {
		addRequirement(null)
	}

	private fun updateUI() {
		setTitle(technology.title)
		idLabel.text = technology.id.toString()
		tierLabel.text = technology.tier.name
	}
}
