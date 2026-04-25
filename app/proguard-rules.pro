# Keep JNI bridge
-keep class com.chessapp.engine.EngineBridge { *; }
-keepclassmembers class com.chessapp.engine.EngineBridge { native <methods>; }

# Keep ViewModel subclasses
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Keep all model classes (used in LiveData)
-keep class com.chessapp.model.** { *; }

# Material / AppCompat internal reflection
-dontwarn com.google.android.material.**
-keep class com.google.android.material.** { *; }
-keep interface com.google.android.material.** { *; }

# Prevent stripping enum values used via Difficulty.valueOf()
-keepclassmembers enum com.chessapp.model.GameState$Difficulty {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
