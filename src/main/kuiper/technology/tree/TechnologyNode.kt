package technology.tree

import LogInterface
import SignalBus
import godot.GraphNode
import godot.HorizontalAlignment
import godot.Label
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.core.Color
import godot.extensions.getNodeAs
import technology.Technology
import technology.editor.PortDirection
import technology.editor.TechPortConnection

/**
 * This is for the game, not the editor
 * See [technology.editor.TechNode] for the editor version
 */
@RegisterClass
class TechnologyNode : GraphNode(), LogInterface {

    @Export
    @RegisterProperty
    override var logEnabled: Boolean = false

    // Globals
    private lateinit var signalBus: SignalBus

    // Data
    var technology: Technology = Technology.EMPTY
    private var slotCounter = 0
    private var slots: MutableMap<Int, TechPortConnection> = mutableMapOf()
        private set
    val unlockPorts: Map<Int, TechPortConnection>
        get() = slots.filter { it.value.direction == PortDirection.OUT }
    val requirePorts: Map<Int, TechPortConnection>
        get() = slots.filter { it.value.direction == PortDirection.IN }

    @RegisterFunction
    override fun _ready() {
        signalBus = getNodeAs("/root/SignalBus")!!

        updateUI()
    }

    @RegisterFunction
    override fun _process(delta: Double) {

    }

    /**
     * Add a requirement by creating a new slot on the left edge of the node
     */
    fun addRequirement(techId: Int) {
        addSlot(direction = PortDirection.IN)
        slots[slotCounter] =
            TechPortConnection(techId = techId, port = requirePorts.size, direction = PortDirection.IN)
    }

    @RegisterFunction
    fun addUnlocks(techId: Int) {
        addSlot(direction = PortDirection.OUT)
        slots[slotCounter] =
            TechPortConnection(techId = techId, port = unlockPorts.size, direction = PortDirection.OUT)
    }

    /**
     * Add a new slot and label to the node, with the given direction
     * I don't really need the labels to be visible, just the slot icons
     * But the GraphEdit control needs the labels for the slots to work
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

    private fun updateUI() {
        setTitle("[T${technology.tier.ordinal}] ${technology.title}")
        setTooltipText(technology.description)
    }

    /**
     * Return true if there is already a connection for this technology
     */
    fun slotConnected(techId: Int): Boolean {
        return slots.values.any { it.techId == techId }
    }
}
