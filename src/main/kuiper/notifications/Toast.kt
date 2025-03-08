package notifications

import SignalBus
import godot.Panel
import godot.ProgressBar
import godot.RichTextLabel
import godot.Timer
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.annotation.RegisterSignal
import godot.core.connect
import godot.core.signal0
import godot.extensions.getNodeAs

@RegisterClass
class Toast : Panel() {

	// Globals
	private lateinit var signalBus: SignalBus

	@RegisterSignal
	val toastExpired by signal0()

	// UI elements
	private lateinit var label: RichTextLabel
	private lateinit var progressBar: ProgressBar
	lateinit var timer: Timer

	// Data
	private val delay: Double = 1.0

	@RegisterFunction
	override fun _ready() {
		visible = false
		signalBus = getNodeAs("/root/SignalBus")!!

		label = getNodeAs("%RichTextLabel")!!
		progressBar = getNodeAs("%ProgressBar")!!
		timer = getNodeAs("%Timer")!!

		signalBus.toast.connect { toast ->
			visible = false
			label.text = ""
			showToast(toast)
		}

		timer.timeout.connect {
			visible = false
		}

		signalBus.updateToast.connect { toast ->
			updateToast(toast)

		}
	}

	@RegisterFunction
	override fun _process(delta: Double) {

	}

	@RegisterFunction
	fun showToast(details: ToastDetails) {
		visible = true
		label.text = details.message
		progressBar.visible = details.showProgress
		progressBar.value = details.progress
		timer.start(delay)
	}

	@RegisterFunction
	fun updateToast(details: ToastDetails) {
		getTree()?.createTimer(0.5)?.timeout?.connect {
			label.text = details.message
			progressBar.visible = details.showProgress
			progressBar.value = details.progress
		}
	}
}
