package tv.bootv.app;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.biometrics.BiometricPrompt;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class SecurityManager {

    private static final String PREF_NAME = "siska_security";
    private static final String KEY_LOCK_ENABLED = "lock_enabled";
    private static final String KEY_PIN_HASH = "pin_hash";
    private static final String KEY_BIOMETRIC_ENABLED = "biometric_enabled";

    public interface BiometricCallback {
        void onSuccess();
        void onUsePin();
        void onError(String message);
        void onFailed();
    }

    private final SharedPreferences mPrefs;
    private final Context mContext;

    public SecurityManager(Context context) {
        mContext = context.getApplicationContext();
        mPrefs = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isDefaultPin() {
        return mPrefs.getBoolean("is_default_pin", false);
    }

    public boolean isLockEnabled() {
        return isPinSet() && mPrefs.getBoolean(KEY_LOCK_ENABLED, false);
    }

    public void setLockEnabled(boolean enabled) {
        mPrefs.edit().putBoolean(KEY_LOCK_ENABLED, enabled).apply();
    }

    public boolean isBiometricEnabled() {
        return isLockEnabled() && mPrefs.getBoolean(KEY_BIOMETRIC_ENABLED, false);
    }

    public void setBiometricEnabled(boolean enabled) {
        mPrefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply();
    }

    public boolean isPinSet() {
        String hash = mPrefs.getString(KEY_PIN_HASH, null);
        return hash != null && !hash.isEmpty();
    }

    public boolean verifyPin(String inputPin) {
        if (inputPin == null || inputPin.isEmpty()) return false;
        String storedHash = mPrefs.getString(KEY_PIN_HASH, "");
        String inputHash = hashString(inputPin);
        return storedHash.equals(inputHash);
    }

    public void savePin(String pin) {
        String hash = hashString(pin);
        mPrefs.edit()
                .putString(KEY_PIN_HASH, hash)
                .putInt("pin_length", pin.length())
                .putBoolean(KEY_LOCK_ENABLED, true)
                .putBoolean("is_default_pin", false)
                .apply();
    }

    public int getPinLength() {
        return mPrefs.getInt("pin_length", 4);
    }

    public void removePin() {
        mPrefs.edit()
                .remove(KEY_PIN_HASH)
                .remove("is_default_pin")
                .remove("pin_length")
                .putBoolean(KEY_LOCK_ENABLED, false)
                .putBoolean(KEY_BIOMETRIC_ENABLED, false)
                .commit();
    }

    private String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }

    public boolean isBiometricSupported() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false;
        }

        PackageManager pm = mContext.getPackageManager();
        if (pm == null) return false;

        boolean hasFeature = pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT);
        if (Build.VERSION.SDK_INT >= 29) {
            hasFeature = hasFeature || pm.hasSystemFeature("android.hardware.biometrics.face")
                    || pm.hasSystemFeature("android.hardware.biometrics.iris");
        }

        if (!hasFeature) return false;

        try {
            FingerprintManager fm = (FingerprintManager) mContext.getSystemService(Context.FINGERPRINT_SERVICE);
            if (fm != null) {
                return fm.isHardwareDetected() && fm.hasEnrolledFingerprints();
            }
        } catch (Exception ignored) {}

        return false;
    }

    public void authenticateWithBiometrics(final Activity activity, final BiometricCallback callback) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || !isBiometricSupported()) {
            callback.onUsePin();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                BiometricPrompt.Builder builder = new BiometricPrompt.Builder(activity)
                        .setTitle("Unlock BOOTV")
                        .setSubtitle("Confirm your fingerprint to access BOOTV")
                        .setDescription("Created by BOOTLOOP • Secure App Lock")
                        .setNegativeButton("Use PIN Code", activity.getMainExecutor(), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                callback.onUsePin();
                            }
                        });

                BiometricPrompt prompt = builder.build();
                CancellationSignal cancellationSignal = new CancellationSignal();

                prompt.authenticate(cancellationSignal, activity.getMainExecutor(), new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        callback.onSuccess();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        // errorCode 13 = BIOMETRIC_ERROR_NEGATIVE_BUTTON, 10 = USER_CANCELED, 5 = CANCELED
                        if (errorCode == 13 || errorCode == BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED || errorCode == BiometricPrompt.BIOMETRIC_ERROR_CANCELED) {
                            callback.onUsePin();
                        } else {
                            callback.onError(errString != null ? errString.toString() : "Biometric error");
                        }
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        callback.onFailed();
                    }
                });
            } catch (Exception e) {
                callback.onUsePin();
            }
        } else {
            try {
                FingerprintManager fm = (FingerprintManager) mContext.getSystemService(Context.FINGERPRINT_SERVICE);
                if (fm != null && fm.isHardwareDetected() && fm.hasEnrolledFingerprints()) {
                    CancellationSignal cancellationSignal = new CancellationSignal();
                    fm.authenticate(null, cancellationSignal, 0, new FingerprintManager.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                            super.onAuthenticationSucceeded(result);
                            callback.onSuccess();
                        }

                        @Override
                        public void onAuthenticationError(int errorCode, CharSequence errString) {
                            super.onAuthenticationError(errorCode, errString);
                            callback.onError(errString != null ? errString.toString() : "Fingerprint error");
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            super.onAuthenticationFailed();
                            callback.onFailed();
                        }
                    }, null);
                } else {
                    callback.onUsePin();
                }
            } catch (Exception e) {
                callback.onUsePin();
            }
        }
    }
}
