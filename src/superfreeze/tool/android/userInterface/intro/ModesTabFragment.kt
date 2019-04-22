

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager

import com.google.android.material.tabs.TabLayout
import superfreeze.tool.android.R

class ModesTabFragment : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_intro_modes)

        val toolbar = findViewById(R.id.toolbar) as Toolbar
        val tabLayout = findViewById(R.id.tab_layout) as TabLayout
        val viewPager = findViewById(R.id.pager) as ViewPager

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
                else -> return "Second Tab"
            }
        }
    }

}