package superfreeze.tool.android

/**
 * The freeze mode of an app: ALWAYS_FREEZE, NEVER_FREEZE or FREEZE_WHEN_INACTIVE
 */
enum class FreezeMode {

	/**
	 * This app will always be frozen if it is running, regardless of when it was used last.
	 */
	ALWAYS_FREEZE,

	/**
	 * This app will never be frozen, even if it has been running in background for whatever time.
	 */
	NEVER_FREEZE,

	/**
	 * This app will be frozen if it was not used for a specific time but is running in background.
	 */
	FREEZE_WHEN_INACTIVE
}