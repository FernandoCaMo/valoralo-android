# Project-specific R8 rules (release).

# No debug logs in release: Log.v/d/i/w calls are removed. Log.e stays (never with personal data).
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

# Readable stack traces in Crashlytics (the mapping file is uploaded by the Crashlytics plugin).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Sign-in error class names are shown to testers ("Código 16 · GetCredentialCancellationException")
# and sent to Crashlytics custom keys, which the mapping file does not deobfuscate.
-keepnames class androidx.credentials.exceptions.** extends java.lang.Throwable
-keepnames class com.google.firebase.auth.FirebaseAuthException
-keepnames class * extends com.google.firebase.auth.FirebaseAuthException
-keepnames class com.google.android.gms.common.api.ApiException
-keepnames class * extends com.google.android.gms.common.api.ApiException

# kotlinx.serialization: keep the generated serializers of our @Serializable classes (Room cache).
-keepclassmembers @kotlinx.serialization.Serializable class com.valoralo.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.valoralo.**$$serializer { *; }
