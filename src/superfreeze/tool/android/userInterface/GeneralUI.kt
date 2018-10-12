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

/**
 * This file contains functions responsible for general UI things like requesting permissions.
 */

package superfreeze.tool.android.userInterface

import android.Manifest
import android.app.AlertDialog
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresApi
import superfreeze.tool.android.R

/**
 * Request the usage stats permission. MUST BE CALLED ONLY FROM MainActivity.onCreate!!!
 */
internal fun requestUsageStatsPermission(context: MainActivity, doAfterwards: () -> Unit) {
	if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
		return
	}

	if (!usageStatsPermissionGranted(context)) {

		// Actually we want the dialog to be only shown in onResume, not in onCreate as the app intro is supposed to be shown before this dialog:
		MainActivity.doOnResume {

			AlertDialog.Builder(context, android.R.style.Theme_Material_Light_Dialog)
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
					.setNeutralButton(context.getString(R.string.not_now)) { _, _ ->
						//directly load running applications
						doAfterwards()
					}
					//TODO add negative button "never"
					.setIcon(R.mipmap.ic_launcher)
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
	toast(context, "Please select SuperFreeze, then enable access", Toast.LENGTH_LONG)
}

private fun usageStatsPermissionGranted(context: Context): Boolean {

	//On earlier versions there are no usage stats
	if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
		return false
	}

	val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

	val mode = appOpsManager.checkOpNoThrow(
			AppOpsManager.OPSTR_GET_USAGE_STATS,
			Process.myUid(),
			context.packageName)

	return if (mode == AppOpsManager.MODE_DEFAULT) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			context.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED
		} else {
			false//TODO check if this assumption is right: At Lollipop, mode will be AppOpsManager.MODE_ALLOWED if it was allowed
		}
	} else {
		mode == AppOpsManager.MODE_ALLOWED
	}


}

private fun toast(context: Context, s: String, duration: Int) {
	Toast.makeText(context, s, duration).show()
}