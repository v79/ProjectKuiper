import actions.ActionCard
import godot.Node
import godot.annotation.*
import godot.core.connect
import godot.core.signal1
import godot.core.signal2

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


    @RegisterFunction
    override fun _ready() {
        onScreenResized.connect { w, h ->
            updateScreenSize(w, h)
        }
    }

    private fun updateScreenSize(width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
    }
}