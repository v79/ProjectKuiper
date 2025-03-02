package technology.editor

import godot.Node
import godot.annotation.RegisterClass
import technology.Technology

@RegisterClass
class TechWrapper : Node() {
    var technology = Technology.EMPTY
}