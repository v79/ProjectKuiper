package screens.kuiper.pullDownPanel

import godot.*
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.core.Vector2
import godot.extensions.getNodeAs
import godot.global.GD

@RegisterClass
class PullDownPanel : Control() {

	@Export
	@RegisterProperty
	var contents: Control? = null

	private var isExpanded = false
	private var direction: Int = 0 // -1 for shrinking, 0 for idle, 1 for expanding
	private var maxHeight: Double = 25.0
	private var minHeight = 25.0
	lateinit var handle: ColorRect

	lateinit var innerPanelContainer: PanelContainer


	@RegisterFunction
	override fun _ready() {
		GD.print(this)
		this.getChildren().forEachIndexed { index, child ->
			GD.print("$index: $child")
		}
		// the zeroth child is the Panel node on the original scene
		// the first child is the contents added to the game scene
		contents = getChild(1) as Control
		maxHeight = contents!!.getRect().size.y

		innerPanelContainer = getNodeAs("VBoxContainer/PanelContainer")!!
		minHeight = innerPanelContainer.getRect().size.y
		GD.print("Contents height: $maxHeight")
		GD.print("Panel height: ${getRect().size.y}")
		handle = getNodeAs("VBoxContainer/Handle")!!

		contents?.let {
			it.setPosition(Vector2(this.position.x, -maxHeight))
			it.visible = true
		}


	}


	@RegisterFunction
	override fun _input(event: InputEvent?) {
		if (event is InputEventKey) {
			if (event.getKeycode() == Key.KEY_H && event.pressed) {
				GD.print("H key pressed")
				if (!isExpanded) {
					direction = 1
					contents?.visible = true
				} else {
					direction = -1
				}
			}

		}
	}

	@RegisterFunction
	override fun _process(delta: Double) {
		if (direction == 1) {
			val contentFinalYPos = maxHeight
			if (innerPanelContainer.getRect().size.y < maxHeight) {
				GD.print("Growing panel")
				GD.print("Contents current y: ${contents?.position?.y}")
				var currentY = innerPanelContainer.getRect().size.y
				currentY += 10
				innerPanelContainer.setSize(Vector2(innerPanelContainer.getRect().size.x, currentY))
				handle.setPosition(Vector2(handle.position.x, currentY))
				contents?.let {
					it.setPosition(Vector2(it.position.x, currentY-contentFinalYPos))
				}
			} else {
				direction = 0
				isExpanded = true
			}
		}
		if (direction == -1) {
			if (innerPanelContainer.getRect().size.y > minHeight) {
				GD.print("Shrinking panel")
				GD.print("Contents current y: ${contents?.position?.y}")
				var currentY = innerPanelContainer.getRect().size.y
				currentY -= 10
//				currentY.coerceAtLeast(minHeight)
				innerPanelContainer.setSize(Vector2(innerPanelContainer.getRect().size.x, currentY))
				handle.setPosition(Vector2(handle.position.x, currentY))
				contents?.let {
					it.setPosition(Vector2(it.position.x, it.position.y - 10))
				}
			} else {
				direction = 0
				isExpanded = false
				contents?.visible = false
			}
		}
	}


}
