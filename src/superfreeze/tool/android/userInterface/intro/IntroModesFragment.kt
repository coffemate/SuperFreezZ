package superfreeze.tool.android


import android.app.TabActivity
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TabHost
import androidx.fragment.app.Fragment
import com.github.paolorotolo.appintro.ISlidePolicy


class IntroModesFragment : Fragment(), ISlidePolicy {

    var allWatched = false
    var _host: TabHost? = null

    override fun isPolicyRespected(): Boolean {
        return allWatched
    }

    override fun onUserIllegallyRequestedNextPage() {

        val tabs = _host
        if (tabs != null) {

            val pageNr = tabs.currentTab
            if(pageNr < 2){
                tabs.currentTab += 1
            }

            if(pageNr == 2){
                allWatched = true
            }


        } else {
            System.err.println("IntroModesFragment | TabHost is null. " +
                    "User interactions can't be handled")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


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


        var spec1 = host.newTabSpec("mode_1")
        spec1.setContent(R.id.tab_one_container)
        spec1.setIndicator(icon1)
        host.addTab(spec1)

        var spec2 = host.newTabSpec("mode_2")
        spec2.setContent(R.id.tab_two_container)
        spec2.setIndicator(icon2)
        host.addTab(spec2)

        var spec3 = host.newTabSpec("mode_3")
        spec3.setContent(R.id.tab_three_container)
        spec3.setIndicator(icon3)
        host.addTab(spec3)

        _host = host;
        return layout;
        // Inflate the layout for this fragment

    }


    fun scaleBitmap(width: Int, bitmap: Bitmap?): Bitmap? {

        if (bitmap == null) {
            System.out.println("isNull")
            return null
        }

        var newHeight = Math.floor(bitmap.height as Double * (width as Double / bitmap.width as Double)).toInt()

        return Bitmap.createScaledBitmap(bitmap, width, newHeight, true)
    }

}
