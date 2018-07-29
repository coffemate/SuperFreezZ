/*
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

import android.app.Activity
import android.view.ActionMode
import android.view.MenuItem
import android.view.Menu


/**
 * If the user long-presses an app, then a special menu is shown at the top and additional app can be selected.
 * This file contains helper functions for this.
 */
class SelectItemsHelper{
	private var isMultiSelect = false

	private var multiselectList = ArrayList<Int>()
	private var mActionMode: ActionMode? = null

	private val mActionModeCallback = object : ActionMode.Callback {

		override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
			// Inflate a menu resource providing context menu items
			val inflater = mode.menuInflater
			inflater.inflate(R.menu.menu_multi_select, menu)
			context_menu = menu
			return true
		}

		override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
			return false // Return false if nothing is done
		}

		override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
			return false
		}

		override fun onDestroyActionMode(mode: ActionMode) {

		}
	}

	fun toggleItemSelect(position: Int, activity: Activity) {
		if (!isMultiSelect) {
			isMultiSelect = true

			if (mActionMode == null) {
				mActionMode = activity.startActionMode(mActionModeCallback);
			}
		}

		if (mActionMode != null) {
			if (multiselectList.contains(position))
				multiselectList.remove(position)
			else
				multiselectList.add(position)


			if (multiselectList.isEmpty())
				mActionMode!!.setTitle("");
			else
				mActionMode!!.setTitle("" + multiselectList.size());


			refreshAdapter()
		}

	}
}
