package technology.web

import godot.*
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.core.Color
import godot.extensions.getNodeAs
import godot.global.GD
import technology.Technology

@Deprecated("Newer version coming soon")
@RegisterClass
class OldTechNode : GraphNode() {

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
		//if (id != null) {
		requirePorts.add(connectionCount)
		GD.print("Added incoming port slot: $connectionCount")
		//}
		connectionCount++
	}

	@RegisterFunction
	fun addUnlocks(id: Int?) {
		val newLabel: Label = Label().apply {
			text = "Unlocks"
			setName("Unlocks_$connectionCount")
			setHorizontalAlignment(HorizontalAlignment.HORIZONTAL_ALIGNMENT_RIGHT)

		}
		addChild(newLabel)

		moveChild(newLabel, connectionCount)
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
		if (id != null) {
			unlockPorts.add(connectionCount)
			GD.print("Added outgoing port slot: $connectionCount")
		}
		connectionCount++
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

	fun toSuperString(): String {
		val sBuilder = StringBuilder()
		sBuilder.append("${technology.id} ${technology.title}: ")
		sBuilder.append("Requires: ")
		technology.requires.forEachIndexed { index, reqId ->
			sBuilder.append("id: $reqId @port: ")
			if (requirePorts.size != 0 && requirePorts.size > index) {
				sBuilder.append("[${requirePorts[index]}]")
			} else {
				sBuilder.append("[x]")
			}
			sBuilder.append(", ")
		}
		return sBuilder.toString()
	}
}
