package notifications

import actions.Action
import kotlinx.serialization.Serializable
import technology.Science
import technology.Technology

/**
 * General in-game notifications which will be displayed on the right-hand notification panel.
 */
@Serializable
sealed class Notification(val message: String, val persistent: Boolean = false) {
    class ResearchComplete(val technology: Technology, message: String) :
        Notification(message = message, persistent = true)

    class ResearchProgress(val technology: Technology, message: String) : Notification(message = message)
    class ResearchStalled(val technology: Technology, message: String) : Notification(message = message)
    class ActionComplete(val action: Action, message: String) : Notification(message = message)
    class NoScienceWarning(val science: Science, message: String) : Notification(message = message)
}