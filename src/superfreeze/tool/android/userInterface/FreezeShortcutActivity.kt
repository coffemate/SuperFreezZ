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

package superfreeze.tool.android.userInterface

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.annotations.Contract
import superfreeze.tool.android.BuildConfig
import superfreeze.tool.android.R
import superfreeze.tool.android.Waiter
import superfreeze.tool.android.backend.FreezerService
import superfreeze.tool.android.backend.getAppsPendingFreeze
import superfreeze.tool.android.cloneAndRetainAll
import superfreeze.tool.android.database.neverCalled
import superfreeze.tool.android.database.prefUseAccessibilityService

/**
 * This activity
 *  - creates a shortcut some launcher can use
 *  - performs the freeze when the "Freeze" shortcut (or Floating Action Button) is clicked.
 *  It is invisible to the user but in the background while freezing.
 */
class FreezeShortcutActivity : Activity() {

	/**
	 * Whether the activity is in the process of being created.
	 */
	private var isBeingNewlyCreated = true

	private val waiterForNextFreeze = Waiter()
	private var somethingWentWrong = false
	private var failedFreezeAttempts = 0

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		isBeingNewlyCreated = true
		activity = this
		failedFreezeAttempts = 0

		if (Intent.ACTION_CREATE_SHORTCUT == intent.action) {
			setResult(RESULT_OK, createShortcutResultIntent(this))
			finish()
		} else {
			Log.i(TAG, "Performing Freeze.")
			performFreeze()
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		onFreezeFinishedListener?.invoke()
		onFreezeFinishedListener = null
		activity = null
	}


	private fun performFreeze() {

		// Sometimes the accessibility service is disabled for some reason.
		// In this case, tell the user to re-enable it:
		if (!FreezerService.isEnabled && prefUseAccessibilityService) {
			promptForAccessibility()
			return
		}


		if (!FreezerService.isEnabled && neverCalled("dialog-how-to-freeze-without-accessibility-service", this)) {
			AlertDialog.Builder(this, R.style.myAlertDialog)
				.setTitle(R.string.freeze_manually)
				.setMessage(R.string.Press_forcestop_ok_back)
				.setCancelable(false)
				.setPositiveButton(android.R.string.ok) { _, _ ->
					performFreeze()
				}
				.setNegativeButton(R.string.freeze_manually_no) { _, _ ->
					promptForAccessibility()
				}
				.show()
			return
		}

		val appsPendingFreeze = getAppsPendingFreeze(applicationContext)
		if (appsPendingFreeze.isEmpty()) {
			Toast.makeText(this, getString(R.string.NothingToFreeze), Toast.LENGTH_SHORT).show()
			finish()
			return
		}


		somethingWentWrong = false
		isWorking = true

		// Now we can do the actual freezing work:
		GlobalScope.launch {
			freezeAll(appsPendingFreeze)

			runOnUiThread {
				isWorking = false

				if (somethingWentWrong) {
					// Try again:
					performFreeze()
				} else {
					Log.v(TAG, "Finished freezing")
					finish()
				}
			}
		}
	}

	private fun promptForAccessibility() {
		showAccessibilityDialog(this)
		doOnReenterActivity {
			prefUseAccessibilityService = FreezerService.isEnabled
			performFreeze()
			false
		}
	}

	/**
	 * Called after one app could not be frozen
	 */
	internal fun onAppCouldNotBeFrozen() {
		runOnUiThread {
			if (failedFreezeAttempts >= 2) {
				finish()
				Log.e(TAG, "Giving up, too many failed freeze attempts")
			} else {
				somethingWentWrong = true
				failedFreezeAttempts++
				val i = Intent(applicationContext, FreezeShortcutActivity::class.java)
				i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
				startActivity(i)
			}
		}
	}

	override fun onResume() {
		super.onResume()

		// doOnReenterActivity() is used to register actions that should take place when the app is entered the next time,
		// NOT after onCreate finished
		if (!isBeingNewlyCreated) {
			// Maybe the next app is waiting to be frozen
			waiterForNextFreeze.notify()

			//Execute all tasks and retain only those that returned true.
			toBeDoneOnReenterActivity.cloneAndRetainAll { it() }
		}

		isBeingNewlyCreated = false
	}

	/**
	 * Freezes all apps in the "apps" list or all apps that are pending freeze.
	 * @param apps: A list of apps to be frozen. If it is null or not given, a list of apps that pend freeze is computed automatically.
	 * @return A function that has to be called when the current activity is entered again so that the next app can be frozen.
	 * It returns whether it wants to be executed again.
	 */
	private suspend fun freezeAll(apps: List<String>? = null) {
		val appsNonNull = apps ?: getAppsPendingFreeze(this)

		// Always freeze SuperFreezZ itself last:
		val appsSuperfreezzLast =
			if (appsNonNull.contains(BuildConfig.APPLICATION_ID))
				appsNonNull.sortedBy { it == BuildConfig.APPLICATION_ID }
			else
				appsNonNull

		for (app in appsSuperfreezzLast) {
			freezeApp(app, this)
			waiterForNextFreeze.wait()
		}
	}

	@Suppress("unused")
	fun getRandomNumber(): Int {
		return 5    // chosen by fair dice roll,
		// guaranteed to be random.

		// Greetings to anyone reviewing this code!
	}

	private val toBeDoneOnReenterActivity: MutableList<() -> Boolean> = mutableListOf()
	/**
	 * Execute the task when this activity was left and re-entered.
	 * Only call from the UI thread!
	 */
	private fun doOnReenterActivity(task: () -> Boolean) {
		toBeDoneOnReenterActivity.add(task)
	}

	companion object {

		/**
		 * Returns an intent containing information for a launcher how to create a shortcut.
		 * See e.g https://developer.android.com/reference/android/content/pm/ShortcutManager.html#createShortcutResultIntent(android.content.pm.ShortcutInfo)
		 */
		fun createShortcutResultIntent(activity: Activity): Intent {
			/*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				// There is a nice new api for shortcuts from Android O on, which we use here:
				val shortcutManager = activity.getSystemService(ShortcutManager::class.java)
				return shortcutManager.createShortcutResultIntent(
						ShortcutInfo.Builder(activity.applicationContext, "FreezeShortcut").build()
				)
			}*/

			// ...but for older versions we need to do everything manually :-(,
			// so actually using the new api does not have any benefits:
			val shortcutIntent = createShortcutIntent(activity)

			val intent = Intent()
			intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
			intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, activity.getString(R.string.freeze_shortcut_label))
			val iconResource = Intent.ShortcutIconResource.fromContext(
					activity, R.drawable.ic_freeze
			)
			intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource)
			return intent
		}

		internal fun createShortcutIntent(context: Context): Intent {
			val shortcutIntent = Intent()
			shortcutIntent.setClassName(context, FreezeShortcutActivity::class.java.name)
			shortcutIntent.addFlags(
				Intent.FLAG_ACTIVITY_CLEAR_TASK +
						Intent.FLAG_ACTIVITY_NEW_TASK +
						Intent.FLAG_ACTIVITY_NO_ANIMATION
			)
			return shortcutIntent
		}

		/**
		 * Freeze a package.
		 * @param packageName The name of the package to freeze
		 */
		@Contract(pure = true)
		internal fun freezeApp(packageName: String, context: Context) {

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && FreezerService.isEnabled) {
				// performFreeze will wait for the Force stop button to appear and then click Force stop, Ok, Back.
				FreezerService.performFreeze()
			}

			val intent = Intent()
			intent.action = "android.settings.APPLICATION_DETAILS_SETTINGS"
			intent.data = Uri.fromParts("package", packageName, null)
			context.startActivity(intent)
		}


		internal var activity: FreezeShortcutActivity? = null
			private set

		var isWorking = false

		internal var onFreezeFinishedListener: (() -> Unit)? = null
	}
}


private const val TAG = "SF-FreezeShortcutAct"