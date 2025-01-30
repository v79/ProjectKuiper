package actions

import godot.Input
import godot.Label
import godot.Node2D
import godot.StaticBody2D
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.core.Vector2
import godot.core.asNodePath
import godot.core.asStringName
import godot.extensions.getNodeAs

@RegisterClass
class ActionCard : Node2D() {

	@RegisterProperty
	@Export
	var cardName = "Card"

	@RegisterProperty
	@Export
	var influenceCost = 3

	private var action: Action? = null

	@RegisterProperty
	@Export
	var clickRadius = 200
	private var dragging = false
	private var isDraggable = false
	private var startPosition = Vector2()
	private var offset = Vector2()

	// UI elements
	private lateinit var cardNameLabel: Label
	private lateinit var influenceCostLabel: Label


	@RegisterFunction
	override fun _ready() {
		startPosition = position
		cardNameLabel = getNodeAs("PanelContainer/VBoxContainer/HBoxContainer/CardName")!!
	}

	@RegisterFunction
	override fun _process(delta: Double) {
		if (isDraggable) {
			if (Input.isActionJustPressed("mouse_left_click".asStringName())) {
				offset = getGlobalMousePosition() - globalPosition
			}
			if (Input.isActionPressed("mouse_left_click".asStringName())) {
				dragging = true
				globalPosition = getGlobalMousePosition() - offset
			} else if (Input.isActionJustReleased("mouse_left_click".asStringName())) {
				getTree()!!.createTween()?.tweenProperty(this, "position".asNodePath(), startPosition, 0.2)
				dragging = false
			}
		}
	}

	@RegisterFunction
	fun _on_area_2d_mouse_entered() {
		if (!dragging) {
			isDraggable = true
		}
	}

	@RegisterFunction
	fun _on_area_2d_mouse_exited() {
		if (!dragging) {
			isDraggable = false
		}
	}

	@RegisterFunction
	fun _on_area_2d_body_entered(body: StaticBody2D) {

	}

	@RegisterFunction
	fun _on_area_2d_body_exited() {

	}

}
