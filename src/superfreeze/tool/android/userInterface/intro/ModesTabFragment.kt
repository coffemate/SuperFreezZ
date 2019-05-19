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

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
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