package hexgrid.map.editor

import LogInterface
import SignalBus
import godot.annotation.Export
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterProperty
import godot.api.*
import godot.core.Vector2
import godot.core.asStringName
import godot.core.connect
import godot.extension.getNodeAs
import hexgrid.Hex
import hexgrid.HexMode
import state.Location
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
    private val placeHexPopup: PopupPanel by lazy { getNodeAs("%PlaceHexPopupPanel")!! }
    private val phNameEdit: LineEdit by lazy { getNodeAs("%PlaceHexNameEdit")!! }
    private val phCoordLbl: Label by lazy { getNodeAs("%PHCoordLbl")!! }
    private val phUnlockedAtStart: CheckBox by lazy { getNodeAs("%PHHexUnlocked")!! }
    private val hexCoordsLbl: Label by lazy { getNodeAs("%HexCoordsLbl")!! }

    // Data
    private var grid = Array(dimension) { Array(dimension) { EditorData(Vector2.ZERO) } }
    private var selectedRow = -1
    private var selectedCol = -1

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
                hex.setPosition(data.vector)
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
            placeHexPopup.visible = true
        }
    }

    @RegisterFunction
    fun onConfirmLocation() {
        saveHexLocation(selectedRow, selectedCol, phNameEdit.text)
    }

    @RegisterFunction
    fun onCancelLocation() {
        selectedRow = -1
        selectedCol = -1
        placeHexPopup.visible = false
    }

    @RegisterFunction
    fun onPHUnlockedAtStartToggled(toggledOn: Boolean) {
        logInfo("Unlocked at start toggled to $toggledOn")
    }

    @RegisterFunction
    fun onSaveSponsorButtonPressed() {
        val sponsorName = sponsorNameEdit.text
        val sponsorDesc = sponsorDescEdit.text
        logInfo("Saving sponsor $sponsorName with description $sponsorDesc")

        grid.forEachIndexed { i, row ->
            row.forEachIndexed { j, data ->
                val hex = getNodeAtHex(i, j)
                if (hex == null) {
                    logError("No hex found at $i $j")
                    return
                } else {
                    if (hex.hexUnlocked) {
                        logInfo("Hex ($i,$j) ${hex.location?.name} (Starts unlocked: ${data.unlockedAtStart})")
                    }
                }
            }
        }
    }

    private fun calculateGridCoordinates(xCount: Int, yCount: Int): Array<Array<EditorData>> {
        // flat topped, evenq orientation
        val hexCoords = Array(xCount) { Array(yCount) { EditorData(Vector2.ZERO) } }
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
                hexCoords[i][j] = EditorData(Vector2(x, y))
            }
        }
        return hexCoords
    }

    private fun saveHexLocation(row: Int, col: Int, name: String) {
        val hex = grid[row][col]
        val hexNode = getNodeAtHex(row, col) ?: return
        hexNode.location = Location(name)
        val locLabel = hexNode.getNodeAs<Label>("%LocationLabel")!!
        locLabel.text = name
        locLabel.visible = true
        locLabel.setPosition(Vector2(-20.0, -20.0))
        placeHexPopup.visible = false
        selectedCol = -1
        selectedRow = -1
        phNameEdit.text = ""
        phUnlockedAtStart.buttonPressed = false
        hexNode.hexUnlocked =
            true  // for now, we'll just unlock it, but should take into account hexUnlockedAtStart checkbox
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

    private fun getNodeAtHex(row: Int, col: Int): Hex? {
        val node = getNodeAs("Hex_${row}_$col".asStringName()) as Hex?
        if (node == null) {
            logError("Hex not found at $row $col")
        }
        return node
    }
}

data class EditorData(val vector: Vector2, var unlockedAtStart: Boolean = false)