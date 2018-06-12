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

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Here the defaultUncaughtExceptionHandler (responsible for uncought exceptions) is set when the
 * app is started.
 */
public class MyApplication extends Application {
    @Override
    public void onCreate () {
        super.onCreate();

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
    }

    private String getStackTrace(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter, true));
        return stringWriter.getBuffer().toString();
    }
}
