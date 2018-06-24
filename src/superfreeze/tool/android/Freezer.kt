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

package superfreeze.tool.android

import android.app.ProgressDialog
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import org.jetbrains.annotations.Contract


/**
 * This file is responsible for freezing apps and connected things, like loading the list of apps
 * that were not freezed yet.
 */


/**
 * Freeze a package.
 * @param packageName The name of the package to freeze
 * @param context The context of the calling application
 */
@Contract(pure = true)
internal fun freezeApp(packageName: String, context: Context) {
	val intent = Intent()
	intent.action = "android.settings.APPLICATION_DETAILS_SETTINGS"
	intent.data = Uri.fromParts("package", packageName, null)
	context.startActivity(intent)

	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
		FreezerService.performFreeze()
	}

	//remember that the app was freezed
	val preferences = context.getSharedPreferences("lastAppFreeze", Context.MODE_PRIVATE)
	val editor = preferences.edit()
	editor.putLong(packageName, System.currentTimeMillis())
	editor.apply()//preferences.getLong();
}

/**
 * Loads all running applications and add them to MainActivity.
 * Currently, as a workaround, this is those apps that were interacted with since the last freeze.
 * @param mainActivity The MainActivity
 * @param context The application context.
 */
internal fun loadRunningApplications(mainActivity: MainActivity, context: Context) {

	val loader = object : AsyncTask<Void, UsedPackage, Void>() {
		private var dialog: ProgressDialog = ProgressDialog.show(mainActivity, context.getString(R.string.dlg_loading_title), context.getString(R.string.dlg_loading_body))
		private val usageStatsMap: Map<String, UsageStats>? = getAggregatedUsageStats(context)

		override fun doInBackground(vararg params: Void): Void? {
			val packages =
					context.packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
							.filter {
								//Add the package only if it is NOT a system app:
								! it.applicationInfo.flags.isFlagSet(ApplicationInfo.FLAG_SYSTEM)
										//...and if it is running
										&& isRunning(it)
							}
							.map{ UsedPackage(it, usageStatsMap?.get(it.packageName)) }
							.sorted()

			for (usedPackage in packages) {
				publishProgress(usedPackage)
			}
			return null
		}

		override fun onProgressUpdate(vararg values: UsedPackage) {
			mainActivity.addItem(values[0].packageInfo)
		}

		override fun onPostExecute(aVoid: Void?) {
			super.onPostExecute(aVoid)
			dialog.dismiss()
		}

	}

	loader.execute()
}


/**
 * Queries usage stats for the last two years by calling usageStatsManager.queryAndAggregateUsageStats().

 * @return A map with the package names of running apps or null if it could not be determined (on older versions of Android)
 * @see android.app.usage.UsageStatsManager.queryAndAggregateUsageStats
 */
internal fun getAggregatedUsageStats(context: Context): Map<String, UsageStats>? {
	if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
		//In Android versions older than LOLLIPOP there is no UsageStatsManager
		return null
	}
	val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

	//Get all data starting two years ago
	val now = System.currentTimeMillis()
	val startDate = now - 1000L*60*60*24*365*2

	return usageStatsManager.queryAndAggregateUsageStats(startDate, now)
}

private fun isRunning(packageInfo: PackageInfo): Boolean {
	return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
		! packageInfo.applicationInfo.flags.isFlagSet(ApplicationInfo.FLAG_STOPPED)
	} else {
		//TODO Currently, on older versions, we just show all installed apps
		return true
	}
}

private class UsedPackage(val packageInfo: PackageInfo, usageStats: UsageStats?): Comparable<UsedPackage> {

	/**
	 * The timestamp at which this app was used last or 0 if it was never used/no infos are available
	 */
	internal val lastTimeUsed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
		usageStats?.lastTimeUsed
				?: Long.MIN_VALUE
	} else {
		Long.MIN_VALUE
	}

	override fun compareTo(other: UsedPackage): Int {
		return this.lastTimeUsed.compareTo(other.lastTimeUsed)
	}

}

private fun Int.isFlagSet(value: Int): Boolean {
	return (this and value) == value
}