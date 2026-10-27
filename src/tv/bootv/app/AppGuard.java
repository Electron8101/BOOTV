package tv.bootv.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.webkit.CookieManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.security.MessageDigest;

/**
 * Enterprise-grade runtime integrity protection, dynamic string encryption,
 * anti-tamper signature validation, and anti-cloning defense system for BOOTV.
 */
public final class AppGuard {

    private static final int CIPHER_KEY = 0x7B;

    // Dynamic encrypted byte arrays (XOR + Transposition)
    private static final int[] ENC_TARGET_URL = {2, 19, 40, 57, 53, 9, 7, 10, 97, 102, 119, 154, 143, 245, 164, 187, 149};
    private static final int[] ENC_VERSION_URL = {2, 19, 40, 57, 53, 9, 7, 10, 96, 110, 115, 223, 137, 178, 164, 165, 207, 213, 217, 234, 243, 241, 27, 26, 12, 43, 49, 47, 74, 5, 67, 114, 103, 40, 185, 133, 131, 176, 188, 183, 221, 193, 156, 160, 190, 74, 95, 47, 21, 24, 24, 111, 25, 78, 121, 124, 108, 208, 130, 132, 172, 184, 169, 210, 196, 137, 246, 250, 233, 29};
    private static final int[] ENC_TG_URL = {2, 19, 40, 57, 53, 9, 7, 10, 102, 33, 105, 148, 193, 153, 159, 130, 238, 251, 227, 214, 198, 178};
    private static final int[] ENC_PKG = {30, 17, 114, 43, 41, 92, 92, 83, 60, 110, 116, 129};
    private static final int[] ENC_SIG = {46, 34, 110, 123, 7, 114, 30, 23, 87, 57, 54, 201, 175, 152, 224, 139, 140, 133, 237, 220, 212, 186, 74, 49, 85, 104, 102, 3, 127, 110, 102, 92, 79, 49, 185, 172, 211, 146, 137, 132, 240, 157, 144, 212, 184, 78, 71, 40, 104, 99, 14, 11, 7, 17, 93, 32, 67, 199, 205, 162, 239, 136, 130, 139};
    private static final int[] ENC_DOMAIN = {25, 14, 47, 34, 39, 29, 92, 83};

    private static String sDecryptedTargetUrl = null;
    private static String sDecryptedVersionUrl = null;
    private static String sDecryptedTgUrl = null;
    private static String sDecryptedPkg = null;
    private static String sDecryptedSig = null;
    private static String sDecryptedDomain = null;
    private static boolean sIntegrityVerified = false;

    private AppGuard() {}

    /**
     * Decrypts encrypted byte arrays dynamically in memory.
     */
    private static String decrypt(int[] cipher, int key) {
        if (cipher == null) return "";
        char[] chars = new char[cipher.length];
        for (int i = 0; i < cipher.length; i++) {
            chars[i] = (char) (cipher[i] ^ key ^ ((i * 11 + 17) & 0xFF));
        }
        return new String(chars);
    }

    public static synchronized String getTargetUrl() {
        if (sDecryptedTargetUrl == null) {
            sDecryptedTargetUrl = decrypt(ENC_TARGET_URL, CIPHER_KEY);
        }
        return sDecryptedTargetUrl;
    }

    public static synchronized String getMainDomain() {
        if (sDecryptedDomain == null) {
            sDecryptedDomain = decrypt(ENC_DOMAIN, CIPHER_KEY);
        }
        return sDecryptedDomain;
    }

    public static synchronized String getVersionUrl() {
        if (sDecryptedVersionUrl == null) {
            sDecryptedVersionUrl = decrypt(ENC_VERSION_URL, CIPHER_KEY);
        }
        return sDecryptedVersionUrl;
    }

    public static synchronized String getTelegramUrl() {
        if (sDecryptedTgUrl == null) {
            sDecryptedTgUrl = decrypt(ENC_TG_URL, CIPHER_KEY);
        }
        return sDecryptedTgUrl;
    }

    public static synchronized String getExpectedPackage() {
        if (sDecryptedPkg == null) {
            sDecryptedPkg = decrypt(ENC_PKG, CIPHER_KEY);
        }
        return sDecryptedPkg;
    }

    public static synchronized String getExpectedSignature() {
        if (sDecryptedSig == null) {
            sDecryptedSig = decrypt(ENC_SIG, CIPHER_KEY);
        }
        return sDecryptedSig;
    }

    /**
     * Complete cryptographic and environment verification.
     * Returns true if application is authentic, false if tampered/cloned.
     */
    public static boolean verifyIntegrity(final Activity activity) {
        if (sIntegrityVerified) return true;
        if (activity == null) return false;

        // 1. Anti-Clone Package Name Validation
        String currentPkg = activity.getPackageName();
        if (!getExpectedPackage().equals(currentPkg) && !"tv.bootv.app.beta".equals(currentPkg)) {
            triggerTamperAlert(activity, "App cloning or package renaming detected.");
            return false;
        }

        // 2. Cryptographic Signature Self-Verification
        String currentSig = getCertificateSha256(activity);
        String expectedSig = getExpectedSignature();
        if (currentSig == null || !currentSig.equalsIgnoreCase(expectedSig)) {
            triggerTamperAlert(activity, "Unauthorized modification or re-signing detected.");
            return false;
        }

        // 3. Anti-Debugging & Hooking Detection
        if (isDebuggerAttached(activity)) {
            triggerTamperAlert(activity, "Active debugging or instrumentation detected.");
            return false;
        }

        sIntegrityVerified = true;
        return true;
    }

    private static String getCertificateSha256(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            String pkgName = context.getPackageName();
            Signature[] signatures = null;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageInfo pi = pm.getPackageInfo(pkgName, PackageManager.GET_SIGNING_CERTIFICATES);
                if (pi.signingInfo != null) {
                    if (pi.signingInfo.hasMultipleSigners()) {
                        signatures = pi.signingInfo.getApkContentsSigners();
                    } else {
                        signatures = pi.signingInfo.getSigningCertificateHistory();
                    }
                }
            } else {
                PackageInfo pi = pm.getPackageInfo(pkgName, PackageManager.GET_SIGNATURES);
                signatures = pi.signatures;
            }

            if (signatures != null && signatures.length > 0) {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] digest = md.digest(signatures[0].toByteArray());
                StringBuilder sb = new StringBuilder();
                for (byte b : digest) {
                    sb.append(String.format("%02X", b));
                }
                return sb.toString();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static boolean isDebuggerAttached(Context context) {
        try {
            if (Debug.isDebuggerConnected() || Debug.waitingForDebugger()) {
                return true;
            }

            // Check if process is being traced via ptrace (Frida, GDB, LLDB)
            File status = new File("/proc/self/status");
            if (status.exists() && status.canRead()) {
                BufferedReader reader = new BufferedReader(new FileReader(status));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("TracerPid:")) {
                        String[] parts = line.split(":");
                        if (parts.length > 1) {
                            int tracerPid = Integer.parseInt(parts[1].trim());
                            if (tracerPid > 0) {
                                reader.close();
                                return true;
                            }
                        }
                        break;
                    }
                }
                reader.close();
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static void triggerTamperAlert(final Activity activity, final String detail) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        CookieManager.getInstance().removeAllCookies(null);
                    }
                } catch (Exception ignored) {}

                try {
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setTitle("🚨 Security Alert");
                    builder.setMessage("Security verification failed.\n\n"
                            + "This copy of BOOTV has been modified, re-signed, or cloned without permission.\n\n"
                            + "Execution has been permanently halted.");
                    builder.setCancelable(false);
                    builder.setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            terminate();
                        }
                    });

                    AlertDialog dialog = builder.create();
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.show();
                } catch (Exception e) {
                    terminate();
                }
            }
        });

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                terminate();
            }
        }, 3500);
    }

    private static void terminate() {
        Process.killProcess(Process.myPid());
        System.exit(0);
    }
}
