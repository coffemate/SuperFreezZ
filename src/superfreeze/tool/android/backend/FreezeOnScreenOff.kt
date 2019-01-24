package superfreeze.tool.android.backend


import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.KEYGUARD_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import superfreeze.tool.android.userInterface.FreezeShortcutActivity



class ScreenReceiver(private val context: Context) : BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent) {
		if (intent.action == Intent.ACTION_SCREEN_OFF) {

			if (getAppsPendingFreeze(context).isEmpty()) {
				return
			}

			enableScreenUntilFrozen(this.context)

			context.startActivity(Intent(context, FreezeShortcutActivity::class.java).apply {
				addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			})
		}
	}
}

internal fun registerScreenReceiver(context: Context): ScreenReceiver {
	val filter = IntentFilter().apply {
		addAction(Intent.ACTION_SCREEN_OFF)
	}
	val screenReceiver = ScreenReceiver(context)
	context.registerReceiver(screenReceiver, filter)
	return screenReceiver
}

private fun enableScreenUntilFrozen(context: Context) {
	val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager?
	val wl = pm!!.newWakeLock(PowerManager.FULL_WAKE_LOCK
			or PowerManager.ACQUIRE_CAUSES_WAKEUP, "keepawake_until_frozen:")
	wl.acquire(3 * 60 * 1000L /*3 minutes*/)

	val km = context.getSystemService(KEYGUARD_SERVICE) as KeyguardManager?
	val kl = km!!.newKeyguardLock("SuperFreezZ")
	kl.disableKeyguard()

	FreezeShortcutActivity.onFreezeFinishedListener = {
		wl.release()
		kl.reenableKeyguard()
	}
}

private const val TAG = "FreezeOnScreenOff"