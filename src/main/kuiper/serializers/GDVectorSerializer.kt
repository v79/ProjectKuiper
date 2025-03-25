package serializers

import godot.core.Vector2
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object GDVectorSerializer : KSerializer<Vector2> {
    override val descriptor: SerialDescriptor =
        SerialDescriptor("godot.core.Vector2", GDVectorSurrogate.serializer().descriptor)

    override fun serialize(encoder: Encoder, value: Vector2) {
        val surrogate = GDVectorSurrogate(value.x, value.y)
        encoder.encodeSerializableValue(GDVectorSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): Vector2 {
        val surrogate = decoder.decodeSerializableValue(GDVectorSurrogate.serializer())
        return Vector2(surrogate.x, surrogate.y)
    }

}

@Serializable
@SerialName("Vector2")
private class GDVectorSurrogate(
    val x: Double = 0.0,
    val y: Double = 0.0
)