package tv.bootv.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
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
import java.net.HttpURLConnection;
import java.net.URL;

public class AppUpdateManager {

    private static final String REMOTE_VERSION_URL =
            "https://raw.githubusercontent.com/Electron8101/BOOTV/main/version.json";

    public static void checkForUpdates(final Activity activity, final boolean isManualCheck) {
        if (activity == null || activity.isFinishing()) return;

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
                        if (isManualCheck) showToast(activity, "Unable to check for updates right now");
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
                    long currentVersionCode = 0;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        currentVersionCode = pInfo.getLongVersionCode();
                    } else {
                        currentVersionCode = pInfo.versionCode;
                    }

                    final String currentVersionName = pInfo.versionName;

                    if (remoteVersionCode > currentVersionCode && !apkUrl.isEmpty()) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!activity.isFinishing()) {
                                    showUpdateDialog(activity, remoteVersionName, currentVersionName, apkUrl, changelog);
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
                    if (conn != null) {
                        try { conn.disconnect(); } catch (Exception ignored) {}
                    }
                }
            }
        }).start();
    }

    private static void showUpdateDialog(final Activity activity,
                                         final String remoteVersionName,
                                         final String currentVersionName,
                                         final String apkUrl,
                                         final String changelog) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        LayoutInflater inflater = activity.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_update, null);
        builder.setView(dialogView);
        builder.setCancelable(true);

        final AlertDialog dialog = builder.create();
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
        txtVersionSubtitle.setText("Current: v" + currentVersionName + " • Ready to install");
        txtChangelog.setText(changelog);

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnUpdate.setEnabled(false);
                btnCancel.setEnabled(false);
                layoutProgress.setVisibility(View.VISIBLE);
                startDownload(activity, apkUrl, pbDownload, txtStatus, txtPercent, dialog);
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
