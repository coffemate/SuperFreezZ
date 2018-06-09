package superfreeze.tool.android

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.AppOpsManager
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.annotation.RequiresApi
import android.support.v4.view.MenuItemCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SearchView
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

/**
 * The activity that is shown
 */
class MainActivity : AppCompatActivity() {
	private lateinit var appsListAdapter: AppsListAdapter

	private lateinit var progressBar: ProgressBar
	private lateinit var permissionResolver: PermissionResolver

	private var appWasLeftForUsageStatsSettings: Boolean = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		setSupportActionBar(toolbar)

		val listView = list

		appsListAdapter = AppsListAdapter(this)
		listView.layoutManager = LinearLayoutManager(this)
		listView.adapter = appsListAdapter

		progressBar = progress
		progressBar.visibility = View.VISIBLE

		permissionResolver = PermissionResolver(this)

		requestUsageStatsPermissionAndLoadApps()
	}


	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
		if (!permissionResolver.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
			super.onRequestPermissionsResult(requestCode, permissions, grantResults)
		}
	}

	/**
	 * At startup, there will be a spinning progress bar at the top right hand corner.
	 * Invoking this method will hide this progress bar.
	 */
	fun hideProgressBar() {
		progressBar.visibility = View.GONE
	}

	/**
	 * This will add item to the apps list.
	 * @param item The item to add, as a PackageInfo.
	 */
	fun addItem(item: PackageInfo) {
		appsListAdapter.addItem(item)
	}

	/**
	 * The method that is responsible for showing the search icon in the top right hand corner.
	 * @param menu The Menu to which the search icon is added.
	 */
	@SuppressLint("RestrictedApi")
	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.main, menu)

		val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
		val searchView = MenuItemCompat.getActionView(menu.findItem(R.id.action_search)) as SearchView
		searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
		searchView.setOnQueryTextFocusChangeListener { _, queryTextFocused ->
			if (!queryTextFocused && searchView.query.isEmpty()) {
				val supportActionBar = supportActionBar
				if(supportActionBar != null) {
					supportActionBar.collapseActionView()
				} else {
					Log.e("SuperFreezeUI", "There is no action bar")
				}
			}
		}
		searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
			override fun onQueryTextSubmit(s: String): Boolean {
				return false
			}

			override fun onQueryTextChange(s: String): Boolean {
				appsListAdapter.setSearchPattern(s)
				return true
			}
		})

		return super.onCreateOptionsMenu(menu)
	}

	override fun onResume() {
		super.onResume()
		if (appWasLeftForUsageStatsSettings) {

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				if (!usageStatsPermissionGranted()) {
					toast("You did not enable usagestats access.", Toast.LENGTH_SHORT)
				}
			}
			loadRunningApplications(this, applicationContext)
			appWasLeftForUsageStatsSettings = false

		}
	}

	private fun requestUsageStatsPermissionAndLoadApps() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			return
		}

		if (!usageStatsPermissionGranted()) {

			AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog)
					.setTitle("UsageStats access")
					.setMessage("If you enable UsageStats access, SuperFreeze can:\n - see which apps have been awoken since last freeze\n - freeze only apps you did not use for some time.")
					.setPositiveButton("Enable", { _, _ ->
						showUsageStatsSettings()
						appWasLeftForUsageStatsSettings = true
					})
					.setNeutralButton("Now now", { _, _ ->
						//directly load running applications
						loadRunningApplications(this, applicationContext)
					})
					//TODO add negative button "never"
					.setIcon(R.mipmap.ic_launcher)
					.setCancelable(false)
					.show()
		} else {
			//directly load running applications
			loadRunningApplications(this, applicationContext)
		}

	}

	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	private fun showUsageStatsSettings() {
		val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
		startActivity(intent)
		toast("Please select SuperFreeze, then enable access", Toast.LENGTH_LONG)
	}

	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	private fun usageStatsPermissionGranted(): Boolean {
		val appOpsManager = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

			val mode = appOpsManager.checkOpNoThrow(
					AppOpsManager.OPSTR_GET_USAGE_STATS,
					android.os.Process.myUid(),
					packageName)

			return if (mode == AppOpsManager.MODE_DEFAULT) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					checkCallingOrSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED
				} else {
					false//TODO check if this assumption is right: At Lollipop, mode will be AppOpsManager.MODE_ALLOWED if it was allowed
				}
			} else {
				mode == AppOpsManager.MODE_ALLOWED
			}


	}

	private fun toast(s: String, duration: Int) {
		Toast.makeText(this, s, duration).show()
	}
}
