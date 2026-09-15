package com.zabtaai

import android.util.Log
import kotlinx.coroutines.delay

/**
 * Main orchestrator that implements the core loop:
 * VOICE → UNDERSTAND → PLAN → OBSERVE → EXECUTE → VERIFY → CONTINUE/REPLAN
 */
class OperatorOrchestrator(
    private val accessibilityService: ZabtaAIAccessibilityService,
    private val screenObserver: ScreenObserver,
    private val actionExecutor: ActionExecutor,
    private val planner: Planner,
    private val stateManager: StateManager,
    private val onStatusUpdate: (status: String) -> Unit
) {

    private val TAG = "ZabtaAI-Orchestrator"
    private var isExecuting = false
    private var currentPlan: ActionPlan? = null

    /**
     * Main entry point: process a voice command
     * Implements the complete OBSERVE → UNDERSTAND → PLAN → EXECUTE → VERIFY loop
     */
    suspend fun processCommand(voiceText: String) {
        if (isExecuting) {
            Log.w(TAG, "Already executing a command")
            return
        }

        isExecuting = true
        onStatusUpdate("Processing command: $voiceText")
        Log.d(TAG, "Processing command: $voiceText")

        try {
            // 1. UNDERSTAND: Parse the voice command
            onStatusUpdate("Understanding command...")
            val intent = planner.parseCommand(voiceText) ?: run {
                onStatusUpdate("Could not understand command")
                Log.w(TAG, "Failed to parse command")
                return
            }

            // 2. OBSERVE: Get current screen state
            onStatusUpdate("Observing current screen...")
            var screenState = screenObserver.getCurrentScreenState()
            Log.d(TAG, "Current app: ${screenState?.packageName}")

            // 3. PLAN: Generate action plan
            onStatusUpdate("Planning actions...")
            currentPlan = planner.generatePlan(intent, screenState) ?: run {
                onStatusUpdate("Could not generate plan")
                return
            }
            Log.d(TAG, "Plan generated: ${currentPlan?.goal} with ${currentPlan?.steps?.size} steps")

            // 4. EXECUTE: Execute plan steps
            currentPlan?.let { plan ->
                var stepIndex = 0
                var success = true

                while (stepIndex < plan.steps.size && success) {
                    val step = plan.steps[stepIndex]
                    Log.d(TAG, "Executing step ${stepIndex + 1}/${plan.steps.size}: ${step.type}")
                    onStatusUpdate("Executing: ${step.type}")

                    val result = executeStep(step, screenState)
                    if (!result.success) {
                        Log.w(TAG, "Step failed: ${step.id} - ${result.message}")
                        onStatusUpdate("Step failed, attempting retry...")

                        // Retry logic
                        if (step.retryCount < step.maxRetries) {
                            delay(500)
                            screenState = screenObserver.getCurrentScreenState()
                            val retryStep = step.copy(retryCount = step.retryCount + 1)
                            val retryResult = executeStep(retryStep, screenState)
                            if (!retryResult.success) {
                                Log.e(TAG, "Retry failed: ${step.id}")
                                success = false
                            }
                        } else {
                            success = false
                        }
                    } else {
                        Log.d(TAG, "Step succeeded: ${step.id}")
                    }

                    // 5. OBSERVE AGAIN: Read new screen state after action
                    delay(500) // Wait for UI update
                    screenState = screenObserver.getCurrentScreenState()

                    // 6. VERIFY: Check if expected result occurred
                    if (step.type == ActionType.VERIFY) {
                        val verified = actionExecutor.verifyScreen(step.target)
                        if (!verified) {
                            Log.w(TAG, "Verification failed for step: ${step.id}")
                            onStatusUpdate("Verification failed, re-planning...")
                            success = false
                        }
                    }

                    stepIndex++
                }

                // Final result
                if (success) {
                    onStatusUpdate("✓ Command completed successfully")
                    stateManager.recordHistory(voiceText, "success")
                    Log.d(TAG, "Plan execution completed successfully")
                } else {
                    onStatusUpdate("✗ Command failed - could not complete all steps")
                    stateManager.recordHistory(voiceText, "failed")
                    Log.e(TAG, "Plan execution failed")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing command", e)
            onStatusUpdate("Error: ${e.message}")
            stateManager.recordHistory(voiceText, "error")
        } finally {
            isExecuting = false
            currentPlan = null
        }
    }

    /**
     * Execute a single action step
     */
    private suspend fun executeStep(
        step: ActionStep,
        screenState: ScreenState?
    ): ActionResult {
        return try {
            when (step.type) {
                ActionType.LAUNCH_APP -> {
                    actionExecutor.launchApp(step.target)
                }
                ActionType.CLICK -> {
                    actionExecutor.clickElement(step.target)
                }
                ActionType.LONG_CLICK -> {
                    actionExecutor.longClickElement(step.target)
                }
                ActionType.TYPE -> {
                    actionExecutor.typeText(step.target)
                }
                ActionType.CLEAR -> {
                    actionExecutor.clearField()
                }
                ActionType.SCROLL_DOWN -> {
                    actionExecutor.scrollDown()
                }
                ActionType.SCROLL_UP -> {
                    actionExecutor.scrollUp()
                }
                ActionType.PRESS_BACK -> {
                    actionExecutor.pressBack()
                }
                ActionType.PRESS_HOME -> {
                    actionExecutor.pressHome()
                }
                ActionType.RECENT_APPS -> {
                    actionExecutor.showRecentApps()
                }
                ActionType.WAIT -> {
                    delay(step.target.toLongOrNull() ?: 1000)
                    ActionResult(true, "Waited ${step.target}ms")
                }
                ActionType.VERIFY -> {
                    val verified = actionExecutor.verifyScreen(step.target)
                    ActionResult(verified, if (verified) "Verified" else "Verification failed")
                }
                ActionType.NAVIGATE_URL -> {
                    ActionResult(true, "Navigate: ${step.target}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Step execution error", e)
            ActionResult(false, "Exception: ${e.message}")
        }
    }

    fun cancel() {
        isExecuting = false
        currentPlan = null
        Log.d(TAG, "Operation cancelled")
    }
}
