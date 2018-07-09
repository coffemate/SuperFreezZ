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

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
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
}

/**
 * Loads all running applications and add them to MainActivity.
 * @param mainActivity The MainActivity
 * @param context The application context.
 */
internal fun loadRunningApplications(mainActivity: MainActivity, context: Context) {

	Thread {
		val packages =
				context.packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
						.filter {
							//Add the package only if it is NOT a system app:
							! it.applicationInfo.flags.isFlagSet(ApplicationInfo.FLAG_SYSTEM)
						}

		mainActivity.runOnUiThread {
			mainActivity.setItems(packages)
		}
	}.start()

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

internal fun isRunning(applicationInfo: ApplicationInfo): Boolean {
	return ! applicationInfo.flags.isFlagSet(ApplicationInfo.FLAG_STOPPED)
}

private class UsedPackage(val packageInfo: PackageInfo, usageStats: UsageStats?): Comparable<UsedPackage> {

	/**
	 * The timestamp at which this app was used last or 0 if it was never used/no infos are available
	 */
	internal val lastTimeUsed = getLastTimeUsed(usageStats)

	override fun compareTo(other: UsedPackage): Int {
		return this.lastTimeUsed.compareTo(other.lastTimeUsed)
	}

}

internal fun isPendingFreeze(packageInfo: PackageInfo, usageStats: UsageStats?): Boolean {
	return isPendingFreeze(packageInfo.packageName, packageInfo.applicationInfo, usageStats)
}

internal fun isPendingFreeze(packageName: String, applicationInfo: ApplicationInfo, usageStats: UsageStats?) : Boolean {
	if (!isRunning(applicationInfo)) {
		return false
	}
	return System.currentTimeMillis() - getLastTimeUsed(usageStats)  >  1000L*60*60*24*1 //TODO replace 1 with 7
}

private fun getLastTimeUsed(usageStats: UsageStats?): Long {
	return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
		usageStats?.lastTimeUsed
				?: Long.MIN_VALUE
	} else {
		Long.MIN_VALUE
	}
}

private fun Int.isFlagSet(value: Int): Boolean {
	return (this and value) == value
}
