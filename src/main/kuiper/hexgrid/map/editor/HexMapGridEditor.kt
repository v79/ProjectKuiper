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
import hexgrid.Hex
import hexgrid.HexMode
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import state.Location
import state.Sponsor
import technology.Science
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

    private val chooseSponsorButton: MenuButton by lazy { getNodeAs("%LoadSponsorBtn")!! }

    // Data
    private var grid =
        Array(dimension) { Array(dimension) { HexData(dimension, dimension, Location(""), Vector2.ZERO) } }
    private var selectedRow = -1
    private var selectedCol = -1
    private var sponsor: Sponsor? = null
    private var sponsorJsonPath: String = "res://assets/data/sponsors.json"
    private var sponsors: MutableList<Sponsor> = mutableListOf()
    private var nextSponsorId = 0

    @RegisterFunction
    override fun _ready() {
        // load the sponsors
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
        grid.forEachIndexed { i, row ->
            row.forEachIndexed { j, data ->
                val hex = hexScene.instantiate() as Hex
                hex.hexMode = HexMode.EDITOR
                hex.row = i
                hex.col = j
                hex.location = Location("location $i,$j")
                hex.editorSignalBus = signalBus
                hex.setName("Hex_${i}_$j")
                hex.colour = godot.core.Color.mediumPurple
                val locLabel = hex.getNodeAs<Label>("%LocationLabel")!!
                locLabel.visible = false
                hex.setPosition(data.position)
                addChild(hex)
            }
        }

        signalBus.editor_placeHex.connect { row, col ->
            hexCoordsLbl.text = "($row,$col)"
            selectedRow = row
            selectedCol = col
            phCoordLbl.text = "@$row,$col"
            val hex = getNodeAtHex(row, col)
            if (hex != null && hex.hexUnlocked) {
                phNameEdit.text = hex.location?.name ?: ""
                phUnlockedAtStart.buttonPressed = true
            }
            placeHexPopup.setPosition(getGlobalMousePosition().toVector2i().minus(Vector2i(50.0, 100.0)))
            placeHexPopup.visible = true
        }
    }

    @RegisterFunction
    fun onConfirmLocation() {
        storeHexLocation(selectedRow, selectedCol, phNameEdit.text)
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
        sponsor = storeSponsorDetails()
        sponsor?.let { saveSponsor(it) }
    }

    @RegisterFunction
    fun onNewSponsorButtonPressed() {
        log("Creating new sponsor")
        nextSponsorId++
        sponsor = Sponsor(
            id = nextSponsorId,
            name = "",
            colour = Color.white,
            introText = "",
            startingResources = mapOf(
                ResourceType.GOLD to 0,
                ResourceType.INFLUENCE to 0,
                ResourceType.CONSTRUCTION_MATERIALS to 0
            ),
            baseScienceRate = mapOf(
                Science.PHYSICS to 0.0f,
                Science.ENGINEERING to 0.0f,
                Science.BIOCHEMISTRY to 0.0f,
                Science.MATHEMATICS to 0.0f,
                Science.ASTRONOMY to 0.0f,
                Science.PSYCHOLOGY to 0.0f,
                Science.EUREKA to 0.0f
            ),
            startingTechs = emptyList(),
            hexDimensions = Pair(dimension, dimension),
            hexGrid = grid
        )
        sponsor?.let {
            updateUI()
        }
    }

    @RegisterFunction
    fun onSponsorChosen(id: Int) {
        log("Switching to sponsor $id")
        sponsor = sponsors.find { it.id == id }
        if (sponsor == null) {
            logError("Failed to switch to sponsor $id; null returned")
            return
        }
        updateUI()
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
            grid = it.hexGrid
        }

        // update the grid
        grid.forEach { editorData ->
            editorData.forEach { value ->
                if (value.location.name.isNotEmpty()) {
                    storeHexLocation(value.row, value.column, value.location.name)
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

        grid.forEachIndexed { i, row ->
            row.forEachIndexed { j, data ->
                val hex = getNodeAtHex(i, j)
                if (hex == null) {
                    logError("No hex found at $i $j")
                    return null
                } else {
                    if (hex.hexUnlocked) {
                        logInfo("Hex ($i,$j) ${hex.location?.name} (Starts unlocked: ${data.unlockedAtStart})")
                        data.location = hex.location ?: Location("<no name>", data.unlockedAtStart)
                    }
                    sponsor.hexGrid[i][j] = data
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
        sponsors.find { it.id == sponsor.id }?.let {
            log("Updating existing sponsor ${it.id} ${it.name}")
            it == sponsor
        } ?: sponsors.add(sponsor)
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
    private fun calculateGridCoordinates(xCount: Int, yCount: Int): Array<Array<HexData>> {
        // flat topped, evenq orientation
        val hexCoords = Array(xCount) { Array(yCount) { HexData(xCount, yCount, Location(""), Vector2.ZERO) } }
        val radius = 75.0
        val diameter = radius * 2
        val width = diameter
        val height = sqrt(3.0) * radius
        val horizDistance = 3.0 / 2.0 * radius

        // we are calculating the coordinates of the centre of each hex
        for (i in 0 until xCount) {
            for (j in 0 until yCount) {
                val x = i * horizDistance
                val y = j * height + (i % 2) * (height / 2)
                hexCoords[i][j] = HexData(i, j, Location(""), Vector2(x, y))
            }
        }
        return hexCoords
    }

    /**
     * Store the details of the given hex into the grid
     */
    private fun storeHexLocation(row: Int, col: Int, name: String) {
        val hex = grid[row][col]
        hex.unlockedAtStart = phUnlockedAtStart.buttonPressed
        val hexNode = getNodeAtHex(row, col) ?: return
        val location = Location(name, phUnlockedAtStart.buttonPressed)
        hexNode.location = location
        hex.location = location
        val locLabel = hexNode.getNodeAs<Label>("%LocationLabel")!!
        locLabel.text = name
        locLabel.visible = true
        locLabel.setPosition(Vector2(-20.0, -20.0))
        placeHexPopup.visible = false
        selectedCol = -1
        selectedRow = -1
        phNameEdit.text = ""
        phUnlockedAtStart.buttonPressed = false
        hexNode.hexUnlocked = true
        hexNode.zIndex += 1
    }

    private fun resetGridDisplay(): Unit = TODO()

    /*  fun axial_to_oddq(hex: Hex) {
          val col = hex.q
          val row = hex.r + (hex.q - (hex.q&1)) / 2
          return OffsetCoord(col, row)
      }

      fun oddq_to_axial(hex: Hex) {
          val q = hex.col
          val r = hex.row - (hex.col - (hex.col&1)) / 2
          return Hex(q, r)
      }*/

    /**
     * Get the hex at the given row and column
     */
    private fun getNodeAtHex(row: Int, col: Int): Hex? {
        val node = getNodeAs("Hex_${row}_$col".asStringName()) as Hex?
        if (node == null) {
            logError("Hex not found at $row $col")
        }
        return node
    }
}
