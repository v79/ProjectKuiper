package technology

import godot.core.Color
import kotlinx.serialization.Serializable

/**
 * Represents a science, such as Physics or Astronomy.
 * What other properties could a science have?
 * - a multiplier for the rate of research
 */
@Serializable
enum class Science(val displayName: String, var multiplier: Float, var spritePath: String = "") {
    PHYSICS("Physics", 1.0f, "res://assets/textures/icons/icon-physics-128x128.png") {
        override fun color() = Color(0.0, 0.0, 0.5) //000080ff
    },
    ASTRONOMY("Astronomy", 1.0f, "res://assets/textures/icons/icon-astronomy-128x128.png") {
        override fun color() = Color(0.68, 0.71, 0.5)  // afb600ff
    },
    BIOCHEMISTRY("Biochemistry", 1.0f, "res://assets/textures/icons/icon-biochem-128x128.png") {
        override fun color() = Color(0.0, 0.6, 0.0)
    },
    MATHEMATICS("Mathematics", 1.0f, "res://assets/textures/icons/icon-maths-128x128.png") {
        override fun color() = Color(0.53, 0.0, 0.0)
    },
    PSYCHOLOGY("Psychology & Sociology", 1.0f, "res://assets/textures/icons/icon-psychology-128x128.png") {
        override fun color() = Color(0.67, 0.47, .86)
    },
    ENGINEERING("Engineering", 1.0f, "res://assets/textures/icons/icon-engineering-128x128.png") {
        override fun color() = Color(0.66, 0.35, 0.0)
    },
    EUREKA("Eureka", 1.0f, "res://assets/textures/icons/icon-eureka-128x128.png") {
        override fun color() = Color(1.0, 1.0, 1.0)
    };

    abstract fun color(): Color

    companion object {
        fun all(): List<Science> {
            return listOf(PHYSICS, ASTRONOMY, BIOCHEMISTRY, ENGINEERING, MATHEMATICS, PSYCHOLOGY)
        }
    }
}

/**
 * icon thoughts:
 * - physics: atom
 * - astronomy: telescope
 * - biochemistry: dna
 * - mathematics: pi
 * - psychology: brain
 * - engineering: gear
 * - unknown: question mark
 */