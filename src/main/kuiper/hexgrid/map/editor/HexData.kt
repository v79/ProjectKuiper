package hexgrid.map.editor

import godot.core.Vector2
import kotlinx.serialization.Serializable
import serializers.GDVectorSerializer
import state.Location

/**
 * Represents a single hex in a zone, consisting of a row and column, a location, and a position on screen
 */
@Serializable
data class HexData(
    var row: Int,
    var column: Int,
    var location: Location,
    @Serializable(with = GDVectorSerializer::class) val position: Vector2,
    var unlockedAtStart: Boolean = false
)