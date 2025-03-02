package technology.editor

import LogInterface
import SignalBus
import godot.*
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.core.connect
import godot.extensions.getNodeAs
import technology.TechStatus
import technology.TechTier

@RegisterClass
class TechEditor : Control(), LogInterface {

	@RegisterProperty
	@Export
	override var logEnabled: Boolean = false

	@RegisterProperty
	@Export
	var techWrapper: TechWrapper = TechWrapper()

	// Globals
	private lateinit var signalBus: SignalBus


	// UI Elements
	private lateinit var titleEdit: LineEdit
	private lateinit var tierMenu: MenuButton
	private lateinit var descriptionEdit: TextEdit
	private lateinit var statusMenu: MenuButton
	private lateinit var idLabel: Label
	private lateinit var requirementsList: ItemList
	private lateinit var unlocksList: ItemList
	private lateinit var confirmDeletePanel: PopupPanel
	private lateinit var confirmDeleteButton: Button
	private lateinit var cancelDeleteButton: Button
	private lateinit var confirmDeleteLabel: Label

	@RegisterFunction
	override fun _ready() {
		signalBus = getNodeAs("/root/SignalBus")!!

		titleEdit = getNodeAs("%TitleEdit")!!
		tierMenu = getNodeAs("%TierMenu")!!
		descriptionEdit = getNodeAs("%DescriptionEdit")!!
		statusMenu = getNodeAs("%StatusMenu")!!
		idLabel = getNodeAs("%IDLabel")!!
		requirementsList = getNodeAs("%RequirementList")!!
		unlocksList = getNodeAs("%UnlocksList")!!

		confirmDeletePanel = getNodeAs("%ConfirmDeletePanel")!!
		confirmDeleteButton = getNodeAs("%Delete")!!
		cancelDeleteButton = getNodeAs("%CancelDelete")!!
		confirmDeleteLabel = getNodeAs("%ConfirmDeleteLabel")!!

		tierMenu.getPopup()?.idPressed?.connect { id ->
			setTier(id.toInt())
		}
		statusMenu.getPopup()?.idPressed?.connect { id ->
			setStatus(id.toInt())
		}
	}

	@RegisterFunction
	override fun _process(delta: Double) {

	}

	@RegisterFunction
	fun onVisibilityChanged() {
		updateUI()
	}

	@RegisterFunction
	fun onCancelButtonPressed() {
		visible = false
	}

	@RegisterFunction
	fun onSaveButtonPressed() {
		log("Saving updated technology...")
		log(techWrapper.technology.toString())
		signalBus.editor_techSaved.emit(techWrapper)
		visible = false
	}

	@RegisterFunction
	fun onTitleTextChanged(newText: String) {
		techWrapper.technology.title = newText
	}

	@RegisterFunction
	fun onDescriptionTextChanged() {
		techWrapper.technology.description = descriptionEdit.text
	}

	@RegisterFunction
	fun onDeleteButtonPressed() {
		confirmDeleteLabel.text = "${techWrapper.technology.id}: ${techWrapper.technology.title}"
		confirmDeletePanel.show()
	}

	@RegisterFunction
	fun onCancelDeleteButtonPressed() {
		confirmDeletePanel.hide()
	}

	@RegisterFunction
	fun onConfirmDeleteButtonPressed() {
		logWarning("Deleting technology... ${techWrapper.technology.title}")
		confirmDeletePanel.hide()
		this.hide()
		signalBus.editor_deleteTech.emit(techWrapper)
	}

	/**
	 * Menu items are zero index; tiers are 1-indexed as I am not showing Tier 0 as an option
	 */
	fun setTier(tierID: Int) {
		techWrapper.technology.tier = TechTier.entries[tierID+1]
		tierMenu.text = techWrapper.technology.tier.name
	}

	fun setStatus(statusID: Int) {
		techWrapper.technology.status = TechStatus.entries[statusID]
		statusMenu.text = techWrapper.technology.status.name
	}

	fun updateUI() {
		if (techWrapper.technology.id > 0) {
			techWrapper.technology.let { tech ->
				titleEdit.text = tech.title
				descriptionEdit.text = tech.description
				idLabel.text = tech.id.toString()
				statusMenu.text = tech.status.toString()
				tierMenu.text = tech.tier.name

				// These are just IDs; would be nice if I could lookup the entire tree
				tech.requires.forEach { req ->
					requirementsList.clear()
					requirementsList.addItem(text = req.toString(), selectable = false)
				}
			}
		}
	}
}
