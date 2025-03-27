package state

import actions.ResourceType
import godot.core.Color
import hexgrid.map.editor.HexData
import kotlinx.serialization.Serializable
import serializers.GDColorSerializer
import technology.Science

@Serializable
data class Sponsor(
    val id: Int, val name: String, @Serializable(with = GDColorSerializer::class) val colour: Color,
    val introText: String,
    val startingResources: Map<ResourceType, Int>,
    val baseScienceRate: Map<Science, Float>,
    val startingTechs: List<Int> = emptyList(),
    val hexDimensions: Pair<Int, Int> = Pair(1, 1),
    val hexGrid: Array<Array<HexData>> = emptyArray()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Sponsor

        if (id != other.id) return false
        if (name != other.name) return false
        if (colour != other.colour) return false
        if (introText != other.introText) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + name.hashCode()
        result = 31 * result + colour.hashCode()
        result = 31 * result + introText.hashCode()
        return result
    }
}