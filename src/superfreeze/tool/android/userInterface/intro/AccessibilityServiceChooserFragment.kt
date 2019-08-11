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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.github.paolorotolo.appintro.ISlidePolicy
import superfreeze.tool.android.R
import superfreeze.tool.android.expectNonNull
import superfreeze.tool.android.userInterface.showAccessibilityDialog


/**
 * Shows a screen to let the user choose whether to use the accessibility service or not. Used in [IntroActivity].
 */
class AccessibilityServiceChooserFragment : Fragment(), ISlidePolicy {

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        val layout = inflater.inflate(R.layout.fragment_accessibility_service_chooser, container, false)
        layout.findViewById<View>(R.id.accessibilityYes).setOnClickListener {
            showAccessibilityDialog(context ?: activity!!)
        }
        layout.findViewById<View>(R.id.accessibilityNo).setOnClickListener {
            Log.i(TAG, "Done on click")
            done()
        }

        return layout
    }


    private fun done() {
        (this.activity as? IntroActivity)
                .expectNonNull(TAG)
                ?.done()
    }

    override fun isPolicyRespected(): Boolean {
        return false // The user is supposed to select yes or no, not to press "Done"
    }

    override fun onUserIllegallyRequestedNextPage() {
        Toast.makeText(context ?: activity, "Please select 'Yes' or 'No'", Toast.LENGTH_LONG).show()
    }
}

private const val TAG = "SF-AccessibilitySCFragm"