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
import superfreeze.tool.android.backend.FreezeMode
import superfreeze.tool.android.backend.expectNonNull

val values = FreezeMode.values()
private const val TAG = "DatabaseBackend"

internal fun getFreezeMode(context: Context, packageName: String): FreezeMode {

	val sharedPreferences = getFreezeModesPreferences(context)
	val standardFreezeMode = mGetDefaultSharedPreferences(context)
			?.getString("standard_freeze_mode", FreezeMode.FREEZE_WHEN_INACTIVE.ordinal.toString())
			?.toIntOrNull()
			.expectNonNull(TAG)
			?: FreezeMode.FREEZE_WHEN_INACTIVE.ordinal

	val ordinal = sharedPreferences.getInt(packageName, standardFreezeMode)
	val result = values[ordinal]

	return if (result == FreezeMode.FREEZE_WHEN_INACTIVE && !usageStatsAvailable) {
		FreezeMode.NEVER_FREEZE
		// If the usage stats are not available, FREEZE_WHEN_INACTIVE is not available, either. Instead, we use
		// NEVER_FREEZE here.
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

internal fun isFirstLaunch(context: Context): Boolean {
	val sharedPreferences = getMainPreferences(context)
	return sharedPreferences.getBoolean("FirstLaunch", true)
}

internal fun firstLaunchCompleted(context: Context) {
	val sharedPreferences = getMainPreferences(context)
	with(sharedPreferences.edit()) {
		putBoolean("FirstLaunch", false)
		apply()
	}
}

private fun getFreezeModesPreferences(context: Context): SharedPreferences {
	return context.getSharedPreferences("${BuildConfig.APPLICATION_ID}.FREEZE_MODES", Context.MODE_PRIVATE)
}

private fun getMainPreferences(context: Context): SharedPreferences {
	return context.getSharedPreferences("${BuildConfig.APPLICATION_ID}.MAIN", Context.MODE_PRIVATE)
}

internal fun mGetDefaultSharedPreferences(context: Context): SharedPreferences? {
	return PreferenceManager.getDefaultSharedPreferences(context)
}

internal var usageStatsAvailable: Boolean = false