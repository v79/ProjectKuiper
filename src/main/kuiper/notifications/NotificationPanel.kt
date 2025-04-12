package notifications

import LogInterface
import SignalBus
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.api.PackedScene
import godot.api.ResourceLoader
import godot.api.VBoxContainer
import godot.core.Vector2
import godot.core.connect
import godot.extension.getNodeAs
import technology.TechStatus
import technology.TechTier
import technology.Technology

@RegisterClass
class NotificationPanel : VBoxContainer(), LogInterface {

    @RegisterProperty
    @Export
    override var logEnabled: Boolean = true

    // Globals
    private val signalBus: SignalBus by lazy { getNodeAs("/root/Kuiper/SignalBus")!! }

    // Packed scenes
    private val notificationItemScene =
        ResourceLoader.load("res://src/main/kuiper/notifications/notification_item.tscn") as PackedScene

    // UI Elements


    // Data
    private var notifications: MutableList<Notification> = mutableListOf()
    private var itemCount: Int = 0
    private var height: Int = 100
    private var minSpacing: Int = 10
    private val itemSize: Int = 80

    @RegisterFunction
    override fun _ready() {

        signalBus.notify.connect { wrapper ->
            wrapper.notification?.let { notification ->
                addNotification(notification)
            }
        }

        signalBus.nextTurn.connect {
            clearTransientNotifications()
        }
    }

    @RegisterFunction
    override fun _process(delta: Double) {

    }

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

    private fun clearTransientNotifications() {
        getChildren().forEach {
            if (it is NotificationItem) {
                if (!it.notification!!.persistent) {
                    it.queueFree()
                }
            }
        }
    }

    @RegisterFunction
    fun _on_button_pressed() {
        log("Creating dummy notification")
        val n = ResearchProgressNotification(
            Technology(
                -1, "Dummy tech", "Fake technology", TechTier.TIER_2, TechStatus.RESEARCHED
            ), "You have made progress in researching a new technology"
        )
        addNotification(n)
    }
}
