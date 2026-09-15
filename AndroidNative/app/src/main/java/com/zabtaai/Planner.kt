package com.zabtaai

import android.util.Log

/**
 * Command intent model
 */
data class CommandIntent(
    val action: String,
    val target: String,
    val parameters: Map<String, String> = emptyMap(),
    val confidence: Float = 1.0f
)

/**
 * Action plan step
 */
data class ActionPlan(
    val goal: String,
    val steps: List<ActionStep> = emptyList(),
    val currentStepIndex: Int = 0
)

data class ActionStep(
    val id: String,
    val type: ActionType,
    val target: String,
    val parameters: Map<String, String> = emptyMap(),
    val retryCount: Int = 0,
    val maxRetries: Int = 3
)

enum class ActionType {
    LAUNCH_APP,
    CLICK,
    LONG_CLICK,
    TYPE,
    CLEAR,
    SCROLL_UP,
    SCROLL_DOWN,
    PRESS_BACK,
    PRESS_HOME,
    RECENT_APPS,
    WAIT,
    VERIFY,
    NAVIGATE_URL
}

/**
 * Planner parses commands and generates action plans.
 * Converts natural-language commands into executable steps.
 */
class Planner {

    private val TAG = "ZabtaAI-Planner"
    private val appPackages = mapOf(
        "chrome" to "com.android.chrome",
        "google" to "com.android.chrome", // Opens Chrome
        "youtube" to "com.google.android.youtube",
        "kodular" to "com.android.chrome", // Web app
        "mit app inventor" to "com.android.chrome",
        "gmail" to "com.google.android.gm",
        "maps" to "com.google.android.apps.maps",
        "camera" to "com.android.camera",
        "settings" to "com.android.settings"
    )

    /**
     * Parse natural language command into CommandIntent
     */
    fun parseCommand(text: String): CommandIntent? {
        val lowerText = text.lowercase().trim()

        Log.d(TAG, "Parsing command: $text")

        return when {
            // Launch app commands
            lowerText.contains("kholo") || lowerText.contains("खोलो") ||
            lowerText.contains("کھولو") -> {
                parseAppLaunchCommand(lowerText)
            }
            // Search commands
            lowerText.contains("search") || lowerText.contains("سرچ") ||
            lowerText.contains("par") || lowerText.contains("پر") -> {
                parseSearchCommand(lowerText)
            }
            // Navigation commands
            lowerText.contains("back") || lowerText.contains("واپس") ||
            lowerText.contains("پیچھے") || lowerText.contains("jao") -> {
                CommandIntent("back", "")
            }
            lowerText.contains("home") || lowerText.contains("ہوم") -> {
                CommandIntent("home", "")
            }
            lowerText.contains("recent") -> {
                CommandIntent("recent_apps", "")
            }
            // Scroll commands
            lowerText.contains("scroll") || lowerText.contains("neeche") ||
            lowerText.contains("نیچے") || lowerText.contains("اوپر") ||
            lowerText.contains("up") -> {
                parseScrollCommand(lowerText)
            }
            // Click/Select commands
            lowerText.contains("click") || lowerText.contains("dabao") ||
            lowerText.contains("دبائو") || lowerText.contains("button") ||
            lowerText.contains("select") -> {
                parseClickCommand(lowerText)
            }
            // OFF/ON commands
            lowerText.contains("off") || lowerText.contains("آف") ||
            lowerText.contains("بند") -> {
                CommandIntent("deactivate", "zabtaai")
            }
            lowerText.contains("on") || lowerText.contains("آن") -> {
                CommandIntent("activate", "zabtaai")
            }
            else -> {
                Log.w(TAG, "Could not parse command: $text")
                null
            }
        }
    }

    private fun parseAppLaunchCommand(text: String): CommandIntent? {
        for ((keyword, packageName) in appPackages) {
            if (text.contains(keyword)) {
                return CommandIntent("launch_app", packageName)
            }
        }
        return null
    }

    private fun parseSearchCommand(text: String): CommandIntent? {
        // Example: "google par youtube search karo"
        val parts = text.split(" ")
        val searchTerm = parts.dropWhile { !it.contains("par") && !it.contains("پر") }
            .drop(1)
            .joinToString(" ")
            .replace("search karo", "")
            .replace("کریں", "")
            .trim()

        return CommandIntent(
            "search",
            "com.android.chrome",
            mapOf("query" to searchTerm)
        )
    }

    private fun parseScrollCommand(text: String): CommandIntent? {
        val direction = when {
            text.contains("down") || text.contains("neeche") || text.contains("نیچے") -> "down"
            text.contains("up") || text.contains("opar") || text.contains("اوپر") -> "up"
            else -> "down"
        }
        return CommandIntent("scroll", "", mapOf("direction" to direction))
    }

    private fun parseClickCommand(text: String): CommandIntent? {
        // Extract button/element name if possible
        val target = text.replace("dabao", "")
            .replace("دبائو", "")
            .replace("button", "")
            .replace("click", "")
            .replace("ye", "")
            .replace("yeh", "")
            .trim()

        return CommandIntent("click", target)
    }

    /**
     * Generate action plan from CommandIntent
     */
    fun generatePlan(
        intent: CommandIntent,
        currentScreenState: ScreenState?
    ): ActionPlan? {
        Log.d(TAG, "Generating plan for: ${intent.action}")

        return when (intent.action) {
            "launch_app" -> {
                ActionPlan(
                    goal = "Launch app: ${intent.target}",
                    steps = listOf(
                        ActionStep(
                            id = "launch_1",
                            type = ActionType.LAUNCH_APP,
                            target = intent.target
                        ),
                        ActionStep(
                            id = "wait_1",
                            type = ActionType.WAIT,
                            target = "3000"
                        ),
                        ActionStep(
                            id = "verify_1",
                            type = ActionType.VERIFY,
                            target = intent.target
                        )
                    )
                )
            }
            "search" -> {
                ActionPlan(
                    goal = "Search for: ${intent.parameters["query"]}",
                    steps = listOf(
                        ActionStep(
                            id = "search_1",
                            type = ActionType.LAUNCH_APP,
                            target = "com.android.chrome"
                        ),
                        ActionStep(
                            id = "search_2",
                            type = ActionType.WAIT,
                            target = "2000"
                        ),
                        ActionStep(
                            id = "search_3",
                            type = ActionType.CLICK,
                            target = "search"
                        ),
                        ActionStep(
                            id = "search_4",
                            type = ActionType.TYPE,
                            target = intent.parameters["query"] ?: ""
                        ),
                        ActionStep(
                            id = "search_5",
                            type = ActionType.CLICK,
                            target = "enter"
                        )
                    )
                )
            }
            "scroll" -> {
                val direction = intent.parameters["direction"] ?: "down"
                ActionPlan(
                    goal = "Scroll $direction",
                    steps = listOf(
                        ActionStep(
                            id = "scroll_1",
                            type = if (direction == "up") ActionType.SCROLL_UP else ActionType.SCROLL_DOWN,
                            target = ""
                        )
                    )
                )
            }
            "back" -> {
                ActionPlan(
                    goal = "Press Back",
                    steps = listOf(
                        ActionStep(
                            id = "back_1",
                            type = ActionType.PRESS_BACK,
                            target = ""
                        )
                    )
                )
            }
            "home" -> {
                ActionPlan(
                    goal = "Press Home",
                    steps = listOf(
                        ActionStep(
                            id = "home_1",
                            type = ActionType.PRESS_HOME,
                            target = ""
                        )
                    )
                )
            }
            "recent_apps" -> {
                ActionPlan(
                    goal = "Show Recent Apps",
                    steps = listOf(
                        ActionStep(
                            id = "recent_1",
                            type = ActionType.RECENT_APPS,
                            target = ""
                        )
                    )
                )
            }
            "click" -> {
                ActionPlan(
                    goal = "Click: ${intent.target}",
                    steps = listOf(
                        ActionStep(
                            id = "click_1",
                            type = ActionType.CLICK,
                            target = intent.target
                        )
                    )
                )
            }
            else -> {
                Log.w(TAG, "Unknown action: ${intent.action}")
                null
            }
        }
    }
}
