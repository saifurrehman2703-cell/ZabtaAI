package com.zabtaai

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent

/**
 * REAL Android AccessibilityService implementation.
 * Uses genuine Android Accessibility APIs to:
 * - Read UI hierarchy (AccessibilityNodeInfo)
 * - Find clickable elements
 * - Detect text, descriptions, hints
 * - Perform clicks, long-clicks, text input
 * - Observe UI changes
 * - Monitor foreground app changes
 */
class ZabtaAIAccessibilityService : AccessibilityService() {
    
    private val TAG = "ZabtaAI-Accessibility"
    private val handler = Handler(Looper.getMainLooper())
    private var lastForegroundPackage = ""
    private var uiObservers = mutableListOf<UIObserver>()
    
    interface UIObserver {
        fun onUIChanged(rootNode: AccessibilityNodeInfo?)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "AccessibilityService Connected")
        
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or 
                   AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE
            notificationTimeout = 100
        }
        setServiceInfo(info)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val packageName = event.packageName.toString()
                if (packageName != lastForegroundPackage) {
                    lastForegroundPackage = packageName
                    Log.d(TAG, "Foreground app changed: $packageName")
                    notifyUIObservers()
                }
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                Log.d(TAG, "Window content changed")
                notifyUIObservers()
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                Log.d(TAG, "View clicked: ${event.source?.contentDescription}")
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }

    /**
     * Get current root node (REAL AccessibilityNodeInfo)
     */
    fun getRootNode(): AccessibilityNodeInfo? {
        return rootInActiveWindow
    }

    /**
     * Find nodes by text (REAL search through accessibility tree)
     */
    fun findNodesByText(text: String, exact: Boolean = false): List<AccessibilityNodeInfo> {
        val results = mutableListOf<AccessibilityNodeInfo>()
        val root = rootInActiveWindow ?: return results
        
        findNodesRecursive(root, text, exact, results)
        return results
    }

    private fun findNodesRecursive(
        node: AccessibilityNodeInfo,
        text: String,
        exact: Boolean,
        results: MutableList<AccessibilityNodeInfo>
    ) {
        val nodeText = node.text?.toString() ?: ""
        val matches = if (exact) {
            nodeText.equals(text, ignoreCase = true)
        } else {
            nodeText.contains(text, ignoreCase = true)
        }
        
        if (matches) {
            results.add(node)
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                findNodesRecursive(child, text, exact, results)
            }
        }
    }

    /**
     * Find clickable nodes (REAL accessibility tree analysis)
     */
    fun findClickableNodes(): List<AccessibilityNodeInfo> {
        val results = mutableListOf<AccessibilityNodeInfo>()
        val root = rootInActiveWindow ?: return results
        
        findClickableNodesRecursive(root, results)
        return results
    }

    private fun findClickableNodesRecursive(
        node: AccessibilityNodeInfo,
        results: MutableList<AccessibilityNodeInfo>
    ) {
        if (node.isClickable && node.isVisibleToUser) {
            results.add(node)
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null && child.isVisibleToUser) {
                findClickableNodesRecursive(child, results)
            }
        }
    }

    /**
     * Find editable fields (REAL text input detection)
     */
    fun findEditableNodes(): List<AccessibilityNodeInfo> {
        val results = mutableListOf<AccessibilityNodeInfo>()
        val root = rootInActiveWindow ?: return results
        
        findEditableNodesRecursive(root, results)
        return results
    }

    private fun findEditableNodesRecursive(
        node: AccessibilityNodeInfo,
        results: MutableList<AccessibilityNodeInfo>
    ) {
        if (node.isEditable && node.isVisibleToUser) {
            results.add(node)
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null && child.isVisibleToUser) {
                findEditableNodesRecursive(child, results)
            }
        }
    }

    /**
     * Find scrollable nodes (REAL scroll detection)
     */
    fun findScrollableNodes(): List<AccessibilityNodeInfo> {
        val results = mutableListOf<AccessibilityNodeInfo>()
        val root = rootInActiveWindow ?: return results
        
        findScrollableNodesRecursive(root, results)
        return results
    }

    private fun findScrollableNodesRecursive(
        node: AccessibilityNodeInfo,
        results: MutableList<AccessibilityNodeInfo>
    ) {
        if (node.isScrollable && node.isVisibleToUser) {
            results.add(node)
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null && child.isVisibleToUser) {
                findScrollableNodesRecursive(child, results)
            }
        }
    }

    /**
     * Perform real click action on node
     */
    fun performClick(node: AccessibilityNodeInfo): Boolean {
        if (!node.isClickable) return false
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    /**
     * Perform real long-click action on node
     */
    fun performLongClick(node: AccessibilityNodeInfo): Boolean {
        return node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
    }

    /**
     * Set text in an editable node (REAL text input)
     */
    fun setText(node: AccessibilityNodeInfo, text: String): Boolean {
        if (!node.isEditable) return false
        
        val bundle = android.os.Bundle()
        bundle.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
    }

    /**
     * Clear text in an editable node
     */
    fun clearText(node: AccessibilityNodeInfo): Boolean {
        if (!node.isEditable) return false
        return setText(node, "")
    }

    /**
     * Focus a node (REAL focus action)
     */
    fun focusNode(node: AccessibilityNodeInfo): Boolean {
        return node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
    }

    /**
     * Scroll node in direction (REAL scroll)
     */
    fun scrollNode(node: AccessibilityNodeInfo, direction: ScrollDirection): Boolean {
        val action = when (direction) {
            ScrollDirection.UP -> AccessibilityNodeInfo.ACTION_SCROLL_UP
            ScrollDirection.DOWN -> AccessibilityNodeInfo.ACTION_SCROLL_DOWN
            ScrollDirection.LEFT -> AccessibilityNodeInfo.ACTION_SCROLL_LEFT
            ScrollDirection.RIGHT -> AccessibilityNodeInfo.ACTION_SCROLL_RIGHT
        }
        return node.performAction(action)
    }

    /**
     * Press Back button (REAL Android Back action)
     */
    fun pressBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    /**
     * Press Home button (REAL Android Home action)
     */
    fun pressHome(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }

    /**
     * Show recent apps (REAL Android Recent Apps)
     */
    fun showRecentApps(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_RECENTS)
    }

    /**
     * Get current foreground app package
     */
    fun getCurrentPackage(): String = lastForegroundPackage

    /**
     * Register observer for UI changes
     */
    fun registerUIObserver(observer: UIObserver) {
        uiObservers.add(observer)
    }

    /**
     * Unregister observer
     */
    fun unregisterUIObserver(observer: UIObserver) {
        uiObservers.remove(observer)
    }

    private fun notifyUIObservers() {
        val rootNode = rootInActiveWindow
        for (observer in uiObservers) {
            observer.onUIChanged(rootNode)
        }
    }

    enum class ScrollDirection {
        UP, DOWN, LEFT, RIGHT
    }
}
