# ===================================
# General Android Configuration
# ===================================

# Keep all classes in main package
-keep class com.maciel.wavereaderkmm.** { *; }

# Keep native methods (JNI)
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep custom view classes and their constructors
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ===================================
# Kotlin-specific Rules
# ===================================

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }

# Keep Kotlin coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Keep Kotlin reflect
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.reflect.**

# ===================================
# Kotlinx Serialization
# ===================================

# Keep serialization annotations
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep serialization implementation
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}

-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep all @Serializable classes
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    <fields>;
    <methods>;
}

# Keep serializer for custom classes
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

-if @kotlinx.serialization.Serializable class ** {
    static **$Companion Companion;
}
-keepclassmembers class <2>$serializer {
    <fields>;
}

# Keep serializer implementations
-keep,includedescriptorclasses class **$$serializer { *; }
-keepclassmembers class ** {
    *** Companion;
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ===================================
# Jetpack Compose
# ===================================

# Keep Compose runtime
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Compose UI
-keep class androidx.compose.ui.** { *; }
-dontwarn androidx.compose.ui.**

# Keep Compose material
-keep class androidx.compose.material3.** { *; }
-dontwarn androidx.compose.material3.**

# Keep Compose navigation
-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

# Keep @Composable functions
-keep @androidx.compose.runtime.Composable class * { *; }
-keep @androidx.compose.runtime.Composable interface * { *; }

# ===================================
# Ktor (Networking)
# ===================================

# Keep Ktor client classes
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { *; }
-dontwarn io.ktor.**

# Keep Ktor serialization
-keep class io.ktor.serialization.** { *; }
-dontwarn io.ktor.serialization.**

# ===================================
# Google Play Services & Maps
# ===================================

# Keep Google Play Services
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Keep Google Maps
-keep class com.google.android.libraries.maps.** { *; }
-keep interface com.google.android.libraries.maps.** { *; }
-dontwarn com.google.android.libraries.maps.**

# Keep Maps Compose
-keep class com.google.maps.android.compose.** { *; }
-dontwarn com.google.maps.android.compose.**

# ===================================
# Firebase
# ===================================

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Keep Firebase Auth
-keep class com.google.firebase.auth.** { *; }
-dontwarn com.google.firebase.auth.**

# Keep Firebase Firestore
-keep class com.google.firebase.firestore.** { *; }
-dontwarn com.google.firebase.firestore.**

# ===================================
# AndroidX and Support Libraries
# ===================================

# Keep AndroidX core
-keep class androidx.core.** { *; }
-dontwarn androidx.core.**

# Keep AndroidX lifecycle
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**

# Keep AndroidX activity
-keep class androidx.activity.** { *; }
-dontwarn androidx.activity.**

# ===================================
# Wave Reader Specific Data Models
# ===================================

# If you have data classes for wave measurements, keep them
# Uncomment and customize these if you have specific model classes:

# -keep class com.maciel.wavereaderkmm.data.** { *; }
# -keep class com.maciel.wavereaderkmm.model.** { *; }

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel {
    <init>();
}

# Keep your repository classes
# -keep class com.maciel.wavereaderkmm.repository.** { *; }

# ===================================
# Location & Sensors
# ===================================

# Keep location services
-keep class android.location.** { *; }
-dontwarn android.location.**

# Keep sensor-related classes (for accelerometer)
-keep class android.hardware.** { *; }
-dontwarn android.hardware.**

# ===================================
# Debugging and Optimization
# ===================================

# Remove logging in release builds (optional - uncomment to enable)
 -assumenosideeffects class android.util.Log {
     public static *** d(...);
     public static *** v(...);
     public static *** i(...);
 }

# Preserve line numbers for debugging stack traces
-keepattributes SourceFile,LineNumberTable

# Rename source file attribute to hide actual source file name
-renamesourcefileattribute SourceFile
