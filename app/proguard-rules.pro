# ProGuard / R8 Configuration for SIPAGAR KITA (v1.6.1)
# Optimization and shrinking rules for release builds

# 1. Retrofit & OkHttp
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# 2. Moshi JSON Parsing & Adapters
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <fields>;
}
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**

# 3. Application Data Models & API DTOs (Preserve JSON Serialization)
-keep class com.example.model.** { *; }
-keepclassmembers class com.example.model.** { *; }
-keep class com.example.data.api.** { *; }
-keepclassmembers class com.example.data.api.** { *; }

# 4. Room Database Entities, DAOs, & Migrations
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class com.example.data.local.** { *; }
-keepclassmembers class com.example.data.local.** { *; }
-dontwarn androidx.room.paging.**

# 5. Kotlin Coroutines & Flow
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# 6. Jetpack Compose & Coil Image Loading
-keep class coil.** { *; }
-dontwarn coil.**
-keep class androidx.compose.ui.** { *; }
-dontwarn androidx.compose.**

# 7. Preserve Line Numbers for Crash Reporting
-keepattributes SourceFile, LineNumberTable

# Aturan Anti-Rusak untuk Model Data Firestore
-keep class com.aistudio.cuacamukomuko.wmzq.model.** { *; }
-keepclassmembers class com.aistudio.cuacamukomuko.wmzq.model.** { *; }
-keep class com.example.security.** { *; }
-keepclassmembers class com.example.security.** { *; }
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# 8. WorkManager & Background Services (Mencegah Force Close pasca-update APK saat R8 aktif)
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class com.example.service.** { *; }
-keep class com.example.data.sync.** { *; }

# 9. Firebase App Check, reCAPTCHA Enterprise, SessionManager & Auth Repository (Mencegah gagal verifikasi PIN Cloud / Sesi Petugas di Release APK v1.3.0)
-keep class com.google.firebase.appcheck.** { *; }
-keep class com.google.android.recaptcha.** { *; }
-dontwarn com.google.firebase.appcheck.**
-dontwarn com.google.android.recaptcha.**
-keep class com.example.data.repository.** { *; }
-keepclassmembers class com.example.data.repository.** { *; }
-keep class com.example.data.auth.** { *; }
-keepclassmembers class com.example.data.auth.** { *; }
-keep class com.example.util.SessionManager { *; }
-keep class com.example.util.SessionManager$** { *; }
-keep class com.example.SessionManager { *; }
-keep class com.example.SessionManager$** { *; }
-keep class com.google.firebase.auth.** { *; }
-keep class com.google.firebase.firestore.** { *; }
-keep class androidx.credentials.** { *; }
-dontwarn androidx.credentials.**


