# ------------------------------------------------------------------------------
# R8 / ProGuard Optimization Rules for GlassNotes
# ------------------------------------------------------------------------------

# General Android Optimization & Line Numbers for Stacktraces
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ------------------------------------------------------------------------------
# Jetpack Compose Rules
# ------------------------------------------------------------------------------
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.animation.** { *; }
-keep class androidx.navigation.** { *; }
-dontwarn androidx.compose.**

# ------------------------------------------------------------------------------
# Room Database
# ------------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Entity class * { *; }
-keepclassmembers class * {
    @androidx.room.TypeConverter *;
}
-keep class * extends androidx.room.migration.Migration
-dontwarn androidx.room.paging.**

# ------------------------------------------------------------------------------
# AndroidX DataStore & Glance Widget
# ------------------------------------------------------------------------------
-keep class androidx.datastore.preferences.protobuf.** { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver
-keep class androidx.glance.** { *; }

# ------------------------------------------------------------------------------
# Biometric Authentication
# ------------------------------------------------------------------------------
-keep class androidx.biometric.** { *; }
-keepclassmembers class * extends androidx.biometric.BiometricPrompt$AuthenticationCallback {
    public void onAuthenticationError(int, java.lang.CharSequence);
    public void onAuthenticationSucceeded(androidx.biometric.BiometricPrompt$AuthenticationResult);
    public void onAuthenticationFailed();
}

# ------------------------------------------------------------------------------
# OkHttp & Coroutines
# ------------------------------------------------------------------------------
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-keepclassmembers class * extends okhttp3.Interceptor {
    public <init>();
    public okhttp3.Response intercept(okhttp3.Interceptor$Chain);
}
-dontwarn kotlinx.coroutines.**

# ------------------------------------------------------------------------------
# Firebase & Google Play Services
# ------------------------------------------------------------------------------
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ------------------------------------------------------------------------------
# WebView JavaScript Interface
# ------------------------------------------------------------------------------
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ------------------------------------------------------------------------------
# Secret Vault & Application Architecture
# ------------------------------------------------------------------------------
-keep class com.example.vault.** { *; }
-keep class com.example.vault.model.** { *; }
-keep class com.example.data.db.** { *; }
-keep class com.example.ui.viewmodel.** { *; }

-keep class com.example.vault.ui.lock.VaultLockScreenKt { *; }
-keep class com.example.vault.ui.apps.VaultNotesAppKt { *; }
-keep class com.example.vault.ui.apps.VaultFilesAppKt { *; }
-keep class com.example.vault.ui.apps.VaultGalleryAppKt { *; }
-keep class com.example.vault.ui.apps.VaultBrowserAppKt { *; }
-keep class com.example.vault.security.VaultSecurityManager { *; }
-keep class com.example.vault.util.VaultFaceBiometricHelper { *; }
-keep class com.example.vault.util.PanicSensorManager { *; }
-keep class com.example.vault.data.VaultRepository { *; }
