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

/**
 * The freeze mode of an app: ALWAYS_FREEZE, NEVER_FREEZE or FREEZE_WHEN_INACTIVE
 */
enum class FreezeMode {

	/**
	 * This app will always be frozen if it is running, regardless of when it was used last.
	 */
	ALWAYS_FREEZE,

	/**
	 * This app will never be frozen, even if it has been running in background for whatever time.
	 */
	NEVER_FREEZE,

	/**
	 * This app will be frozen if it was not used for a specific time but is running in background.
	 */
	FREEZE_WHEN_INACTIVE
}