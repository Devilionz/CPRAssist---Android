# CPR Assist Wear - ProGuard Rules

# Keep ViewModel classes
-keep class com.cprassist.wear.WearViewModel { *; }

# Kotlin coroutines
-keepclassmembers class kotlinx.coroutines.** {
    *;
}

# Compose
-keep class androidx.compose.** { *; }
-keep class androidx.wear.compose.** { *; }
