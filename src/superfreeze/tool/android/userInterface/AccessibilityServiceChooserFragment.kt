package superfreeze.tool.android.userInterface

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import superfreeze.tool.android.R
import superfreeze.tool.android.backend.FreezerService

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [AccessibilityServiceChooserFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [AccessibilityServiceChooserFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class AccessibilityServiceChooserFragment : Fragment() {
	//private var listener: OnFragmentInteractionListener? = null


	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
	                          savedInstanceState: Bundle?): View? {

		// Inflate the layout for this fragment
		val layout = inflater.inflate(R.layout.fragment_accessibility_service_chooser, container, false)
		layout.findViewById<LinearLayout>(R.id.accessibilityÝes).setOnClickListener {
			showUsagestatsDialog()
		}
		layout.findViewById<LinearLayout>(R.id.accessibilityNo).setOnClickListener {
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
				.setNegativeButton("Cancel") { _, _->
					//do nothing
				}
				.setIcon(R.mipmap.ic_launcher)
				.setCancelable(false)
				.show()
	}
}


