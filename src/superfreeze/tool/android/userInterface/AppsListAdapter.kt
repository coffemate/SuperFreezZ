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


package superfreeze.tool.android.userInterface

import android.app.usage.UsageStats
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
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
import kotlinx.android.synthetic.main.activity_main.*
import superfreeze.tool.android.FreezeMode
import superfreeze.tool.android.R
import superfreeze.tool.android.backend.freezeApp
import superfreeze.tool.android.backend.isPendingFreeze
import superfreeze.tool.android.database.getFreezeMode
import superfreeze.tool.android.database.setFreezeMode
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


/**
 * This class is responsible for viewing the list of installed apps.
 */
internal class AppsListAdapter internal constructor(private val mainActivity: MainActivity) : RecyclerView.Adapter<AppsListAdapter.AbstractViewHolder>() {
	private val tFactory = ThreadFactory { r ->
		Thread(r).apply { isDaemon = true }
	}

	/**
	 * This list contains all apps in the list exactly once. That is, apps that appear twice in the list are contained only once.
	 */
	private val appsList = ArrayList<ListItemApp>()

	/**
	 * This list contains all apps and the section headers, as shown to the user when not searching.
	 */
	private var originalList = emptyList<AbstractListItem>()

	/**
	 * This list contains the items as shown to the user, including section headers. While the user is searching, this is a clone of originalList.
	 */
	private var list = emptyList<AbstractListItem>()


	var listPendingFreeze: List<String>? = null
		private set

	private val executorServiceIcons = Executors.newFixedThreadPool(3, tFactory)
	private val packageManager: PackageManager = mainActivity.packageManager

	private val cacheAppName = Collections.synchronizedMap(LinkedHashMap<String, String>(10, 1.5f, true))
	private val cacheAppIcon = Collections.synchronizedMap(LinkedHashMap<String, Drawable>(10, 1.5f, true))

	private val comparator = kotlin.Comparator<ListItemApp> { o1, o2 ->  o1.text.compareTo(o2.text) }

	var searchPattern: String = ""
		set(value) {
			field = value.toLowerCase()
			refreshList()
		}




	override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): AbstractViewHolder {
		return if (i == 0) {
			ViewHolderApp(
					LayoutInflater.from(viewGroup.context).inflate(R.layout.list_item, viewGroup, false),
					viewGroup.context,
					FreezeMode.FREEZE_WHEN_INACTIVE)
		} else {
			ViewHolderSectionHeader(
					LayoutInflater.from(viewGroup.context).inflate(R.layout.list_section_header, viewGroup, false))
		}
	}

	override fun onBindViewHolder(holder: AbstractViewHolder, i: Int) {
		list[i].bindViewHolder(holder)
	}

	override fun getItemCount(): Int {
		return list.size
	}

	override fun getItemViewType(position: Int): Int {
		return list[position].type
	}



	internal fun setAndLoadItems(packages: List<PackageInfo>, usageStatsMap: Map<String, UsageStats>?) {
		appsList.clear()
		appsList.addAll(packages.map {
			ListItemApp(it.packageName)
		})


		@Suppress("UNCHECKED_CAST")
		loadAllNames(appsList) {
			mainActivity.runOnUiThread {
				Collections.sort(appsList, comparator)
				refreshOriginalList(usageStatsMap)
				refreshList()
				notifyDataSetChanged()
				mainActivity.hideProgressBar()
				mainActivity.reportFullyDrawn()
			}
		}

		refreshOriginalList(usageStatsMap)
		refreshList()
	}


	internal fun refresh(usageStatsMap: Map<String, UsageStats>?) {
		for (app in appsList) {
			app.refresh()
		}
		refreshOriginalList(usageStatsMap)
		refreshList()
	}

	internal fun trimMemory() {
		cacheAppIcon.clear()
	}





	@Suppress("UNCHECKED_CAST")
	private fun refreshOriginalList(usageStatsMap: Map<String, UsageStats>?) {

		//We need to test whether the applications are still installed and remove those that are not.
		//Apparently, there is no better way for this than trying to access the applicationInfo.
		appsList.removeAll{
			try {
				it.applicationInfo
				false
			} catch (e: PackageManager.NameNotFoundException) {
				true
			}
		}

		val listPendingFreeze =
				appsList.filter {
					isPendingFreeze(it.freezeMode, it.applicationInfo, usageStatsMap?.get(it.packageName))
				}


		originalList =
				if (listPendingFreeze.isEmpty()) {
					listOf(ListItemSectionHeader("ALL APPS")) +
							appsList
				} else {
					listOf(ListItemSectionHeader("PENDING FREEZE")) +
							listPendingFreeze +
							ListItemSectionHeader("ALL APPS") +
							appsList
				}

		this.listPendingFreeze = listPendingFreeze.map{ it.packageName }
	}

	private fun refreshList() {
		list =
				if (searchPattern.isEmpty()) {
					originalList
				} else {

					// When the user is searching, the more relevant apps (that is, those
					// that start with the search pattern) are shown at the top:
					val (importantApps, otherApps) =
							appsList
									.filter { it.isMatchingSearchPattern() }
									.partition {
										cacheAppName[it.packageName]?.toLowerCase()?.startsWith(searchPattern) != false
									}
					importantApps + otherApps

				}

		notifyDataSetChanged()
	}

	private fun loadAllNames(items: List<ListItemApp>, onAllNamesLoaded: () -> Unit) {
		val executorServiceNames = Executors.newFixedThreadPool(3, tFactory)
		for (item in items) {
			executorServiceNames.submit {
				cacheAppName[item.packageName] = item.applicationInfo.loadLabel(packageManager).toString()
			}
		}
		executorServiceNames.shutdown()
		Thread {
			val finished = executorServiceNames.awaitTermination(2, TimeUnit.MINUTES)
			if (!finished)
				Log.e(TAG, "After 2 minutes, some app names were still not loaded")
			onAllNamesLoaded()
		}.start()
	}


	internal abstract class AbstractViewHolder(v: View) : RecyclerView.ViewHolder(v) {
		abstract fun setName(name: String, highlight: String?)
	}

	internal inner class ViewHolderApp(v: View, private val context: Context, freezeMode: FreezeMode) : AbstractViewHolder(v), OnClickListener {

		private val txtAppName: TextView = v.findViewById(R.id.txtAppName)
		val imgIcon: ImageView = v.findViewById(R.id.imgIcon)
		private val symbolAlwaysFreeze = v.findViewById<ImageView>(R.id.imageAlwaysFreeze)
		private val symbolFreezeWhenInactive = v.findViewById<ImageView>(R.id.imageFreezeWhenInactive)
		private val symbolNeverFreeze = v.findViewById<ImageView>(R.id.imageNeverFreeze)
		var listItem: ListItemApp? = null

		init {
			v.setOnClickListener(this)

			symbolAlwaysFreeze.setOnClickListener {
				setFreezeModeTo(FreezeMode.ALWAYS_FREEZE, changeSettings = true)
			}

			symbolFreezeWhenInactive.setOnClickListener {
				setFreezeModeTo(FreezeMode.FREEZE_WHEN_INACTIVE, changeSettings = true)
			}

			symbolNeverFreeze.setOnClickListener {
				setFreezeModeTo(FreezeMode.NEVER_FREEZE, changeSettings = true)
			}

			setFreezeModeTo(freezeMode, changeSettings = false)
		}

		//Usually, if the settings changed, this means that a snackbar with an undo button should be shown
		internal fun setFreezeModeTo(freezeMode: FreezeMode, changeSettings: Boolean, showSnackbar: Boolean = changeSettings) {
			val oldFreezeMode = listItem?.freezeMode

			val colorGreyedOut = ContextCompat.getColor(context, R.color.button_greyed_out)

			if (changeSettings) {
				val listItem = list.getOrNull(adapterPosition) as ListItemApp?
				listItem?.freezeMode = freezeMode
			}

			val snackbarText: String

			when(freezeMode) {
				FreezeMode.ALWAYS_FREEZE ->  {
					symbolAlwaysFreeze.setColorFilter(null)
					symbolFreezeWhenInactive.setColorFilter(colorGreyedOut)
					symbolNeverFreeze.setColorFilter(colorGreyedOut)
					snackbarText = " will always be frozen."
				}
				FreezeMode.FREEZE_WHEN_INACTIVE -> {
					symbolAlwaysFreeze.setColorFilter(colorGreyedOut)
					symbolFreezeWhenInactive.setColorFilter(null)
					symbolNeverFreeze.setColorFilter(colorGreyedOut)
					snackbarText = " will be frozen if it hasn't been used for a long time."
				}
				FreezeMode.NEVER_FREEZE -> {
					symbolAlwaysFreeze.setColorFilter(colorGreyedOut)
					symbolFreezeWhenInactive.setColorFilter(colorGreyedOut)
					symbolNeverFreeze.setColorFilter(null)
					snackbarText = " will never be frozen"
				}
			}

			if (showSnackbar && freezeMode != oldFreezeMode) {
				Snackbar.make(mainActivity.myCoordinatorLayout,
						txtAppName.text.toString() + snackbarText,
						Snackbar.LENGTH_LONG)
						.setAction(R.string.undo) {
							if (oldFreezeMode != null) {
								setFreezeModeTo(oldFreezeMode, changeSettings = true, showSnackbar = false)
							} else {
								Log.e(TAG, "oldFreezeMode was null")
								RuntimeException().printStackTrace()
							}
						}
						.show()
			}
		}

		/**
		 * This method defines what is done when a list item (that is, an app) is clicked.
		 * @param v The clicked view.
		 */
		override fun onClick(v: View) {
			list[adapterPosition].freeze(context)
		}

		override fun setName(name: String, highlight: String?) {
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

		fun loadImage(item: AbstractListItem) {
			imgIcon.setImageDrawable(cacheAppIcon[item.packageName])
			if (cacheAppIcon[item.packageName] == null) {
				executorServiceIcons.submit {
					item.loadNameAndIcon(this)
				}
			}
		}
	}

	internal class ViewHolderSectionHeader(private val v: View): AbstractViewHolder(v) {

		override fun setName(name: String, highlight: String?) {
			v.findViewById<TextView>(R.id.textView).text = name
		}
	}


	internal abstract class AbstractListItem {
		abstract fun loadNameAndIcon(viewHolder: ViewHolderApp)
		abstract fun freeze(context: Context)
		abstract fun refresh()
		abstract fun isMatchingSearchPattern(): Boolean
		abstract fun bindViewHolder(holder: AbstractViewHolder)

		abstract val applicationInfo: ApplicationInfo?
		abstract val packageName: String?
		abstract val text: String
		abstract val type: Int
	}

	internal inner class ListItemApp(override val packageName: String) : AbstractListItem() {
		override fun refresh() {
			_applicationInfo = null
		}

		override val type = 0
		override val applicationInfo: ApplicationInfo
			get() {
				val info = _applicationInfo
					?: packageManager.getApplicationInfo(packageName, 0)
				_applicationInfo = info
				return info
			}
		private var _applicationInfo: ApplicationInfo? = null

		override fun loadNameAndIcon(viewHolder: ViewHolderApp) {
			var first = true
			do {
				try {
					val appName = cacheAppName[packageName]
							?: applicationInfo.loadLabel(packageManager).toString()

					val icon = applicationInfo.loadIcon(packageManager)

					mainActivity.runOnUiThread {
						cacheAppName[packageName] = appName
						cacheAppIcon[packageName] = icon
						viewHolder.setName(appName, searchPattern)
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

		override fun freeze(context: Context) {
			freezeApp(packageName, context)
		}

		override val text: String
			get() = cacheAppName[packageName] ?: packageName

		var freezeMode: FreezeMode
			get() = getFreezeMode(mainActivity, packageName)
			set(value) = setFreezeMode(mainActivity, packageName, value)

		override fun isMatchingSearchPattern(): Boolean {
			if (searchPattern.isEmpty()) {
				return true// empty search pattern: Show all apps
			} else if (cacheAppName[packageName]?.toLowerCase()?.contains(searchPattern) != false) {
				return true// search in application name
			}

			return false
		}

		override fun bindViewHolder(holder: AbstractViewHolder) {
			holder.setName(text, searchPattern)

			if (holder is ViewHolderApp) {

				holder.listItem = this
				holder.loadImage(this)
				holder.setFreezeModeTo(freezeMode, changeSettings = false)

			} else {
				Log.e(TAG, "Holder of $text is not ViewHolderApp while the item at this position is ListItemApp")
				RuntimeException().printStackTrace()
			}
		}
	}

	internal class ListItemSectionHeader(override val text: String) : AbstractListItem() {

		override val type = 1

		//These functions here do nothing:
		override fun refresh() {
		}
		override fun loadNameAndIcon(viewHolder: ViewHolderApp) {
		}
		override fun freeze(context: Context) {
		}

		override fun isMatchingSearchPattern() = true

		override fun bindViewHolder(holder: AbstractViewHolder) {
			holder.setName(text, "")
		}

		override val packageName: String? get() = null
		override val applicationInfo: ApplicationInfo? get() = null

	}


	companion object {
		private const val TAG = "AppsListAdapter"
	}

}

