package superfreeze.tool.android


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.github.paolorotolo.appintro.ISlidePolicy


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
