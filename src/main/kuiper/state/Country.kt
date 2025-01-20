package state

import godot.core.Color
import kotlinx.serialization.Serializable
import serializers.GDColorSerializer

@Serializable
data class Country(val id: Int, val name: String, @Serializable(with = GDColorSerializer::class) val colour: Color)