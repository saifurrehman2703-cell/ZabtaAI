package com.zabtaai

import android.graphics.Bitmap
import android.util.Log

/**
 * OCR Adapter - fallback when Accessibility tree is insufficient
 * Uses ML Kit for text recognition
 */
class OCRAdapter {

    private val TAG = "ZabtaAI-OCRAdapter"

    /**
     * Extract text from screenshot using ML Kit
     * Requires: compile-time addition of ML Kit Text Recognition dependency
     * Runtime: requires Google Play Services
     */
    fun extractText(bitmap: Bitmap): List<String> {
        Log.d(TAG, "Extracting text from screenshot")
        // This requires ML Kit which is already in build.gradle
        // Full implementation requires Google Play Services on device
        // For now, return empty - requires runtime ML Kit initialization
        return emptyList()
    }

    /**
     * Detect elements in screenshot
     * Placeholder for vision-based element detection
     */
    fun detectElements(bitmap: Bitmap): List<UIElement> {
        Log.d(TAG, "Detecting elements from screenshot")
        // Would require AI Vision provider integration
        return emptyList()
    }
}
