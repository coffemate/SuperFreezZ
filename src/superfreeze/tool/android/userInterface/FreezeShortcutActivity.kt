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
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import superfreeze.tool.android.BuildConfig
import superfreeze.tool.android.R
import superfreeze.tool.android.backend.FreezerService
import superfreeze.tool.android.backend.freezeAll
import superfreeze.tool.android.backend.getAppsPendingFreeze
import superfreeze.tool.android.backend.setFreezerExceptionHandler
import superfreeze.tool.android.database.neverCalled

const val FREEZE_ACTION = "${BuildConfig.APPLICATION_ID}.FREEZE"

/**
 * This activity
 *  - creates a shortcut some launcher can use
 *  - performs the freeze when the "Freeze" shortcut (or Floating Action Button) is clicked.
 */
class FreezeShortcutActivity : Activity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val action = intent.action

		if (Intent.ACTION_CREATE_SHORTCUT == action) {
			setupShortcut()
			finish()
		} else {
			performFreeze()
		}
	}

	private fun setupShortcut() {
		val intent: Intent
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			// There is a nice new api for shortcuts from Android O on, which we use here:
			val shortcutManager = getSystemService(ShortcutManager::class.java)
			intent = shortcutManager.createShortcutResultIntent(
				ShortcutInfo.Builder(applicationContext, "FreezeShortcut").build()
			)
		} else {
			// ...but for older versions we need to do everything manually :-(,
			// so actually using the new api does not have any benefits:
			val shortcutIntent = Intent(FREEZE_ACTION)
			shortcutIntent.setClassName(this, this.javaClass.name)
			intent = Intent()
			intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
			intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.freeze_shortcut_label))
			val iconResource = Intent.ShortcutIconResource.fromContext(
				this, R.drawable.ic_freeze
			)
			intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource)
		}
		// Now, return the result to the launcher
		setResult(Activity.RESULT_OK, intent)
	}

	private fun performFreeze(triesLeft: Int = 2) {

		// Tell the user how to manually freeze if necessary, see https://gitlab.com/SuperFreezZ/SuperFreezZ/issues/14
		fun showFreezeManuallyDialog() {
			AlertDialog.Builder(this, R.style.myAlertDialog)
				.setTitle(R.string.freeze_manually)
				.setMessage(R.string.Press_forcestop_ok_back)
				.setPositiveButton(android.R.string.ok) { _, _ ->
					performFreeze(triesLeft)
				}
				.setNegativeButton(R.string.freeze_manually_no) { _, _ ->
					showAccessibilityDialog(this)
					doOnResume {
						showFreezeManuallyDialog()
						false
					}
				}
				.show()
			return
		}
		if (!FreezerService.isEnabled && neverCalled("dialog-how-to-freeze-without-accessibility-service", this)) {
			showFreezeManuallyDialog()
			return
		}


		val appsPendingFreeze = getAppsPendingFreeze(applicationContext, this)
		if (appsPendingFreeze.isEmpty()) {
			Toast.makeText(this, getString(R.string.NothingToFreeze), Toast.LENGTH_SHORT).show()
			finish()
			return
		}

		val resume = freezeAll(applicationContext, activity = this)
		doOnResume {
			val appsLeft = resume()
			if (!appsLeft) finish()
			appsLeft
		}

		setFreezerExceptionHandler {
			toBeDoneOnResume.clear() // Abort the current freeze process
			if (triesLeft > 0) {
				runOnUiThread {
					performFreeze(triesLeft = triesLeft - 1)
				}
			}
		}
	}

	override fun onResume() {
		super.onResume()

		//Execute all tasks and retain only those that returned true.
		toBeDoneOnResume.retainAll { it() }
	}

	private val toBeDoneOnResume: MutableList<() -> Boolean> = mutableListOf()

	private fun doOnResume(task: () -> Boolean) {
		toBeDoneOnResume.add(task)
	}
}