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

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager

import com.google.android.material.tabs.TabLayout
import superfreeze.tool.android.R

@SuppressLint("Registered")
class ModesTabFragment : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_intro_modes)

        val toolbar = findViewById<Toolbar>(R.id.modes_toolbar)
        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
        val viewPager = findViewById<ViewPager>(R.id.pager)

        if (toolbar != null) {
            setSupportActionBar(toolbar)
        }

        viewPager.adapter = SectionPagerAdapter(supportFragmentManager)
        tabLayout.setupWithViewPager(viewPager)
    }

    inner class SectionPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            /* when (position) {
                 0 -> return fr()
                 1 -> return SecondTabFragment()
                 else -> return SecondTabFragment()
             }*/
            return Fragment()
        }

        override fun getCount(): Int {
            return 2
        }

        override fun getPageTitle(position: Int): CharSequence? {
            when (position) {
                0 -> return "First Tab"
                1 -> return "Second Tab"
            }
            return "Second Tab"
        }
    }

}