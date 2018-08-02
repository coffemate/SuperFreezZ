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

package superfreeze.tool.android.database

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import superfreeze.tool.android.FreezeMode

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
	return activity.getSharedPreferences("superfreeze.tool.android.FREEZE_MODES", Context.MODE_PRIVATE)
}