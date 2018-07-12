package superfreeze.tool.android

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity

/**
 * This is actually not an activity, it just returns a shortcut some launcher can use.
 */
class CreateShortcutActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
		super.onCreate(savedInstanceState, persistentState)

		val intent = intent
		val action = intent.action

		if (Intent.ACTION_CREATE_SHORTCUT == action) {
			setupShortcut()
			finish()
			return
		}

		freeze()
	}

	private fun setupShortcut() {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
}