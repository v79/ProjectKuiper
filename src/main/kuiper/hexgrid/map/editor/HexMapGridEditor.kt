package hexgrid.map.editor

import LogInterface
import actions.ResourceType
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.api.*
import godot.core.*
import godot.extension.getNodeAs
import godot.global.GD
import hexgrid.Hex
import hexgrid.HexMode
import kotlinx.serialization.json.Json
import state.Location
import state.Sponsor
import technology.Science
import utils.clearChildren
import utils.slug
import kotlin.math.sqrt

@RegisterClass
class HexMapGridEditor : GridContainer(), LogInterface {

    override var logEnabled: Boolean = true

    @RegisterProperty
    @Export
    var dimension: Int = 6

    // Globals
    private val signalBus: MapEditorSignalBus by lazy { getNodeAs("/root/HexMapEditor/MapEditorSignalBus")!! }

    // Packed scenes
    private val hexScene = ResourceLoader.load("res://src/main/kuiper/hexgrid/Hex.tscn") as PackedScene
    private val locadationDetailsScene =
        ResourceLoader.load("res://src/main/kuiper/hexgrid/map/editor/location_details_panel.tscn") as PackedScene

    // UI elements
    private val sponsorNameEdit: LineEdit by lazy { getNodeAs("%SponsorNameEdit")!! }
    private val sponsorIdLabel: Label by lazy { getNodeAs("%SponsorIdLbl")!! }
    private val sponsorDescEdit: TextEdit by lazy { getNodeAs("%SponsorDescEdit")!! }
    private val sponsorColorPicker: ColorPickerButton by lazy { getNodeAs("%ColorPickerButton")!! }
    private val sponsorStartGold: SpinBox by lazy { getNodeAs("%SponsorStartGoldEdit")!! }
    private val sponsorStartInf: SpinBox by lazy { getNodeAs("%SponsorStartInfEdit")!! }
    private val sponsorStartConMats: SpinBox by lazy { getNodeAs("%SponsorStartConMatsEdit")!! }
    private val physics: SpinBox by lazy { getNodeAs("%PhysicsEdit")!! }
    private val engineering: SpinBox by lazy { getNodeAs("%EngineeringEdit")!! }
    private val biochemistry: SpinBox by lazy { getNodeAs("%BiochemistryEdit")!! }
    private val maths: SpinBox by lazy { getNodeAs("%MathsEdit")!! }
    private val astronomy: SpinBox by lazy { getNodeAs("%AstronomyEdit")!! }
    private val psychology: SpinBox by lazy { getNodeAs("%PsychologyEdit")!! }
    private val eureka: SpinBox by lazy { getNodeAs("%EurekaEdit")!! }

    private val placeHexPopup: PopupPanel by lazy { getNodeAs("%PlaceHexPopupPanel")!! }
    private val phNameEdit: LineEdit by lazy { getNodeAs("%PlaceHexNameEdit")!! }
    private val phCoordLbl: Label by lazy { getNodeAs("%PHCoordLbl")!! }
    private val phUnlockedAtStart: CheckBox by lazy { getNodeAs("%PHHexUnlocked")!! }
    private val hexCoordsLbl: Label by lazy { getNodeAs("%HexCoordsLbl")!! }
    private val locationListBox: VBoxContainer by lazy { getNodeAs("%LocationListBox")!! }
    private val loadSponsorBtn: Button by lazy { getNodeAs("%LoadSponsorBtn")!! }
    private val saveSponsorBtn: Button by lazy { getNodeAs("%SaveSponsorBtn")!! }
    private val confirmSaveDialog: ConfirmationDialog by lazy { getNodeAs("%SaveConfirmationDialog")!! }
    private val loadFileDialog: FileDialog by lazy { getNodeAs("%LoadFileDialog")!! }

    private val sponsorValid: Boolean
        get() {
            return sponsorNameEdit.text.isNotEmpty() && sponsorDescEdit.text.isNotEmpty() && sponsorIdLabel.text.isNotEmpty() && countLocations() > 0
        }

    // Data
    private var grid = Array(dimension) {
        Array(dimension) {
            Location(
                row = dimension,
                column = dimension,
                name = "",
                position = Vector2.ZERO,
                unlocked = false
            )
        }
    }
    private var selectedRow = -1
    private var selectedCol = -1
    private var sponsor: Sponsor? = null
    private var nextSponsorId = 0

    @RegisterFunction
    override fun _ready() {
        // get the most recent sponsorID
        nextSponsorId = getSponsorIdMax() + 1
        resetGridDisplay()


        // set up the grid
        grid = calculateGridCoordinates(dimension, dimension)
        grid.forEachIndexed { col, row ->
            row.forEachIndexed { j, location ->
                val hex = hexScene.instantiate() as Hex
                hex.hexMode = HexMode.EDITOR_BLANK
                hex.row = j
                hex.col = col
                hex.location = location
                hex.editorSignalBus = signalBus
                hex.setName("Hex_${col}_$j")
                hex.setPosition(location.position)
                addChild(hex)
            }
        }

        signalBus.editor_placeHex.connect { col, row ->
            hexCoordsLbl.text = "(c$col,r$row)"
            selectedCol = col
            selectedRow = row
            phCoordLbl.text = "@c$col,r$row"
            val hex = getNodeAtHex(col, row)
            if (hex != null) {
                phNameEdit.text = hex.location?.name ?: ""
                phUnlockedAtStart.buttonPressed = hex.location?.unlocked ?: false
                placeHexPopup.setPosition(getGlobalMousePosition().toVector2i().minus(Vector2i(50.0, 100.0)))
                placeHexPopup.visible = true
            }
        }

        signalBus.editor_updateLocation.connect { col, row, newName, newUnlocked ->
            updateLocation(col, row, newName, newUnlocked)
        }
    }

    @RegisterFunction
    override fun _process(delta: Double) {
        saveSponsorBtn.disabled = !sponsorValid
    }

    @RegisterFunction
    fun onConfirmLocation() {
        storeHexLocation(selectedCol, selectedRow, phNameEdit.text, phUnlockedAtStart.buttonPressed)
    }

    @RegisterFunction
    fun onCancelLocation() {
        selectedRow = -1
        selectedCol = -1
        placeHexPopup.visible = false
    }

    @RegisterFunction
    fun onPHUnlockedAtStartToggled(toggledOn: Boolean) {
        // no need to do anything here
        // logInfo("Unlocked at start toggled to $toggledOn")
    }


    @RegisterFunction
    fun onSaveSponsorButtonPressed() {
        sponsor = extractSponsorDetails()

        confirmSaveDialog.let { dialog ->
            dialog.title = "Save sponsor ${sponsor?.name}?"
            dialog.dialogText = "The sponsor has ${countLocations()} locations. Do you want to save it?"
            dialog.show()
        }
    }

    @RegisterFunction
    fun onNewSponsorButtonPressed() {
        log("Creating new sponsor")
        resetGridDisplay()
        sponsor = Sponsor(
            id = nextSponsorId, name = "", colour = Color.white, introText = "", startingResources = mapOf(
                ResourceType.GOLD to 100, ResourceType.INFLUENCE to 10, ResourceType.CONSTRUCTION_MATERIALS to 100
            ), baseScienceRate = mapOf(
                Science.PHYSICS to 10.0f,
                Science.ENGINEERING to 10.0f,
                Science.BIOCHEMISTRY to 10.0f,
                Science.MATHEMATICS to 10.0f,
                Science.ASTRONOMY to 10.0f,
                Science.PSYCHOLOGY to 10.0f,
                Science.EUREKA to 10.0f
            ), startingTechs = emptyList(), hexDimensions = Pair(dimension, dimension), hexGrid = grid
        )
        sponsor?.let {
            updateUI()
        }
        nextSponsorId++
    }

    @RegisterFunction
    fun onLoadSponsorButtonPressed() {
        loadFileDialog.currentDir = "res://assets/data/sponsors"
        loadFileDialog.filters = PackedStringArray(listOf("*.sponsor.json").toVariantArray())
        loadFileDialog.show()
    }

    @RegisterFunction
    fun onLoadFileDialogFileSelected(path: String) {
        log("Loading sponsor file $path")
        resetGridDisplay()
        resetLocationList()
        val json = Json {
            allowStructuredMapKeys = true
        }
        val sponsorFile =
            FileAccess.open(path, FileAccess.ModeFlags.READ)
        sponsorFile?.let { file ->
            val jsonString = file.getAsText()
            try {
                sponsor = json.decodeFromString(Sponsor.serializer(), jsonString)
                updateUI()
            } catch (e: Exception) {
                logError("Failed to parse sponsor file $path: ${e.message}")
            }
        }
    }

    private fun updateUI() {
        sponsor?.let {
            sponsorNameEdit.setText(it.name)
            sponsorIdLabel.setText(it.id.toString())
            sponsorDescEdit.setText(it.introText)
            sponsorColorPicker.color = it.colour
            sponsorStartGold.setValue(it.startingResources[ResourceType.GOLD]?.toDouble() ?: 0.0)
            sponsorStartInf.setValue(it.startingResources[ResourceType.INFLUENCE]?.toDouble() ?: 0.0)
            sponsorStartConMats.setValue(it.startingResources[ResourceType.CONSTRUCTION_MATERIALS]?.toDouble() ?: 0.0)
            physics.setValue(it.baseScienceRate[Science.PHYSICS]?.toDouble() ?: 0.0)
            engineering.setValue(it.baseScienceRate[Science.ENGINEERING]?.toDouble() ?: 0.0)
            biochemistry.setValue(it.baseScienceRate[Science.BIOCHEMISTRY]?.toDouble() ?: 0.0)
            maths.setValue(it.baseScienceRate[Science.MATHEMATICS]?.toDouble() ?: 0.0)
            astronomy.setValue(it.baseScienceRate[Science.ASTRONOMY]?.toDouble() ?: 0.0)
            psychology.setValue(it.baseScienceRate[Science.PSYCHOLOGY]?.toDouble() ?: 0.0)
            eureka.setValue(it.baseScienceRate[Science.EUREKA]?.toDouble() ?: 0.0)
            grid =
                it.hexGrid // ultimately, I want the sponsor's hexGrid to be minimal, not complete with all the empty hexes
        }

        // update the grid
        grid.forEach { col ->
            col.forEach { location ->
                if (location.name.isNotEmpty()) {
                    storeHexLocation(location.column, location.row, location.name, location.unlocked)
                } else {
                    val hex = getNodeAtHex(location.column, location.row)
                    if (hex != null) {
                        hex.location = null
                        hex.zIndex -= 1
                        hex.unhighlight()
                    }
                }
            }
        }
    }

    /**
     * Populate the list of locations
     */
    private fun updateLocationList() {
        log("Updating location list")
        grid.forEach { column ->
            column.forEach { location ->
                if (location.name.isNotEmpty()) {
                    if (!locationListBox.hasNode("Location_${location.column}_${location.row})".asNodePath())) {
                        GD.print("Creating LocationDetailsPanel for ${location.column},${location.row}")
                        val newPanel = locadationDetailsScene.instantiate() as LocationDetailsPanel
                        newPanel.signalBus = signalBus
                        newPanel.col = location.column
                        newPanel.row = location.row
                        newPanel.unlockedCheckbox.buttonPressed = location.unlocked
                        newPanel.locationNameEdit.text = location.name
                        newPanel.setName("Location_${location.column}_${location.row}")
                        locationListBox.addChild(newPanel)
                    }
                }
            }
        }
    }

    /**
     * Extract the sponsor information from the UI and store it in a Sponsor object
     */
    private fun extractSponsorDetails(): Sponsor? {
        val sponsorName = sponsorNameEdit.text
        val sponsorDesc = sponsorDescEdit.text
        logInfo("Extracting sponsor '$sponsorName' with description '$sponsorDesc'.")
        if (sponsorName.isBlank() || sponsorDesc.isBlank()) {
            logError("Sponsor name and description must be provided")
            return null
        }

        // extract the sponsor data
        val startGold = sponsorStartGold.getLineEdit()!!.text
        val startInf = sponsorStartInf.getLineEdit()!!.text
        val startConMats = sponsorStartConMats.getLineEdit()!!.text

        val sponsor = Sponsor(
            id = nextSponsorId,
            name = sponsorName,
            colour = sponsorColorPicker.color,
            introText = sponsorDesc,
            startingResources = mapOf(
                ResourceType.GOLD to startGold.toInt(),
                ResourceType.INFLUENCE to startInf.toInt(),
                ResourceType.CONSTRUCTION_MATERIALS to startConMats.toInt()
            ),
            baseScienceRate = mapOf(
                Science.PHYSICS to physics.getLineEdit()!!.text.toFloat(),
                Science.ENGINEERING to engineering.getLineEdit()!!.text.toFloat(),
                Science.BIOCHEMISTRY to biochemistry.getLineEdit()!!.text.toFloat(),
                Science.MATHEMATICS to maths.getLineEdit()!!.text.toFloat(),
                Science.ASTRONOMY to astronomy.getLineEdit()!!.text.toFloat(),
                Science.PSYCHOLOGY to psychology.getLineEdit()!!.text.toFloat(),
                Science.EUREKA to eureka.getLineEdit()!!.text.toFloat()
            ),
            startingTechs = emptyList(),
            hexDimensions = Pair(dimension, dimension),
            hexGrid = grid
        )

        grid.forEachIndexed { col, row ->
            row.forEachIndexed { j, location ->
                val hex = getNodeAtHex(col, j)
                if (hex == null) {
                    logError("No hex found at $col, $j")
                    return null
                } else {
                    location.name = hex.location?.name ?: ""
                    location.unlocked = hex.location?.unlocked ?: false
                    sponsor.hexGrid[col][j] = location
                }
            }
        }
        return sponsor
    }

    /**
     * Save the given sponsor to the sponsors.json file
     */
    @RegisterFunction
    fun onSaveConfirmationConfirmed() {
        if (sponsor == null) {
            logError("No sponsor to save")
            return
        } else {
            val filename = "res://assets/data/sponsors/${sponsor?.name?.slug()}.sponsor.json"
            if (FileAccess.fileExists(filename)) {
                logWarning("Overwriting existing save file $filename")
            }
            val json = Json {
                prettyPrint = true
                encodeDefaults = true
                allowStructuredMapKeys = true
            }

            val jsonString = json.encodeToString(sponsor)
            val file = FileAccess.open(filename, FileAccess.ModeFlags.WRITE)
            file?.let { file ->
                file.storeString(jsonString)
                file.close()
                logInfo("Saved sponsor to $filename")
            } ?: run {
                logError("Failed to create sponsor file $filename")
            }
        }
    }

    @RegisterFunction
    fun deleteSponsor() {
        logError("Deleting sponsors not supported")
    }

    /**
     * Get the highest sponsor ID from the sponsors directory
     */
    private fun getSponsorIdMax(): Int {
        if (!DirAccess.dirExistsAbsolute("res://assets/data/sponsors")) {
            logError("Sponsors data directory does not exist")
            return -1
        }
        val sponsorIds: MutableList<Int> = mutableListOf()
        val dir = DirAccess.open("res://assets/data/sponsors")
        var count = 0
        val json: Json = Json {
            allowStructuredMapKeys = true
        }
        dir?.let { dir ->
            dir.listDirBegin()
            var fileName = dir.getNext()
            while (fileName != "") {
                if (fileName.endsWith(".sponsor.json")) {
                    log("Found sponsor file $fileName")
                    val sponsorFile =
                        FileAccess.open("res://assets/data/sponsors/${fileName}", FileAccess.ModeFlags.READ)
                    sponsorFile?.let { file ->
                        val jsonString = file.getAsText()
                        try {
                            val sponsor = json.decodeFromString(Sponsor.serializer(), jsonString)
                            sponsorIds.add(sponsor.id)
                        } catch (e: Exception) {
                            logError("Failed to parse sponsor file $fileName: ${e.message}")
                        }
                        file.close()
                    } ?: run {
                        logError("Failed to open sponsor file $fileName")
                    }
                    count++
                }
                fileName = dir.getNext()
            }
        }
        log("Found $count sponsors; maxId is ${sponsorIds.maxOrNull()}")
        return sponsorIds.maxOrNull() ?: -1
    }

    /**
     * Calculate the grid pixel coordinates for the given number of rows and columns
     */
    private fun calculateGridCoordinates(xCount: Int, yCount: Int): Array<Array<Location>> {
        // flat topped, even q orientation
        val hexCoords = Array(xCount) {
            Array(yCount) {
                Location(
                    row = xCount,
                    column = yCount,
                    name = "",
                    position = Vector2.ZERO,
                    unlocked = false
                )
            }
        }

        val height = sqrt(3.0) * Hex.HEX_RADIUS
        val horizDistance = 3.0 / 2.0 * Hex.HEX_RADIUS

        // we are calculating the coordinates of the centre of each hex
        for (i in 0 until xCount) {
            for (j in 0 until yCount) {
                val x = i * horizDistance
                val y = j * height + (i % 2) * (height / 2)
                hexCoords[i][j] = Location(
                    row = j,
                    column = i,
                    name = "",
                    position = Vector2(x, y),
                    unlocked = false
                )
            }
        }
        return hexCoords
    }

    /**
     * Store the details of the given hex into the grid
     */
    private fun storeHexLocation(col: Int, row: Int, name: String, unlocked: Boolean) {
        GD.print("Storing hex location $col, $row with name $name and unlocked $unlocked")
        grid[col][row].apply {
            this.unlocked = unlocked
            this.name = name
        }
        val hexNode = getNodeAtHex(col, row) ?: return
        hexNode.location = grid[col][row]
        placeHexPopup.visible = false
        selectedCol = -1
        selectedRow = -1
        phNameEdit.text = ""
        phUnlockedAtStart.buttonPressed = false
        hexNode.zIndex += 1
        hexNode.hexMode = HexMode.EDITOR_LOCATION_SET
        addLocationListEntry(grid[col][row])
        hexNode.queueRedraw()
    }

    /**
     * Update the hex location with the given name and unlocked status. Highlight it.
     */
    @RegisterFunction
    fun updateLocation(col: Int, row: Int, name: String, unlocked: Boolean) {
        log("Updating location hex location $col, $row with name $name and unlocked $unlocked")
        grid[col][row].let {
            it.name = name
            it.unlocked = unlocked
        }
        log("Updated grid[$col][$row] to $name, $unlocked (${grid[col][row].unlocked})")
        val hexNode = getNodeAtHex(col, row) ?: return
        hexNode.location = grid[col][row]
        hexNode.zIndex += 1
        hexNode.highlight()
    }

    /**
     * Add the given location to the list of locations
     */
    private fun addLocationListEntry(location: Location) {
        val newPanel = locadationDetailsScene.instantiate() as LocationDetailsPanel
        newPanel.signalBus = signalBus
        newPanel.col = location.column
        newPanel.row = location.row
        newPanel.unlockedCheckbox.buttonPressed = location.unlocked
        newPanel.locationNameEdit.text = location.name
        newPanel.setName("Location_${location.column}_${location.row}")
        locationListBox.addChild(newPanel)
    }


    /**
     * Reset the grid display to blank
     */
    private fun resetGridDisplay() {
        grid.forEach { col ->
            col.forEach { location ->
                val hexNode = getNodeAtHex(location.column, location.row) ?: return
                hexNode.unhighlight()
                location.unlocked = false
                location.name = ""
                hexNode.hexMode = HexMode.EDITOR_BLANK
            }
        }
    }

    /**
     * Clear the location list
     */
    private fun resetLocationList() {
        locationListBox.clearChildren()
    }

    /**
     * Get the hex at the given row and column
     */
    private fun getNodeAtHex(col: Int, row: Int): Hex? {
        val node = getNodeAs("Hex_${col}_$row".asStringName()) as Hex?
        if (node == null) {
            logError("Hex not found at c$col,r$row")
        }
        return node
    }

    /**
     * Ensure that there is at least one location in the grid
     */
    private fun countLocations(): Int {
        var count = 0
        grid.forEach { col ->
            col.forEach { location ->
                if (location.name.isNotEmpty()) {
                    count++
                }
            }
        }
        return count
    }
}
