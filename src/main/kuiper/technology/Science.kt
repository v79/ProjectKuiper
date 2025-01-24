package technology

import godot.core.Color
import kotlinx.serialization.Serializable

/**
 * Represents a science, such as Physics or Astronomy.
 * What other properties could a science have?
 * - a multiplier for the rate of research
 */
@Serializable
enum class Science(val label: String, var multiplier: Float, var color: Color) {
    PHYSICS("Physics", 1.0f, Color(0.0, 0.0, 1.0)),
    ASTRONOMY("Astronomy", 1.0f, Color(0.5, 0.0, 0.5)),
    BIOCHEMISTRY("Biochemistry", 1.0f, Color(0.0, 0.5, 0.5)),
    GEOLOGY("Geology", 1.0f, Color(0.5, 0.25, 0.0)),
    MATHEMATICS("Mathematics", 1.0f, Color(1.0, 1.0, 0.0)),
    PSYCHOLOGY("Psychology & Sociology", 1.0f, Color(0.0, 1.0, 1.0)),
    UNKNOWN("Unknown", 1.0f, Color(0.0, 0.0, 0.0));
}
