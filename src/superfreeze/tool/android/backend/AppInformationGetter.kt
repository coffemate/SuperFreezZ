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
 * This file contains functions that get necessary information about apps.
 */

package superfreeze.tool.android.backend

import android.Manifest
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import superfreeze.tool.android.BuildConfig
import superfreeze.tool.android.R
import superfreeze.tool.android.database.FreezeMode
import superfreeze.tool.android.database.getFreezeMode
import superfreeze.tool.android.database.mGetDefaultSharedPreferences
import superfreeze.tool.android.database.usageStatsAvailable
import superfreeze.tool.android.expectNonNull
import superfreeze.tool.android.logErrorAndStackTrace


private const val TAG = "AppInformationGetter"

/**
 * Gets the applications to show in SF. Do not use from the UI thread.
 */
internal fun getApplications(context: Context): List<PackageInfo> {
	val preferences = mGetDefaultSharedPreferences(context)
	val shownSpecialApps = preferences.getStringSet("appslist_show_special", setOf())
			?: setOf()
	val packageManager = context.packageManager

	val intent = Intent("android.intent.action.MAIN")
	intent.addCategory("android.intent.category.HOME")
	val launcherApplication = packageManager.resolveActivity(intent,
			PackageManager.MATCH_DEFAULT_ONLY).activityInfo.packageName

	val showLauncher = shownSpecialApps.contains("Launcher")
	val showSuperFreezZ = shownSpecialApps.contains("SuperFreezZ")
	val showSystem = shownSpecialApps.contains("Systemapps")

	fun isToBeShown(info: PackageInfo): Boolean {
		if (!showLauncher && info.packageName == launcherApplication) return false
		if (!showSuperFreezZ && info.packageName == BuildConfig.APPLICATION_ID) return false
		if (!showSystem && info.applicationInfo.flags.isFlagSet(ApplicationInfo.FLAG_SYSTEM)) return false
		return true
	}

	return packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
		.filter(::isToBeShown)
}

/**
 * Queries usage stats for the last two years by calling usageStatsManager.queryAndAggregateUsageStats().
 * TAKING VERY LONG, CALL ONLY ONCE DUE TO PERFORMANCE REASONS!
 *
 * @return A map with the package names of running apps or null if it could not be determined (on older versions of Android)
 * @see android.app.usage.UsageStatsManager.queryAndAggregateUsageStats
 */
internal fun getRecentAggregatedUsageStats(context: Context): Map<String, UsageStats>? {
	if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
		//In Android versions older than LOLLIPOP there is no UsageStatsManager
		return null
	}
	val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

	//Get all data starting with the whatever time the user specified in the settings ago
	val preferences = mGetDefaultSharedPreferences(context)
	val numberOfDays = preferences.getString("autofreeze_delay", "7")?.toIntOrNull().expectNonNull(TAG) ?: 7
	val now = System.currentTimeMillis()
	val startDate = now - 1000L * 60 * 60 * 24 * numberOfDays

	return usageStatsManager.queryAndAggregateUsageStats(startDate, now)
}

internal fun getAllAggregatedUsageStats(context: Context): Map<String, UsageStats>? {
	if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
		//In Android versions older than LOLLIPOP there is no UsageStatsManager
		return null
	}
	val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

	val now = System.currentTimeMillis()
	val startDate = now - 1000L * 60 * 60 * 24 * 356 * 2

	return usageStatsManager.queryAndAggregateUsageStats(startDate, now)
}

internal fun isRunning(applicationInfo: ApplicationInfo): Boolean {
	return !applicationInfo.flags.isFlagSet(ApplicationInfo.FLAG_STOPPED)
}

internal fun isPendingFreeze(packageInfo: PackageInfo, usageStats: UsageStats?, context: Context): Boolean {
	return isPendingFreeze(
		getFreezeMode(context, packageInfo.packageName),
		packageInfo.applicationInfo,
		usageStats
	)
}

internal fun isPendingFreeze(
		freezeMode: FreezeMode,
		applicationInfo: ApplicationInfo,
		usageStats: UsageStats?
): Boolean {

	if (!isRunning(applicationInfo)) {
		return false
	}

	return when (freezeMode) {

		FreezeMode.ALWAYS -> true

		FreezeMode.NEVER -> false

		FreezeMode.WHEN_INACTIVE -> {
			unusedRecently(usageStats)
		}
	}
}

internal fun getPendingFreezeExplanation(
		freezeMode: FreezeMode,
		applicationInfo: ApplicationInfo,
		usageStats: UsageStats?,
		context: Context
): String {

	fun string(stringID: Int) = context.getString(stringID)

	val isRunning = isRunning(applicationInfo)

	return when (freezeMode) {

		FreezeMode.ALWAYS ->
			if (isRunning) string(R.string.pending_freeze) else string(R.string.frozen)

		FreezeMode.NEVER ->
			string(R.string.freeze_off)

		FreezeMode.WHEN_INACTIVE -> {
			if (unusedRecently(usageStats)) {
				if (isRunning) string(R.string.pending_freeze) else string(R.string.frozen)
			} else {
				if (isRunning)
					string(R.string.used_recently)
				else
					string(R.string.used_recently) + string(R.string.space_AND_space) + string(R.string.frozen)
			}
		}
	}
}


@SuppressLint("NewApi")
private fun unusedRecently(usageStats: UsageStats?): Boolean {
	// return System.currentTimeMillis() - getLastTimeUsed(usageStats) > 1000L * 60 * 60 * 24 * 7
	// TODO option in settings to switch between the above and the below approach

	return when {
		Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP -> {
			logErrorAndStackTrace("usageStats", "unusedRecently was called on an older Android version")
			false
		}
		usageStats == null -> true // There are no usagestats of this package -> it was not used recently
		usageStats.packageName == BuildConfig.APPLICATION_ID -> false // The user is just using SuperFreezZ :-)
		else -> usageStats.totalTimeInForeground < 1000L * 2
	}
}

/**
 * Queries the usage stats and returns those apps that are pending freeze.
 */
internal fun getAppsPendingFreeze(context: Context): List<String> {

	val usageStatsMap = getRecentAggregatedUsageStats(context)
	return getApplications(context)
		.asSequence()
		.filter { isPendingFreeze(it, usageStatsMap?.get(it.packageName), context) }
		.map { it.packageName }
		.toList()
}

// Currently unused, instead
// usageStats.totalTimeInForeground < 1000L * 2
// is used directly currently (an app is only counted as 'used' if it was used at least 2s)
private fun getLastTimeUsed(usageStats: UsageStats?): Long {
	return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
		usageStats?.lastTimeUsed
			?: 0
	} else {
		0
	}
}

private fun Int.isFlagSet(value: Int): Boolean {
	return (this and value) == value
}

/**
 * Finds out whether the usage stats permission was granted, updates the usageStatsAvailable Variable accordingly and
 * returns the result.
 */
internal fun usageStatsPermissionGranted(context: Context): Boolean {

	//On earlier versions there are no usage stats
	if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
		return false
	}

	val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

	val mode = appOpsManager.checkOpNoThrow(
			AppOpsManager.OPSTR_GET_USAGE_STATS,
			Process.myUid(),
		context.packageName
	)

	val result = if (mode == AppOpsManager.MODE_DEFAULT) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			context.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED
		} else {
			false//TODO check if this assumption is right: At Lollipop, mode will be AppOpsManager.MODE_ALLOWED if it was allowed
		}
	} else {
		mode == AppOpsManager.MODE_ALLOWED
	}

	usageStatsAvailable = result
	return result
}