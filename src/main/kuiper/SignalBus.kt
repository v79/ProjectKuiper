import actions.ActionCard
import actions.ActionWrapper
import godot.Control
import godot.Node
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterSignal
import godot.core.connect
import godot.core.signal0
import godot.core.signal1
import godot.core.signal2
import hexgrid.Hex

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
	val updateScience by signal2<String, Float>("science", "value")

	@RegisterSignal
	val updateResource by signal2<String, Float>("resourceType", "value")

	// Signals relating to active, ongoing actions
	@RegisterSignal
	val updateOngoingAction by signal2<Int, Int>("actionId", "turnsLeft")

	@RegisterSignal
	val actionCompleted by signal1<ActionWrapper>("action")

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
