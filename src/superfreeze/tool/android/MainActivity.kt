/*
Copyright (c) 2015 axxapy
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

package superfreeze.tool.android

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.AppOpsManager
import android.app.SearchManager
import android.app.usage.UsageStats
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.support.annotation.RequiresApi
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
 * The activity that is shown at startup
 */
class MainActivity : AppCompatActivity() {
	private lateinit var appsListAdapter: AppsListAdapter

	private lateinit var progressBar: ProgressBar

	private val usageStatsMap: Map<String, UsageStats>? by lazy {
		getAggregatedUsageStats(this)
	}

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

		requestUsageStatsPermissionAndLoadApps()
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
	 * @param items The items to add, as PackageInfo's.
	 */
	private fun setItems(items: List<PackageInfo>) {
		appsListAdapter.setAndLoadItems(items, usageStatsMap)
	}

	/**
	 * The method that is responsible for showing the search icon in the top right hand corner.
	 * @param menu The Menu to which the search icon is added.
	 */
	@SuppressLint("RestrictedApi")
	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.main, menu)

		val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
		val searchView = menu.findItem(R.id.action_search).actionView as SearchView
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
				appsListAdapter.searchPattern = s
				return true
			}
		})
		fab.setOnClickListener {
			val freezeNext = freezeAll(applicationContext, apps = appsListAdapter.listPendingFreeze)
			doOnResume(freezeNext)
		}
		return super.onCreateOptionsMenu(menu)
	}

	override fun onResume() {
		super.onResume()

		//Execute all tasks and retain only those that returned true.
		toBeDoneOnResume.retainAll { it() }

		if (!FreezerService.busy()) {
			appsListAdapter.refresh(getAggregatedUsageStats(this))
			appsListAdapter.filterList()
		}
	}


	override fun onConfigurationChanged(newConfig: Configuration?) {
		super.onConfigurationChanged(newConfig)

		//This is necessary so that the list items change their look when the screen is rotated.
		val listView = list
		listView.adapter = null
		listView.layoutManager = null
		listView.recycledViewPool.clear()
		listView.adapter = appsListAdapter
		listView.layoutManager = LinearLayoutManager(this)
		appsListAdapter.notifyDataSetChanged()
	}

	companion object {
		private val toBeDoneOnResume: MutableList<() -> Boolean> = mutableListOf()
		/**
		 * Execute this task on resume.
		 * @param task If it returns true, then it will be executed again at the next onResume.
		 */
		internal fun doOnResume(task: ()->Boolean) {
			toBeDoneOnResume.add(task)
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
					.setPositiveButton("Enable") { _, _ ->
						showUsageStatsSettings()
						doOnResume {

							if (!usageStatsPermissionGranted()) {
								toast("You did not enable usagestats access.", Toast.LENGTH_SHORT)
							}
							loadRunningApplications()

							//Do not execute again
							false
						}
					}
					.setNeutralButton("Not now") { _, _ ->
						//directly load running applications
						loadRunningApplications()
					}
					//TODO add negative button "never"
					.setIcon(R.mipmap.ic_launcher)
					.setCancelable(false)
					.show()
		} else {
			//directly load running applications
			loadRunningApplications()
		}

	}

	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	private fun showUsageStatsSettings() {
		val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
		startActivity(intent)
		toast("Please select SuperFreeze, then enable access", Toast.LENGTH_LONG)
	}

	private fun usageStatsPermissionGranted(): Boolean {

		//On earlier versions there are no usage stats
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			return false
		}

		val appOpsManager = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

			val mode = appOpsManager.checkOpNoThrow(
					AppOpsManager.OPSTR_GET_USAGE_STATS,
					Process.myUid(),
					packageName)

			return if (mode == AppOpsManager.MODE_DEFAULT) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED
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

	private fun loadRunningApplications() {

		Thread {
			val packages = getRunningApplications(applicationContext)

			runOnUiThread {
				setItems(packages)
			}
		}.start()

	}
}
