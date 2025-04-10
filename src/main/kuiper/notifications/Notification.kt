package notifications

import actions.Action
import kotlinx.serialization.Serializable
import technology.Science
import technology.Technology

/**
 * General in-game notifications which will be displayed on the right-hand notification panel.
 * That message property should be a val.
 */

@Serializable
sealed class Notification {
    abstract var message: String
    open var persistent: Boolean = false
}

@Serializable
class ResearchCompleteNotification(val technology: Technology, override var message: String) :
    Notification()

@Serializable
class ResearchProgressNotification(val technology: Technology, override var message: String) :
    Notification()

@Serializable
class ResearchStalledNotification(val technology: Technology, override var message: String) :
    Notification()

@Serializable
class ActionCompleteNotification(val action: Action, override var message: String) :
    Notification()

@Serializable
class NoScienceWarningNotification(val science: Science, override var message: String, val count: Int = 15) :
    Notification()

@Serializable
class TechUnlockedNotification(val technology: Technology, override var message: String) :
    Notification() {
    init {
        persistent = true
    }
}