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

package superfreeze.tool.android

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast

const val TAG = "SF-MyApplication"

class MyApplication : Application() {
	override fun onCreate() {
		super.onCreate()


		/*//This can be used to delay the app start for debugging purposes (especially the Android Studio Profiler):
		val start = System.currentTimeMillis()
		while (start + 10000 > System.currentTimeMillis()) {
			Thread.sleep(10);
		}*/

		// Setup handler for uncaught exceptions.
		Thread.setDefaultUncaughtExceptionHandler { _, e ->
			e.printStackTrace()

			//Share info about the exception so that it can be viewed or sent to someone else

			val message = "Android ${android.os.Build.VERSION.RELEASE}: $e \n\n ${getStackTrace(e)}"
			val subject = "Crash report SuperFreezZ v${BuildConfig.VERSION_NAME}"
			val email = "superfreezz-automated@gmx.de"

			Toast.makeText(
				this,
				getString(R.string.sf_crashed),
				Toast.LENGTH_LONG
			).show()

			var sharingIntent = Intent(
				Intent.ACTION_SENDTO, Uri.fromParts(
					"mailto", email, null
				)
			)
			sharingIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
			sharingIntent.putExtra(Intent.EXTRA_SUBJECT, subject)
			sharingIntent.putExtra(Intent.EXTRA_TEXT, message)

			if (sharingIntent.resolveActivity(packageManager) == null) {
				Log.w(TAG, "No email client found to send crash report")
				sharingIntent = Intent(Intent.ACTION_SEND)
				sharingIntent.type = "text/plain"
				sharingIntent.putExtra(Intent.EXTRA_SUBJECT, subject)
				sharingIntent.putExtra(Intent.EXTRA_TEXT, message)
				sharingIntent.putExtra(Intent.EXTRA_EMAIL, email)
			}

			val chooser =
				Intent.createChooser(sharingIntent, resources.getString(R.string.share_exception))
			chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			chooser.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)

			startActivity(chooser)
			System.exit(10)
		}

		/*
		//Set the thread policy so that a lot of bad things happen when the app hangs a little too long.
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
				.detectDiskReads()
				.detectDiskWrites()
				.detectNetwork()
				.penaltyLog()
				.penaltyFlashScreen()
				.penaltyDeath()
				.build());

		StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
				.detectLeakedSqlLiteObjects()
				.detectLeakedClosableObjects()
				.penaltyLog()
				.penaltyDeath()
				.build());
		*/
	}

}
