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
 * This file contains functions responsible for general UI things like requesting permissions.
 */

package superfreeze.tool.android.userInterface

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import superfreeze.tool.android.R
import superfreeze.tool.android.backend.usageStatsPermissionGranted
import superfreeze.tool.android.database.neverCalled
import superfreeze.tool.android.userInterface.mainActivity.MainActivity

/**
 * Request the usage stats permission. MUST BE CALLED ONLY FROM MainActivity.onCreate!!!
 */
internal fun requestUsageStatsPermission(context: MainActivity, doAfterwards: () -> Unit) {

	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
			&& !usageStatsPermissionGranted(context)
			&& neverCalled("requestUsageStatsPermission", context)) {

		// Actually we want the dialog to be only shown in onResume, not in onCreate as the app intro is supposed to be shown before this dialog:
		MainActivity.doOnResume {

			AlertDialog.Builder(context, R.style.myAlertDialog)
				.setTitle(context.getString(R.string.usagestats_access))
				.setMessage(context.getString(R.string.usatestats_explanation))
				.setPositiveButton(context.getString(R.string.enable)) { _, _ ->
					showUsageStatsSettings(context)
					MainActivity.doOnResume {

						if (!usageStatsPermissionGranted(context)) {
							toast(context, context.getString(R.string.usagestats_not_enabled), Toast.LENGTH_SHORT)
						}
						doAfterwards()

						//Do not execute again
						false
					}
				}
				.setNegativeButton(context.getString(android.R.string.no)) { _, _ ->
					//directly load running applications
					doAfterwards()
				}
				//TODO add negative button "never"
				.setIcon(R.drawable.symbol_freeze_when_inactive)
				.setCancelable(false)
				.show()

			false // do not execute again
		}
	} else {
		//directly load running applications
		doAfterwards()
	}

}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
private fun showUsageStatsSettings(context: Context) {
	val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
	context.startActivity(intent)
	toast(context, "Please select SuperFreezZ, then enable access", Toast.LENGTH_LONG)
}


internal fun showAccessibilityDialog(context: Context) {
	AlertDialog.Builder(context, R.style.myAlertDialog)
		.setTitle("Accessibility Service")
		.setMessage("SuperFreezZ needs the accessibility service in order to automate freezing.\n\nPlease select SuperFreezZ, then enable accessibility service.")
		.setPositiveButton(context.getString(R.string.enable)) { _, _ ->
			val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
			context.startActivity(intent)
		}
		.setIcon(R.mipmap.ic_launcher)
		.setCancelable(false)
		.show()
}



internal fun showSortChooserDialog(context: Context, onChosen: (which: Int) -> Unit) {
	AlertDialog.Builder(context)
			.setTitle("Sort by")
			.setItems(R.array.sortmodes) { _, which ->
				onChosen(which)
			}
			.show()
}

private fun toast(context: Context, s: String, duration: Int) {
	Toast.makeText(context, s, duration).show()
}

private const val TAG = "GeneralUI"