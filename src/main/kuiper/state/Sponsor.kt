package state

import actions.ResourceType
import godot.core.Color
import kotlinx.serialization.Serializable
import serializers.GDColorSerializer
import technology.Science

@Serializable
data class Sponsor(
    val id: Int, val name: String, @Serializable(with = GDColorSerializer::class) val colour: Color,
    val introText: String,
    val startingResources: Map<ResourceType, Int>,
    val baseScienceRate: Map<Science, Float>,
    val startingTechs: List<Int> = emptyList()
)