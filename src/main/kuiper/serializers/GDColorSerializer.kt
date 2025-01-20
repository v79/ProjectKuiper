package serializers

import godot.core.Color
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A custom serializer for [Color] objects.
 */
object GDColorSerializer : KSerializer<Color> {
    override val descriptor: SerialDescriptor =
        SerialDescriptor("godot.core.Color", GDColorSurrogate.serializer().descriptor)

    override fun serialize(encoder: Encoder, value: Color) {
        val surrogate = GDColorSurrogate(
            value.r,
            value.g,
            value.b,
            value.a,
            value.r8,
            value.g8,
            value.b8,
            value.a8,
            value.h,
            value.s,
            value.v
        )
        encoder.encodeSerializableValue(GDColorSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): Color {
        val surrogate = decoder.decodeSerializableValue(GDColorSurrogate.serializer())
        return Color(surrogate.r, surrogate.g, surrogate.b, surrogate.a)
    }
}

@Serializable
@SerialName("Color")
private class GDColorSurrogate(
    val r: Double = 0.0,
    val g: Double = 0.0,
    val b: Double = 0.0,
    val a: Double = 0.0,
    val r8: Int = 0,
    val g8: Int = 0,
    val b8: Int = 0,
    val a8: Int = 0,
    val h: Double = 0.0,
    val s: Double = 0.0,
    val v: Double = 0.0
)