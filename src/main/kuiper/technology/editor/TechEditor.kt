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
import technology.Science
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
	private lateinit var physicsRange: ScienceRangeEdit
	private lateinit var astronomyRange: ScienceRangeEdit
	private lateinit var biochemistryRange: ScienceRangeEdit
	private lateinit var mathematicsRange: ScienceRangeEdit
	private lateinit var psychologyRange: ScienceRangeEdit
	private lateinit var engineeringRange: ScienceRangeEdit
	private lateinit var multiplierEdit: LineEdit

	// Data
	var multiplier: Double = 1.0

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

		physicsRange = getNodeAs("%PhysicsRange")!!
		astronomyRange = getNodeAs("%AstronomyRange")!!
		biochemistryRange = getNodeAs("%BiochemistryRange")!!
		mathematicsRange = getNodeAs("%MathematicsRange")!!
		psychologyRange = getNodeAs("%PsychologyRange")!!
		engineeringRange = getNodeAs("%EngineeringRange")!!

		multiplierEdit = getNodeAs("%MultiplierEdit")!!


		tierMenu.getPopup()?.idPressed?.connect { id ->
			setTier(id.toInt())
		}
		statusMenu.getPopup()?.idPressed?.connect { id ->
			setStatus(id.toInt())
		}

		setupScienceRanges()
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

	@RegisterFunction
	fun onMultiplierEditChanged(newText: String) {
		val asDouble = newText.toDoubleOrNull()
		if (asDouble != null) {
			multiplier = asDouble
		} else {
			logError("Invalid multiplier value: $newText")
			multiplierEdit.setText("1.0")
		}
	}

	/**
	 * Menu items are zero index; tiers are 1-indexed as I am not showing Tier 0 as an option
	 */
	fun setTier(tierID: Int) {
		techWrapper.technology.tier = TechTier.entries[tierID + 1]
		tierMenu.text = techWrapper.technology.tier.name
	}

	fun setStatus(statusID: Int) {
		techWrapper.technology.status = TechStatus.entries[statusID]
		statusMenu.text = techWrapper.technology.status.name
	}

	private fun setupScienceRanges() {
		multiplierEdit.setText("$multiplier")
		physicsRange.setLabel("${Science.PHYSICS.bbCodeIcon(16)} Physics")
		astronomyRange.setLabel("${Science.ASTRONOMY.bbCodeIcon(16)} Astronomy")
		biochemistryRange.setLabel("${Science.BIOCHEMISTRY.bbCodeIcon(16)} Biochemistry")
		mathematicsRange.setLabel("${Science.MATHEMATICS.bbCodeIcon(16)} Mathematics")
		psychologyRange.setLabel("${Science.PSYCHOLOGY.bbCodeIcon(16)} Psychology")
		engineeringRange.setLabel("${Science.ENGINEERING.bbCodeIcon(16)} Engineering")
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
