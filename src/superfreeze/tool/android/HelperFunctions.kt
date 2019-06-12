/*
Copyright (c) 2018 Hocuri

This file is part of SuperFreezZ.

SuperFreezZ is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

SuperFreezZ is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with SuperFreezZ.  If not, see <http://www.gnu.org/licenses/>.
*/


package superfreeze.tool.android

import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter

/**
 * This function gets all indexes of item in the receiver list.
 * @param item the item to search in the list
 * @receiver The list to search through
 * @return a list of indexes
 */
fun <E> List<E>.allIndexesOf(item: E): List<Int> {
	val result = mutableListOf<Int>()
	forEachIndexed { index, currentItem ->
		if (item === currentItem) result.add(index)
	}
	return result
}

/**
 * This function test whether the receiver (that is, the thing this function is called on) is null.
 * If yes, it logs an error.
 * In any case, it returns the receiver.
 * @receiver The object to test for being null
 * @param tag The current file's/class' name. Used to for the message that is being logged in case the receiver is null.
 * @return the receiver
 */
fun <T> T?.expectNonNull(tag: String): T? {
	if (this == null) {
		logErrorAndStackTrace(tag, "A variable that should not have been null was null, proceeding anyway.")
	}

	return this
}

internal fun logErrorAndStackTrace(tag: String, msg: String) {
	Log.e(tag, msg)
	val stackTrace = getStackTrace(NullPointerException())
	Log.e(tag, stackTrace)
}

/**
 * This function gets the stack trace as a string from the throwable.
 * @param throwable The throwable
 * @return the stack trace of throwable
 */
fun getStackTrace(throwable: Throwable): String {
	val stringWriter = StringWriter()
	throwable.printStackTrace(PrintWriter(stringWriter, true))
	return stringWriter.buffer.toString()
}

