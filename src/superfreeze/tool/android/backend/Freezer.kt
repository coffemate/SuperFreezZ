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
 * This file is responsible for freezing apps. It acts as an abstraction layer between the UI and FreezerService.
 */

package superfreeze.tool.android.backend

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import org.jetbrains.annotations.Contract
import superfreeze.tool.android.BuildConfig

/**
 * Freeze a package.
 * @param packageName The name of the package to freeze
 * @param context The context of the calling application
 */
@Contract(pure = true)
internal fun freezeApp(packageName: String, context: Context) {

	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && FreezerService.isEnabled) {
		FreezerService.performFreeze()
	}

	val intent = Intent()
	intent.action = "android.settings.APPLICATION_DETAILS_SETTINGS"
	intent.data = Uri.fromParts("package", packageName, null)
	context.startActivity(intent)
}


/**
 * Freezes all apps in the "apps" list or all apps that are pending freeze.
 * @param context: The context
 * @param apps: A list of apps to be frozen. If it is null or not given, a list of apps that pend freeze is computed automatically.
 * @return A function that has to be called when the current activity is entered again so that the next app can be frozen.
 * It returns whether it wants to be executed again.
 */
internal suspend fun freezeAll(context: Context, apps: List<String>? = null): () -> Boolean {
	return _freezeAll(
		context,
		(apps ?: getAppsPendingFreeze(context))
			// The following line is necessary to always freeze SuperFreezZ itself last:
			.sortedBy{ it == BuildConfig.APPLICATION_ID }
	)
}

private suspend fun _freezeAll(context: Context, apps: List<String>): () -> Boolean {
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

	// The returned function will be called in onResume:
	return ::freezeNext
}

internal fun setFreezerExceptionHandler(function: () -> Unit) {
	FreezerService.setExceptionHandler(function)
}


fun getRandomNumber(): Int {
	return 5    // chosen by fair dice roll,
	            // guaranteed to be random.

	// Greetings to anyone reviewing this code!
}