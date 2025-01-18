package state

import godot.Node
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.global.GD
import kotlinx.serialization.Serializable

/**
 * Complete game state which is persisted between scenes and will be serialized to disk to save the game
 */
@RegisterClass
@Serializable
class GameState : Node() {

	@RegisterProperty
	@Export
	var year: Int = 1980

	var country: Country? = null

	fun nextTurn() {
		GD.print("GameState: Next turn")
		year++
	}

	fun stateToString(): String {
		return "GameState(country=${country?.name}, year=$year)"
	}

	@RegisterFunction
	fun save() {
		GD.print("GameState: Saving game state")
	}

}
