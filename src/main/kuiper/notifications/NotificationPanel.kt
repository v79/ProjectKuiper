package notifications

import LogInterface
import SignalBus
import godot.PackedScene
import godot.ResourceLoader
import godot.VBoxContainer
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.core.Vector2
import godot.core.connect
import godot.extensions.getNodeAs
import technology.TechStatus
import technology.TechTier
import technology.Technology

@RegisterClass
class NotificationPanel : VBoxContainer(), LogInterface {

    @RegisterProperty
    @Export
    override var logEnabled: Boolean = true

    // Globals
    private lateinit var signalBus: SignalBus

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
        signalBus = getNodeAs("/root/SignalBus")!!

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
        log("Clearing transient notifications")
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
        val n = Notification.ResearchProgress(
            Technology(
                -1, "Dummy tech", "Fake technology", TechTier.TIER_2, TechStatus.RESEARCHED
            ), "You have made progress in researching a new technology"
        )
        addNotification(n)
    }
}
