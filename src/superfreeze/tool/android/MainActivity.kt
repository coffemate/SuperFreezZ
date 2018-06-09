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
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast


class MainActivity : AppCompatActivity() {
	private var appsListAdapter: AppsListAdapter? = null

	private var progressBar: ProgressBar? = null
	private var permissionResolver: PermissionResolver? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		setSupportActionBar(findViewById(R.id.toolbar) as Toolbar)

		val listView = findViewById(android.R.id.list) as RecyclerView

		appsListAdapter = AppsListAdapter(this)
		listView.layoutManager = LinearLayoutManager(this)
		listView.adapter = appsListAdapter

		progressBar = findViewById(android.R.id.progress) as ProgressBar
		progressBar!!.visibility = View.VISIBLE

		loadRunningApplications(this, applicationContext)

		permissionResolver = PermissionResolver(this)

		requestUsageStatsPermission()
	}


	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
		if (!permissionResolver!!.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
			super.onRequestPermissionsResult(requestCode, permissions, grantResults)
		}
	}

	fun hideProgressBar() {
		progressBar?.visibility = View.GONE
	}

	fun addItem(item: PackageInfo) {
		appsListAdapter!!.addItem(item)
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
				val supportActionBar = supportActionBar!!
				supportActionBar.collapseActionView()
			}
		}
		searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
			override fun onQueryTextSubmit(s: String): Boolean {
				return false
			}

			override fun onQueryTextChange(s: String): Boolean {
				appsListAdapter!!.setSearchPattern(s)
				return true
			}
		})

		return super.onCreateOptionsMenu(menu)
	}

	private fun requestUsageStatsPermission() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			return
		}

		if (!usageStatsPermissionGranted()) {

			AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog)
					.setTitle("UsageStats access")
					.setMessage("If you enable UsageStats access, SuperFreeze can:\n - see which apps have been awoken since last freeze\n - freeze only apps you did not use for some time.")
					.setPositiveButton("Enable", { _, _ ->
						showUsageStatsSettings()
					})
					.setNeutralButton("Now now", { _, _ ->
						//do nothing
					})
					//TODO add negative button "never"
					.setIcon(R.mipmap.ic_launcher)
					.show()
		}

	}

	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	private fun showUsageStatsSettings() {
		val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
		startActivity(intent)
		showToast("Please select SuperFreeze, then enable access", Toast.LENGTH_LONG)
	}

	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	private fun usageStatsPermissionGranted(): Boolean {
		val appOpsManager = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

		try {
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
		} catch (e: NullPointerException) {
			Log.w("", e)
			return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				checkCallingOrSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED
			} else {
				false//TODO check if this assumption is right: At Lollipop, mode will be AppOpsManager.MODE_ALLOWED if it was allowed
			}
		}

	}

	private fun showToast(s: String, duration: Int) {
		Toast.makeText(this, s, duration).show()
	}
}
