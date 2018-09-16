package superfreeze.tool.android.backend

import superfreeze.tool.android.userInterface.AppsListAdapter

internal fun <E> List<E>.allIndexesOf(item: E): List<Int> {
	val result = mutableListOf<Int>()
	forEachIndexed {
		index, currentItem ->
		if (item === currentItem) result.add(index)
	}
	return result
}