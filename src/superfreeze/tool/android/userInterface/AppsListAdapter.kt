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
import superfreeze.tool.android.*
import superfreeze.tool.android.backend.freezeApp
import superfreeze.tool.android.backend.isPendingFreeze
import superfreeze.tool.android.database.getFreezeMode
import superfreeze.tool.android.database.setFreezeMode
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit


/**
 * This class is responsible for viewing the list of installed apps.
 */
internal class AppsListAdapter internal constructor(private val mainActivity: MainActivity) : RecyclerView.Adapter<AppsListAdapter.AbstractViewHolder>() {
	private val tFactory = ThreadFactory { r ->
		val t = Thread(r)
		t.isDaemon = true
		t
	}

	/**
	 * This list contains all user apps and the section headers.
	 */
	private val listOriginal = ArrayList<AbstractListItem>()

	/**
	 * This list contains the items that are shown to the user, including section headers.
	 */
	private val list = ArrayList<AbstractListItem>()

	var listPendingFreeze: List<String>? = null
		private set

	private val executorServiceIcons = Executors.newFixedThreadPool(3, tFactory)
	private val packageManager: PackageManager = mainActivity.packageManager

	private val cacheAppName = Collections.synchronizedMap(LinkedHashMap<String, String>(10, 1.5f, true))
	private val cacheAppIcon = Collections.synchronizedMap(LinkedHashMap<String, Drawable>(10, 1.5f, true))

	var searchPattern: String = ""
		set(value) {
			field = value.toLowerCase()
			filterList()
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




	internal fun filterList() {
		list.clear()
		list.addAll(listOriginal.filter { it.isToBeShown() })

		notifyDataSetChanged()
	}

	internal fun setAndLoadItems(packages: List<PackageInfo>, usageStatsMap: Map<String, UsageStats>?) {
		listOriginal.clear()
		listOriginal.addAll(packages.map {
			ListItemApp(it.packageName)
		})

		sortList(usageStatsMap)

		loadAllNames(listOriginal.filter { it is ListItemApp } as List<ListItemApp>) {
			mainActivity.runOnUiThread {
				notifyDataSetChanged()
				mainActivity.hideProgressBar()
			}
		}
	}

	internal fun refresh(usageStatsMap: Map<String, UsageStats>?) {
		for (app in listOriginal) {
			app.refresh()
		}
		sortList(usageStatsMap)
	}



	@Suppress("UNCHECKED_CAST")
	private fun sortList(usageStatsMap: Map<String, UsageStats>?) {
		val (listPendingFreeze, listNotPendingFreeze) = (listOriginal
				.filter { it is ListItemApp } as List<ListItemApp>)
				.partition { isPendingFreeze(it.freezeMode, it.applicationInfo, usageStatsMap?.get(it.packageName)) }

		listOriginal.clear()

		if (!listPendingFreeze.isEmpty()) {
			addSectionHeader("PENDING FREEZE")
			for (info in listPendingFreeze) {
				addItem(info)
			}
		}

		if (!listNotPendingFreeze.isEmpty()) {
			addSectionHeader("OTHERS")
			for (info in listNotPendingFreeze) {
				addItem(info)
			}
		}
		notifyDataSetChanged()

		this.listPendingFreeze = listPendingFreeze.map{ it.packageName }
	}

	private fun addSectionHeader(title: String) {
		addItem(ListItemSectionHeader(title))
	}

	private fun addItem(item: AbstractListItem) {
		listOriginal.add(item)
		if (item.isToBeShown()) {
			list.add(item)
		}
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
		}
	}



	internal abstract class AbstractViewHolder(v: View) : RecyclerView.ViewHolder(v) {
		abstract fun setName(name: String, highlight: String?)
	}

	internal inner class ViewHolderApp(v: View, private val context: Context, freezeMode: FreezeMode) : AbstractViewHolder(v), OnClickListener, View.OnLongClickListener {

		private val txtAppName: TextView = v.findViewById(R.id.txtAppName)
		val imgIcon: ImageView = v.findViewById(R.id.imgIcon)
		private val symbolAlwaysFreeze = v.findViewById<ImageView>(R.id.imageAlwaysFreeze)
		private val symbolFreezeWhenInactive = v.findViewById<ImageView>(R.id.imageFreezeWhenInactive)
		private val symbolNeverFreeze = v.findViewById<ImageView>(R.id.imageNeverFreeze)

		init {
			v.setOnClickListener(this)
			v.setOnLongClickListener(this)

			symbolAlwaysFreeze.setOnClickListener {
				setFreezeModeTo(FreezeMode.ALWAYS_FREEZE)
			}

			symbolFreezeWhenInactive.setOnClickListener {
				setFreezeModeTo(FreezeMode.FREEZE_WHEN_INACTIVE)
			}

			symbolNeverFreeze.setOnClickListener {
				setFreezeModeTo(FreezeMode.NEVER_FREEZE)
			}

			setFreezeModeTo(freezeMode)
		}

		internal fun setFreezeModeTo(freezeMode: FreezeMode, changeSettings: Boolean = true) {
			val colorGreyedOut = ContextCompat.getColor(context, R.color.button_greyed_out)

			if (changeSettings){
				val listItem = list.getOrNull(adapterPosition) as ListItemApp?
				listItem?.freezeMode = freezeMode
			}

			when(freezeMode) {
				FreezeMode.ALWAYS_FREEZE ->  {
					symbolAlwaysFreeze.setColorFilter(null)
					symbolFreezeWhenInactive.setColorFilter(colorGreyedOut)
					symbolNeverFreeze.setColorFilter(colorGreyedOut)
				}
				FreezeMode.FREEZE_WHEN_INACTIVE -> {
					symbolAlwaysFreeze.setColorFilter(colorGreyedOut)
					symbolFreezeWhenInactive.setColorFilter(null)
					symbolNeverFreeze.setColorFilter(colorGreyedOut)
				}
				FreezeMode.NEVER_FREEZE -> {
					symbolAlwaysFreeze.setColorFilter(colorGreyedOut)
					symbolFreezeWhenInactive.setColorFilter(colorGreyedOut)
					symbolNeverFreeze.setColorFilter(null)
				}
			}
		}

		/**
		 * This method defines what is done when a list item (that is, an app) is clicked.
		 * @param v The clicked view.
		 */
		override fun onClick(v: View) {
			list[adapterPosition].freeze(context)
		}
		override fun onLongClick(v: View?): Boolean {

			return true
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
		abstract fun isToBeShown(): Boolean
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

		override fun isToBeShown(): Boolean {
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

		override fun isToBeShown() = true

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