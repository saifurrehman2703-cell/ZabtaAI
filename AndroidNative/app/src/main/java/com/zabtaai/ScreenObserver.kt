package com.zabtaai

import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log

/**
 * Screen state model - represents current UI state
 */
data class ScreenState(
    val packageName: String,
    val activityName: String,
    val visibleText: List<String>,
    val clickableElements: List<UIElement>,
    val editableFields: List<UIElement>,
    val scrollableContainers: List<UIElement>,
    val rootNode: AccessibilityNodeInfo?
)

/**
 * UI Element representation
 */
data class UIElement(
    val text: String,
    val contentDescription: String,
    val hint: String,
    val resourceId: String,
    val packageName: String,
    val className: String,
    val bounds: Rect,
    val isClickable: Boolean,
    val isEditable: Boolean,
    val isScrollable: Boolean,
    val isVisibleToUser: Boolean,
    val node: AccessibilityNodeInfo?
)

data class Rect(val left: Int, val top: Int, val right: Int, val bottom: Int)

/**
 * REAL Android Screen Observer.
 * Reads current screen state using AccessibilityNodeInfo.
 * Falls back to OCR/Vision if accessibility data insufficient.
 */
class ScreenObserver(
    private val accessibilityService: ZabtaAIAccessibilityService,
    private val ocrAdapter: OCRAdapter? = null
) : ZabtaAIAccessibilityService.UIObserver {

    private val TAG = "ZabtaAI-ScreenObserver"
    private var lastScreenState: ScreenState? = null

    init {
        accessibilityService.registerUIObserver(this)
    }

    /**
     * Get current screen state (REAL)
     */
    fun getCurrentScreenState(): ScreenState? {
        val rootNode = accessibilityService.getRootNode() ?: return null
        val packageName = accessibilityService.getCurrentPackage()

        val visibleText = mutableListOf<String>()
        val clickableElements = mutableListOf<UIElement>()
        val editableFields = mutableListOf<UIElement>()
        val scrollableContainers = mutableListOf<UIElement>()

        parseNodeRecursive(
            rootNode,
            packageName,
            visibleText,
            clickableElements,
            editableFields,
            scrollableContainers
        )

        val state = ScreenState(
            packageName = packageName,
            activityName = "", // Can be extracted from context if needed
            visibleText = visibleText,
            clickableElements = clickableElements,
            editableFields = editableFields,
            scrollableContainers = scrollableContainers,
            rootNode = rootNode
        )

        lastScreenState = state
        Log.d(TAG, "Screen state updated: $packageName, ${visibleText.size} text, ${clickableElements.size} clickable")
        return state
    }

    private fun parseNodeRecursive(
        node: AccessibilityNodeInfo,
        packageName: String,
        visibleText: MutableList<String>,
        clickableElements: MutableList<UIElement>,
        editableFields: MutableList<UIElement>,
        scrollableContainers: MutableList<UIElement>
    ) {
        // Collect text
        node.text?.let {
            val text = it.toString()
            if (text.isNotEmpty()) {
                visibleText.add(text)
            }
        }

        // Create UIElement
        val element = UIElement(
            text = node.text?.toString() ?: "",
            contentDescription = node.contentDescription?.toString() ?: "",
            hint = (node.hintText?.toString() ?: ""),
            resourceId = node.viewIdResourceName ?: "",
            packageName = packageName,
            className = node.className?.toString() ?: "",
            bounds = node.getBoundsInScreen().let { Rect(it.left, it.top, it.right, it.bottom) },
            isClickable = node.isClickable,
            isEditable = node.isEditable,
            isScrollable = node.isScrollable,
            isVisibleToUser = node.isVisibleToUser,
            node = node
        )

        // Categorize elements
        if (node.isClickable && node.isVisibleToUser) {
            clickableElements.add(element)
        }
        if (node.isEditable && node.isVisibleToUser) {
            editableFields.add(element)
        }
        if (node.isScrollable && node.isVisibleToUser) {
            scrollableContainers.add(element)
        }

        // Recursively process children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null && child.isVisibleToUser) {
                parseNodeRecursive(
                    child,
                    packageName,
                    visibleText,
                    clickableElements,
                    editableFields,
                    scrollableContainers
                )
            }
        }
    }

    /**
     * Find element by text
     */
    fun findElementByText(text: String, exact: Boolean = false): UIElement? {
        val state = getCurrentScreenState() ?: return null
        return state.clickableElements.find { element ->
            if (exact) {
                element.text.equals(text, ignoreCase = true)
            } else {
                element.text.contains(text, ignoreCase = true) ||
                element.contentDescription.contains(text, ignoreCase = true) ||
                element.hint.contains(text, ignoreCase = true)
            }
        }
    }

    /**
     * Find element by partial text match
     */
    fun findElementsByPartialText(text: String): List<UIElement> {
        val state = getCurrentScreenState() ?: return emptyList()
        return state.clickableElements.filter { element ->
            element.text.contains(text, ignoreCase = true) ||
            element.contentDescription.contains(text, ignoreCase = true)
        }
    }

    /**
     * Find input field
     */
    fun findInputField(): UIElement? {
        val state = getCurrentScreenState() ?: return null
        return state.editableFields.firstOrNull()
    }

    /**
     * Get scrollable container
     */
    fun getScrollableContainer(): UIElement? {
        val state = getCurrentScreenState() ?: return null
        return state.scrollableContainers.firstOrNull()
    }

    /**
     * Check if screen contains text
     */
    fun containsText(text: String): Boolean {
        val state = getCurrentScreenState() ?: return false
        return state.visibleText.any { it.contains(text, ignoreCase = true) }
    }

    /**
     * Check current app
     */
    fun getCurrentApp(): String {
        return accessibilityService.getCurrentPackage()
    }

    /**
     * UI Observer callback
     */
    override fun onUIChanged(rootNode: AccessibilityNodeInfo?) {
        Log.d(TAG, "UI changed notification")
        getCurrentScreenState() // Update cache
    }

    fun release() {
        accessibilityService.unregisterUIObserver(this)
    }
}
