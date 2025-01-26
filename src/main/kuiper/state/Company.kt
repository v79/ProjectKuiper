package state

import kotlinx.serialization.Serializable
import technology.Science

/**
 * The company that the player is running
 * It will likely have many other properties over time
 */
@Serializable
class Company(var name: String, var sciences: MutableMap<Science, Float>)
