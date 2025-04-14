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
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import state.Location
import state.Sponsor
import technology.Science
import utils.clearChildren
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
    private val chooseSponsorButton: MenuButton by lazy { getNodeAs("%LoadSponsorBtn")!! }

    // Data
    private var grid = Array(dimension) {
        Array(dimension) {
            Location(
                row = dimension, column = dimension, name = "", position = Vector2.ZERO, unlocked = false
            )
        }
    }
    private var selectedRow = -1
    private var selectedCol = -1
    private var sponsor: Sponsor? = null
    private var sponsorJsonPath: String = "res://assets/data/sponsors.json"
    private var sponsors: MutableList<Sponsor> = mutableListOf()
    private var nextSponsorId = 0

    @RegisterFunction
    override fun _ready() {
        // load the sponsors
        resetGridDisplay()
        sponsors = loadSponsors()
        if (sponsors.size > 0) {
            sponsors.forEach { sponsor ->
                chooseSponsorButton.getPopup()!!.addItem("${sponsor.id} - ${sponsor.name}", sponsor.id)
            }
            nextSponsorId = sponsors.size
        }
        sponsorIdLabel.setText(nextSponsorId.toString())

        chooseSponsorButton.getPopup()!!.idPressed.connect { id ->
            onSponsorChosen(id.toInt())
        }

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
    fun onConfirmLocation() {
        storeHexLocation(selectedCol, selectedRow, phNameEdit.text, phUnlockedAtStart.buttonPressed)
    }

    @RegisterFunction
    fun onCancelLocation() {
        selectedRow = -1
        selectedCol = -1
        placeHexPopup.visible = false
        phUnlockedAtStart.buttonPressed = false
    }

    @RegisterFunction
    fun onPHUnlockedAtStartToggled(toggledOn: Boolean) {
        // no need to do anything here
        // logInfo("Unlocked at start toggled to $toggledOn")
    }


    @RegisterFunction
    fun onSaveSponsorButtonPressed() {
        sponsor = storeSponsorDetails()
        sponsor?.let { saveSponsor(it) }
    }

    @RegisterFunction
    fun onNewSponsorButtonPressed() {
        log("Creating new sponsor")
        resetGridDisplay() // clear the grid, removes locations
        nextSponsorId++
        sponsor = Sponsor(
            id = nextSponsorId, name = "", colour = Color.white, introText = "", startingResources = mapOf(
                ResourceType.GOLD to 0, ResourceType.INFLUENCE to 0, ResourceType.CONSTRUCTION_MATERIALS to 0
            ), baseScienceRate = mapOf(
                Science.PHYSICS to 0.0f,
                Science.ENGINEERING to 0.0f,
                Science.BIOCHEMISTRY to 0.0f,
                Science.MATHEMATICS to 0.0f,
                Science.ASTRONOMY to 0.0f,
                Science.PSYCHOLOGY to 0.0f,
                Science.EUREKA to 0.0f
            ), startingTechs = emptyList(), hexDimensions = Pair(dimension, dimension), hexGrid = grid
        )
        sponsor?.let {
            updateUI()
        }
    }

    @RegisterFunction
    fun onSponsorChosen(id: Int) {
        log("Switching to sponsor $id")
        resetGridDisplay()
        sponsor = sponsors.find { it.id == id }
        if (sponsor == null) {
            logError("Failed to switch to sponsor $id; null returned")
            return
        }
        updateUI()
        locationListBox.clearChildren()
        sponsor?.hexGrid?.forEach { row ->
            row.forEachIndexed { j, location ->
                if (location.name.isNotEmpty()) {
                    addLocationListEntry(location)
                }
            }
        }
        sponsor?.let {
            grid = it.hexGrid
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
                    log("Storing hex location $col, $location.row with name ${location.name} and unlocked ${location.unlocked}")
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
     * Extract the sponsor information from the UI and store it in a Sponsor object
     */
    private fun storeSponsorDetails(): Sponsor? {
        val sponsorName = sponsorNameEdit.text
        val sponsorDesc = sponsorDescEdit.text
        logInfo("Saving sponsor '$sponsorName' with description '$sponsorDesc'.")
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
        nextSponsorId++

        grid.forEachIndexed { col, row ->
            row.forEachIndexed { j, location ->
                val hex = getNodeAtHex(col, j)
                if (hex == null) {
                    logError("No hex found at $col, $j")
                    return null
                } else {
//                    logInfo("Hex ($col,$j) ${hex.location?.name} (Starts locked: ${location})")
                    location.name = hex.location?.name ?: ""
                    location.unlocked = hex.location?.unlocked ?: false

                    sponsor.hexGrid[col][j] = location
                }
            }
        }
        logInfo("Sponsor details: $sponsor")
        return sponsor
    }

    /**
     * Save the given sponsor to the sponsors.json file
     */
    private fun saveSponsor(sponsor: Sponsor) {
        val json = Json {
            prettyPrint = true
            encodeDefaults = true
            allowStructuredMapKeys = true
        }
        val existingSponsor = sponsors.find { it.id == sponsor.id }
        if (existingSponsor != null) {
            log("Replacing existing sponsor ${existingSponsor.id} ${existingSponsor.name}")
            sponsors.remove(existingSponsor)
            sponsors.add(sponsor)
            GD.print(sponsor)
            sponsor.hexGrid.forEachIndexed { col, row ->
                row.forEachIndexed { j, location ->
                    GD.print("\tLocation ($col,$j) ${location.name} (Unlocked: ${location.unlocked})")
                }
            }
        } else {
            log("Adding new sponsor ${sponsor.id} ${sponsor.name}")
            sponsors.add(sponsor)
        }

        val sponsorJson = json.encodeToString(ListSerializer(Sponsor.serializer()), sponsors)
        if (!DirAccess.dirExistsAbsolute("res://assets/data")) {
            DirAccess.makeDirRecursiveAbsolute("res://assets/data")
        }
        val sponsorFile = FileAccess.open(
            "res://assets/data/sponsors.json", FileAccess.ModeFlags.WRITE
        )
        log("Saving sponsor file ${ProjectSettings.globalizePath(sponsorFile?.getPath() ?: "null")}")
        sponsorFile?.let {
            it.storeString(sponsorJson)
            it.close()
        }
    }

    @RegisterFunction
    fun deleteSponsor() {
        val currentSponsor = sponsor?.id
        val sponsor = sponsors.find { it.id == currentSponsor }
        if (sponsor != null) {
            sponsors.remove(sponsor)
            chooseSponsorButton.getPopup()!!.removeItem(sponsor.id)
        }
    }

    /**
     * Load the sponsors from the sponsors.json file
     */
    private fun loadSponsors(): MutableList<Sponsor> {
        val sponsors = mutableListOf<Sponsor>()
        if (!DirAccess.dirExistsAbsolute("res://assets/data")) {
            logError("No data directory found")
            return sponsors
        }
        if (!FileAccess.fileExists(sponsorJsonPath)) {
            logError("No sponsors.json file found")
            return sponsors
        } else {
            val sponsorFile = FileAccess.open(sponsorJsonPath, FileAccess.ModeFlags.READ)!!
            val sponsorJson = sponsorFile.getAsText()
            val json = Json {
                prettyPrint = true
                encodeDefaults = true
                allowStructuredMapKeys = true
            }
            val sponsorList = json.decodeFromString(ListSerializer(Sponsor.serializer()), sponsorJson)
            sponsors.addAll(sponsorList)
            log("Loaded ${sponsors.size} sponsors")
        }
        return sponsors
    }

    /**
     * Calculate the grid pixel coordinates for the given number of rows and columns
     */
    private fun calculateGridCoordinates(xCount: Int, yCount: Int): Array<Array<Location>> {
        // flat topped, even q orientation
        val hexCoords = Array(xCount) {
            Array(yCount) {
                Location(
                    row = xCount, column = yCount, name = "", position = Vector2.ZERO, unlocked = false
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
                    row = j, column = i, name = "", position = Vector2(x, y), unlocked = false
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
     * Clear the grid and reset the display, wiping all locations
     */
    private fun resetGridDisplay() {
        grid.forEach { col ->
            col.forEach { location ->
                val hexNode = getNodeAtHex(location.column, location.row) ?: return
                hexNode.hexMode = HexMode.EDITOR_BLANK
                location.unlocked = false
                location.name = ""
                hexNode.unhighlight()
            }
        }
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
}
