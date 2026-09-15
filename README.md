# ZabtaAI - Real Android AI Phone Operator

ZabtaAI is a real Android application that understands natural Urdu/Hindi/English voice commands and performs actual actions on your Android phone through accessibility APIs.

## What ZabtaAI Does

- **Listens** to your voice commands in Urdu, Hindi, or English
- **Observes** the real Android screen using AccessibilityNodeInfo and screenshot analysis
- **Plans** the required actions dynamically (not hardcoded sequences)
- **Executes** real actions: launch apps, click buttons, type text, scroll, swipe, navigate
- **Verifies** each action by observing the resulting screen state
- **Reports** results back to you in Urdu

## Example Commands

```
"Chrome kholo"                          → Launches real Chrome
"Google kholo"                          → Opens Google through Chrome
"Google par YouTube search karo"        → Searches for YouTube on Google
"neeche scroll karo"                    → Scrolls the current screen down
"back karo"                             → Presses Android Back button
"ye button dabao"                       → Clicks visible button
"ZabtaAI off ho jao"                    → Stops voice listening
"ZabtaAI on ho jao"                     → Resumes voice listening
```

## Architecture

See `/docs/ARCHITECTURE.md` for detailed design.

### Core Components

1. **AccessibilityService** - Real UI observation and interaction
2. **ForegroundService** - Background operation
3. **SpeechRecognitionEngine** - Urdu/Hindi/English voice input
4. **ScreenObserver** - Current screen state analysis
5. **ActionExecutor** - Real Android actions
6. **Planner/Orchestrator** - Dynamic action planning
7. **ChromeOperator** - Chrome automation
8. **StateManager** - ON/OFF control with visible status

## Quick Setup

### Requirements
- Android device running Android 13 or higher
- Android Studio (for building)
- Gradle build tools

### Enable Accessibility Service
1. Settings → Accessibility
2. Find and enable ZabtaAI
3. Grant all permissions

### Grant Microphone Permission
1. Settings → Apps → ZabtaAI
2. Permissions → Microphone → Allow

### Build & Install

```bash
cd AndroidNative
./gradlew clean build
# Install the APK
```

### Activate
1. Open ZabtaAI app
2. Tap ACTIVATE or say "ZabtaAI on ho jao"
3. Watch for ACTIVE status indicator
4. Start speaking commands

## File Structure

```
ZabtaAI/
├── README.md
├── AndroidNative/                 # Main native Android project
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/com/zabtaai/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── ZabtaAIAccessibilityService.kt
│   │   │   │   ├── ZabtaAIForegroundService.kt
│   │   │   │   ├── SpeechRecognitionEngine.kt
│   │   │   │   ├── ScreenObserver.kt
│   │   │   │   ├── ActionExecutor.kt
│   │   │   │   ├── Planner.kt
│   │   │   │   ├── ChromeOperator.kt
│   │   │   │   ├── OCRAdapter.kt
│   │   │   │   ├── StateManager.kt
│   │   │   │   └── utils/
│   │   │   └── res/
│   │   └── build.gradle
│   ├── build.gradle
│   └── settings.gradle
├── Extension/                     # Native extension for MIT App Inventor
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   └── java/com/zabtaai/extension/
│   │       ├── ZabtaAIExtension.kt
│   │       └── Bridge.kt
│   └── build.gradle
├── AIA/                           # MIT App Inventor/Kodular project
│   ├── ZabtaAI.aia
│   └── src/
├── docs/                          # Documentation
│   ├── ARCHITECTURE.md
│   ├── NATIVE_DEVELOPMENT.md
│   ├── EXTENSION_BUILD.md
│   ├── API_REFERENCE.md
│   └── TROUBLESHOOTING.md
├── tests/                         # Unit and integration tests
│   ├── AccessibilityServiceTest.kt
│   ├── ActionExecutorTest.kt
│   ├── PlannerTest.kt
│   ├── ScreenObserverTest.kt
│   └── IntegrationTest.kt
├── assets/                        # App resources
│   ├── urdu_strings.json
│   ├── hindi_strings.json
│   └── app_icon.png
└── scripts/                       # Build and validation scripts
    ├── build-extension.sh
    ├── build-apk.sh
    └── validate-project.sh
```

## Permissions Required

- ✅ Accessibility Service - UI observation and interaction
- ✅ Microphone - Speech input
- ✅ Notifications - Status display
- ✅ Foreground Service - Background operation
- ⚠️ Overlay - Optional status indicator
- ⚠️ Screen Capture - Optional OCR/screenshot fallback

## Privacy & Security

- ✅ No covert recording - Status always visible
- ✅ No secret uploads - Data stays on device
- ✅ User controlled - Explicit ON/OFF state
- ✅ No lock bypass - Respects Android security
- ✅ History control - User can clear history

## Building from Source

### Full Android Build
```bash
cd AndroidNative
./gradlew clean build
./gradlew installDebug
```

### Extension Build
```bash
cd Extension
./gradlew build
# Output: Extension/build/outputs/extension/ZabtaAIExtension.aix
```

### Build APK for Distribution
```bash
cd AndroidNative
./gradlew clean buildRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

## Documentation

- `/docs/ARCHITECTURE.md` - System design and component overview
- `/docs/NATIVE_DEVELOPMENT.md` - Native Android development guide
- `/docs/EXTENSION_BUILD.md` - Building native extension
- `/docs/API_REFERENCE.md` - Public API documentation
- `/docs/TROUBLESHOOTING.md` - Common issues and solutions

## Troubleshooting

### ZabtaAI not responding
1. Verify Accessibility Service is enabled
2. Check Microphone permission is granted
3. Check ACTIVE status is shown
4. See `/docs/TROUBLESHOOTING.md`

### Actions not working
1. Verify target app supports accessibility
2. Check if app has accessibility restrictions
3. Review screen observation in logs

### Build fails
1. Update Android SDK to latest
2. Verify Gradle version compatibility
3. Check NDK is installed if required
4. See `/docs/NATIVE_DEVELOPMENT.md`

## Future Enhancements

- Advanced gesture recognition
- Multi-step macro recording
- App-specific operators
- Real-time AI vision
- Cloud-based advanced NLP
- Custom voice command training

## License

Open source - modify and use freely.

---

**ZabtaAI: Real AI Phone Control**

Built with real Android Accessibility APIs. Not simulation. Not fake UI.
