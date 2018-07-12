package superfreeze.tool.android

import android.app.Activity
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity



/**
 * This is actually not an activity, it just returns a shortcut some launcher can use.
 */
class FreezeShortcutActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
		super.onCreate(savedInstanceState, persistentState)

		val intent = intent
		val action = intent.action

		if (Intent.ACTION_CREATE_SHORTCUT == action) {
			setupShortcut()
		} else {
			freezeAll(applicationContext)
		}

		finish()
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
}