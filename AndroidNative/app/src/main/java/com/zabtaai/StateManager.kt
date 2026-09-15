package com.zabtaai

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages ZabtaAI state, history, and settings
 */
class StateManager(private val context: Context) {

    private val TAG = "ZabtaAI-StateManager"
    private val historyFile = File(context.filesDir, "zabtaai_history.txt")
    private val maxHistoryLines = 100
    
    var isActive = false
        private set

    init {
        Log.d(TAG, "StateManager initialized")
    }

    /**
     * Set active state
     */
    fun setActive(active: Boolean) {
        isActive = active
        Log.d(TAG, "State changed to: ${if (active) "ACTIVE" else "OFF"}")
    }

    /**
     * Record command in history
     */
    fun recordHistory(command: String, result: String) {
        try {
            val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val entry = "[$timestamp] $command → $result\n"
            
            // Append to history file
            historyFile.appendText(entry)
            
            // Keep only last 100 entries
            val lines = historyFile.readLines()
            if (lines.size > maxHistoryLines) {
                val recentLines = lines.takeLast(maxHistoryLines)
                historyFile.writeText(recentLines.joinToString("\n") + "\n")
            }
            
            Log.d(TAG, "History recorded: $command → $result")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record history", e)
        }
    }

    /**
     * Get recent history
     */
    fun getHistory(count: Int = 10): List<String> {
        return try {
            if (!historyFile.exists()) return emptyList()
            historyFile.readLines().takeLast(count)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read history", e)
            emptyList()
        }
    }

    /**
     * Clear history
     */
    fun clearHistory() {
        try {
            historyFile.delete()
            Log.d(TAG, "History cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear history", e)
        }
    }
}
