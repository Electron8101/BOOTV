# ProGuard / R8 Rules for BOOTV Anti-Reverse-Engineering & Minification

# Keep all Android application components with their public constructors and methods
-keep public class * extends android.app.Activity {
    public <init>();
    *;
}

-keep public class * extends android.content.ContentProvider {
    public <init>();
    *;
}

-keep public class * extends android.app.Application {
    public <init>();
    *;
}

-keep public class * extends android.content.BroadcastReceiver {
    public <init>();
    *;
}

-keep public class * extends android.app.Service {
    public <init>();
    *;
}

# Explicitly preserve MainActivity entrypoint and constructor
-keep public class tv.bootv.app.MainActivity {
    public <init>();
    public void applyEdgeToEdgeImmersive();
    public void enforceUpdateLockout(java.lang.String, java.lang.String, java.lang.String);
    *;
}

# Preserve AppFileProvider
-keep public class tv.bootv.app.AppFileProvider {
    public <init>();
    *;
}

# Preserve AppGuard entrypoints
-keep class tv.bootv.app.AppGuard {
    public static boolean verifyIntegrity(android.app.Activity);
    public static java.lang.String getTargetUrl();
    public static java.lang.String getMainDomain();
    public static java.lang.String getVersionUrl();
    public static java.lang.String getTelegramUrl();
    *;
}

# Preserve AppUpdateManager
-keep class tv.bootv.app.AppUpdateManager {
    public static *;
    *;
}

# Preserve SecurityManager & GestureController
-keep class tv.bootv.app.SecurityManager { *; }
-keep class tv.bootv.app.GestureController { *; }

# Obfuscation settings
-dontusemixedcaseclassnames
-allowaccessmodification
-renamesourcefileattribute "BOOTV"

# Suppress harmless warnings
-dontwarn android.**
-dontwarn java.**
