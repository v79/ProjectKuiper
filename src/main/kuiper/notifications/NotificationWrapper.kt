package notifications

import godot.annotation.RegisterClass

@RegisterClass
class NotificationWrapper() : godot.Object() {
    constructor(notification: Notification) : this() {
        this.notification = notification
    }

    var notification: Notification? = null
}