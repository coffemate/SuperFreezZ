package superfreeze.tool.android

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.view.accessibility.AccessibilityEvent

class FreezerService : AccessibilityService() {
	override fun onAccessibilityEvent(event: AccessibilityEvent?) {
	}

	override fun onInterrupt() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			val r = rootInActiveWindow
		}
	}


}
