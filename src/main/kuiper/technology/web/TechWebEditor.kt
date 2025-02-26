package technology.web

import LogInterface
import godot.Control
import godot.annotation.*
import godot.core.signal1
import godot.global.GD
import technology.TechStatus
import technology.TechTier
import technology.Technology

@RegisterClass
class TechWebEditor : Control(), LogInterface {

    @RegisterProperty
    @Export
    override var logEnabled: Boolean = true

    private val technologies: MutableList<Technology> = mutableListOf()

    // Signals
    @RegisterSignal
    val nodeAdded by signal1<TechWrapper>("technology_added")

    @RegisterFunction
    override fun _ready() {
        val startingTech = Technology(
            0,
            "Starting Point",
            "The beginning of all things",
            TechTier.TIER_0,
            TechStatus.RESEARCHED
        )
        technologies.add(startingTech)
        nodeAdded.emit(TechWrapper().apply { technology = startingTech })

        log("TechWebEditor ready: getRect() = ${getRect()}")
    }

    @RegisterFunction
    override fun _process(delta: Double) {

    }

    @RegisterFunction
    fun _onAddButtonClicked() {
        GD.print("Add button clicked")
        val newTech =
            Technology(technologies.size, "New Tech", "A new technology", TechTier.TIER_1, TechStatus.UNLOCKED)
        technologies.add(newTech)
        nodeAdded.emit(TechWrapper().apply { technology = newTech })
    }
}
