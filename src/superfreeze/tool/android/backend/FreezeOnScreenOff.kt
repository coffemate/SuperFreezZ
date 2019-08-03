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

/**
 * This file is responsible to initiate "freeze on screen off", if this feature is enabled.
 */

package superfreeze.tool.android.backend

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.KEYGUARD_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import superfreeze.tool.android.database.getPrefs
import superfreeze.tool.android.database.prefUseAccessibilityService
import superfreeze.tool.android.userInterface.FreezeShortcutActivity
import kotlin.math.min

/**
 * Registers a BroadcastReceiver for freezing apps "on screen off".
 * @param screenLockerFunction
 * A function that locks the screen on newer Android versions
 * (performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)). This is needed to lock the screen
 * after freezing the apps.
 */
internal fun freezeOnScreenOff_init(
	context: Context,
	screenLockerFunction: () -> Unit
): BroadcastReceiver {
	val filter = IntentFilter().apply {
		addAction(Intent.ACTION_SCREEN_OFF)
	}
	context.registerReceiver(screenReceiver, filter)
	return screenReceiver
}

/**
 * Receives and handles a broadcast when the screen turns off.
 */
private val screenReceiver by lazy {
	object : BroadcastReceiver() {
		// TODO Do also use the screenLockerFunction in newer versions of Android
		private var lastTime = 0L
		private var originalBrightness = -1
		private var originalTimeout = -1

		override fun onReceive(context: Context, intent: Intent) {
			if (intent.action == Intent.ACTION_SCREEN_OFF) {

				Log.i(TAG, "Screen off.")
				resetScreenIfNecessary(context)

				if (FreezeShortcutActivity.isWorking) {
					FreezeShortcutActivity.activity?.finish()
					FreezerService.stopAnyCurrentFreezing()
					return
				}

				FreezerService.stopAnyCurrentFreezing() // If a freeze was already running, stop it

				if (getPrefs(context).getBoolean("freeze_on_screen_off", false)
					&& context.prefUseAccessibilityService) {

					if (getAppsPendingFreeze(context).isEmpty()) {
						return
					}

					// Throttle to once a minute:
					if (lastTime + 60 * 1000 > System.currentTimeMillis()) {
						lastTime = min(System.currentTimeMillis(), lastTime)
						return
					}

					lastTime = System.currentTimeMillis()

					enableScreenUntilFrozen(context)

					context.startActivity(FreezeShortcutActivity.createShortcutIntent(context))
				}
			}
		}

		// TODO Actually use screenLockerFunction on newer versions of Android
		private fun enableScreenUntilFrozen(context: Context) {
			Log.i(TAG, "turning screen on for freeze...")

			val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager?
			val wl = pm?.newWakeLock(
				PowerManager.FULL_WAKE_LOCK
						or PowerManager.ACQUIRE_CAUSES_WAKEUP, "keepawake_until_frozen:"
			)
			wl?.acquire(3 * 60 * 1000L /*3 minutes*/) ?: Log.w(TAG, "wl was null")

			val km = context.getSystemService(KEYGUARD_SERVICE) as KeyguardManager?
			val kl = km!!.newKeyguardLock("SuperFreezZ")
			kl.disableKeyguard()

			try {
				originalBrightness = Settings.System.getInt(
					context.contentResolver,
					Settings.System.SCREEN_BRIGHTNESS, 120
				)

				Settings.System.putInt(
					context.contentResolver,
					Settings.System.SCREEN_BRIGHTNESS, 0
				)
			} catch (e: SecurityException) {
				Log.w(TAG, "Could not change screen brightness")
			}

			FreezeShortcutActivity.onFreezeFinishedListener = {
				turnScreenOffAfterFreeze(wl, kl, context)
			}
		}

		private fun turnScreenOffAfterFreeze(
			wl: PowerManager.WakeLock?,
			kl: KeyguardManager.KeyguardLock,
			context: Context
		) {
			Log.i(TAG, "turning screen off after freeze...")

			try {
				wl?.release()
			} catch (e: RuntimeException) { // See https://stackoverflow.com/a/24057982
				Log.w(TAG, "release failed: ${e.message}")
			}

			kl.reenableKeyguard()

			// Turn screen off:
			try {
				originalTimeout = Settings.System.getInt(
					context.contentResolver,
					Settings.System.SCREEN_OFF_TIMEOUT,
					1 * 60 * 1000
				)
				Settings.System.putInt(
					context.contentResolver,
					Settings.System.SCREEN_OFF_TIMEOUT,
					0
				)
			} catch (e: SecurityException) {
				Log.w(TAG, "Could not change screen timeout")
			}

			// We do not have to take care about calling resetScreenIfNecessary here;
			// onReceive will call it when the screen goes off
		}

		/**
		 * Resets the settings that were changed
		 */
		private fun resetScreenIfNecessary(context: Context) {
			try {

				if (originalBrightness >= 0) {
					Log.i(TAG, "Reset brightness")
					Settings.System.putInt(
						context.contentResolver,
						Settings.System.SCREEN_BRIGHTNESS,
						originalBrightness
					)
					originalBrightness = -1
				}

				if (originalTimeout >= 0) {
					Log.i(TAG, "Reset timeout")
					Settings.System.putInt(
						context.contentResolver,
						Settings.System.SCREEN_OFF_TIMEOUT,
						originalTimeout
					)
					originalTimeout = -1
				}

			} catch (e: SecurityException) {
				Log.e(TAG, "Could not change screen brightness and timeout")
			}
		}
	}
}

private const val TAG = "SF-FreezeOnScreenOff"