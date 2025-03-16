package actions

import SignalBus
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.Node
import godot.extension.getNodeAs
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
        deck.addAll(actions)
        shuffle()
    }

    @RegisterFunction
    fun shuffle() {
        deck.shuffle()
    }

    @RegisterFunction
    fun getCard() {
        if (deck.isEmpty()) {
            GD.printErr("Resetting deck as it is empty, probably not the desired behaviour")
        } else {
            val action = deck.removeAt(0)
            signalBus.dealCardFromDeck.emit(ActionWrapper(action))
        }
    }

}
