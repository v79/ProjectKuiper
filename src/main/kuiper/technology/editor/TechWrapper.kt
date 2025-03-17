package technology.editor

import godot.annotation.RegisterClass
import godot.api.Node
import technology.Technology

@RegisterClass
class TechWrapper : Node() {
    var technology = Technology.EMPTY
}