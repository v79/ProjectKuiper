package src.main.kuiper.cards

import godot.Marker2D
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.core.Color
import godot.core.Vector2
import godot.core.asCachedStringName

@RegisterClass
class DropTarget : Marker2D() {

	@RegisterFunction
	override fun _draw() {
		drawCircle(Vector2.ZERO, 75f, Color.azure)
	}

	@RegisterFunction
	fun select() {
		getTree()?.getNodesInGroup("dropTargets".asCachedStringName())?.forEach {
			(it as DropTarget).deselect()
			modulate = Color.paleVioletRed
		}
	}

	@RegisterFunction
	fun deselect() {
		modulate = Color.white
	}
}
