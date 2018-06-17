package superfreeze.tool.android

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.support.annotation.RequiresApi
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo



class FreezerService : AccessibilityService() {

	private enum class NextAction {
		PRESS_FORCE_STOP, PRESS_OK, PRESS_BACK, DO_NOTHING
	}

	private var nextAction = NextAction.DO_NOTHING

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

	private fun pressForceStopButton() {
		TODO()
	}


	override fun onInterrupt() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			val r = rootInActiveWindow
		}
	}

	@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	private fun pressOkButton(nodeInfo: AccessibilityNodeInfo) {

		var list = nodeInfo.findAccessibilityNodeInfosByViewId("com.android.settings:id/left_button")
		for (node in list) {
			node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
		}

		list = nodeInfo.findAccessibilityNodeInfosByViewId("android:id/button1")

		for (node in list) {
			node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
		}
	}

	/**
	 * Clicks the "Force Stop", the "OK" and the "Back" button.
	 */
	@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
	public fun performFreeze() {
		//Clicks the "Force Stop" button TODO


		//Clicks the "OK" button TODO


		//Clicks the "Back" button
		performGlobalAction(GLOBAL_ACTION_BACK)
	}
}
