package org.liamjd.kuiper

import godot.Input
import godot.Node
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.core.asCachedStringName

@RegisterClass
class GameSetup : Node() {


	// Called when the node enters the scene tree for the first time.
	@RegisterFunction
	override fun _ready() {

	}

	// Called every frame. 'delta' is the elapsed time since the previous frame.
	@RegisterFunction
	override fun _process(delta: Double) {
		if (Input.isActionPressed("ui_cancel".asCachedStringName())) {
			getTree()?.changeSceneToFile("res://src/main/scenes/main_menu.tscn")
		}
	}
}
