package hexgrid.map.editor

import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.CheckBox
import godot.api.Label
import godot.api.LineEdit
import godot.api.PanelContainer
import godot.extension.getNodeAs
import godot.global.GD

@RegisterClass
class LocationDetailsPanel : PanelContainer() {

    var signalBus: MapEditorSignalBus? = null

    // UI elements
    val locationNameEdit: LineEdit by lazy { getNodeAs("%LocationNameEdit")!! }
    val unlockedCheckbox: CheckBox by lazy { getNodeAs("%UnlockedCheckbox")!! }
    private val colRowLabel: Label by lazy { getNodeAs("%ColRowLabel")!! }

    // Data
    var col: Int = -1
    var row: Int = -1

    @RegisterFunction
    override fun _ready() {
        colRowLabel.text = "(c$col, r$row)"
    }

    @RegisterFunction
    fun locationNameChanged(newName: String) {
        if (newName.isNotEmpty()) {
            signalBus?.editor_updateLocation?.emit(
                col, row,
                newName,
                unlockedCheckbox.buttonPressed
            )
        }
    }

    @RegisterFunction
    fun unlockedCheckboxToggled(checked: Boolean) {
        GD.print("Checkbox for ${locationNameEdit.text} toggled: $checked")
        signalBus?.editor_updateLocation?.emit(
            col,
            row,
            locationNameEdit.text,
            checked
        )
    }

    @RegisterFunction
    fun deleteLocationPressed() {
        signalBus?.editor_deleteLocation?.emit(
            col, row
        )
    }

}
