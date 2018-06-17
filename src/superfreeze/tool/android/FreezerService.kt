package superfreeze.tool.android

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.support.annotation.RequiresApi
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
					pressForceStopButton()
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
	private fun pressForceStopButton() {

		val rootNode = rootInActiveWindow

		var nodesToClick = rootNode.findAccessibilityNodeInfosByText("FORCE STOP")

		if (nodesToClick.isEmpty())
			nodesToClick = rootNode.findAccessibilityNodeInfosByViewId("com.android.settings:id/right_button")

		if (nodesToClick.isEmpty())
			nodesToClick = rootNode.findAccessibilityNodeInfosByViewId("com.android.settings:id/force_stop_button")

		clickAll(nodesToClick)
	}


	@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	private fun pressOkButton(nodeInfo: AccessibilityNodeInfo) {

		clickAll(nodeInfo.findAccessibilityNodeInfosByText(getString(android.R.string.ok)))

	}


	@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
	private fun clickAll(nodes: List<AccessibilityNodeInfo>) {
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
