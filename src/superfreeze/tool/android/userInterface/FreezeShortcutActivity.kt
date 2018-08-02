package superfreeze.tool.android.userInterface

import android.app.Activity
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import superfreeze.tool.android.R
import superfreeze.tool.android.backend.freezeAll
import superfreeze.tool.android.backend.getAppsPendingFreeze


/**
 * This is actually not an activity, it just returns a shortcut some launcher can use.
 */
class FreezeShortcutActivity : Activity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val action = intent.action

		if (Intent.ACTION_CREATE_SHORTCUT == action) {
			setupShortcut()
			finish()
		} else {
			performFreeze()
		}
	}

	private fun setupShortcut() {
		val shortcutIntent = Intent("FREEZE")
		shortcutIntent.setClassName(this, this.javaClass.name)
		val intent: Intent
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val shortcutManager = getSystemService(ShortcutManager::class.java)
			intent = shortcutManager.createShortcutResultIntent(ShortcutInfo.Builder(applicationContext, "FreezeShortcut").build())
		} else {
			intent = Intent()
			intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
			intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.freeze_shortcut_short_label))
			val iconResource = Intent.ShortcutIconResource.fromContext(
					this, R.drawable.ic_freeze)
			intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource)
		}
		// Now, return the result to the launcher
		setResult(Activity.RESULT_OK, intent)
	}

	private fun performFreeze() {
		val appsPendingFreeze = getAppsPendingFreeze(applicationContext, this)
		if (appsPendingFreeze.isEmpty()) {
			Toast.makeText(this, getString(R.string.NothingToFreeze), Toast.LENGTH_SHORT).show()
			finish()
		}

		val freezeNext = freezeAll(applicationContext, activity = this)
		doOnResume {
			val appsLeft = freezeNext()
			if (!appsLeft) {
				finish()
			}
			appsLeft
		}
	}

	override fun onResume() {
		super.onResume()

		//Execute all tasks and retain only those that returned true.
		toBeDoneOnResume.retainAll { it() }
	}

	private val toBeDoneOnResume: MutableList<() -> Boolean> = mutableListOf()

	private fun doOnResume(task: ()->Boolean) {
		toBeDoneOnResume.add(task)
	}
}