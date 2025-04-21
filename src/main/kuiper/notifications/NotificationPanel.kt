package notifications

import LogInterface
import SignalBus
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.PackedScene
import godot.api.ResourceLoader
import godot.api.VBoxContainer
import godot.core.Vector2
import godot.core.connect
import godot.extension.getNodeAs

@RegisterClass
class NotificationPanel : VBoxContainer(), LogInterface {

    override var logEnabled: Boolean = true

    // Globals
    private val signalBus: SignalBus by lazy { getNodeAs("/root/Kuiper/SignalBus")!! }

    // Packed scenes
    private val notificationItemScene =
        ResourceLoader.load("res://src/main/kuiper/notifications/notification_item.tscn") as PackedScene

    // UI Elements

    // Data
    private var itemCount: Int = 0
    private var height: Int = 100
    private var minSpacing: Int = 10
    private val itemSize: Int = 80
    private var year: Int = 1980

    @RegisterFunction
    override fun _ready() {

        signalBus.notify.connect { wrapper ->
            wrapper.notification?.let { notification ->
                addNotification(notification)
            }
        }

        signalBus.nextTurn.connect { newYear ->
            year = newYear
            clearTransientNotifications()
        }
    }

    @RegisterFunction
    override fun _process(delta: Double) {

    }

    /**
     * Adds a notification to the panel.
     */
    private fun addNotification(notification: Notification) {
        val item = notificationItemScene.instantiate() as NotificationItem
        item.ready.connect {
            item.let {
                it.setNotification(notification)
                it.setName("NotificationItem_${itemCount}")
                it.setPosition(Vector2(0, 0))
            }
        }
        addChild(item)
        itemCount++
    }

    /**
     * Clears all notifications that are not persistent. This should be called at the end of each turn.
     * Persistent notifications are those that should remain on the screen until the player dismisses them.
     */
    private fun clearTransientNotifications() {
        getChildren().forEach {
            if (it is NotificationItem) {
                if (!it.notification!!.persistent && (it.notification!!.year + 1) < year) {
                    it.dismissNotification()
                }
            }
        }
    }

}
