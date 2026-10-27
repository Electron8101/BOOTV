package tv.bootv.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AppUpdateManager {

    public static final String EXTRA_SHOW_UPDATE = "tv.bootv.app.EXTRA_SHOW_UPDATE";
    private static final String REMOTE_VERSION_URL =
            "https://raw.githubusercontent.com/Electron8101/BOOTV/main/version.json";

    private static final String PREF_NAME = "bootv_update_prefs";
    private static final String KEY_AUTO_UPDATE = "pref_auto_update_enabled";
    private static final String KEY_LAST_DISMISSED_VERSION = "pref_last_dismissed_version";
    private static final String KEY_LAST_DISMISSED_TIME = "pref_last_dismissed_time";

    private static final long DISMISS_SNOOZE_MS = 30 * 60 * 1000L; // 30 minutes snooze on 'Later'
    private static final String NOTIFICATION_CHANNEL_ID = "bootv_ota_channel";
    private static final int NOTIFICATION_ID = 2001;

    private static ScheduledExecutorService sScheduler;
    private static WeakReference<Activity> sCurrentActivityRef;
    private static AlertDialog sCurrentDialog;
    private static volatile boolean sIsChecking = false;

    public static boolean isAutoUpdateEnabled(Context context) {
        if (context == null) return true;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_AUTO_UPDATE, true);
    }

    public static void setAutoUpdateEnabled(Context context, boolean enabled) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_AUTO_UPDATE, enabled).apply();
    }

    public static synchronized void startPeriodicUpdateChecker(final Activity activity) {
        if (activity == null) return;
        sCurrentActivityRef = new WeakReference<>(activity);

        if (!isAutoUpdateEnabled(activity)) {
            return;
        }

        if (sScheduler != null && !sScheduler.isShutdown()) {
            return;
        }

        sScheduler = Executors.newSingleThreadScheduledExecutor();
        sScheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    if (sCurrentActivityRef == null) return;
                    final Activity act = sCurrentActivityRef.get();
                    if (act == null || act.isFinishing()) return;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && act.isDestroyed()) return;

                    if (isAutoUpdateEnabled(act)) {
                        checkForUpdates(act, false);
                    }
                } catch (Throwable ignored) {
                }
            }
        }, 2, 300, TimeUnit.SECONDS); // Check 2 seconds after startup, then every 5 minutes while running
    }

    public static synchronized void stopPeriodicUpdateChecker() {
        if (sScheduler != null && !sScheduler.isShutdown()) {
            try {
                sScheduler.shutdownNow();
            } catch (Exception ignored) {}
            sScheduler = null;
        }
    }

    public static void checkForUpdates(final Activity activity, final boolean isManualCheck) {
        if (activity == null || activity.isFinishing()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed()) return;

        if (sIsChecking && !isManualCheck) return;
        sIsChecking = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(REMOTE_VERSION_URL);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(6000);
                    conn.setReadTimeout(6000);
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("User-Agent", "BOOTV-Updater");
                    conn.connect();

                    if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        if (isManualCheck) showToast(activity, "Unable to reach update server right now");
                        return;
                    }

                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    reader.close();

                    JSONObject json = new JSONObject(sb.toString());
                    final int remoteVersionCode = json.optInt("versionCode", 0);
                    final String remoteVersionName = json.optString("versionName", "");
                    final String apkUrl = json.optString("apkUrl", "");
                    final String changelog = json.optString("changelog", "• Bug fixes and stability improvements");

                    PackageManager pm = activity.getPackageManager();
                    PackageInfo pInfo = pm.getPackageInfo(activity.getPackageName(), 0);
                    long currentVersionCode;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        currentVersionCode = pInfo.getLongVersionCode();
                    } else {
                        currentVersionCode = pInfo.versionCode;
                    }

                    final String currentVersionName = pInfo.versionName;

                    if (remoteVersionCode > currentVersionCode && !apkUrl.isEmpty()) {
                        final boolean forceUpdate = json.optBoolean("forceUpdate", true);

                        // If forceUpdate is true, old versions MUST update! No snooze or bypass allowed!
                        if (!forceUpdate && !isManualCheck && isDismissedRecently(activity, remoteVersionCode)) {
                            return;
                        }

                        // Post System Notification (Status Bar)
                        postSystemUpdateNotification(activity, remoteVersionName, changelog);

                        // Trigger Lockout and Instant On-Screen Popup
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!activity.isFinishing() && (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 || !activity.isDestroyed())) {
                                    if (forceUpdate && activity instanceof MainActivity) {
                                        ((MainActivity) activity).enforceUpdateLockout(remoteVersionName, changelog, apkUrl);
                                    }
                                    showUpdateDialog(activity, remoteVersionName, currentVersionName, apkUrl, changelog, remoteVersionCode, forceUpdate);
                                }
                            }
                        });
                    } else if (isManualCheck) {
                        showToast(activity, "BOOTV is up to date (v" + currentVersionName + ")");
                    }

                } catch (Exception e) {
                    if (isManualCheck) {
                        showToast(activity, "Check failed: " + e.getMessage());
                    }
                } finally {
                    sIsChecking = false;
                    if (conn != null) {
                        try { conn.disconnect(); } catch (Exception ignored) {}
                    }
                }
            }
        }).start();
    }

    private static boolean isDismissedRecently(Context context, int versionCode) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        int dismissedVersion = prefs.getInt(KEY_LAST_DISMISSED_VERSION, -1);
        long dismissedTime = prefs.getLong(KEY_LAST_DISMISSED_TIME, 0);
        if (dismissedVersion == versionCode) {
            long elapsed = System.currentTimeMillis() - dismissedTime;
            return elapsed < DISMISS_SNOOZE_MS;
        }
        return false;
    }

    private static void postSystemUpdateNotification(Context context, String remoteVersionName, String changelog) {
        try {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        NOTIFICATION_CHANNEL_ID,
                        "BOOTV App Updates",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Instant alerts for new BOOTV app updates");
                channel.enableLights(true);
                channel.setLightColor(0xFFFF3333);
                channel.enableVibration(true);
                nm.createNotificationChannel(channel);
            }

            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra(EXTRA_SHOW_UPDATE, true);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            PendingIntent pi = PendingIntent.getActivity(context, 101, intent, flags);

            Notification.Builder nb;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nb = new Notification.Builder(context, NOTIFICATION_CHANNEL_ID);
            } else {
                nb = new Notification.Builder(context);
                nb.setPriority(Notification.PRIORITY_HIGH);
            }

            nb.setSmallIcon(R.mipmap.ic_launcher)
              .setContentTitle("⚡ BOOTV Update Available: v" + remoteVersionName)
              .setContentText("A new update is available. Tap to install now.")
              .setAutoCancel(true)
              .setContentIntent(pi);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                nb.setStyle(new Notification.BigTextStyle()
                        .setBigContentTitle("⚡ BOOTV v" + remoteVersionName + " Available")
                        .bigText("Changelog:\n" + changelog + "\n\nTap to install the latest version immediately."));
            }

            nm.notify(NOTIFICATION_ID, nb.build());
        } catch (Throwable ignored) {
        }
    }

    private static void showUpdateDialog(final Activity activity,
                                         final String remoteVersionName,
                                         final String currentVersionName,
                                         final String apkUrl,
                                         final String changelog,
                                         final int remoteVersionCode,
                                         final boolean forceUpdate) {
        if (sCurrentDialog != null && sCurrentDialog.isShowing()) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        LayoutInflater inflater = activity.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_update, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        final AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        sCurrentDialog = dialog;

        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface d, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    activity.finishAffinity();
                    System.exit(0);
                    return true;
                }
                return true;
            }
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView txtVersionTitle = dialogView.findViewById(R.id.txt_update_version_title);
        TextView txtVersionSubtitle = dialogView.findViewById(R.id.txt_update_version_subtitle);
        TextView txtChangelog = dialogView.findViewById(R.id.txt_update_changelog);
        final LinearLayout layoutProgress = dialogView.findViewById(R.id.layout_download_progress);
        final ProgressBar pbDownload = dialogView.findViewById(R.id.pb_download);
        final TextView txtStatus = dialogView.findViewById(R.id.txt_download_status);
        final TextView txtPercent = dialogView.findViewById(R.id.txt_download_percent);
        final Button btnCancel = dialogView.findViewById(R.id.btn_update_cancel);
        final Button btnUpdate = dialogView.findViewById(R.id.btn_update_now);

        txtVersionTitle.setText("Update to v" + remoteVersionName);
        txtVersionSubtitle.setText("Mandatory Update • Tap Update Now to continue");
        btnCancel.setText("Later");
        txtChangelog.setText(changelog);

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If user presses Later, the app will close
                activity.finishAffinity();
                System.exit(0);
            }
        });

        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                prefs.edit().remove(KEY_LAST_DISMISSED_VERSION).remove(KEY_LAST_DISMISSED_TIME).apply();

                btnUpdate.setEnabled(false);
                btnCancel.setEnabled(false);
                layoutProgress.setVisibility(View.VISIBLE);
                startDownload(activity, apkUrl, pbDownload, txtStatus, txtPercent, dialog);
            }
        });

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface d) {
                if (sCurrentDialog == dialog) {
                    sCurrentDialog = null;
                }
                if (activity instanceof MainActivity) {
                    ((MainActivity) activity).applyEdgeToEdgeImmersive();
                }
            }
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(window.getAttributes());
            lp.dimAmount = 0.70f;
            int displayWidth = activity.getResources().getDisplayMetrics().widthPixels;
            int maxDialogWidth = (int) (460 * activity.getResources().getDisplayMetrics().density);
            lp.width = Math.min((int) (displayWidth * 0.90f), maxDialogWidth);
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(lp);
        }
    }

    private static void startDownload(final Activity activity,
                                      final String apkUrl,
                                      final ProgressBar progressBar,
                                      final TextView txtStatus,
                                      final TextView txtPercent,
                                      final AlertDialog dialog) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                InputStream input = null;
                FileOutputStream output = null;
                try {
                    URL url = new URL(apkUrl);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(15000);
                    conn.connect();

                    int fileLength = conn.getContentLength();
                    input = conn.getInputStream();

                    File cacheDir = activity.getExternalCacheDir();
                    if (cacheDir == null) cacheDir = activity.getCacheDir();

                    final File apkFile = new File(cacheDir, "BOOTV_update.apk");
                    if (apkFile.exists()) apkFile.delete();

                    output = new FileOutputStream(apkFile);

                    byte[] data = new byte[8192];
                    long total = 0;
                    int count;
                    int lastReportedPercent = -1;

                    while ((count = input.read(data)) != -1) {
                        total += count;
                        output.write(data, 0, count);

                        if (fileLength > 0) {
                            final int percent = (int) (total * 100 / fileLength);
                            if (percent != lastReportedPercent) {
                                lastReportedPercent = percent;
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        progressBar.setProgress(percent);
                                        txtPercent.setText(percent + "%");
                                    }
                                });
                            }
                        }
                    }

                    output.flush();

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txtStatus.setText("Download complete! Launching installer...");
                            txtPercent.setText("100%");
                            dialog.dismiss();
                            installApk(activity, apkFile);
                        }
                    });

                } catch (final Exception e) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(activity, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                        }
                    });
                } finally {
                    try { if (output != null) output.close(); } catch (Exception ignored) {}
                    try { if (input != null) input.close(); } catch (Exception ignored) {}
                    if (conn != null) {
                        try { conn.disconnect(); } catch (Exception ignored) {}
                    }
                }
            }
        }).start();
    }

    public static void installApk(Activity activity, File apkFile) {
        if (activity == null || apkFile == null || !apkFile.exists()) return;

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri apkUri;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                apkUri = AppFileProvider.getUriForFile(activity, apkFile);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                apkUri = Uri.fromFile(apkFile);
            }

            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);

        } catch (Exception e) {
            Toast.makeText(activity, "Unable to launch installer: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private static void showToast(final Context context, final String message) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
