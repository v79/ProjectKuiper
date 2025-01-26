package technology

import kotlinx.serialization.Serializable

/**
 * I really don't know what will be in the Action class yet, nor how it will work, so this is just a stub
 */

@Serializable
data class Action(val id: Int, val title: String, val description: String)