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

package superfreeze.tool.android;

import android.app.Application;
import android.content.Intent;

import static superfreeze.tool.android.backend.HelperFunctionsKt.getStackTrace;


public class MyApplication extends Application {
	@Override
	public void onCreate () {
		super.onCreate();

	    /*
	    //This can be used to delay the app start for debugging purposes (especially the Android Studio Profiler):
	    long start = System.currentTimeMillis();
	    while (start + 5000 > System.currentTimeMillis()) {
		    try {
			    Thread.sleep(10);
		    } catch (InterruptedException e) {
			    e.printStackTrace();
		    }

	    }*/

		// Setup handler for uncaught exceptions.
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread thread, Throwable e) {
				e.printStackTrace();

				//Share info about the exception so that it can be viewed or sent to someone else
				Intent sharingIntent = new Intent(Intent.ACTION_SEND);
				sharingIntent.setType("text/plain");
				sharingIntent.putExtra(Intent.EXTRA_SUBJECT, e.getClass());

				String message = e.toString() + "\n\n" + getStackTrace(e);
				sharingIntent.putExtra(Intent.EXTRA_TEXT, message);

				startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.share_exception)));
			}
		});

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
