package superfreeze.tool.android

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences

val standardFreezeMode = FreezeMode.FREEZE_WHEN_INACTIVE
val values = FreezeMode.values()
private const val TAG = "DatabaseBackend"

internal fun getFreezeMode(activity: Activity, packageName: String): FreezeMode {
	val sharedPreferences = getFreezeModesPreferences(activity) ?: return standardFreezeMode
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