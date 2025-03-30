package hexgrid.map.editor

import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterSignal
import godot.api.Node
import godot.core.signal2

@RegisterClass
class MapEditorSignalBus : Node() {

    @RegisterSignal("place_row", "place_col")
    val editor_placeHex by signal2<Int, Int>()

    @RegisterSignal("clear_row", "clear_col")
    val editor_clearHex by signal2<Int, Int>()

    @RegisterFunction
    override fun _ready() {

    }

    @RegisterFunction
    override fun _process(delta: Double) {

    }
}
