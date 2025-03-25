package hexgrid.map.editor

import LogInterface
import SignalBus
import actions.ResourceType
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.api.*
import godot.core.Vector2
import godot.core.Vector2i
import godot.core.asStringName
import godot.core.connect
import godot.extension.getNodeAs
import hexgrid.Hex
import hexgrid.HexMode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import serializers.GDVectorSerializer
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
    private lateinit var signalBus: SignalBus

    // Packed scenes
    private val hexScene = ResourceLoader.load("res://src/main/kuiper/hexgrid/Hex.tscn") as PackedScene

    // UI elements
    private val sponsorNameEdit: LineEdit by lazy { getNodeAs("%SponsorNameEdit")!! }
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

    // Data
    private var grid =
        Array(dimension) { Array(dimension) { EditorData(dimension, dimension, Location(""), Vector2.ZERO) } }
    private var selectedRow = -1
    private var selectedCol = -1
    private var sponsor: Sponsor? = null
    private var sponsorCount: Int = 0

    @RegisterFunction
    override fun _ready() {
        signalBus = getNodeAs("/root/SignalBus")!!

        // set up the grid
        grid = calculateGridCoordinates(dimension, dimension)
        grid.forEachIndexed { i, row ->
            row.forEachIndexed { j, data ->
                val hex = hexScene.instantiate() as Hex
                hex.hexMode = HexMode.EDITOR
                hex.row = i
                hex.col = j
                hex.location = Location("location $i,$j")
                hex.setName("Hex_${i}_$j")
                hex.colour = godot.core.Color.mediumPurple
                val locLabel = hex.getNodeAs<Label>("%LocationLabel")!!
                locLabel.visible = false
//                locLabel.setText("${vector2.x},${vector2.y}")
//                locLabel.setPosition(Vector2(-20.0,-20.0))
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
            placeHexPopup.setPosition(getGlobalMousePosition().toVector2i().minus(Vector2i(50.0, 50.0)))
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
        val sponsorName = sponsorNameEdit.text
        val sponsorDesc = sponsorDescEdit.text
        logInfo("Saving sponsor '$sponsorName' with description '$sponsorDesc'.")

        // extract the sponsor data
        val startGold = sponsorStartGold.getLineEdit()!!.text
        val startInf = sponsorStartInf.getLineEdit()!!.text
        val startConMats = sponsorStartConMats.getLineEdit()!!.text

        val sponsor = Sponsor(
            id = sponsorCount + 1,
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

        grid.forEachIndexed { i, row ->
            row.forEachIndexed { j, data ->
                val hex = getNodeAtHex(i, j)
                if (hex == null) {
                    logError("No hex found at $i $j")
                    return
                } else {
                    if (hex.hexUnlocked) {
                        logInfo("Hex ($i,$j) ${hex.location?.name} (Starts unlocked: ${data.unlockedAtStart})")
                        data.location = hex.location ?: Location("<no name>", data.unlockedAtStart)
                    }
                    sponsor.hexGrid[i][j] = data
                }
            }
        }
        saveSponsor(sponsor)
    }

    private fun saveSponsor(sponsor: Sponsor) {
        val json = Json {
            prettyPrint = true
            encodeDefaults = true
            allowStructuredMapKeys = true
        }
        val sponsorJson = json.encodeToString(Sponsor.serializer(), sponsor)
        log(sponsorJson)
    }

    /**
     * Calculate the grid pixel coordinates for the given number of rows and columns
     */
    private fun calculateGridCoordinates(xCount: Int, yCount: Int): Array<Array<EditorData>> {
        // flat topped, evenq orientation
        val hexCoords = Array(xCount) { Array(yCount) { EditorData(xCount, yCount, Location(""), Vector2.ZERO) } }
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
                hexCoords[i][j] = EditorData(i, j, Location(""), Vector2(x, y))
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

@Serializable
data class EditorData(
    var row: Int,
    var column: Int,
    var location: Location,
    @Serializable(with = GDVectorSerializer::class) val position: Vector2,
    var unlockedAtStart: Boolean = false
)
