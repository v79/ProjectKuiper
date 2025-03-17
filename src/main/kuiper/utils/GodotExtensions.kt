package utils

import godot.api.Node

/**
 * Check if a node has children
 */
fun Node.hasChildren(): Boolean {
    return this.getChildCount() > 0
}

/**
 * Clear all children from a node by calling queueFree on each child
 */
fun Node.clearChildren() {
    this.getChildren().forEach { it.queueFree() }
}