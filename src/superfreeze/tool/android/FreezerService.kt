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
					pressForceStopButton(event.source)
					nextAction = NextAction.PRESS_OK
				}
				NextAction.PRESS_OK -> {
					pressOkButton(event.source)
					nextAction = NextAction.PRESS_BACK
				}
				NextAction.PRESS_BACK -> {
					performGlobalAction(GLOBAL_ACTION_BACK)
					nextAction = NextAction.DO_NOTHING
				}
			}

		}
	}

	override fun onInterrupt() {
	}

	@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	private fun pressForceStopButton(node: AccessibilityNodeInfo) {

		var nodesToClick = node.findAccessibilityNodeInfosByText("FORCE STOP")

		if (nodesToClick.isEmpty())
			nodesToClick = node.findAccessibilityNodeInfosByViewId("com.android.settings:id/right_button")

		if (nodesToClick.isEmpty())
			nodesToClick = node.findAccessibilityNodeInfosByViewId("com.android.settings:id/force_stop_button")

		clickAll(nodesToClick, "force stop")
	}


	@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	private fun pressOkButton(node: AccessibilityNodeInfo) {

		clickAll(node.findAccessibilityNodeInfosByText(getString(android.R.string.ok)), "OK")

	}


	@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
	private fun clickAll(nodes: List<AccessibilityNodeInfo>, buttonName: String) {

		if (nodes.isEmpty()) {
			Log.w(TAG, "Could not find the $buttonName button.")
			nextAction = NextAction.DO_NOTHING
		} else if (nodes.size > 1) {
			Log.w(TAG, "Found more than one $buttonName button, clicking them all.")
		}

		for (node in nodes) {
			node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
		}
	}

	internal companion object {

		private var nextAction = NextAction.DO_NOTHING

		/**
		 * Clicks the "Force Stop", the "OK" and the "Back" button.
		 */
		@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
		internal fun performFreeze() {
			if (nextAction == NextAction.DO_NOTHING)
				nextAction = NextAction.PRESS_FORCE_STOP
		}
	}
}

private const val TAG = "FreezerService"
