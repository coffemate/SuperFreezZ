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
 * This file is responsible for the database (that is, the SharedPreference's).
 */

package superfreeze.tool.android.database

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import superfreeze.tool.android.BuildConfig
import superfreeze.tool.android.backend.FreezeMode

val standardFreezeMode = FreezeMode.FREEZE_WHEN_INACTIVE
val values = FreezeMode.values()
private const val TAG = "DatabaseBackend"

internal fun getFreezeMode(activity: Activity, packageName: String): FreezeMode {
	val sharedPreferences = getFreezeModesPreferences(activity)
			?: return standardFreezeMode
	val ordinal = sharedPreferences.getInt(packageName, standardFreezeMode.ordinal)
	return values[ordinal]
}

internal fun setFreezeMode(activity: Activity, packageName: String, freezeMode: FreezeMode) {
	val sharedPreferences = getFreezeModesPreferences(activity) ?: return
	with (sharedPreferences.edit()) {
		putInt(packageName, freezeMode.ordinal)
		apply()
	}
}

private fun getFreezeModesPreferences(activity: Activity): SharedPreferences? {
	return activity.getSharedPreferences("${BuildConfig.APPLICATION_ID}.FREEZE_MODES", Context.MODE_PRIVATE)
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