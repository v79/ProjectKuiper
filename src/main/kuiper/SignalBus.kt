import actions.ActionCard
import actions.ActionWrapper
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterSignal
import godot.api.Node
import godot.core.*
import hexgrid.Hex
import notifications.NotificationWrapper

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
    @RegisterSignal("width", "height")
    val onScreenResized by signal2<Int, Int>()

    // Signals relating to card dragging
    @RegisterSignal("card")
    val draggingCard by signal1<ActionCard>()

    @RegisterSignal("card")
    val droppedCard by signal1<ActionCard>()

    // Signals relating to hexes
    @RegisterSignal("hex")
    val cardOnHex by signal1<Hex>()

    @RegisterSignal("hex")
    val cardOffHex by signal1<Hex>()

    // Signals relating to playing actions on hexes
    @RegisterSignal("hex", "card")
    val showActionConfirmation by signal2<Hex, ActionCard>()

    @RegisterSignal
    val cancelActionConfirmation by signal0()

    @RegisterSignal("hex", "action")
    val confirmAction by signal2<Hex, ActionWrapper>()

    // Signals relating to the card deck
    @RegisterSignal("action")
    val dealCardFromDeck by signal1<ActionWrapper>()

    // Signals for updating UI each turn
    @RegisterSignal
    val nextTurn by signal0()

    @RegisterSignal("notification")
    val notify by signal1<NotificationWrapper>()

    @RegisterSignal("science", "value")
    val updateScience by signal2<String, Float>()

    @RegisterSignal("resourceType", "newValue")
    val updateResource by signal2<String, Float>()

    // Signals relating to active, ongoing actions
    @RegisterSignal("actionId", "turnsLeft")
    val updateOngoingAction by signal2<Int, Int>()

    @RegisterSignal("action")
    val actionCompleted by signal1<ActionWrapper>()

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
