import godot.global.GD

interface LogInterface {

    var logEnabled: Boolean

    fun log(message: String) {
        if (logEnabled) {
            GD.printRich("[color=orange]$message[/color]")
        }
    }

    fun logError(message: String) {
        if (logEnabled) {
            GD.printErr(message)
        }
    }

    fun logWarning(message: String) {
        if (logEnabled) {
            GD.printRich("[color=yellow]$message[/color]")
        }
    }
}