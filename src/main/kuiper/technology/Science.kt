package technology

import godot.core.Color
import kotlinx.serialization.Serializable

/**
 * Represents a science, such as Physics or Astronomy.
 * What other properties could a science have?
 * - a multiplier for the rate of research
 */
@Serializable
enum class Science(val displayName: String, var multiplier: Float) {
    PHYSICS("Physics", 1.0f) {
        override fun color() = Color(0.0, 0.0, 1.0)
    },
    ASTRONOMY("Astronomy", 1.0f) {
        override fun color() = Color(0.5, 0.0, 0.5)
    },
    BIOCHEMISTRY("Biochemistry", 1.0f) {
        override fun color() = Color(0.0, 0.5, 0.5)
    },
    MATHEMATICS("Mathematics", 1.0f) {
        override fun color() = Color(1.0, 1.0, 0.0)
    },
    PSYCHOLOGY("Psychology & Sociology", 1.0f) {
        override fun color() = Color(0.0, 1.0, 1.0)
    },
    ENGINEERING("Engineering", 1.0f) {
        override fun color() = Color(0.5, 0.5, 0.0, 0.0)
    },
    UNKNOWN("Unknown", 1.0f) {
        override fun color() = Color(0.0, 0.0, 0.0)
    };

    abstract fun color(): Color

    companion object {
        fun all(): List<Science> {
            return listOf(PHYSICS, ASTRONOMY, BIOCHEMISTRY, ENGINEERING, MATHEMATICS, PSYCHOLOGY)
        }
    }
}
