package actions

import kotlinx.serialization.Serializable
import technology.Science

/**
 * A mutation is a change to a property of the company, such a s resource or science
 */
interface Mutation {
    var type: MutationType
}

/**
 * A science mutation is different from a regular mutation, as it affects the rate of science research
 * Science is not accumulated - it is spent each turn on technology research and reset to zero at the beginning of each turn
 * But the science rate is a property of the company, and can be increased or decreased
 */
@Serializable
class ScienceMutation(
    val science: Science, override var type: MutationType, val amount: Float
) : Mutation {
    override fun toString(): String {
        val sBuilder = StringBuilder()
        if (amount > 0.0f) {
            sBuilder.append("Increase ")
        } else {
            sBuilder.append("Decrease ")
        }
        sBuilder.append("${science.displayName} by $amount per turn")

        return sBuilder.toString()
    }
}

/**
 * A resource mutation is a change to a persistent resource of the company, such as money or influence
 */
@Serializable
class ResourceMutation(
    val resource: ResourceType,
    override var type: MutationType,
    val amountPerYear: Int,
    val completionAmount: Int? = null
) : Mutation {
    override fun toString(): String {
        val sBuilder = StringBuilder()
        if (amountPerYear != 0) {
            if (type == MutationType.ADD) {
                sBuilder.append("Gain ")
            } else if (type == MutationType.SUBTRACT) {
                sBuilder.append("Spend ")
            }
            sBuilder.append("${resource.displayName} by $amountPerYear per turn")
        }
        if (completionAmount != null) {
            sBuilder.append("Set ${resource.displayName} to $completionAmount when completed")
        }

        return sBuilder.toString()
    }
}

/**
 * The type of mutation that an action can perform
 * ADD - add a flat amount to the property, or a negative amount to subtract. Not useful for science rates
 * SET - set the property to a specific value, useful to reset to zero
 * RATE_MULTIPLY - multiply the rate of the property by the amount - useful for science research rates
 */
enum class MutationType {
    ADD, SUBTRACT, RESET, RATE_MULTIPLY
}