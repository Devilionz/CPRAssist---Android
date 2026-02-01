# CPR Assist ProGuard Rules

# Keep data models for serialization
-keep class com.cprassist.data.models.** { *; }

# Keep event types for reflection
-keepclassmembers enum com.cprassist.data.models.EventType {
    *;
}

# Keep service classes
-keep class com.cprassist.services.** { *; }

# Kotlin coroutines
-keepclassmembers class kotlinx.coroutines.** {
    *;
}

# Compose
-keep class androidx.compose.** { *; }
