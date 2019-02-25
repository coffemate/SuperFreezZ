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
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import superfreeze.tool.android.database.mGetDefaultSharedPreferences
import superfreeze.tool.android.userInterface.FreezeShortcutActivity

/**
 * Receives and handles a broadcast when the screen turns off.
 * @param screenLockerFunction
 * A function that locks the screen on newer Android versions. (performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)). This is needed to lock the screen
 * after freezing the apps.
 */
class ScreenReceiver(private val context: Context, private val screenLockerFunction: () -> Unit) :
	BroadcastReceiver() {
	private var lastTime = 0L
	private var originalBrightness = -1
	private var originalTimeout = -1

	override fun onReceive(context: Context, intent: Intent) {
		if (intent.action == Intent.ACTION_SCREEN_OFF
			&& mGetDefaultSharedPreferences(context)?.getBoolean(
				"freeze_on_screen_off",
				false
			) == true
		) {
			FreezerService.abort() // If a freeze was already running, abort it

			if (getAppsPendingFreeze(context).isEmpty()) {
				// Reset screen timeout and brightness to the original values:
				if (originalBrightness > 0 && originalTimeout > 0) {
					try {
						android.provider.Settings.System.putInt(
							context.contentResolver,
							android.provider.Settings.System.SCREEN_BRIGHTNESS,
							originalBrightness
						)
						android.provider.Settings.System.putInt(
							context.contentResolver,
							Settings.System.SCREEN_OFF_TIMEOUT,
							originalTimeout
						)
					} catch (e: SecurityException) {
						Log.w(TAG, "Could not write change screen brightness an timeout")
					}
					originalBrightness = -1
					originalTimeout = -1
				}
				return
			}

			// Throttle to once a minute:
			if (lastTime + 60 * 1000 > System.currentTimeMillis()) {
				lastTime = Math.min(System.currentTimeMillis(), lastTime)
				return
			} else {
				lastTime = System.currentTimeMillis()
			}

			enableScreenUntilFrozen(this.context)

			context.startActivity(Intent(context, FreezeShortcutActivity::class.java).apply {
				addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			})
		}
	}

	private fun enableScreenUntilFrozen(context: Context) {
		Log.i(TAG, "turning screen on for freeze...")

		val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager?
		val wl = pm!!.newWakeLock(
			PowerManager.FULL_WAKE_LOCK
					or PowerManager.ACQUIRE_CAUSES_WAKEUP, "keepawake_until_frozen:"
		)
		wl.acquire(3 * 60 * 1000L /*3 minutes*/)

		val km = context.getSystemService(KEYGUARD_SERVICE) as KeyguardManager?
		val kl = km!!.newKeyguardLock("SuperFreezZ")
		kl.disableKeyguard()

		try {
			originalBrightness = android.provider.Settings.System.getInt(
				context.contentResolver,
				android.provider.Settings.System.SCREEN_BRIGHTNESS, 120
			)

			android.provider.Settings.System.putInt(
				context.contentResolver,
				android.provider.Settings.System.SCREEN_BRIGHTNESS, 0
			)
		} catch (e: SecurityException) {
			Log.w(TAG, "Could not write change screen brightness and timeout")
		}

		FreezeShortcutActivity.onFreezeFinishedListener = {
			Log.i(TAG, "turning screen off after freeze...")
			wl.release()
			kl.reenableKeyguard()

			// Turn screen off:
			try {
				originalTimeout = android.provider.Settings.System.getInt(
					context.contentResolver,
					Settings.System.SCREEN_OFF_TIMEOUT,
					1 * 60 * 1000
				)
				android.provider.Settings.System.putInt(
					context.contentResolver,
					Settings.System.SCREEN_OFF_TIMEOUT,
					0
				)
			} catch (e: SecurityException) {
				Log.w(TAG, "Could not write change screen brightness and timeout")
			}

		}
	}

}

internal fun registerScreenReceiver(
	context: Context,
	screenLockerFunction: () -> Unit
): ScreenReceiver {
	val filter = IntentFilter().apply {
		addAction(Intent.ACTION_SCREEN_OFF)
	}
	val screenReceiver = ScreenReceiver(context, screenLockerFunction)
	context.registerReceiver(screenReceiver, filter)
	return screenReceiver
}

private const val TAG = "FreezeOnScreenOff"