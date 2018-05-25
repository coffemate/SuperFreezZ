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
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, e.getClass());

                String message = e.toString() + "\n\n" + getStackTrace(e);
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, message);

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
