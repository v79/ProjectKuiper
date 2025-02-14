package actions

import SignalBus
import godot.Node
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.extensions.getNodeAs
import godot.global.GD
import loaders.DataLoader

@RegisterClass
class CardDeck : Node() {

	// Globals
	private lateinit var signalBus: SignalBus
	private lateinit var dataLoader: DataLoader

	private val actions: List<Action> by lazy { dataLoader.loadActionsData() }
	private val deck: MutableList<Action> = mutableListOf()

	@RegisterFunction
	override fun _ready() {
		dataLoader = getNodeAs("/root/DataLoader")!!
		signalBus = getNodeAs("/root/SignalBus")!!
		GD.print("CardDeck ready - ${actions.size} actions loaded")
		deck.addAll(actions)
		shuffle()
	}

	@RegisterFunction
	fun shuffle() {
		deck.shuffle()
	}

	@RegisterFunction
	fun getCard() {
		GD.print("Getting card from deck")
		if (deck.isEmpty()) {
			GD.print("Resetting deck as it is empty, probably not the desired behaviour")
			deck.addAll(actions)
			shuffle()
		}
		val action = deck.removeAt(0)
		signalBus.dealCardFromDeck.emit(ActionWrapper(action))
	}

}
