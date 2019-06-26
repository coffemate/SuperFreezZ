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

package superfreeze.tool.android

import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test

class AsyncDelegatedTest {

	@Test
	fun getValue() {
		val b by AsyncDelegated {
			"hi"
		}
		Thread.sleep(100)
		Assert.assertEquals("hi", b)
		Assert.assertEquals("hi", b)
	}

	@Test
	fun getValue2() {
		val b by AsyncDelegated {
			Thread.sleep(100)
			"hi"
		}
		Assert.assertEquals("hi", b)
		Assert.assertEquals("hi", b)
	}

	@Test
	fun getValue3() {
		val b by AsyncDelegated {
			delay(100)
			"hi"
		}
		Assert.assertEquals("hi", b)
		Assert.assertEquals("hi", b)
	}

	@Test
	fun getValue4() = runBlocking {
		(0..1000).map {
			launch {
				val b by AsyncDelegated {
					delay(10)
					"hi"
				}
				repeat(1000) {
					Assert.assertEquals("hi", b)
				}
			}
		}.joinAll()
	}
}
