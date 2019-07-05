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
 * This file is responsible for the database (that is, the SharedPreference's).
 */

package superfreeze.tool.android.database

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import superfreeze.tool.android.BuildConfig
import superfreeze.tool.android.expectNonNull

val values = FreezeMode.values()
private const val TAG = "DatabaseBackend"

internal fun getFreezeMode(context: Context, packageName: String): FreezeMode {

	val sharedPreferences = getFreezeModesPreferences(context)
	val standardFreezeMode = getPrefs(context)
			.getString("standard_freeze_mode", FreezeMode.WHEN_INACTIVE.ordinal.toString())
			?.toIntOrNull()
			.expectNonNull(TAG)
			?: FreezeMode.WHEN_INACTIVE.ordinal

	val ordinal = sharedPreferences.getInt(packageName, standardFreezeMode)
	val result = values[ordinal]

	return if (result == FreezeMode.WHEN_INACTIVE && !usageStatsAvailable) {
		FreezeMode.NEVER
		// If the usage stats are not available, WHEN_INACTIVE is not available, either. Instead, we use
		// NEVER here.
	} else {
		result
	}
}

internal fun setFreezeMode(context: Context, packageName: String, freezeMode: FreezeMode) {
	val sharedPreferences = getFreezeModesPreferences(context)
	with(sharedPreferences.edit()) {
		putInt(packageName, freezeMode.ordinal)
		apply()
	}
}

internal fun neverCalled(id: String, activity: Activity): Boolean {
	val sharedPreferences = activity.getSharedPreferences(id, Context.MODE_PRIVATE)
	val first = sharedPreferences.getBoolean(id, true)
	if (first) {
		with(sharedPreferences.edit()) {
			putBoolean(id, false)
			apply()
		}
	}
	return first
}

private fun getFreezeModesPreferences(context: Context): SharedPreferences {
	return context.getSharedPreferences("${BuildConfig.APPLICATION_ID}.FREEZE_MODES", Context.MODE_PRIVATE)
}

private fun getMainPreferences(context: Context): SharedPreferences {
	return context.getSharedPreferences("${BuildConfig.APPLICATION_ID}.MAIN", Context.MODE_PRIVATE)
}

internal fun getPrefs(context: Context): SharedPreferences {
	return PreferenceManager.getDefaultSharedPreferences(context)
}

internal var Context.prefListSortMode
	get() = getMainPreferences(this).getInt("ListSortMode", 0)
	set(v) = getMainPreferences(this).edit().putInt("ListSortMode", v).apply()

internal var Context.prefIntroAlreadyShown
	get() = getMainPreferences(this).getBoolean("FirstLaunch", true)
	set(v) = getMainPreferences(this).edit().putBoolean("FirstLaunch", v).apply()

internal var Context.prefUseAccessibilityService
	get() = getMainPreferences(this).getBoolean("use_accessibility_service", false)
	set(v) = getMainPreferences(this).edit().putBoolean("use_accessibility_service", v).apply()

internal var usageStatsAvailable: Boolean = false

/**
 * The freeze mode of an app: ALWAYS, NEVER or WHEN_INACTIVE
 */
enum class FreezeMode {

	/**
	 * This app will always be frozen if it is running, regardless of when it was used last.
	 */
	ALWAYS,

	/**
	 * This app will never be frozen, even if it has been running in background for whatever time.
	 */
	NEVER,

	/**
	 * This app will be frozen if it was not used for a specific time but is running in background.
	 */
	WHEN_INACTIVE
}