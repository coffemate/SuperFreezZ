/*
Copyright (c) 2018 Hocuri

This file is part of SuperFreezZ.

SuperFreezZ is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

SuperFreezZ is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with SuperFreezZ.  If not, see <http://www.gnu.org/licenses/>.
*/

package superfreeze.tool.android.backend

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.Handler
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi

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

		//We are only interested in WINDOW_STATE_CHANGED events
		if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
			return
		}

		when (nextAction) {
			NextAction.DO_NOTHING -> { }

			NextAction.PRESS_FORCE_STOP -> {
				if (event.className == "com.android.settings.applications.InstalledAppDetailsTop") {
					pressForceStopButton(event.source)
				} else {
					Log.w(TAG, "awaited InstalledAppDetailsTop to be the next screen but it was ${event.className}")
					wrongScreenShown()
				}
			}

			NextAction.PRESS_OK -> {
				if (event.className == "android.app.AlertDialog") {
					pressOkButton(event.source)
				} else {
					Log.w(TAG, "awaited AlertDialog to be the next screen but it was ${event.className}")
					wrongScreenShown()
				}
			}

			NextAction.PRESS_BACK -> {
				pressBackButton()
			}
		}
	}

	private fun wrongScreenShown() {
		// If the last action was more than 8 seconds ago, something went wrong and we should abort not to destroy anything.
		if (System.currentTimeMillis() - lastActionTimestamp > 8000) {
			Log.e(
				TAG,
				"An unexpected screen turned up and the last action was more than 8 seconds ago. Something went wrong. Aborted not to destroy anything"
			)
			abort() // Abort everything, it is to late to do anything :-(
		}
		// else do nothing and simply wait for the next screen to show up.
	}

	override fun onInterrupt() {
	}

	@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	private fun pressForceStopButton(node: AccessibilityNodeInfo) {

		var nodesToClick = node.findAccessibilityNodeInfosByText("FORCE STOP")

		if (nodesToClick.isEmpty())
			nodesToClick = node.findAccessibilityNodeInfosByViewId("com.android.settings:id/force_stop_button")

		if (nodesToClick.isEmpty())
			nodesToClick = node.findAccessibilityNodeInfosByViewId("com.android.settings:id/right_button")

		val success = clickAll(nodesToClick, "force stop")

		if (success) nextAction = NextAction.PRESS_OK
	}


	@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	private fun pressOkButton(node: AccessibilityNodeInfo) {
		val success = clickAll(node.findAccessibilityNodeInfosByText(getString(android.R.string.ok)), "OK")
		if (success) nextAction = NextAction.PRESS_BACK
	}


	@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
	private fun pressBackButton() {
		nextAction = NextAction.DO_NOTHING

		timeoutHandler.removeCallbacksAndMessages(null)

		performGlobalAction(GLOBAL_ACTION_BACK)
	}

	/**
	 * Clicks all nodes. If it succeeds, it returns true. If it does not succeed, it handles this by
	 * itself, sets the nextAction to what makes sense and returns true.
	 */
	@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
	private fun clickAll(nodes: List<AccessibilityNodeInfo>, buttonName: String): Boolean {

		if (nodes.isEmpty()) {
			Log.e(TAG, "Could not find the $buttonName button.")
			abort()
			Thread(exceptionHandler).start()
			return false
		} else if (nodes.size > 1) {
			Log.w(TAG, "Found more than one $buttonName button, clicking them all.")
		}

		val clickableNodes = nodes.filter { it.isClickable && it.isEnabled }

		if (clickableNodes.isEmpty()) {
			Log.e(TAG, "The button(s) is/are not clickable, aborting.")
			// Just do not press the button but immediately press Back and act as if the app was successfully frozen:
			// A disabled or not clickable button probably means that the app already is frozen.
			pressBackButton()
			return false
		}

		for (node in clickableNodes) {
			node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
		}

		notifyThereIsStillMovement()

		return true
	}

	override fun onServiceConnected() {
		isEnabled = true
	}

	override fun onDestroy() {
		Log.w(TAG, "FreezerService was destroyed.")
		isEnabled = false
		abort()
	}

	internal companion object {

		private var nextAction = NextAction.DO_NOTHING
		/**
		 * The timestamp of the last action. This can be clicking a button or performFreeze() being called.
		 */
		private var lastActionTimestamp = 0L
		var isEnabled = false
			private set

		private var exceptionHandler = { }

		private val timeoutHandler = Handler()

		/**
		 * Clicks the "Force Stop", the "OK" and the "Back" button when the corresponding screen turns up.
		 * Call this BEFORE starting the settings about the app you want to freeze!
		 */
		@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
		internal fun performFreeze() {
			if (nextAction == NextAction.DO_NOTHING) {
				nextAction = NextAction.PRESS_FORCE_STOP
				notifyThereIsStillMovement()
			} else {
				Log.w(TAG, "Attempted to freeze, but was still busy (nextAction was $nextAction)")
			}
		}

		fun setExceptionHandler(function: () -> Unit) {
			exceptionHandler = function
		}

		private fun notifyThereIsStillMovement() {
			timeoutHandler.removeCallbacksAndMessages(null)

			// After 4 seconds, assume that something went wrong
			timeoutHandler.postDelayed({
				Log.w(TAG, "timeout")
				abort()
				exceptionHandler()
			}, 4000)

			lastActionTimestamp = System.currentTimeMillis()
		}

		/**
		 * Cleans up when no more apps shall be frozen or before exceptionHandler() is called
		 * (in the latter case, exceptionHandler() will care about restarting freeze)
		 */
		private fun abort() {
			Log.w(TAG, "aborting")
			nextAction = NextAction.DO_NOTHING
			timeoutHandler.removeCallbacksAndMessages(null)
		}
	}

}

private const val TAG = "FreezerService"
