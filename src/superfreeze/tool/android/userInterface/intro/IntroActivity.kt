/*
Copyright (c) 2018,2019 Hocuri
Copyright (c) 2019 Robin Naumann

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


package superfreeze.tool.android.userInterface.intro

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.github.paolorotolo.appintro.AppIntro
import superfreeze.tool.android.BuildConfig
import superfreeze.tool.android.database.prefIntroAlreadyShown

/**
 * Shows the intro slides on the very first startup
 */
class IntroActivity : AppIntro() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val actionBar = supportActionBar
		actionBar?.hide()

		val action = intent.action
		if (SHOW_ACCESSIBILITY_SERVICE_CHOOSER == action) {

			addSlide(AccessibilityServiceChooserFragment())

		} else {

			addSlide(IntroFragment())
			addSlide(IntroModesFragment())
			addSlide(AccessibilityServiceChooserFragment())
		}

		showSkipButton(false)
		setDoneText("")

	}

	//If the user presses the back button it tends to break the AppIntro route logic
	override fun onBackPressed() {
		//do nothing
	}

	override fun onDonePressed(currentFragment: Fragment?) {
		super.onDonePressed(currentFragment)
		done()
	}

	fun done() {
		prefIntroAlreadyShown = false
		finish()
	}

	companion object {

		const val SHOW_ACCESSIBILITY_SERVICE_CHOOSER =
			BuildConfig.APPLICATION_ID + "SHOW_ACCESSIBILITY_SERVICE_CHOOSER"
	}

}
