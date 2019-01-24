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
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import superfreeze.tool.android.BuildConfig
import superfreeze.tool.android.R
import superfreeze.tool.android.backend.FreezerService
import superfreeze.tool.android.backend.freezeAll
import superfreeze.tool.android.backend.getAppsPendingFreeze
import superfreeze.tool.android.backend.setFreezerExceptionHandler
import superfreeze.tool.android.database.mGetDefaultSharedPreferences
import superfreeze.tool.android.database.neverCalled

const val FREEZE_ACTION = "${BuildConfig.APPLICATION_ID}.FREEZE"

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

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		isBeingNewlyCreated = true

		if (Intent.ACTION_CREATE_SHORTCUT == intent.action) {
			setupShortcut()
			finish()
		} else {
			Log.v(TAG, "Performing Freeze.")
			performFreeze()
		}
	}

	private fun setupShortcut() {
		val intent: Intent = createIntent(this)
		// Now, return the result to the launcher
		setResult(Activity.RESULT_OK, intent)
	}

	private fun performFreeze(triesLeft: Int = 1) {

		val prefs = mGetDefaultSharedPreferences(this)

		// Sometimes the accessibility service is disabled for some reason.
		// In this case, tell the user to re-enable it:
		if (!FreezerService.isEnabled && prefs?.getBoolean("use_accessibility_service", false) == true) {
			showAccessibilityDialog(this)
			doOnReenterActivity {
				with(prefs.edit()) {
					putBoolean("use_accessibility_service", FreezerService.isEnabled)
					apply()
				}
				performFreeze()
				false
			}
			return
		}

		// Tell the user how to manually freeze if necessary, see https://gitlab.com/SuperFreezZ/SuperFreezZ/issues/14:
		fun showFreezeManuallyDialog() {
			if (!FreezerService.isEnabled) {
				AlertDialog.Builder(this, R.style.myAlertDialog)
					.setTitle(R.string.freeze_manually)
					.setMessage(R.string.Press_forcestop_ok_back)
					.setPositiveButton(android.R.string.ok) { _, _ ->
						performFreeze()
					}
					.setNegativeButton(R.string.freeze_manually_no) { _, _ ->
						showAccessibilityDialog(this)
						doOnReenterActivity {
							showFreezeManuallyDialog()
							false
						}
					}
					.show()
				return
			}
		}
		if (!FreezerService.isEnabled && neverCalled("dialog-how-to-freeze-without-accessibility-service", this)) {
			showFreezeManuallyDialog()
			return
		}

		val appsPendingFreeze = getAppsPendingFreeze(applicationContext)
		if (appsPendingFreeze.isEmpty()) {
			Toast.makeText(this, getString(R.string.NothingToFreeze), Toast.LENGTH_SHORT).show()
			finish()
			return
		}

		// Now we can do the actual freezing work:

		var somethingWentWrong = false

		val freezeNextApp = freezeAll(this, activity = this)
		doOnReenterActivity {
			Log.v(TAG, "Ready to freeze the next app.")
			val appsLeft = freezeNextApp()

			if (!appsLeft) {
				if (somethingWentWrong && triesLeft > 0) {
					// Sometimes an app can't be frozen due to a bug I did not find yet.
					// So simply try again by calling performFreeze again
					Log.v(TAG, "Didn't finish freezing, trying again.")
					performFreeze(triesLeft - 1)
				} else {
					Log.v(TAG, "Finished freezing")
					onFreezeFinishedListener?.invoke()
					onFreezeFinishedListener = null
					finish()
				}
			}

			appsLeft // Only execute again if there are still apps left
		}
		// We are still in onCreate. When we finish, onResume will be called and the resume() function will be called
		// so that the first app can be frozen.

		setFreezerExceptionHandler {
			runOnUiThread {
				somethingWentWrong = true
				val i =
					Intent(this@FreezeShortcutActivity.applicationContext, this@FreezeShortcutActivity::class.java)
				i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
				startActivity(i)
			}
		}

		return
	}

	override fun onResume() {
		super.onResume()

		// doOnReenterActivity() is used to register actions that should take place when the app is entered the next time,
		// NOT after onCreate finished
		if (!isBeingNewlyCreated) {
			//Execute all tasks and retain only those that returned true.
			toBeDoneOnReenterActivity.cloneAndRetainAll { it() }
		}

		isBeingNewlyCreated = false
	}

	private val toBeDoneOnReenterActivity: MutableList<() -> Boolean> = mutableListOf()


	/**
	 * Execute the task when this activity was left and re-entered
	 */
	private fun doOnReenterActivity(task: () -> Boolean) {
		toBeDoneOnReenterActivity.add(task)
	}

	companion object {
		@JvmStatic
		fun createIntent(activity: Activity): Intent {
			val intent: Intent
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				// There is a nice new api for shortcuts from Android O on, which we use here:
				val shortcutManager = activity.getSystemService(ShortcutManager::class.java)
				intent = shortcutManager.createShortcutResultIntent(
						ShortcutInfo.Builder(activity.applicationContext, "FreezeShortcut").build()
				)
			} else {
				// ...but for older versions we need to do everything manually :-(,
				// so actually using the new api does not have any benefits:
				val shortcutIntent = Intent(FREEZE_ACTION)
				shortcutIntent.setClassName(activity, FreezeShortcutActivity::class.java.name)

				intent = Intent()
				intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
				intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, activity.getString(R.string.freeze_shortcut_label))
				val iconResource = Intent.ShortcutIconResource.fromContext(
						activity, R.drawable.ic_freeze
				)
				intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource)
			}
			return intent
		}

		@JvmStatic
		internal var onFreezeFinishedListener: (() -> Unit)? = null
	}
}

/**
 * Executes retainAll() on a clone of this collection so that it is no problem if new entries
 * are added to the original list while running this.
 */
private fun <E> MutableCollection<E>.cloneAndRetainAll(predicate: (E) -> Boolean) {
	val cloned = this.toMutableList()
	this.clear()
	cloned.retainAll(predicate)
	this.addAll(cloned)
}

private const val TAG = "FreezeShortcutActivity"