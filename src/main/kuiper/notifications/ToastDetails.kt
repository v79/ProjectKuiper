package notifications

import godot.annotation.RegisterClass

@RegisterClass
class ToastDetails() : godot.Object() {

    constructor(msg: String, showProgress: Boolean, progress: Double = 0.0, steps: Int = 1) : this() {
        message = msg
        this.showProgress = showProgress
        this.progress = progress
        this.expectedSteps = steps
    }

    var message: String = ""
    var showProgress: Boolean = true
    var progress: Double = 0.0
    var expectedSteps: Int = 1
}