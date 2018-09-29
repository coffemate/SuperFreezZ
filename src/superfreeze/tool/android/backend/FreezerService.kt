/*
Copyright (c) 2018 Hocceruser

This file is part of SuperFreeze.

SuperFreeze is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

SuperFreeze is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with SuperFreeze.  If not, see <http://www.gnu.org/licenses/>.
*/

package superfreeze.tool.android.backend

import android.accessibilityservice.AccessibilityService
import android.os.Build
import androidx.annotation.RequiresApi
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

		//We are only interested in WINDOW_STATE_CHANGED events
		if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
			return
		}

		when(nextAction) {
			NextAction.DO_NOTHING -> {}

			NextAction.PRESS_FORCE_STOP -> {
				if (event.className == "com.android.settings.applications.InstalledAppDetailsTop") {
					val success = pressForceStopButton(event.source)
					nextAction = if (success) NextAction.PRESS_OK else fail()
				}
			}
			NextAction.PRESS_OK -> {
				if (event.className == "android.app.AlertDialog") {
					val success = pressOkButton(event.source)
					nextAction = if (success) NextAction.PRESS_BACK else fail()
				}
			}
			NextAction.PRESS_BACK -> {
				if (event.className == "com.android.settings.applications.InstalledAppDetailsTop") {
					pressBackButton()
					nextAction = NextAction.DO_NOTHING

					//Execute all tasks and retain only those that returned true.
					toBeDoneOnFinished.retainAll { it() }
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
		toBeDoneOnFinished.clear()
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
			else
				Log.w(TAG, "Attempted to freeze, but was still busy")
		}

		/**
		 * Returns whether this service is busy (i. e. freezing an app)
		 */
		fun busy(): Boolean {
			return (nextAction != NextAction.DO_NOTHING
					&& isEnabled)
		}

		private val toBeDoneOnFinished: MutableList<() -> Boolean> = mutableListOf()
		/**
		 * Execute this task when finished freezing the current app.
		 * @param task If it returns true, then it will be executed again at the next onResume.
		 */
		internal fun doOnFinished(task: ()->Boolean) {
			if(isEnabled)
				toBeDoneOnFinished.add(task)
		}
	}

	private fun fail(): NextAction {
		toBeDoneOnFinished.clear()
		return NextAction.DO_NOTHING
	}
}

private const val TAG = "FreezerService"
