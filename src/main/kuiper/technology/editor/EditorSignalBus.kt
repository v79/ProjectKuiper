package technology.editor

import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterSignal
import godot.api.Node
import godot.core.signal1

@RegisterClass
class EditorSignalBus : Node() {

    @RegisterSignal("tech_saved")
    val editor_techSaved by signal1<TechWrapper>()

    @RegisterSignal("delete_tech")
    val editor_deleteTech by signal1<TechWrapper>()

    @RegisterFunction
    override fun _ready() {

    }


    @RegisterFunction
    override fun _process(delta: Double) {

    }
}
