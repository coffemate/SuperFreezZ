package superfreeze.tool.android.userInterface.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TabHost
import androidx.fragment.app.Fragment
import com.github.paolorotolo.appintro.ISlidePolicy
import superfreeze.tool.android.R


class IntroModesFragment : Fragment(), ISlidePolicy {

    private var allWatched = false
    private var _host: TabHost? = null

    override fun isPolicyRespected(): Boolean {
        return allWatched
    }

    override fun onUserIllegallyRequestedNextPage() {

        val tabs = _host
        if (tabs != null) {


            if (tabs.currentTab < 2) {
                tabs.currentTab += 1
            }

            if (tabs.currentTab == 2) {
                allWatched = true
            }


        } else {
            System.err.println("IntroModesFragment | TabHost is null. " +
                    "User interactions can't be handled")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val layout = inflater.inflate(R.layout.fragment_intro_modes, container, false)

        val host = layout.findViewById(R.id.tab_host) as TabHost
        host.setup()

        val layoutParams = LinearLayout.LayoutParams(200, 200, 1.0F)

        val icon1 = ImageView(this.context)
        icon1.setPadding(0, 50, 0, 50)
        icon1.setImageDrawable(resources.getDrawable(R.drawable.symbol_always_freeze))
        icon1.layoutParams = layoutParams

        val icon2 = ImageView(this.context)
        icon2.setPadding(0, 50, 0, 50)
        icon2.setImageDrawable(resources.getDrawable(R.drawable.symbol_freeze_when_inactive))
        icon2.layoutParams = layoutParams

        val icon3 = ImageView(this.context)
        icon3.setPadding(0, 50, 0, 50)
        icon3.setImageDrawable(resources.getDrawable(R.drawable.symbol_never_freeze))
        icon3.layoutParams = layoutParams


        val spec1 = host.newTabSpec("mode_1")
        spec1.setContent(R.id.tab_one_container)
        spec1.setIndicator(icon1)
        host.addTab(spec1)

        val spec2 = host.newTabSpec("mode_2")
        spec2.setContent(R.id.tab_two_container)
        spec2.setIndicator(icon2)
        host.addTab(spec2)

        val spec3 = host.newTabSpec("mode_3")
        spec3.setContent(R.id.tab_three_container)
        spec3.setIndicator(icon3)
        host.addTab(spec3)

        _host = host
        return layout
        // Inflate the layout for this fragment

    }

}
