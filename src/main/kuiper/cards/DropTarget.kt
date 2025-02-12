package cards

import godot.Marker2D
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.core.Color
import godot.core.Vector2
import godot.core.asCachedStringName

@Deprecated("This class is not used in the final version of the game")
@RegisterClass
class DropTarget : Marker2D() {

	@RegisterProperty
	var isSelected = false

	@RegisterFunction
	override fun _draw() {
		drawCircle(Vector2.ZERO, 75f, Color.mediumPurple)
	}

	@RegisterFunction
	fun select() {
		// unselect all
		getTree()?.getNodesInGroup("dropTargets".asCachedStringName())?.forEach { (it as DropTarget).deselect() }
		// select this one
		modulate = Color.paleVioletRed
		isSelected = true
	}

	@RegisterFunction
	fun deselect() {
		modulate = Color.white
		isSelected = false
	}
}
