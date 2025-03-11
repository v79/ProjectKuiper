import actions.ActionCard
import actions.ActionWrapper
import godot.Control
import godot.Node
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterSignal
import godot.core.*
import hexgrid.Hex
import notifications.NotificationWrapper
import technology.editor.TechWrapper

/**
 * A global signal bus for the game
 * These are signals that are global to the game and can be listened to by any node
 * Makes it easier to send signals between nodes that are not directly connected
 */
@RegisterClass
class SignalBus : Node() {

    var screenWidth: Int = 1600
        private set
    var screenHeight: Int = 900
        private set

    // Screen has been resized
    @RegisterSignal
    val onScreenResized by signal2<Int, Int>("width", "height")

    // Signals relating to card dragging
    @RegisterSignal
    val draggingCard by signal1<ActionCard>("card")

    @RegisterSignal
    val droppedCard by signal1<ActionCard>("card")

    // signals relating to pulldown panels
    @RegisterSignal
    val recalcPulldownPanelSignal by signal1<Control>("panel_name")

    // Signals relating to hexes
    @RegisterSignal
    val cardOnHex by signal1<Hex>("hex")

    @RegisterSignal
    val cardOffHex by signal1<Hex>("hex")

    // Signals relating to playing actions on hexes
    @RegisterSignal
    val showActionConfirmation by signal2<Hex, ActionCard>("hex", "card")

    @RegisterSignal
    val cancelActionConfirmation by signal0()

    @RegisterSignal
    val confirmAction by signal2<Hex, ActionWrapper>("hex", "action")

    // Signals relating to the card deck
    @RegisterSignal
    val dealCardFromDeck by signal1<ActionWrapper>("action")

    // Signals for updating UI each turn
    @RegisterSignal
    val nextTurn by signal0()

    @RegisterSignal
    val notify by signal1<NotificationWrapper>("notification")

    @RegisterSignal
    val updateScience by signal2<String, Float>("science", "value")

    @RegisterSignal
    val updateResource by signal2<String, Float>("resourceType", "newValue")

    // Signals relating to active, ongoing actions
    @RegisterSignal
    val updateOngoingAction by signal2<Int, Int>("actionId", "turnsLeft")

    @RegisterSignal
    val actionCompleted by signal1<ActionWrapper>("action")

    // Signals relating to the game editor, eg. technology setup
    @RegisterSignal
    val editor_techSaved by signal1<TechWrapper>("tech_saved")

    @RegisterSignal
    val editor_deleteTech by signal1<TechWrapper>("delete_tech")

    @RegisterFunction
    override fun _ready() {
        // Connect to the screen resized signal and propagate it
        getTree()?.root?.sizeChanged?.connect {
            val size = getTree()?.root?.size ?: Vector2i(1600, 900)
            updateScreenSize(size.width, size.height)
            onScreenResized.emit(size.width, size.height)
        }
    }

    /**
     * Update the global properties for screen size
     */
    private fun updateScreenSize(width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
    }

}
