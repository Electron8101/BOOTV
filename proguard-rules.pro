# ProGuard / R8 Rules for BOOTV Anti-Reverse-Engineering & Minification

# Keep Android components & manifest entrypoints
-keep public class tv.bootv.app.MainActivity {
    public void applyEdgeToEdgeImmersive();
    public void enforceUpdateLockout(java.lang.String, java.lang.String, java.lang.String);
}

-keep public class tv.bootv.app.AppFileProvider { *; }

# Keep AppGuard entrypoints
-keep class tv.bootv.app.AppGuard {
    public static boolean verifyIntegrity(android.app.Activity);
    public static java.lang.String getTargetUrl();
    public static java.lang.String getMainDomain();
    public static java.lang.String getVersionUrl();
    public static java.lang.String getTelegramUrl();
}

# Keep AppUpdateManager entrypoints
-keep class tv.bootv.app.AppUpdateManager {
    public static void checkForUpdates(android.app.Activity, boolean);
    public static void startPeriodicUpdateChecker(android.app.Activity);
    public static void stopPeriodicUpdateChecker();
    public static boolean isAutoUpdateEnabled(android.content.Context);
    public static void setAutoUpdateEnabled(android.content.Context, boolean);
    public static final java.lang.String EXTRA_SHOW_UPDATE;
}

# Keep SecurityManager entrypoints
-keep class tv.bootv.app.SecurityManager { *; }
-keep class tv.bootv.app.GestureController { *; }

# Obfuscation settings
-dontusemixedcaseclassnames
-allowaccessmodification
-renamesourcefileattribute "BOOTV"

# Suppress harmless warnings
-dontwarn android.**
-dontwarn java.**
