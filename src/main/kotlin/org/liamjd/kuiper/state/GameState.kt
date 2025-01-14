package org.liamjd.kuiper.state

import godot.Node
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterProperty
import godot.global.GD

/**
 * Complete game state which is persisted between scenes and will be serialized to disk to save the game
 */
@RegisterClass
class GameState() : Node() {

	@RegisterProperty
	@Export
	var year: Int = 1980

	var country: Country? = null

	fun nextTurn() {
		GD.print("Next turn")
		year++
	}

	fun stateToString(): String {
		return "GameState(country=${country?.name}, year=$year)"
	}

}
