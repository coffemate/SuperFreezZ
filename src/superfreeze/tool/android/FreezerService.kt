package superfreeze.tool.android

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo


/**
 * This is the Accessibility service class, responsible to automatically freeze apps.
 */
class FreezerService : AccessibilityService() {

	private enum class NextAction {
		PRESS_FORCE_STOP, PRESS_OK, PRESS_BACK, DO_NOTHING
	}

	override fun onAccessibilityEvent(event: AccessibilityEvent) {

		//Does not work on older versions of Android.
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
			return
		}

		if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {

			when(nextAction) {
				NextAction.DO_NOTHING -> {}

				NextAction.PRESS_FORCE_STOP -> {
					val success = pressForceStopButton(event.source)
					nextAction = if (success) NextAction.PRESS_OK else NextAction.DO_NOTHING
				}
				NextAction.PRESS_OK -> {
					val success = pressOkButton(event.source)
					nextAction = if (success) NextAction.PRESS_BACK else NextAction.DO_NOTHING
				}
				NextAction.PRESS_BACK -> {
					pressBackButton()
					nextAction = NextAction.DO_NOTHING
				}
			}

		}
	}

	override fun onInterrupt() {
	}

	@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	private fun pressForceStopButton(node: AccessibilityNodeInfo): Boolean {

		var nodesToClick = node.findAccessibilityNodeInfosByText("FORCE STOP")

		if (nodesToClick.isEmpty())
			nodesToClick = node.findAccessibilityNodeInfosByViewId("com.android.settings:id/right_button")

		if (nodesToClick.isEmpty())
			nodesToClick = node.findAccessibilityNodeInfosByViewId("com.android.settings:id/force_stop_button")

		return clickAll(nodesToClick, "force stop")
	}


	@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	private fun pressOkButton(node: AccessibilityNodeInfo): Boolean {

		return clickAll(node.findAccessibilityNodeInfosByText(getString(android.R.string.ok)), "OK")

	}

	@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
	private fun pressBackButton() {
		performGlobalAction(GLOBAL_ACTION_BACK)
	}

	@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
	private fun clickAll(nodes: List<AccessibilityNodeInfo>, buttonName: String): Boolean {

		if (nodes.isEmpty()) {
			Log.w(TAG, "Could not find the $buttonName button.")
			return false
		} else if (nodes.size > 1) {
			Log.w(TAG, "Found more than one $buttonName button, clicking them all.")
		}

		val clickableNodes = nodes.filter { it.isClickable && it.isEnabled }

		if (clickableNodes.isEmpty()) {
			Log.i(TAG,"The button is not clickable, aborting.")
			pressBackButton()
			return false
		}

		for (node in clickableNodes) {
			node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
		}

		return true
	}

	public override fun onServiceConnected() {
		isEnabled = true
	}

	override fun onDestroy() {
		isEnabled = false
	}

	internal companion object {

		private var nextAction = NextAction.DO_NOTHING
		private var isEnabled = false

		/**
		 * Clicks the "Force Stop", the "OK" and the "Back" button.
		 */
		@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
		internal fun performFreeze() {
			if (nextAction == NextAction.DO_NOTHING)
				nextAction = NextAction.PRESS_FORCE_STOP
		}

		/**
		 * Returns whether this service is busy (i. e. freezing an app)
		 */
		fun busy(): Boolean {
			return (nextAction != NextAction.DO_NOTHING
					&& isEnabled)
		}
	}
}

private const val TAG = "FreezerService"
