# Add project specific ProGuard rules here.

# Kotlin / Compose
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# CameraX
-keep class androidx.camera.** { *; }

# ML Kit
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_** { *; }
-dontwarn com.google.mlkit.**

# OpenCV
-keep class org.opencv.** { *; }
-dontwarn org.opencv.**

# PdfBox-Android
-keep class com.tom_roush.** { *; }
-dontwarn com.tom_roush.**

# Bouncy Castle (PdfBox dep)
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Keep coroutines internals
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**

# Keep our model classes (Room / serialization)
-keep class com.scantidy.scan.data.db.entity.** { *; }
-keep class com.scantidy.scan.data.model.** { *; }

# Keep ViewModel constructors
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(...);
}
