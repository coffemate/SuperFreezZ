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

import android.app.Activity
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
 * Gets the running applications. Do not use from the UI thread.
 */
internal fun getRunningApplications(context: Context): List<PackageInfo> {
	return context.packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
			.filter {
				//Add the package only if it is NOT a system app:
				!it.applicationInfo.flags.isFlagSet(ApplicationInfo.FLAG_SYSTEM)
			}
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

internal fun isPendingFreeze(packageInfo: PackageInfo, usageStats: UsageStats?, activity: Activity): Boolean {
	return isPendingFreeze(
			getFreezeMode(activity, packageInfo.packageName),
			packageInfo.applicationInfo,
			usageStats)
}


internal fun isPendingFreeze(freezeMode: FreezeMode, applicationInfo: ApplicationInfo, usageStats: UsageStats?) : Boolean {

	if (!isRunning(applicationInfo)) {
		return false
	}

	return when(freezeMode) {

		FreezeMode.ALWAYS_FREEZE -> true

		FreezeMode.NEVER_FREEZE -> false

		FreezeMode.FREEZE_WHEN_INACTIVE -> {
			System.currentTimeMillis() - getLastTimeUsed(usageStats)  >  1000L*60*60*24*1 //TODO replace 1 with 7
		}
	}
}

/**
 * Freezes all apps in the "apps" list or all apps that are pending freeze.
 * @param context: The context
 * @param apps: A list of apps to be frozen. If it is null or not given, a list of apps that pend freeze is computed automatically.
 * @param activity The current activity, needed to access the SharedPreferences to read which apps are pending freeze.
 * @return A function that has to be called when the current activity is entered again so that the next app can be frozen.
 * It returns whether it wants to be executed again.
 */
internal fun freezeAll(context: Context, apps: List<String>? = null, activity: Activity): () -> Boolean {
	return freezeAll(context, apps ?: getAppsPendingFreeze(context, activity))
}

/**
 * Freezes all apps in the "apps" list.
 * @param context: The context
 * @param apps: A list of apps to be frozen.
 * @return A function that has to be called when the current activity is entered again so that the next app can be frozen.
 * It returns whether it wants to be executed again.
 */
internal fun freezeAll(context: Context, apps: List<String>): () -> Boolean {
	var nextIndex = 0

	fun freezeNext(): Boolean {
		if (nextIndex < apps.size) {
			freezeApp(apps[nextIndex], context)
			nextIndex++
		}
		//only execute again if nextIndex is a valid index
		return nextIndex < apps.size
	}

	freezeNext()
	FreezerService.doOnFinished(::freezeNext)
	return ::freezeNext
}

internal fun getAppsPendingFreeze(context: Context, activity: Activity): List<String> {

	val usageStatsMap = getAggregatedUsageStats(context)
	return getRunningApplications(context)
			.filter { isPendingFreeze(it, usageStatsMap?.get(it.packageName), activity) }
			.map { it.packageName }
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