package notifications

import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.*
import godot.core.asCachedStringName
import godot.core.asStringName
import godot.core.connect
import godot.extension.getNodeAs

@RegisterClass
class NotificationItem : Control() {

    // Data
    var notification: Notification? = null
        private set

    // UI Elements
    private lateinit var animationPlayer: AnimationPlayer
    private lateinit var expiryTimer: Timer
    private lateinit var label: RichTextLabel

    @RegisterFunction
    override fun _ready() {
        animationPlayer = getNodeAs("%AnimationPlayer")!!
        expiryTimer = getNodeAs("%ExpiryTimer")!!
        label = getNodeAs("%RichTextLabel")!!
    }

    @RegisterFunction
    override fun _process(delta: Double) {

    }

    @RegisterFunction
    override fun _guiInput(event: InputEvent?) {
        if (event != null) {
            if (event.isActionPressed("mouse_right_click".asCachedStringName())) {
                dismissNotification()
            }
        }
    }

    private fun dismissNotification() {
        animationPlayer.play("dismiss".asStringName())
        expiryTimer.start()
        expiryTimer.timeout.connect {
            queueFree()
        }
    }

    fun setNotification(notification: Notification) {
        this.notification = notification
        this.setTooltipText(notification.message)
        when (notification) {
            is Notification.ResearchProgress -> {
                label.text = "P"
            }

            is Notification.ActionComplete -> {
                label.text = "A"
            }

            is Notification.ResearchComplete -> {
                label.text = "R"
            }

            is Notification.ResearchStalled -> {
                label.text = "S"
            }

            is Notification.NoScienceWarning -> {
                label.text = "0"
            }

            is Notification.TechUnlocked -> {
                label.text = "U"
            }
        }
    }
}
