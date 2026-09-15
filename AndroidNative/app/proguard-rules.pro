# This is a configuration file for ProGuard.

-keep class com.zabtaai.** { *; }
-keep interface com.zabtaai.** { *; }

# Keep accessibility service
-keep class com.zabtaai.ZabtaAIAccessibilityService { *; }
-keep class com.zabtaai.ZabtaAIForegroundService { *; }

# Gson
-keep class com.google.gson.** { *; }
-keepclassmembers class com.google.gson.** { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}
