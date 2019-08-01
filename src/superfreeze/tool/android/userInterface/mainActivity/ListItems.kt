/*
 * Copyright (c) 2019 Hocuri
 *
 * This file is part of SuperFreezZ.
 *
 * SuperFreezZ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SuperFreezZ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SuperFreezZ.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package superfreeze.tool.android.userInterface.mainActivity

/**
 * This file contains classes for different kind of items that are shown in the apps list adapter.
 * AppsListAdapter contains a list of the items shown in the list.
 */

import android.content.Context
import android.content.pm.ApplicationInfo
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import superfreeze.tool.android.R
import superfreeze.tool.android.allIndexesOf
import superfreeze.tool.android.backend.FreezerService
import superfreeze.tool.android.backend.freezeApp
import superfreeze.tool.android.backend.isRunning
import superfreeze.tool.android.database.FreezeMode
import superfreeze.tool.android.database.getFreezeMode
import superfreeze.tool.android.database.setFreezeMode

internal abstract class AbstractListItem {
	abstract fun loadNameAndIcon(viewHolder: ViewHolderApp)
	abstract fun freeze(context: Context)
	abstract fun isMatchingSearchPattern(): Boolean

	abstract val applicationInfo: ApplicationInfo?
	abstract val packageName: String?
	abstract val text: String
	abstract val type: Int
}

internal class ListItemApp(override val packageName: String,
                           private val appsListAdapter: AppsListAdapter
) : AbstractListItem() {
	fun deleteAppInfo() {
		_applicationInfo = null
	}

	override val type = 0
	override val applicationInfo: ApplicationInfo
		get() {
			val info = _applicationInfo
				?: appsListAdapter.packageManager.getApplicationInfo(packageName, 0)
			_applicationInfo = info
			return info
		}
	private var _applicationInfo: ApplicationInfo? = null

	override fun loadNameAndIcon(viewHolder: ViewHolderApp) {
		var first = true
		do {
			try {
				if (appsListAdapter.cacheAppName[packageName] == null) {
					appsListAdapter.cacheAppName[packageName] = applicationInfo.loadLabel(
						appsListAdapter.packageManager
					).toString()
				}

				if (appsListAdapter.cacheAppIcon[packageName] == null) {
					appsListAdapter.cacheAppIcon[packageName] = applicationInfo.loadIcon(
						appsListAdapter.packageManager
					)
				}

				appsListAdapter.mainActivity.runOnUiThread {
					viewHolder.refresh()
				}


			} catch (ex: OutOfMemoryError) {
				appsListAdapter.cacheAppIcon.clear()
				//cacheAppName.clear()
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
			Snackbar.make(
				appsListAdapter.mainActivity.myCoordinatorLayout,
				R.string.already_frozen,
				Snackbar.LENGTH_SHORT
			).show()
		} else {
			freezeApp(packageName, context)
		}
	}

	override val text: String
		get() = appsListAdapter.cacheAppName[packageName] ?: packageName

	var freezeMode: FreezeMode
		get() = getFreezeMode(
			appsListAdapter.mainActivity,
			packageName
		)
		set(value) = setFreezeMode(
			appsListAdapter.mainActivity,
			packageName,
			value
		)

	override fun isMatchingSearchPattern(): Boolean {
		if (appsListAdapter.searchPattern.isEmpty()) {
			return true// empty search pattern: Show all apps
		} else if (text.toLowerCase().contains(appsListAdapter.searchPattern)) {
			return true// search in application name
		}

		return false
	}

	fun isPendingFreeze(): Boolean {
		return superfreeze.tool.android.backend.isPendingFreeze(
			freezeMode,
			applicationInfo,
			appsListAdapter.usageStatsMap?.get(packageName),
			appsListAdapter.mainActivity
		)
	}

	internal fun setFreezeModeTo(
		freezeMode: FreezeMode,
		changeSettings: Boolean,
		showSnackbar: Boolean,
		viewHolder: ViewHolderApp
	) {

		val oldFreezeMode = this.freezeMode
		val wasPendingFreeze = isPendingFreeze()

		if (changeSettings) {
			this.freezeMode = freezeMode
		}

		viewHolder.refresh()

		if (showSnackbar && freezeMode != oldFreezeMode) {
			Snackbar.make(
				appsListAdapter.mainActivity.myCoordinatorLayout,
				R.string.changed_freeze_mode,
				Snackbar.LENGTH_LONG
			)
				.setAction(R.string.undo) {
					setFreezeModeTo(oldFreezeMode, changeSettings = true, showSnackbar = false, viewHolder = viewHolder)
				}
				.show()
		}

		if (changeSettings) {
			refreshListsAfterFreezeModeChange(wasPendingFreeze, viewHolder)
		}
	}

	private fun refreshListsAfterFreezeModeChange(
		wasPendingFreeze: Boolean,
		viewHolder: ViewHolderApp
	) {
		if (appsListAdapter.searchPattern == "") {
			//Refresh the lists and notify the system that this item was potentially removed or added somewhere:

			val isPendingFreeze = isPendingFreeze()

			if ((!wasPendingFreeze) && isPendingFreeze) {
				appsListAdapter.refreshBothLists()
				// The first index of the listItem is the entry in the "PENDING FREEZE" section
				appsListAdapter.notifyItemInserted(appsListAdapter.list.indexOf(this))
			} else if (wasPendingFreeze && (!isPendingFreeze)) {
				val oldIndex = appsListAdapter.list.indexOf(this)
				appsListAdapter.refreshBothLists()
				appsListAdapter.notifyItemRemoved(oldIndex)
			}

			// Also refresh other list entries by getting all indexes of the current item, filtering
			// out this holder's own index (=adapterPosition) and notifying it changed:
			// (This is necessary because sometimes one item has multiple viewholders when it is shown at
			// PENDING FREEZE and ALL APPS.)
			appsListAdapter.list.allIndexesOf(this as AbstractListItem)
				.filter { it != viewHolder.adapterPosition }.forEach {
					appsListAdapter.notifyItemChanged(it)
				}

			//The "PENDING FREEZE" section header might have changed:
			appsListAdapter.notifyItemChanged(0)

		} else {
			appsListAdapter.refreshBothLists()
			// The user is searching, so nothing in the current list changes => we do not need to
			// call notifyItemChanged-or-whatever()
		}
	}
}

internal class ListItemSectionHeader(override val text: String) : AbstractListItem() {

	override val type = 1

	//These functions here do nothing:
	override fun loadNameAndIcon(viewHolder: ViewHolderApp) {
	}

	override fun freeze(context: Context) {
	}

	override fun isMatchingSearchPattern() = true

	override val packageName: String? get() = null
	override val applicationInfo: ApplicationInfo? get() = null

}