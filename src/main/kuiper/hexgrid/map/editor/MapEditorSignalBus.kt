package hexgrid.map.editor

import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterSignal
import godot.api.Node
import godot.core.signal2
import godot.core.signal4

@RegisterClass
class MapEditorSignalBus : Node() {

    @RegisterSignal("place_col", "place_row")
    val editor_placeHex by signal2<Int, Int>()

    @RegisterSignal("clear_col", "clear_row")
    val editor_clearHex by signal2<Int, Int>()

    @RegisterSignal("updateCol", "updateRow", "updateName", "updateUnlocked")
    val editor_updateLocation by signal4<Int, Int, String, Boolean>()

    @RegisterSignal("deleteCol","deleteRow")
    val editor_deleteLocation by signal2<Int, Int>()

    @RegisterFunction
    override fun _ready() {

    }

    @RegisterFunction
    override fun _process(delta: Double) {

    }
}
