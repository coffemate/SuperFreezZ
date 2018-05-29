package superfreeze.tool.android

import android.app.ProgressDialog
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import org.jetbrains.annotations.Contract


/**
 * This file is responsible for freezing apps and connected things, like loading the list of apps
 * that were not freezed yet.
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

	//remember that the app was freezed
	val preferences = context.getSharedPreferences("lastAppFreeze", Context.MODE_PRIVATE)
	val editor = preferences.edit()
	editor.putLong(packageName, System.currentTimeMillis())
	editor.apply()//preferences.getLong();
}

/**
 * Loads all running applications and add them to MainActivity.
 * Currently, as a workaround, this is those apps that were
 * @param mainActivity The MainActivity
 * @param context The application context.
 */
internal fun loadRunningApplications(mainActivity: MainActivity, context: Context) {

	val loader = object : AsyncTask<Void, PackageInfo, Void>() {
		private var dialog: ProgressDialog = ProgressDialog.show(mainActivity, context.getString(R.string.dlg_loading_title), context.getString(R.string.dlg_loading_body))
		private val usageStatsMap: Map<String, UsageStats>? = getAggregatedUsageStats(context)

		override fun doInBackground(vararg params: Void): Void? {
			val packages = context.packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
			for (packageInfo in packages) {
				publishProgress(packageInfo)
			}
			return null
		}

		override fun onProgressUpdate(vararg values: PackageInfo) {
			super.onProgressUpdate(*values)

			//Add the package only if it is NOT a system app
			if (values[0].applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
				//...and if it has been awoken since last freeze
				if (isRunning(values[0].packageName, usageStatsMap, context)) {
					mainActivity.addItem(values[0])
				}
			}
		}

		override fun onPostExecute(aVoid: Void?) {
			super.onPostExecute(aVoid)
			dialog.dismiss()
		}

	}

	loader.execute()
}


/**
 * Queries usage stats for the last two years by calling usageStatsManager.queryAndAggregateUsageStats().

 * @return A map with the package names of running apps or null if it could not be determined (on older versions of Android)
 * @see android.app.usage.UsageStatsManager.queryAndAggregateUsageStats
 */
internal fun getAggregatedUsageStats(context: Context): Map<String, UsageStats>? {
	if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
		//In Android versions older than LOLLIPOP there is no UsageStatsManager
		return null
	}
	val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

	//Get all data starting two years ago
	val now = System.currentTimeMillis()
	val startDate = now - 1000L*60*60*24*365*2

	return usageStatsManager.queryAndAggregateUsageStats(startDate, System.currentTimeMillis())
}

/**
 * Returns true if the app is running.
 * Currently, as a workaround, this returns if the app was interacted with since the last freeze.
 * @param packageName The name of the app
 * @param usageStatsMap The result of queryAndAggregateUsageStats()
 * @param context The application context
 * @return If the app is running
 */
private fun isRunning(packageName: String, usageStatsMap: Map<String, UsageStats>?, context: Context): Boolean {
	val preferences = context.getSharedPreferences("lastAppFreeze", Context.MODE_PRIVATE)
	val lastFreeze = preferences.getLong(packageName, 0)
	if (lastFreeze == 0L) {
		//The app was never freezed
		//simply assume that it was launched once after its installation and is now running
		return true
	}

	if (usageStatsMap == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
		//No usage stats available because we are on an older Android version
		//TODO
		return true
	}

	val usageStats = usageStatsMap[packageName]
			?: //If usageStatsMap[packageName] is null, return false as the app has not been used.
			return false

	return usageStats.lastTimeUsed > lastFreeze
}