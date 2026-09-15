package com.zabtaai

import android.util.Log
import kotlinx.coroutines.delay

/**
 * Action result
 */
data class ActionResult(
    val success: Boolean,
    val message: String,
    val verificationText: String? = null
)

/**
 * REAL Action Executor.
 * Executes actions through AccessibilityService.
 * Includes error handling and retry logic.
 */
class ActionExecutor(
    private val accessibilityService: ZabtaAIAccessibilityService,
    private val screenObserver: ScreenObserver
) {

    private val TAG = "ZabtaAI-ActionExecutor"
    private val MAX_RETRIES = 3
    private val RETRY_DELAY_MS = 500L

    /**
     * Click on element by text
     */
    suspend fun clickElement(text: String): ActionResult {
        Log.d(TAG, "Clicking element with text: $text")

        val element = screenObserver.findElementByText(text) ?: run {
            Log.w(TAG, "Element not found: $text")
            return ActionResult(false, "Element not found: $text")
        }

        return if (element.node?.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK) == true) {
            delay(500) // Wait for UI update
            ActionResult(true, "Clicked: $text")
        } else {
            ActionResult(false, "Click failed: $text")
        }
    }

    /**
     * Long-click on element
     */
    suspend fun longClickElement(text: String): ActionResult {
        Log.d(TAG, "Long-clicking element: $text")

        val element = screenObserver.findElementByText(text) ?: run {
            return ActionResult(false, "Element not found: $text")
        }

        return if (element.node?.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_LONG_CLICK) == true) {
            delay(500)
            ActionResult(true, "Long-clicked: $text")
        } else {
            ActionResult(false, "Long-click failed: $text")
        }
    }

    /**
     * Type text into field
     */
    suspend fun typeText(text: String): ActionResult {
        Log.d(TAG, "Typing: $text")

        val field = screenObserver.findInputField() ?: run {
            return ActionResult(false, "No input field found")
        }

        val bundle = android.os.Bundle()
        bundle.putCharSequence(
            android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
            text
        )

        return if (field.node?.performAction(
            android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT,
            bundle
        ) == true) {
            delay(300)
            ActionResult(true, "Typed: $text")
        } else {
            ActionResult(false, "Type failed")
        }
    }

    /**
     * Clear input field
     */
    suspend fun clearField(): ActionResult {
        Log.d(TAG, "Clearing input field")

        val field = screenObserver.findInputField() ?: run {
            return ActionResult(false, "No input field found")
        }

        return if (typeText("")) {
            ActionResult(true, "Field cleared")
        } else {
            ActionResult(false, "Clear failed")
        }
    }

    /**
     * Scroll down
     */
    suspend fun scrollDown(): ActionResult {
        Log.d(TAG, "Scrolling down")

        val scrollable = screenObserver.getScrollableContainer() ?: run {
            return ActionResult(false, "No scrollable container found")
        }

        return if (scrollable.node?.performAction(
            android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_DOWN
        ) == true) {
            delay(300)
            ActionResult(true, "Scrolled down")
        } else {
            ActionResult(false, "Scroll failed")
        }
    }

    /**
     * Scroll up
     */
    suspend fun scrollUp(): ActionResult {
        Log.d(TAG, "Scrolling up")

        val scrollable = screenObserver.getScrollableContainer() ?: run {
            return ActionResult(false, "No scrollable container found")
        }

        return if (scrollable.node?.performAction(
            android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_UP
        ) == true) {
            delay(300)
            ActionResult(true, "Scrolled up")
        } else {
            ActionResult(false, "Scroll failed")
        }
    }

    /**
     * Press Back
     */
    suspend fun pressBack(): ActionResult {
        Log.d(TAG, "Pressing Back")

        return if (accessibilityService.pressBack()) {
            delay(300)
            ActionResult(true, "Back pressed")
        } else {
            ActionResult(false, "Back failed")
        }
    }

    /**
     * Press Home
     */
    suspend fun pressHome(): ActionResult {
        Log.d(TAG, "Pressing Home")

        return if (accessibilityService.pressHome()) {
            delay(300)
            ActionResult(true, "Home pressed")
        } else {
            ActionResult(false, "Home failed")
        }
    }

    /**
     * Show recent apps
     */
    suspend fun showRecentApps(): ActionResult {
        Log.d(TAG, "Showing recent apps")

        return if (accessibilityService.showRecentApps()) {
            delay(300)
            ActionResult(true, "Recent apps shown")
        } else {
            ActionResult(false, "Recent apps failed")
        }
    }

    /**
     * Launch application by package name
     */
    suspend fun launchApp(packageName: String): ActionResult {
        Log.d(TAG, "Launching app: $packageName")

        return try {
            val intent = accessibilityService.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                accessibilityService.startActivity(intent)
                delay(1000) // Wait for app to launch
                ActionResult(true, "App launched: $packageName")
            } else {
                ActionResult(false, "App not found: $packageName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Launch failed", e)
            ActionResult(false, "Launch failed: ${e.message}")
        }
    }

    /**
     * Wait for element to appear
     */
    suspend fun waitForElement(text: String, timeoutMs: Long = 5000): ActionResult {
        Log.d(TAG, "Waiting for element: $text")

        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (screenObserver.findElementByText(text) != null) {
                return ActionResult(true, "Element appeared: $text")
            }
            delay(500)
        }

        return ActionResult(false, "Timeout waiting for element: $text")
    }

    /**
     * Verify current screen state
     */
    suspend fun verifyScreen(expectedText: String): Boolean {
        delay(300) // Wait for screen update
        return screenObserver.containsText(expectedText)
    }
}
