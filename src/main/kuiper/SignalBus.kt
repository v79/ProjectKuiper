import godot.Node
import godot.annotation.RegisterClass
import godot.annotation.RegisterSignal
import godot.core.signal2

/**
 * A global signal bus for the game
 * These are signals that are global to the game and can be listened to by any node
 * Makes it easier to send signals between nodes that are not directly connected
 */
@RegisterClass
class SignalBus : Node() {
    // Screen has been resized
    @RegisterSignal
    val onScreenResized by signal2<Int, Int>("width", "height")
}