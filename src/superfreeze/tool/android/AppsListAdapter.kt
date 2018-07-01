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

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Handler
import android.support.v7.widget.RecyclerView
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import org.jetbrains.annotations.Contract

import java.util.ArrayList
import java.util.Collections
import java.util.LinkedHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

/**
 * This class is responsible for viewing the list of installed apps.
 */
class AppsListAdapter internal constructor(private val mActivity: MainActivity) : RecyclerView.Adapter<AppsListAdapter.ViewHolder>() {
	private val tFactory = ThreadFactory { r ->
		val t = Thread(r)
		t.isDaemon = true
		t
	}

	/**
	 * This list contains the apps that are shown to the user. This is a subset of listOriginal.
	 */
	private val list = ArrayList<PackageInfo>()

	/**
	 * This list contains all user apps.
	 */
	private val listOriginal = ArrayList<PackageInfo>()

	private var executorServiceNames: ExecutorService? = null
	private val executorServiceIcons = Executors.newFixedThreadPool(3, tFactory)
	private val handler = Handler()
	private val packageManager: PackageManager = mActivity.packageManager

	private var namesToLoad = 0

	private val cacheAppName = Collections.synchronizedMap(LinkedHashMap<String, String>(10, 1.5f, true))
	private val cacheAppIcon = Collections.synchronizedMap(LinkedHashMap<String, Drawable>(10, 1.5f, true))

	private var searchPattern: String? = null

	internal inner class AppNameLoader(private val package_info: PackageInfo) : Runnable {

		override fun run() {
			cacheAppName[package_info.packageName] = package_info.applicationInfo.loadLabel(packageManager) as String
			handler.post {
				namesToLoad--
				if (namesToLoad == 0) {
					mActivity.hideProgressBar()
					executorServiceNames?.shutdown()
					executorServiceNames = null
				}
			}
		}
	}

	internal inner class GuiLoader(private val viewHolder: ViewHolder, private val package_info: PackageInfo) : Runnable {

		override fun run() {
			var first = true
			do {
				try {
					val appName = cacheAppName[package_info.packageName]
							?: package_info.applicationInfo.loadLabel(packageManager) as String

					val icon = package_info.applicationInfo.loadIcon(packageManager)
					cacheAppName[package_info.packageName] = appName
					cacheAppIcon[package_info.packageName] = icon
					handler.post {
						viewHolder.setAppName(appName, searchPattern)
						viewHolder.imgIcon.setImageDrawable(icon)
					}


				} catch (ex: OutOfMemoryError) {
					cacheAppIcon.clear()
					cacheAppName.clear()
					if (first) {
						first = false
						continue
					}
				}

				break
			} while (true)
		}
	}

	inner class ViewHolder(v: View, private val context: Context) : RecyclerView.ViewHolder(v), OnClickListener {
		private val txtAppName: TextView = v.findViewById(R.id.txtAppName)
		var imgIcon: ImageView = v.findViewById(R.id.imgIcon)

		init {
			v.setOnClickListener(this)
		}

		/**
		 * This method defines what is done when a list item (that is, an app) is clicked.
		 * @param v The clicked view.
		 */
		override fun onClick(v: View) {
			freezeApp(getItem(adapterPosition).packageName, context)
		}

		fun setAppName(name: String, highlight: String?) {
			setAndHighlight(txtAppName, name, highlight)
		}

		private fun setAndHighlight(view: TextView, value: String, pattern: String?) {
			view.text = value
			if (pattern == null || pattern.isEmpty()) return // nothing to highlight

			val valueLower = value.toLowerCase()
			var offset = 0
			var index = valueLower.indexOf(pattern, offset)
			while (index >= 0 && offset < valueLower.length) {
				val span = SpannableString(view.text)
				span.setSpan(ForegroundColorSpan(Color.BLUE), index, index + pattern.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
				view.text = span
				offset += index + pattern.length
				index = valueLower.indexOf(pattern, offset)
			}
		}
	}

	override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
		return ViewHolder(
				LayoutInflater.from(viewGroup.context).inflate(R.layout.list_item, viewGroup, false),
				viewGroup.context)
	}

	override fun onBindViewHolder(holder: ViewHolder, i: Int) {
		val item = list[i]

		holder.setAppName(
				cacheAppName[item.packageName] ?: item.packageName,
				searchPattern)

		holder.imgIcon.setImageDrawable(cacheAppIcon[item.packageName])
		if (cacheAppIcon[item.packageName] == null) {
			executorServiceIcons.submit(GuiLoader(holder, item))
		}
	}

	@Contract(pure = true)
	private fun getItem(pos: Int): PackageInfo {
		return list[pos]
	}

	override fun getItemCount(): Int {
		return list.size
	}

	fun addItem(item: PackageInfo) {
		if (executorServiceNames == null) {
			executorServiceNames = Executors.newFixedThreadPool(3, tFactory)
		}
		namesToLoad++
		executorServiceNames!!.submit(AppNameLoader(item))
		listOriginal.add(item)
		if (isToBeShown(item)) {
			list.add(item)
		}
		notifyDataSetChanged()
	}

	fun setSearchPattern(pattern: String) {
		searchPattern = pattern.toLowerCase()
		filterList()
	}


	internal fun filterList() {
		list.clear()
		for (info in listOriginal) {

			if (isToBeShown(info)) {
				list.add(info)
			}
		}
		notifyDataSetChanged()
	}

	/**
	 * Returns true if the the app name contains the search pattern.
	 * @param info The PackageInfo describing the package.
	 * @return Whether the the app name contains the search pattern.
	 */
	private fun isToBeShown(info: PackageInfo): Boolean {

		if (!isRunning(info)) {
			return false
		}

		if (searchPattern.isNullOrEmpty()) {
			return true// empty search pattern: Show all apps
		} else if (cacheAppName[info.packageName]?.toLowerCase()?.contains(searchPattern!!) == true) {
			return true// search in application name
		}

		return false
	}

	internal fun refreshPackageInfoList() {
		for (i in listOriginal.indices) {
			val packageName = listOriginal[i].packageName
			try {
				listOriginal[i] = packageManager.getPackageInfo(packageName, 0)
			} catch (e: PackageManager.NameNotFoundException) {
				e.printStackTrace()
				Log.e(TAG, "The package " + packageName + "was not found when refreshing")
			}

		}
	}

	companion object {
		private const val TAG = "AppsListAdapter"
	}

}