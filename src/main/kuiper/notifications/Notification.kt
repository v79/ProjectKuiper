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
    abstract var year: Int
}

@Serializable
class ResearchCompleteNotification(val technology: Technology, override var message: String, override var year: Int) :
    Notification() {
    init {
        persistent = true
    }
}

@Serializable
class ResearchProgressNotification(val technology: Technology, override var message: String, override var year: Int) :
    Notification()

@Serializable
class ResearchStalledNotification(val technology: Technology, override var message: String, override var year: Int) :
    Notification() {
    init {
        persistent = true
    }
}

@Serializable
class ActionCompleteNotification(val action: Action, override var message: String, override var year: Int) :
    Notification()

@Serializable
class NoScienceWarningNotification(
    val science: Science,
    override var message: String,
    override var year: Int,
    val count: Int = 15
) :
    Notification()

@Serializable
class TechUnlockedNotification(val technology: Technology, override var message: String, override var year: Int) :
    Notification()