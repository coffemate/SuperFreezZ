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


package superfreeze.tool.android.userInterface

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.github.paolorotolo.appintro.ISlidePolicy
import superfreeze.tool.android.R
import superfreeze.tool.android.backend.FreezerService


/**
 * Shows a screen to let the user choose whether to use the accessibility service or not.
 */
class AccessibilityServiceChooserFragment : Fragment(), ISlidePolicy {

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
	                          savedInstanceState: Bundle?): View? {

		// Inflate the layout for this fragment
		val layout = inflater.inflate(R.layout.fragment_accessibility_service_chooser, container, false)
		layout.findViewById<View>(R.id.accessibilityYes).setOnClickListener {
			showUsagestatsDialog()
		}
		layout.findViewById<View>(R.id.accessibilityNo).setOnClickListener {
			IntroActivity.instance.done()
		}

		return layout
	}

	override fun onResume() {
		super.onResume()
		if (FreezerService.isEnabled) {
			IntroActivity.instance.done()
		}
	}

	private fun showUsagestatsDialog() {
		AlertDialog.Builder(context, android.R.style.Theme_Material_Light_Dialog)
				.setTitle("Accessibility Service")
				.setMessage("SuperFreezZ needs the accessibility service in order to automate freezing.\n\nPlease select SuperFreezZ, then enable accessibility service.")
				.setPositiveButton(getString(R.string.enable)) { _, _ ->
					val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
					startActivity(intent)
				}

				.setIcon(R.mipmap.ic_launcher)
				.setCancelable(false)
				.show()
	}

	override fun isPolicyRespected(): Boolean {
		return false // The user is supposed to select yes or no, not to press "Done"
	}

	override fun onUserIllegallyRequestedNextPage() {
		Toast.makeText(context, "Please select 'Yes' or 'No'", Toast.LENGTH_LONG).show()
	}
}


