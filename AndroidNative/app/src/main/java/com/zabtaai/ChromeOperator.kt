package com.zabtaai

import android.util.Log

/**
 * Chrome-specific operator
 * Handles Chrome automation through Accessibility APIs
 */
class ChromeOperator(
    private val screenObserver: ScreenObserver,
    private val actionExecutor: ActionExecutor
) {

    private val TAG = "ZabtaAI-ChromeOperator"
    private val CHROME_PACKAGE = "com.android.chrome"

    /**
     * Open a URL in Chrome
     */
    suspend fun openUrl(url: String): ActionResult {
        Log.d(TAG, "Opening URL: $url")
        // Note: Real implementation requires intent handling
        // This is handled by ActionExecutor.launchApp with URL intent
        return ActionResult(true, "URL handling delegated")
    }

    /**
     * Perform a Google search through Chrome
     */
    suspend fun googleSearch(query: String): ActionResult {
        Log.d(TAG, "Searching: $query")

        // Step 1: Make sure Chrome is active
        val currentApp = screenObserver.getCurrentApp()
        if (currentApp != CHROME_PACKAGE) {
            Log.w(TAG, "Chrome not active, current app: $currentApp")
            return ActionResult(false, "Chrome not active")
        }

        // Step 2: Find search field
        val searchField = screenObserver.findElementByText("Search or type URL")
            ?: screenObserver.findElementByText("Search")
            ?: screenObserver.findInputField()

        if (searchField == null) {
            Log.w(TAG, "Search field not found")
            return ActionResult(false, "Search field not found")
        }

        // Step 3: Click search field
        val clickResult = actionExecutor.clickElement(searchField.text)
        if (!clickResult.success) {
            return ActionResult(false, "Failed to click search field")
        }

        // Step 4: Type search query
        val typeResult = actionExecutor.typeText(query)
        if (!typeResult.success) {
            return ActionResult(false, "Failed to type search query")
        }

        // Step 5: Press Enter to search
        val enterResult = actionExecutor.clickElement("Search")
            ?: actionExecutor.clickElement("Go")
        
        return ActionResult(true, "Search initiated: $query")
    }

    /**
     * Navigate back in Chrome
     */
    suspend fun goBack(): ActionResult {
        Log.d(TAG, "Chrome back")
        return actionExecutor.pressBack()
    }

    /**
     * Scroll page
     */
    suspend fun scrollPage(direction: String): ActionResult {
        Log.d(TAG, "Chrome scroll: $direction")
        return when (direction) {
            "down" -> actionExecutor.scrollDown()
            "up" -> actionExecutor.scrollUp()
            else -> ActionResult(false, "Invalid direction")
        }
    }
}
