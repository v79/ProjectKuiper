package screens.kuiper.pullDownPanel

import SignalBus
import godot.*
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.core.Vector2
import godot.core.connect
import godot.extensions.getNodeAs
import kotlin.math.roundToInt

@RegisterClass
class PullDownPanel : Control() {

    // Globals
    private lateinit var signalBus: SignalBus

    /**
     * The contents of the panel, which should be any Control node
     */
    @Export
    @RegisterProperty
    var contents: Control? = null

    /**
     * The key that will expand or shrink the panel
     */
    @Export
    @RegisterProperty
    var key: Key = Key.KEY_H

    private var isExpanded = false
    private var direction: Int = 0 // -1 for shrinking, 0 for idle, 1 for expanding
    private var maxHeight: Double = 25.0
    private var minHeight = 25.0
    private var contentMinY = 0.0
    private var isDragging = false
    private var initialSize = Vector2(0.0, 0.0)
    private var avgChildHeight: Double = 0.0
    private var basePosition = Vector2(0.0, 0.0)

    private lateinit var handle: ColorRect
    private lateinit var pulldownPanel: PanelContainer


    @RegisterFunction
    override fun _ready() {
        signalBus = getNodeAs("/root/SignalBus")!!

        pulldownPanel = getNodeAs("VBoxContainer/PanelContainer")!!
        handle = getNodeAs("%Handle")!!

        // the zeroth child is the Panel node on the original scene
        // the first child is the contents added to the game scene

        // if the contents are not set, we can't do anything yet
        // but if they are, we can calculate the dimensions of the panel
        if (getChildCount() > 1) {
            contents = getChild(1) as Control?
            _recalculate_pulldown_dimensions(contents!!)
        }

        initialSize = pulldownPanel.size
        minHeight = pulldownPanel.getRect().size.y
        basePosition = pulldownPanel.position

        signalBus.recalcPulldownPanelSignal.connect { control ->
            _recalculate_pulldown_dimensions(control)
        }
    }

    @RegisterFunction
    fun _recalculate_pulldown_dimensions(control: Control) {
        contents = control
        contents?.let {
            maxHeight = it.getRect().size.y
            it.setPosition(Vector2(this.position.x, -maxHeight))
            avgChildHeight = (it.getRect().size.y / it.getChildCount())
        }
    }

    /**
     * If the [key] is pressed, expand or shrink the panel
     */
    @RegisterFunction
    override fun _input(event: InputEvent?) {
        if (event is InputEventKey) {
            if (event.getKeycode() == key && event.pressed) {
                if (!isExpanded) {
                    direction = 1
                    contents?.visible = true
                } else {
                    direction = -1
                }
            }
        }
    }

    /**
     * If the mouse is dragged, resize the panel. It will be clamped to the appropriate size
     */
    @RegisterFunction
    fun _on_handle_gui_input(event: InputEvent) {
        if (event is InputEventMouseMotion) {
            if (isDragging) {
                pulldownPanel.setSize(
                    Vector2(
                        pulldownPanel.getRect().size.x,
                        (pulldownPanel.getRect().size.y + event.relative.y).coerceIn(minHeight, maxHeight)
                    )
                )
                handle.setPosition(
                    Vector2(
                        handle.position.x,
                        (handle.position.y + event.relative.y).coerceIn(minHeight, maxHeight)
                    )
                )
                contents?.setPosition(
                    Vector2(
                        contents!!.position.x,
                        (contents!!.position.y + event.relative.y).coerceIn(
                            minHeight - maxHeight,
                            contentMinY + minHeight
                        )
                    )
                )
            }
        }
        if (event is InputEventMouseButton) {
            if (event.getButtonIndex() == MouseButton.MOUSE_BUTTON_LEFT) {
                if (event.doubleClick) {
                    // double click to expand or shrink the panel
                    direction = if (isExpanded) -1 else 1
                }
                isDragging = event.pressed
                if (isDragging) {
                    contents?.visible = true
                }
            }
        }
        // If we've shrunk the panel to its minimum size, hide the contents
        if (pulldownPanel.size == initialSize) {
            isExpanded = false
            direction = 0
            if (!isDragging) {
                contents?.visible = false
            }
        }
    }

    /**
     * Process the expansion or shrinking of the panel
     */
    @RegisterFunction
    override fun _process(delta: Double) {
        val currentY = pulldownPanel.getRect().size.y
        when (direction) {
            1 -> {
                if (currentY < maxHeight) {
                    pulldownPanel.setSize(
                        Vector2(
                            pulldownPanel.getRect().size.x,
                            currentY + avgChildHeight
                        )
                    )
                    handle.setPosition(Vector2(handle.position.x, currentY + handle.getRect().size.y))
                    contents?.setPosition(Vector2(contents!!.position.x, currentY + avgChildHeight - maxHeight))
                } else {
                    direction = 0
                    isExpanded = true
                }
            }

            0 -> {
                // do nothing
            }

            -1 -> {
                if (currentY.roundToInt() > minHeight) {
                    pulldownPanel.setSize(
                        Vector2(
                            pulldownPanel.getRect().size.x,
                            currentY - avgChildHeight
                        )
                    )
                    handle.setPosition(Vector2(handle.position.x, currentY - handle.getRect().size.y))
                    contents?.setPosition(Vector2(contents!!.position.x, contents!!.position.y - avgChildHeight))
                } else {
                    direction = 0
                    contents?.visible = false
                    isExpanded = false
                }
            }
        }
    }
}
