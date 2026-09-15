# ZabtaAI Architecture & Technical Guide

## System Architecture

```
┌─────────────────────────────────────────────────┐
│         Voice Input (Urdu/Hindi/English)        │
├─────────────────────────────────────────────────┤
│    SpeechRecognitionEngine (Continuous)         │
├─────────────────────────────────────────────────┤
│         Planner (Command Parser)                │
│    - Parse natural language command             │
│    - Generate CommandIntent                     │
├─────────────────────────────────────────────────┤
│    OperatorOrchestrator (Main Loop)             │
│    1. OBSERVE current screen                    │
│    2. UNDERSTAND command intent                 │
│    3. PLAN action steps                         │
│    4. EXECUTE one action at a time              │
│    5. OBSERVE result                            │
│    6. VERIFY goal achievement                   │
│    7. CONTINUE or RE-PLAN                       │
├─────────────────────────────────────────────────┤
│         ScreenObserver                          │
│    - Read AccessibilityNodeInfo tree (Primary)  │
│    - Fall back to OCRAdapter if needed          │
├─────────────────────────────────────────────────┤
│         ActionExecutor                          │
│    - Click, type, scroll, back, home, etc.      │
│    - Real Android Accessibility APIs            │
├─────────────────────────────────────────────────┤
│    ZabtaAIAccessibilityService (Real)           │
│    - Performs actions through Accessibility     │
│    - Monitors UI changes                        │
├─────────────────────────────────────────────────┤
│    ZabtaAIForegroundService                     │
│    - Persistent background operation            │
│    - Microphone service type                    │
├─────────────────────────────────────────────────┤
│         StateManager                            │
│    - ON/OFF state                               │
│    - History tracking                           │
│    - Privacy controls                           │
├─────────────────────────────────────────────────┤
│         Optional: ChromeOperator                │
│    - Specialized Chrome automation              │
│    - URL navigation, search, scrolling           │
└─────────────────────────────────────────────────┘
```

## Component Responsibilities

### SpeechRecognitionEngine
- Continuous listening in Urdu, Hindi, English
- Speech-to-text using Android RecognizerIntent
- Automatic restart on error
- Lifecycle management (start, stop, release)

### Planner
- Parse natural language commands
- Support Urdu/Hindi command keywords
- Generate ActionPlan with steps
- Example: "Chrome kholo aur Google par YouTube search karo"
  - Step 1: Launch Chrome
  - Step 2: Wait for load
  - Step 3: Click search field
  - Step 4: Type search query
  - Step 5: Submit search

### OperatorOrchestrator
- Main execution loop
- OBSERVE → UNDERSTAND → PLAN → EXECUTE → VERIFY flow
- Handles step-by-step execution
- Implements retry logic (max 3 retries)
- Detects failures and reports clearly
- Records success/failure in history

### ScreenObserver
- Primary: AccessibilityNodeInfo tree traversal
- Collects visible text, clickable elements, editable fields, scrollable containers
- Finds elements by text, description, hint, resource ID
- Current app/package detection
- Fallback: OCRAdapter for insufficient accessibility data

### ActionExecutor
- Real action methods through AccessibilityService
- Methods: clickElement, typeText, scrollDown, scrollUp, pressBack, pressHome, showRecentApps, launchApp, waitForElement, verifyScreen
- All methods use real Android Accessibility APIs
- Delay after each action for UI update
- No fake/simulated actions

### ZabtaAIAccessibilityService
- Real Android AccessibilityService
- Observes accessibility events
- Provides UI tree access
- Executes accessibility actions
- UI observer pattern for change notifications

### ZabtaAIForegroundService
- Persistent notification
- Microphone service type (Android 12+)
- START_STICKY restart behavior
- Proper notification channel creation

### StateManager
- Current ON/OFF state
- Command history (local file)
- Clear history function
- User privacy controls

### ChromeOperator
- Specialized Chrome automation
- Find and click search field
- Type search queries
- Navigate URLs
- Scroll pages

## Data Flow

1. **User speaks command in Urdu/Hindi**
   ```
   "Chrome kholo aur Google par YouTube search karo"
   ```

2. **SpeechRecognitionEngine captures audio**
   ```
   onResults() → "Chrome kholo aur Google par YouTube search karo"
   ```

3. **Planner parses command**
   ```
   CommandIntent(
       action="search",
       target="com.android.chrome",
       parameters={"query" to "YouTube"}
   )
   ```

4. **OperatorOrchestrator gets current screen**
   ```
   ScreenState(
       packageName="com.android.launcher",
       visibleText=["Time", "Date", ...],
       clickableElements=[...],
       ...
   )
   ```

5. **Planner generates ActionPlan**
   ```
   ActionPlan(
       goal="Search for: YouTube",
       steps=[
           ActionStep(id="launch_1", type=LAUNCH_APP, target="com.android.chrome"),
           ActionStep(id="wait_1", type=WAIT, target="2000"),
           ActionStep(id="click_1", type=CLICK, target="search"),
           ActionStep(id="type_1", type=TYPE, target="YouTube"),
           ActionStep(id="click_2", type=CLICK, target="enter")
       ]
   )
   ```

6. **OperatorOrchestrator executes each step**
   ```
   For each step:
   - Execute action
   - Observe new screen state
   - Verify expected change
   - Retry if failed (max 3 times)
   - Continue to next step
   ```

7. **ActionExecutor performs real action**
   ```
   clickElement("search") →
   screenObserver.findElementByText("search") →
   element.node?.performAction(ACTION_CLICK) →
   Real Accessibility Action
   ```

8. **ScreenObserver reads result**
   ```
   AccessibilityNodeInfo tree →
   ScreenState with new UI state
   ```

9. **Orchestrator verifies completion**
   ```
   if (screenState.containsText("YouTube")) {
       success = true
   }
   ```

## Error Handling

### Level 1: Action Retry
```kotlin
if (!actionResult.success) {
    retry (max 3 times)
    observe new screen
    try again
}
```

### Level 2: Step Failure
```kotlin
if (stepFailed) {
    log error
    re-observe screen
    report "Step X failed"
    stop and report
}
```

### Level 3: Permission/System
```kotlin
if (accessibilityDisabled) {
    report "Enable Accessibility Service"
}
if (microphonePermissionDenied) {
    report "Grant microphone permission"
}
```

## Urdu/Hindi Language Support

### Command Keywords

**App Launch:**
- kholo / کھولو / खोलो (open)
- Chrome, YouTube, Google, Settings, Camera

**Navigation:**
- back / واپس / पीछे (back)
- home / ہوم / होम (home)
- jao / جاؤ / जाओ (go)
- wapas / واپس (back)

**Search:**
- search / سرچ / खोज (search)
- par / پر / पर (on)
- likho / لکھو / लिखो (write)

**Scroll:**
- neeche / نیچے / नीचे (down)
- opar / اوپر / ऊपर (up)
- scroll / سکرول (scroll)

**Control:**
- click / کلک / क्लिक (click)
- dabao / دبائیں / दबाएं (press)
- button / بٹن / बटन (button)
- off / آف / बंद (off)
- on / آن / चालू (on)

### Output Responses

- "Chrome کھول دیا گیا ہے۔" (Chrome has been opened)
- "میں اسکرین دیکھ رہا ہوں۔" (I'm observing the screen)
- "یہ کام مکمل ہو گیا۔" (This task is complete)
- "بٹن نہیں ملا۔" (Button not found)
- "زبتا AI بند کر دیا گیا ہے۔" (ZabtaAI has been deactivated)

## Testing

### Unit Tests (Can run without device)
- Command parsing
- Action plan generation
- Urdu/Hindi keyword matching
- Retry logic
- Failure handling

### Integration Tests (Require device/emulator)
- Speech recognition
- Accessibility service interaction
- Real screen reading
- Action execution
- Complete workflows

### Manual Acceptance Tests
- TEST 1: "Chrome kholo" → Chrome launches
- TEST 2: "Google par YouTube search karo" → Complete search workflow
- TEST 3: "neeche scroll karo" → Screen scrolls
- TEST 4: "back jao" → Back button pressed
- TEST 5: "home jao" → Home button pressed
- TEST 6: "ZabtaAI off ho jao" → Listening stops, status changes to OFF
- TEST 7: Reactivation → Listening resumes, status becomes ACTIVE
- TEST 8: Screen changes → System re-observes and re-plans
- TEST 9: Element not found → Bounded retry, then failure reported
- TEST 10: Accessibility disabled → Clear user instruction given

## Performance Considerations

- AccessibilityNodeInfo tree parsing: O(n) where n = nodes
- Text matching: Linear search with case-insensitive comparison
- Action execution: Async with 300-1000ms delays for UI update
- Retry backoff: 500ms between retries
- Max retries: 3 per action
- History file: Max 100 entries kept locally
- Memory: Moderate (screen state cached, released on updates)

## Security & Privacy

✅ **No covert recording**
- Microphone only active when "listening"
- ACTIVE/OFF status always visible
- Can be disabled immediately via "ZabtaAI off ho jao"

✅ **No surveillance**
- Local history only (file-based)
- No cloud upload without explicit opt-in
- User can clear history anytime

✅ **No security bypass**
- Respects Android permissions
- Cannot bypass lock screen
- Cannot bypass 2FA
- Cannot bypass authentication
- Cannot access protected apps

✅ **User control**
- ON/OFF state explicit
- History management available
- Accessibility service can be disabled
- Foreground service with notification

## Future Enhancements

1. **Online AI Integration**
   - OpenAI GPT, Google Gemini, or local Llama
   - Better natural language understanding
   - Complex multi-step workflows

2. **Advanced Vision**
   - Screenshot + AI vision for complex UI
   - Custom model integration
   - Gesture recognition

3. **Macro Recording**
   - Record and replay workflows
   - Save favorite commands
   - Custom voice triggers

4. **App-Specific Operators**
   - WhatsApp messaging
   - Gmail composition
   - Google Maps navigation
   - YouTube playlists

5. **Offline Models**
   - On-device LLM
   - Better offline NLP
   - No internet required

---

**Last Updated**: 2024
**Version**: 1.0.0
**Status**: Complete and ready for APK build
