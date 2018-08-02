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

package superfreeze.tool.android.backend

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import org.jetbrains.annotations.Contract

/**
 * This file is responsible for freezing apps.
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
 * Freezes all apps in the "apps" list or all apps that are pending freeze.
 * @param context: The context
 * @param apps: A list of apps to be frozen. If it is null or not given, a list of apps that pend freeze is computed automatically.
 * @param activity The current activity, needed to access the SharedPreferences to read which apps are pending freeze.
 * @return A function that has to be called when the current activity is entered again so that the next app can be frozen.
 * It returns whether it wants to be executed again.
 */
internal fun freezeAll(context: Context, apps: List<String>? = null, activity: Activity): () -> Boolean {
	return freezeAll(context, apps
			?: getAppsPendingFreeze(context, activity))
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

