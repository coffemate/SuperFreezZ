package superfreeze.tool.android.userInterface.settingsActivity

import android.annotation.TargetApi
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.*
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import superfreeze.tool.android.BuildConfig
import superfreeze.tool.android.R
import superfreeze.tool.android.backend.FreezerService
import superfreeze.tool.android.backend.usageStatsPermissionGranted
import superfreeze.tool.android.userInterface.IntroActivity
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader


private const val TAG = "SettingsActivity"

/**
 * A [PreferenceActivity] that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 *
 * See [Android Design: Settings](http://developer.android.com/design/patterns/settings.html)
 * for design guidelines and the [Settings API Guide](http://developer.android.com/guide/topics/ui/settings.html)
 * for more information on developing a Settings UI.
 */
class SettingsActivity : AppCompatPreferenceActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setupActionBar()
	}

	/**
	 * Set up the [android.app.ActionBar], if the API is available.
	 */
	private fun setupActionBar() {
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
	}

	override fun onMenuItemSelected(featureId: Int, item: MenuItem): Boolean {
		val id = item.itemId
		if (id == android.R.id.home) {
			if (!super.onMenuItemSelected(featureId, item)) {
				finishAndRestartMain()
			}
			return true
		}
		return super.onMenuItemSelected(featureId, item)
	}

	override fun onBackPressed() {
		super.onBackPressed()
	}

	private fun finishAndRestartMain() {
		// val intent = Intent(this, MainActivity::class.java)
		// intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
		// startActivity(intent)
		// Main will recreate itself
		finish()
		// This was:
		// NavUtils.navigateUpFromSameTask(this)
		// On newer Android versions the following might be possible:
		// navigateUpTo(Intent(this, MainActivity::class.java))
	}

	/**
	 * {@inheritDoc}
	 */
	override fun onIsMultiPane(): Boolean {
		return isXLargeTablet(this)
	}

	/**
	 * {@inheritDoc}
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	override fun onBuildHeaders(target: List<PreferenceActivity.Header>) {
		loadHeadersFromResource(R.xml.pref_headers, target)
	}

	/**
	 * This method stops fragment injection in malicious applications.
	 * Make sure to deny any unknown fragments here.
	 */
	override fun isValidFragment(fragmentName: String): Boolean {
		return PreferenceFragment::class.java.name == fragmentName
				|| AppsListPreferenceFragment::class.java.name == fragmentName
				|| AboutPreferenceFragment::class.java.name == fragmentName
				|| FreezingAppsPreferenceFragment::class.java.name == fragmentName
	}



	open class MyPreferenceFragment : PreferenceFragment() {
		final override fun onOptionsItemSelected(item: MenuItem): Boolean {
			val id = item.itemId
			if (id == android.R.id.home) {
				/*val intent = Intent(activity, SettingsActivity::class.java)
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
				activity.startActivity(intent)
				activity.finish()*/
				activity.onBackPressed()
				return true
			}
			return super.onOptionsItemSelected(item)
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	class AppsListPreferenceFragment : MyPreferenceFragment() {
		override fun onCreate(savedInstanceState: Bundle?) {
			super.onCreate(savedInstanceState)
			addPreferencesFromResource(R.xml.pref_appslist)
			setHasOptionsMenu(true)

			// Bind the summaries of EditText/List/Dialog/Ringtone preferences
			// to their values. When their values change, their summaries are
			// updated to reflect the new value, per the Android Design
			// guidelines.
			bindPreferenceSummaryToValue(findPreference("standard_freeze_mode"))
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	class FreezingAppsPreferenceFragment : MyPreferenceFragment() {
		private lateinit var useAccessibilityServicePreference: SwitchPreference
		private lateinit var useUsagestatsPreference: SwitchPreference
		override fun onCreate(savedInstanceState: Bundle?) {
			super.onCreate(savedInstanceState)
			addPreferencesFromResource(R.xml.pref_freezing)
			setHasOptionsMenu(true)

			bindPreferenceSummaryToValue(findPreference("autofreeze_delay"))

			useAccessibilityServicePreference = findPreference("use_accessibility_service") as SwitchPreference
			useAccessibilityServicePreference.setOnPreferenceClickListener {
				if (FreezerService.isEnabled) {
					Toast.makeText(activity, "Please disable accessibility service for SuperFreezZ", Toast.LENGTH_LONG).show()
					val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
					startActivity(intent)
				} else {
					val intent = Intent(IntroActivity.SHOW_ACCESSIBILITY_SERVICE_CHOOSER)
					intent.setClass(activity, IntroActivity::class.java)
					startActivity(intent)
				}
				false // Do not change the preference yet
			}

			useUsagestatsPreference = findPreference("use_usagestats") as SwitchPreference
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				useUsagestatsPreference.setOnPreferenceClickListener {
					val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
					startActivity(intent)
					false
				}
			} else {
				useUsagestatsPreference.isEnabled = false
			}

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				findPreference("freeze_on_screen_off").setOnPreferenceChangeListener { _, newValue ->
					if (newValue == true && !Settings.System.canWrite(context)) {

						AlertDialog.Builder(context, R.style.myAlertDialog)
							.setTitle("Modify settings")
							.setMessage("SuperFreezZ needs to modify the settings in order to turn off the screen after freezing.")
							.setPositiveButton("Ok") {_, _ ->
								val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
								intent.data = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
								startActivity(intent)
							}
							.setCancelable(false)
							.show()
					}
					true
				}
			}
		}

		override fun onResume() {
			super.onResume()
			updatePreferenceStates()
		}

		private fun updatePreferenceStates() {
			useAccessibilityServicePreference.isChecked = FreezerService.isEnabled
			useUsagestatsPreference.isChecked = usageStatsPermissionGranted(activity)
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	class AboutPreferenceFragment : MyPreferenceFragment() {
		override fun onCreate(savedInstanceState: Bundle?) {
			super.onCreate(savedInstanceState)
			addPreferencesFromResource(R.xml.pref_about)
			setHasOptionsMenu(true)

			findPreference("send_logs").setOnPreferenceClickListener {
				//Share info about the exception so that it can be viewed or sent to someone else
				val sharingIntent = Intent(Intent.ACTION_SEND)
				sharingIntent.type = "text/plain"
				val logs = getLogs()
				sharingIntent.putExtra(Intent.EXTRA_TEXT, logs)
				startActivity(Intent.createChooser(sharingIntent, "Share logs using..."))
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					// Copy to clipboard:
					Toast.makeText(context ?: activity, "Logs were copied to the clipboard.", Toast.LENGTH_LONG).show()
					val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
					val clip = ClipData.newPlainText("logs", logs)
					clipboard?.primaryClip = clip
				}
				true
			}
		}

		private fun getLogs(): String {
			return try {
				val process = Runtime.getRuntime().exec("logcat -d")
				val bufferedReader = BufferedReader(
						InputStreamReader(process.inputStream))

				bufferedReader.use { it.readText() }

			} catch (e: IOException) {
				Log.e(TAG, "Could not get logs (???)")
				""
			}
		}
	}



	companion object {

		/**
		 * A preference value change listener that updates the preference's summary
		 * to reflect its new value.
		 */
		private val sBindPreferenceSummaryToValueListener = Preference.OnPreferenceChangeListener { preference, value ->
			val stringValue = value.toString()

			if (preference is ListPreference) {
				// For list preferences, look up the correct display value in
				// the preference's 'entries' list.
				val listPreference = preference
				val index = listPreference.findIndexOfValue(stringValue)

				// Set the summary to reflect the new value.
				preference.setSummary(
					if (index >= 0)
						listPreference.entries[index]
					else
						null
				)

			} else if (preference is RingtonePreference) {
				// For ringtone preferences, look up the correct display value
				// using RingtoneManager.
				if (TextUtils.isEmpty(stringValue)) {
					// Empty values correspond to 'silent' (no ringtone).
					preference.setSummary(R.string.pref_ringtone_silent)

				} else {
					val ringtone = RingtoneManager.getRingtone(
						preference.getContext(), Uri.parse(stringValue)
					)

					if (ringtone == null) {
						// Clear the summary if there was a lookup error.
						preference.setSummary(null)
					} else {
						// Set the summary to reflect the new ringtone display
						// name.
						val name = ringtone.getTitle(preference.getContext())
						preference.setSummary(name)
					}
				}

			} else {
				// For all other preferences, set the summary to the value's
				// simple string representation.
				preference.summary = stringValue
			}
			true
		}

		/**
		 * Helper method to determine if the device has an extra-large screen. For
		 * example, 10" tablets are extra-large.
		 */
		private fun isXLargeTablet(context: Context): Boolean {
			return context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_XLARGE
		}

		/**
		 * Binds a preference's summary to its value. More specifically, when the
		 * preference's value is changed, its summary (line of text below the
		 * preference title) is updated to reflect the value. The summary is also
		 * immediately updated upon calling this method. The exact display format is
		 * dependent on the type of preference.

		 * @see .sBindPreferenceSummaryToValueListener
		 */
		private fun bindPreferenceSummaryToValue(preference: Preference) {
			// Set the listener to watch for value changes.
			preference.onPreferenceChangeListener = sBindPreferenceSummaryToValueListener

			// Trigger the listener immediately with the preference's
			// current value.
			sBindPreferenceSummaryToValueListener.onPreferenceChange(
				preference,
				PreferenceManager
					.getDefaultSharedPreferences(preference.context)
					.getString(preference.key, "")
			)
		}

	}
}
