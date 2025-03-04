package technology.tree

import godot.Label
import godot.PanelContainer
import godot.RichTextLabel
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.extensions.getNodeAs
import technology.editor.TechWrapper

@RegisterClass
class TechSummaryPanel : PanelContainer() {

    // UI elements
    private lateinit var titleLabel: Label
    private lateinit var tierLabel: Label
    private lateinit var descriptionLabel: RichTextLabel
    private lateinit var statusLabel: Label

    @RegisterFunction
    override fun _ready() {
        titleLabel = getNodeAs("%TitleLabel")!!
        tierLabel = getNodeAs("%TierLabel")!!
        descriptionLabel = getNodeAs("%DescriptionLabel")!!
        statusLabel = getNodeAs("%StatusLabel")!!
    }


    @RegisterFunction
    override fun _process(delta: Double) {

    }

    @RegisterFunction
    fun updateSummary(techWrapper: TechWrapper) {
        techWrapper.technology.let { tech ->
            titleLabel.text = tech.title
            tierLabel.text = tech.tier.name
            descriptionLabel.text = tech.description
            statusLabel.text = tech.status.name
        }
    }
}
