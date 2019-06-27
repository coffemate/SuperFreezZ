/*
 * Copyright (c) 2019 Hocuri
 * Copyright (c) 2019 Robin Naumann
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

package superfreeze.tool.android.userInterface.intro


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.paolorotolo.appintro.ISlidePolicy
import superfreeze.tool.android.R

/**
 * Fragment for the "Welcome to SuperFreezZ" intro page
 */
class IntroFragment : Fragment(), ISlidePolicy {


    override fun isPolicyRespected(): Boolean {
        return true
    }


    override fun onUserIllegallyRequestedNextPage() {
        // Toast.makeText(context ?: activity, "Please select 'Yes' or 'No'", Toast.LENGTH_LONG).show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_intro, container, false)
    }


}
