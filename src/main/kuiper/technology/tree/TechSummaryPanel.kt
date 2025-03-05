package technology.tree

import godot.Label
import godot.PanelContainer
import godot.RichTextLabel
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.extensions.getNodeAs
import technology.Science
import technology.Technology
import technology.editor.TechWrapper

@RegisterClass
class TechSummaryPanel : PanelContainer() {

    @RegisterProperty
    @Export
    var devMode: Boolean = true

    // UI elements
    private lateinit var titleLabel: Label
    private lateinit var tierLabel: Label
    private lateinit var descriptionLabel: RichTextLabel
    private lateinit var statusLabel: Label
    private lateinit var debugProgressLabel: RichTextLabel

    @RegisterFunction
    override fun _ready() {
        titleLabel = getNodeAs("%TitleLabel")!!
        tierLabel = getNodeAs("%TierLabel")!!
        descriptionLabel = getNodeAs("%DescriptionLabel")!!
        statusLabel = getNodeAs("%StatusLabel")!!
        debugProgressLabel = getNodeAs("%DebugProgressLabel")!!
        if (!devMode) {
            debugProgressLabel.hide()
        } else {
            debugProgressLabel.show()
        }
    }


    @RegisterFunction
    override fun _process(delta: Double) {

    }

    @RegisterFunction
    fun updateSummary(techWrapper: TechWrapper) {
        techWrapper.technology.let { tech ->
            titleLabel.text = tech.title
            tierLabel.text = "${tech.tier.ordinal}: ${tech.tier.description}"
            descriptionLabel.text = tech.description
            statusLabel.text = tech.status.name
            if (devMode) {
                debugProgressLabel.text = buildDebugProgressString(tech)
            }
        }
    }

    private fun buildDebugProgressString(technology: Technology): String {
        val sBuilder = StringBuilder()
        sBuilder.appendLine("Progress: ${technology.progress}/${technology.totalCost} (${technology.progressPct}%)")
        if (technology.researched) {
            sBuilder.appendLine("Researched")
        }
        Science.entries.forEach { science ->
            sBuilder.appendLine("${science.bbCodeIcon(16)}: ${technology.researchProgress[science]}")
        }
        return sBuilder.toString()
    }
}
