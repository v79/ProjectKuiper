package technology.tree

import LogInterface
import SignalBus
import godot.*
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.core.Color
import godot.core.Vector2
import godot.core.asCachedStringName
import godot.extensions.getNodeAs
import technology.Technology
import technology.editor.PortDirection
import utils.clearChildren

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
        val titleBar = getTitlebarHbox()!!
        val titleBarVBox = VBoxContainer()
        val titleFont = ResourceLoader.load("res://assets/fonts/SpaceMono-BoldItalic.ttf") as Font
        val titleLabel = Label().apply {
            text = "[T${technology.tier.ordinal}] ${technology.title}"
            setThemeTypeVariation("TechnologyTitle".asCachedStringName())
        }
        val descLabel = Label().apply {
            text = technology.description
            setCustomMinimumSize(Vector2(180, 0))
            setAutowrapMode(TextServer.AutowrapMode.AUTOWRAP_WORD)
        }
        titleBarVBox.addChild(titleLabel)
        titleBarVBox.addChild(descLabel)
        titleBar.clearChildren()
        titleBar.addChild(titleBarVBox)
        setTooltipText("This is my tooltip")
//        setTitle("[T${technology.tier.ordinal}] ${technology.title}")
    }
}
