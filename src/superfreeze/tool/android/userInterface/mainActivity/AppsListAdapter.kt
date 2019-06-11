/*
The MIT License (MIT)

Copyright (c) 2015 axxapy
Copyright (c) 2018 Hocuri

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package superfreeze.tool.android.userInterface.mainActivity

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
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
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import superfreeze.tool.android.R
import superfreeze.tool.android.allIndexesOf
import superfreeze.tool.android.backend.*
import superfreeze.tool.android.database.FreezeMode
import superfreeze.tool.android.database.getFreezeMode
import superfreeze.tool.android.database.setFreezeMode
import superfreeze.tool.android.database.usageStatsAvailable
import superfreeze.tool.android.expectNonNull
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


/**
 * This class is responsible for viewing the list of installed apps.
 */
internal class AppsListAdapter internal constructor(
	private val mainActivity: MainActivity,
	internal var comparator: kotlin.Comparator<ListItemApp>
) : RecyclerView.Adapter<AppsListAdapter.AbstractViewHolder>() {

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


	private val executorServiceIcons = Executors.newFixedThreadPool(3, tFactory)
	private val packageManager: PackageManager = mainActivity.packageManager

	private val cacheAppName = Collections.synchronizedMap(LinkedHashMap<String, String>(10, 1.5f, true))
	private val cacheAppIcon = Collections.synchronizedMap(LinkedHashMap<String, Drawable>(10, 1.5f, true))

	var searchPattern: String = ""
		set(value) {
			field = value.toLowerCase()
			refreshList()
			notifyDataSetChanged()
		}


	override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): AbstractViewHolder {
		return if (i == 0) {
			ViewHolderApp(
				LayoutInflater.from(viewGroup.context).inflate(R.layout.list_item, viewGroup, false),
				viewGroup.context
			)
		} else {
			ViewHolderSectionHeader(
					LayoutInflater.from(viewGroup.context).inflate(R.layout.list_section_header, viewGroup, false)
			)
		}
	}

	override fun onBindViewHolder(holder: AbstractViewHolder, i: Int) {
		holder.bindTo(list[i])
	}

	override fun getItemCount(): Int {
		return list.size
	}

	override fun getItemViewType(position: Int): Int {
		return list[position].type
	}


	internal fun setAndLoadItems(packages: List<PackageInfo>) {
		appsList.clear()
		appsList.addAll(packages.map {
			ListItemApp(it.packageName)
		})


		@Suppress("UNCHECKED_CAST")
		loadAllNames(appsList) {
			mainActivity.runOnUiThread {
				sortList()
				refreshBothLists()
				notifyDataSetChanged()
				mainActivity.hideProgressBar()
			}
		}

		refreshBothLists()
		notifyDataSetChanged()
	}

	internal fun sortList() {
		Collections.sort(appsList, comparator)
	}


	internal fun refresh() {
		for (app in appsList) {
			app.refresh()
		}

		//We need to test whether the applications are still installed and remove those that are not.
		//Apparently, there is no better way for this than trying to access the applicationInfo.
		appsList.removeAll {
			try {
				it.applicationInfo
				false
			} catch (e: PackageManager.NameNotFoundException) {
				true
			}
		}

		refreshBothLists()
		notifyDataSetChanged()
	}

	internal fun trimMemory() {
		cacheAppIcon.clear()
	}


	// "Both lists" means originalList and list:
	@Suppress("UNCHECKED_CAST")
	private fun refreshBothLists() {

		val listPendingFreeze =
			appsList.filter {
				it.isPendingFreeze()
			}

		originalList =
				if (listPendingFreeze.isEmpty()) {
					listOf(ListItemSectionHeader(mainActivity.getString(R.string.no_apps_pending_freeze)))
				} else {
					listOf(ListItemSectionHeader(mainActivity.getString(R.string.pending_freeze))) +
							listPendingFreeze
				} +

				ListItemSectionHeader(mainActivity.getString(R.string.all_apps)) +
				appsList

		refreshList()
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
								.asSequence()
								.filter { it.isMatchingSearchPattern() }
								.partition {
									cacheAppName[it.packageName]?.toLowerCase()?.startsWith(searchPattern) != false
								}
					importantApps + otherApps

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
		}.start()
	}


	internal abstract class AbstractViewHolder(v: View) : RecyclerView.ViewHolder(v) {
		abstract fun setName(name: String, highlight: String?)
		abstract fun bindTo(item: AbstractListItem)
	}

	internal inner class ViewHolderApp(v: View, private val context: Context) : AbstractViewHolder(v), OnClickListener {

		val imgIcon: ImageView = v.findViewById(R.id.imgIcon)
		
		private val txtAppName: TextView = v.findViewById(R.id.txtAppName)
		private val txtExplanation: TextView = v.findViewById(R.id.txtExplanation)

		private val modeSymbols: Map<FreezeMode, ImageView> = mapOf(
			FreezeMode.ALWAYS to v.findViewById(R.id.imageAlwaysFreeze),
			FreezeMode.NEVER to v.findViewById(R.id.imageNeverFreeze),
			FreezeMode.WHEN_INACTIVE to v.findViewById(R.id.imageFreezeWhenInactive)
		)

		private lateinit var listItem: ListItemApp

		init {
			v.setOnClickListener(this)

			if (!usageStatsAvailable) {
				modeSymbols[FreezeMode.WHEN_INACTIVE]!!.visibility = View.GONE
				// Hide as without usagestats we can not know whether an app is 'inactive'
			}

			// Set what happens when one of the mode symbols is clicked
			modeSymbols.forEach { (mode, view) ->
				view.setOnClickListener {
					listItem.setFreezeModeTo(
						mode,
						changeSettings = true,
						showSnackbar = true,
						viewHolder = this
					)
				}
			}

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				modeSymbols[FreezeMode.ALWAYS]!!.tooltipText = context.getString(R.string.always_freeze_this_app)
				modeSymbols[FreezeMode.WHEN_INACTIVE]!!.tooltipText = context.getString(R.string.freeze_this_app_if_it_has_not_been_used_for_a_longer_time)
				modeSymbols[FreezeMode.NEVER]!!.tooltipText = context.getString(R.string.do_never_freeze_this_app)
			}
		}

		private fun setButtonColours(freezeMode: FreezeMode) {
			val colorGreyedOut = ContextCompat.getColor(context, R.color.button_greyed_out)
			modeSymbols.forEach { (mode, view) ->
				if (mode == freezeMode)
					view.setColorFilter(colorGreyedOut)
				else
					view.colorFilter = null
			}
		}

		private fun refreshExplanation(freezeMode: FreezeMode) {
			txtExplanation.text = getPendingFreezeExplanation(
				freezeMode,
				listItem.applicationInfo,
				mainActivity.usageStatsMap?.get(listItem.packageName),
				context
			)
		}

		/**
		 * This method defines what is done when a list item (that is, an app) is clicked.
		 * @param v The clicked view.
		 */
		override fun onClick(v: View) {
			list.getOrNull(adapterPosition).expectNonNull(TAG)?.freeze(context)
		}

		override fun setName(name: String, highlight: String?) {
			txtAppName.text = name

			if (highlight == null || highlight.isEmpty()) return // nothing to highlight

			val valueLower = name.toLowerCase()
			var offset = 0
			var index = valueLower.indexOf(highlight, offset)
			while (index >= 0 && offset < valueLower.length) {
				val span = SpannableString(txtAppName.text)
				span.setSpan(
					ForegroundColorSpan(Color.BLUE),
					index,
					index + highlight.length,
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
				)
				txtAppName.text = span
				offset += index + highlight.length
				index = valueLower.indexOf(highlight, offset)
			}
		}

		override fun bindTo(item: AbstractListItem) {
			setName(item.text, searchPattern)
			listItem = item as ListItemApp
			loadImage(item)
			item.setFreezeModeTo(item.freezeMode, changeSettings = false, showSnackbar = false, viewHolder = this)
		}

		private fun loadImage(item: AbstractListItem) {
			imgIcon.setImageDrawable(cacheAppIcon[item.packageName])
			if (cacheAppIcon[item.packageName] == null) {
				executorServiceIcons.submit {
					item.loadNameAndIcon(this)
				}
			}
		}

		fun notifyFreezeModeChanged() {
			setButtonColours(listItem.freezeMode)
			refreshExplanation(listItem.freezeMode)
		}
	}

	internal class ViewHolderSectionHeader(v: View) : AbstractViewHolder(v) {
		private val textView = v.findViewById<TextView>(R.id.textView)

		override fun bindTo(item: AbstractListItem) {
			setName(item.text.toUpperCase(), "")
		}

		override fun setName(name: String, highlight: String?) {
			textView.text = name
		}
	}


	internal abstract class AbstractListItem {
		abstract fun loadNameAndIcon(viewHolder: ViewHolderApp)
		abstract fun freeze(context: Context)
		abstract fun refresh()
		abstract fun isMatchingSearchPattern(): Boolean

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
			// If the app is already frozen and the freezer service is not enabled, we can just show
			// the settings page to the user, as if we were freezing it.
			if (FreezerService.isEnabled && !isRunning(applicationInfo)) {
				Snackbar.make(mainActivity.myCoordinatorLayout, R.string.already_frozen, Snackbar.LENGTH_SHORT).show()
			} else {
				freezeApp(packageName, context)
			}
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

		fun isPendingFreeze(): Boolean {
			return isPendingFreeze(freezeMode, applicationInfo, mainActivity.usageStatsMap?.get(packageName))
		}

		internal fun setFreezeModeTo(
			freezeMode: FreezeMode,
			changeSettings: Boolean,
			showSnackbar: Boolean,
			viewHolder: ViewHolderApp
		) {

			val oldFreezeMode = freezeMode
			val wasPendingFreeze = isPendingFreeze()

			if (changeSettings) {
				this.freezeMode = freezeMode
			}

			viewHolder.notifyFreezeModeChanged()

			if (showSnackbar && freezeMode != oldFreezeMode) {
				Snackbar.make(
					mainActivity.myCoordinatorLayout,
					"Changed freeze mode",
					Snackbar.LENGTH_LONG
				)
					.setAction(R.string.undo) {
						setFreezeModeTo(oldFreezeMode, changeSettings = true, showSnackbar = false, viewHolder = viewHolder)
					}
					.show()
			}

			fun refreshListsAfterFreezeModeChange() {
				if (changeSettings && searchPattern == "") {
					//Refresh the lists and notify the system that this item was potentially removed or added somewhere:

					val isPendingFreeze = isPendingFreeze()

					if ((!wasPendingFreeze) && isPendingFreeze) {
						refreshBothLists()
						// The first index of the listItem is the entry in the "PENDING FREEZE" section
						notifyItemInserted(list.indexOf(this))
					} else if (wasPendingFreeze && (!isPendingFreeze)) {
						val oldIndex = list.indexOf(this)
						refreshBothLists()
						notifyItemRemoved(oldIndex)
					}

					// Also refresh other list entries by getting all indexes of the current item, filtering
					// out this holder's own index (=adapterPosition) and notifying it changed:
					// (This is necessary because sometimes one item has multiple viewholders when it is shown at
					// PENDING FREEZE and ALL APPS.)
					list.allIndexesOf(this as AbstractListItem).filter { it != viewHolder.adapterPosition }.forEach {
						notifyItemChanged(it)
					}

					notifyItemChanged(0) //The "PENDING FREEZE" section header might have changed

				} else if (changeSettings) {
					refreshBothLists()
					// The user is searching, so nothing in the current list changes => we do not need to call notifyItemChanged-or-whatever()
				}
			}

			refreshListsAfterFreezeModeChange()
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

		override val packageName: String? get() = null
		override val applicationInfo: ApplicationInfo? get() = null

	}


	companion object {
		private const val TAG = "AppsListAdapter"
	}

}
