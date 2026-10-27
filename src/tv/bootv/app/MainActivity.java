package tv.bootv.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {

    private static final String TARGET_URL = "https://siska.tv/";
    private static final String MAIN_DOMAIN = "siska.tv";

    // Known ad networks, popunder CDNs, and telemetry hosts
    private static final String[] AD_HOST_KEYWORDS = {
        "xadsmart", "atwilio", "popads", "popcash", "popunder", "adsterra",
        "exoclick", "juicyads", "trafficjunky", "propellerads", "adcash",
        "onclick", "googlesyndication", "google-analytics", "googletagmanager",
        "doubleclick", "adservice", "adnxs", "syndication", "liveinternet.ru",
        "bet365", "1xbet", "melbet", "mostbet", "parimatch", "spinbetter",
        "vavada", "adserver", "trafficfactory", "realsrv", "ero-advertising",
        "tsyndicate", "cpmstar", "yadro.ru", "histats", "scorecardresearch",
        "whos.amung.us", "ad-delivery", "adform", "smartadserver", "zergnet",
        "mgid", "outbrain", "taboola", "revcontent", "clicksor", "adcolony",
        "adclick", "adroll", "adsupply", "popmyads", "hilltopads", "monetag",
        "richads", "clickadu", "bidvertiser", "yllix", "adtrue", "bidgear"
    };

    // Injected into third-party video iframes (/e/ embeds) to listen to postMessage commands
    private static final String EMBED_BRIDGE_SCRIPT =
        "<script>" +
        "(function() {" +
        "  function seek(delta) {" +
        "    try {" +
        "      if (window.jwplayer) {" +
        "        var p = (typeof window.jwplayer === 'function') ? window.jwplayer() : null;" +
        "        if (p && typeof p.seek === 'function' && typeof p.getPosition === 'function') {" +
        "          var cur = p.getPosition() || 0;" +
        "          var dur = (typeof p.getDuration === 'function' ? p.getDuration() : 999999) || 999999;" +
        "          p.seek(Math.max(0, Math.min(dur, cur + delta)));" +
        "          return;" +
        "        }" +
        "      }" +
        "    } catch(e) {}" +
        "    try {" +
        "      var vs = document.querySelectorAll('video');" +
        "      for (var i = 0; i < vs.length; i++) {" +
        "        var v = vs[i];" +
        "        var target = Math.max(0, v.currentTime + delta);" +
        "        if (v.duration) target = Math.min(v.duration, target);" +
        "        v.currentTime = target;" +
        "      }" +
        "    } catch(e) {}" +
        "  }" +
        "  function applyPlayState(shouldPlay) {" +
        "    try {" +
        "      if (window.jwplayer) {" +
        "        var p = (typeof window.jwplayer === 'function') ? window.jwplayer() : null;" +
        "        if (p) {" +
        "          if (shouldPlay) {" +
        "            if (typeof p.play === 'function') p.play(true);" +
        "          } else {" +
        "            if (typeof p.pause === 'function') p.pause(true);" +
        "            else if (typeof p.play === 'function') p.play(false);" +
        "          }" +
        "          return;" +
        "        }" +
        "      }" +
        "    } catch(e) {}" +
        "    try {" +
        "      if (window.videojs && typeof window.videojs.getAllPlayers === 'function') {" +
        "        var vjsList = window.videojs.getAllPlayers();" +
        "        for (var k = 0; k < vjsList.length; k++) {" +
        "          var pl = vjsList[k];" +
        "          if (pl) {" +
        "            if (shouldPlay) { if (typeof pl.play === 'function') pl.play(); }" +
        "            else { if (typeof pl.pause === 'function') pl.pause(); }" +
        "          }" +
        "        }" +
        "      }" +
        "    } catch(e) {}" +
        "    try {" +
        "      var vs = document.querySelectorAll('video');" +
        "      for (var i = 0; i < vs.length; i++) {" +
        "        var v = vs[i];" +
        "        if (shouldPlay) { var pr = v.play(); if (pr !== undefined) pr.catch(function(e){}); }" +
        "        else { v.pause(); }" +
        "      }" +
        "    } catch(e) {}" +
        "  }" +
        "  function setSpeed(speed) {" +
        "    try {" +
        "      if (window.jwplayer) {" +
        "        var p = (typeof window.jwplayer === 'function') ? window.jwplayer() : null;" +
        "        if (p && typeof p.setPlaybackRate === 'function') { p.setPlaybackRate(speed); }" +
        "      }" +
        "    } catch(e) {}" +
        "    try {" +
        "      var vs = document.querySelectorAll('video');" +
        "      for (var i = 0; i < vs.length; i++) vs[i].playbackRate = speed;" +
        "    } catch(e) {}" +
        "  }" +
        "  window.addEventListener('message', function(e) {" +
        "    if (!e.data || typeof e.data !== 'object') return;" +
        "    if (e.data.type === 'SISKA_VIDEO_SEEK') {" +
        "      seek(e.data.delta);" +
        "    } else if (e.data.type === 'SISKA_VIDEO_SET_PLAY') {" +
        "      applyPlayState(e.data.play);" +
        "    } else if (e.data.type === 'SISKA_VIDEO_TOGGLE') {" +
        "      applyPlayState(e.data.play !== undefined ? e.data.play : false);" +
        "    } else if (e.data.type === 'SISKA_VIDEO_SPEED') {" +
        "      setSpeed(e.data.speed);" +
        "    }" +
        "  });" +
        "})();" +
        "</script>";

    // Client-side script to neutralize ad popups and overlays
    private static final String ADBLOCK_JS =
        "(function() {" +
        "  try {" +
        "    window.open = function() { return null; };" +
        "    window.onbeforeunload = null;" +
        "    window.onunload = null;" +
        "    function cleanAdOverlays() {" +
        "      var badSelectors = [" +
        "        'iframe[src*=\"ad\"]', 'iframe[src*=\"pop\"]'," +
        "        'div[id*=\"pop\"]', 'div[class*=\"pop\"]'," +
        "        'div[id*=\"banner\"]', 'div[class*=\"banner\"]'," +
        "        'div[style*=\"z-index: 2147483647\"]'," +
        "        'div[style*=\"z-index: 999999\"]'," +
        "        'div[style*=\"z-index: 9999\"]'" +
        "      ];" +
        "      badSelectors.forEach(function(sel) {" +
        "        try {" +
        "          var items = document.querySelectorAll(sel);" +
        "          for (var i = 0; i < items.length; i++) {" +
        "            var el = items[i];" +
        "            if (!el.querySelector('video') && !el.querySelector('iframe')) {" +
        "              el.remove();" +
        "            }" +
        "          }" +
        "        } catch(e) {}" +
        "      });" +
        "    }" +
        "    document.addEventListener('click', function(e) {" +
        "      var target = e.target;" +
        "      while (target && target.tagName !== 'A') {" +
        "        target = target.parentElement;" +
        "      }" +
        "      if (target && target.tagName === 'A') {" +
        "        if (target.target === '_blank') {" +
        "          target.removeAttribute('target');" +
        "        }" +
        "        var href = target.getAttribute('href') || '';" +
        "        if (href && (href.startsWith('http://') || href.startsWith('https://') || href.startsWith('//'))) {" +
        "          if (!href.includes('siska.tv')) {" +
        "            e.preventDefault();" +
        "            e.stopPropagation();" +
        "          }" +
        "        }" +
        "      }" +
        "    }, true);" +
        "    cleanAdOverlays();" +
        "    setInterval(cleanAdOverlays, 1500);" +
        "  } catch(e) {}" +
        "})();";

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private SecurityManager mSecurityManager;
    private boolean mIsAuthenticated = false;
    private final StringBuilder mEnteredPin = new StringBuilder();

    private WebView mWebView;
    private FrameLayout mFullscreenContainer;
    private FrameLayout mVideoSurfaceHolder;
    private View mCustomView;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;

    private ProgressBar mProgressBar;
    private LinearLayout mErrorLayout;
    private Button mBtnRetry;

    // Gesture Controller & Top Bar Views
    private GestureController mGestureController;
    private View mGestureTouchView;
    private View mPlayerTopBar;
    private TextView mBtnPlayerClose;
    private TextView mBtnRewind10;
    private TextView mBtnPlayPause;
    private TextView mBtnForward10;
    private TextView mBtnAspectRatio;
    private TextView mBtnSpeed;
    private TextView mBtnLock;

    // UI & Security Elements
    private FrameLayout mFabSettings;
    private LinearLayout mPopupBootloop;
    private View mLockOverlay;
    private View mDot1, mDot2, mDot3, mDot4;

    private int mOriginalOrientation;
    private String mUserAgentString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSecurityManager = new SecurityManager(this);

        // Enable edge-to-edge into display cutout / notch area
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }

        // Keep screen awake while streaming
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);
        applyEdgeToEdgeImmersive();

        mWebView = findViewById(R.id.webview);
        mFullscreenContainer = findViewById(R.id.fullscreen_container);
        mVideoSurfaceHolder = findViewById(R.id.video_surface_holder);
        mProgressBar = findViewById(R.id.progressBar);
        mErrorLayout = findViewById(R.id.error_layout);
        mBtnRetry = findViewById(R.id.btn_retry);

        // Gesture and Player Controls
        mGestureTouchView = findViewById(R.id.gesture_touch_view);
        mPlayerTopBar = findViewById(R.id.player_top_bar);
        mBtnPlayerClose = findViewById(R.id.btn_player_close);
        mBtnRewind10 = findViewById(R.id.btn_rewind_10);
        mBtnPlayPause = findViewById(R.id.btn_play_pause);
        mBtnForward10 = findViewById(R.id.btn_forward_10);
        mBtnAspectRatio = findViewById(R.id.btn_aspect_ratio);
        mBtnSpeed = findViewById(R.id.btn_speed);
        mBtnLock = findViewById(R.id.btn_lock);

        // UI & Security Elements
        mFabSettings = findViewById(R.id.fab_settings);
        mPopupBootloop = findViewById(R.id.popup_bootloop);
        mLockOverlay = findViewById(R.id.lock_overlay);
        mDot1 = findViewById(R.id.dot_1);
        mDot2 = findViewById(R.id.dot_2);
        mDot3 = findViewById(R.id.dot_3);
        mDot4 = findViewById(R.id.dot_4);

        setupGestureController();
        setupWebView();
        setupSettingsAndKeypad();
        showBootloopSignaturePopup();

        mBtnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mErrorLayout.setVisibility(View.GONE);
                mWebView.setVisibility(View.VISIBLE);
                mWebView.reload();
            }
        });

        // Enforce lock verification
        checkSecurityLock();

        if (savedInstanceState != null) {
            mWebView.restoreState(savedInstanceState);
        } else {
            mWebView.loadUrl(TARGET_URL);
        }

        // Automatic background OTA update check
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) {
                    AppUpdateManager.checkForUpdates(MainActivity.this, false);
                }
            }
        }, 2500);
    }

    private void showBootloopSignaturePopup() {
        mPopupBootloop.setVisibility(View.VISIBLE);
        mPopupBootloop.setAlpha(0f);
        mPopupBootloop.animate().alpha(1f).setDuration(400).start();

        // Auto dismiss after 3.5 seconds
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mPopupBootloop != null) {
                    mPopupBootloop.animate().alpha(0f).setDuration(500).withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            mPopupBootloop.setVisibility(View.GONE);
                        }
                    }).start();
                }
            }
        }, 3500);
    }

    private void updatePinDots() {
        int len = mEnteredPin.length();
        if (mDot1 != null) mDot1.setBackgroundResource(len >= 1 ? R.drawable.pin_dot_filled : R.drawable.pin_dot_empty);
        if (mDot2 != null) mDot2.setBackgroundResource(len >= 2 ? R.drawable.pin_dot_filled : R.drawable.pin_dot_empty);
        if (mDot3 != null) mDot3.setBackgroundResource(len >= 3 ? R.drawable.pin_dot_filled : R.drawable.pin_dot_empty);
        if (mDot4 != null) mDot4.setBackgroundResource(len >= 4 ? R.drawable.pin_dot_filled : R.drawable.pin_dot_empty);
    }

    private void onKeypadDigit(String digit) {
        if (mEnteredPin.length() < 6) {
            mEnteredPin.append(digit);
            updatePinDots();

            // INSTANT AUTO-UNLOCK: As soon as PIN matches, enter app automatically!
            if (mSecurityManager.verifyPin(mEnteredPin.toString())) {
                unlockApp();
                mEnteredPin.setLength(0);
                updatePinDots();
                return;
            }

            int requiredLen = mSecurityManager.getPinLength();
            if (mEnteredPin.length() >= requiredLen) {
                // Incorrect PIN entered
                Toast.makeText(MainActivity.this, "Incorrect PIN. Try again.", Toast.LENGTH_SHORT).show();
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mEnteredPin.setLength(0);
                        updatePinDots();
                    }
                }, 250);
            }
        }
    }

    private void setupSettingsAndKeypad() {
        // Floating Action Button (60% transparent)
        mFabSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettingsDialog();
            }
        });

        // Numeric Keypad Click Listeners
        int[] keyIds = {R.id.key_0, R.id.key_1, R.id.key_2, R.id.key_3, R.id.key_4, R.id.key_5, R.id.key_6, R.id.key_7, R.id.key_8, R.id.key_9};
        for (int id : keyIds) {
            final TextView key = findViewById(id);
            if (key != null) {
                key.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onKeypadDigit(key.getText().toString());
                    }
                });
            }
        }

        View keyDel = findViewById(R.id.key_del);
        if (keyDel != null) {
            keyDel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mEnteredPin.length() > 0) {
                        mEnteredPin.deleteCharAt(mEnteredPin.length() - 1);
                        updatePinDots();
                    }
                }
            });
        }

        View keyBio = findViewById(R.id.key_bio);
        if (keyBio != null) {
            keyBio.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    triggerBiometricUnlock();
                }
            });
        }
    }

    private void checkSecurityLock() {
        if (!mSecurityManager.isPinSet() || !mSecurityManager.isLockEnabled()) {
            mLockOverlay.setVisibility(View.GONE);
            mIsAuthenticated = true;
            return;
        }

        if (!mIsAuthenticated) {
            mLockOverlay.setVisibility(View.VISIBLE);
            mLockOverlay.bringToFront();
            mEnteredPin.setLength(0);
            updatePinDots();

            TextView txtPrompt = findViewById(R.id.txt_lock_prompt);
            if (txtPrompt != null) {
                if (mSecurityManager.isDefaultPin()) {
                    txtPrompt.setText("Enter PIN to Unlock\n(Default: 0000)");
                } else {
                    txtPrompt.setText("Enter PIN to Unlock");
                }
            }

            if (mSecurityManager.isBiometricEnabled() && mSecurityManager.isBiometricSupported()) {
                triggerBiometricUnlock();
            }
        } else {
            mLockOverlay.setVisibility(View.GONE);
        }
    }

    private void triggerBiometricUnlock() {
        mSecurityManager.authenticateWithBiometrics(this, new SecurityManager.BiometricCallback() {
            @Override
            public void onSuccess() {
                unlockApp();
            }

            @Override
            public void onUsePin() {
                // Keep keypad active
            }

            @Override
            public void onError(String message) {
                // Keep keypad active
            }

            @Override
            public void onFailed() {
                Toast.makeText(MainActivity.this, "Fingerprint not recognized", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void unlockApp() {
        mIsAuthenticated = true;
        mLockOverlay.setVisibility(View.GONE);
        applyEdgeToEdgeImmersive();
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_settings, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView btnClose = dialogView.findViewById(R.id.btn_close_settings);
        Button btnDone = dialogView.findViewById(R.id.btn_done_settings);
        final Switch switchPin = dialogView.findViewById(R.id.switch_pin_lock);
        final Button btnChangePin = dialogView.findViewById(R.id.btn_change_pin);
        final Button btnRemovePin = dialogView.findViewById(R.id.btn_remove_pin);
        final Button btnLockNow = dialogView.findViewById(R.id.btn_lock_now);
        final Switch switchFingerprint = dialogView.findViewById(R.id.switch_fingerprint);
        Button btnClearCache = dialogView.findViewById(R.id.btn_clear_cache);

        final Runnable refreshPinButtons = new Runnable() {
            @Override
            public void run() {
                boolean hasPin = mSecurityManager.isPinSet();
                boolean lockEnabled = mSecurityManager.isLockEnabled();
                btnChangePin.setText(hasPin ? "🔑 Change PIN Code" : "🔑 Set PIN Code");
                btnChangePin.setEnabled(true);
                btnChangePin.setAlpha(1.0f);
                if (btnRemovePin != null) {
                    btnRemovePin.setVisibility(hasPin ? View.VISIBLE : View.GONE);
                }
                if (btnLockNow != null) {
                    btnLockNow.setVisibility((hasPin && lockEnabled) ? View.VISIBLE : View.GONE);
                }
                switchFingerprint.setEnabled(hasPin && lockEnabled && mSecurityManager.isBiometricSupported());
            }
        };

        if (btnLockNow != null) {
            btnLockNow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    mIsAuthenticated = false;
                    checkSecurityLock();
                }
            });
        }

        switchPin.setChecked(mSecurityManager.isLockEnabled());
        switchFingerprint.setChecked(mSecurityManager.isBiometricEnabled());
        refreshPinButtons.run();

        switchPin.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!mSecurityManager.isPinSet()) {
                        showPinSetupDialog(new Runnable() {
                            @Override
                            public void run() {
                                mSecurityManager.setLockEnabled(true);
                                switchPin.setChecked(true);
                                switchFingerprint.setEnabled(mSecurityManager.isBiometricSupported());
                                refreshPinButtons.run();
                            }
                        }, new Runnable() {
                            @Override
                            public void run() {
                                switchPin.setChecked(false);
                                refreshPinButtons.run();
                            }
                        });
                    } else {
                        mSecurityManager.setLockEnabled(true);
                        switchFingerprint.setEnabled(mSecurityManager.isBiometricSupported());
                        refreshPinButtons.run();
                    }
                } else {
                    mSecurityManager.setLockEnabled(false);
                    if (mSecurityManager.isDefaultPin()) {
                        mSecurityManager.removePin();
                    }
                    switchFingerprint.setChecked(false);
                    switchFingerprint.setEnabled(false);
                    refreshPinButtons.run();
                    mEnteredPin.setLength(0);
                    mIsAuthenticated = true;
                    mLockOverlay.setVisibility(View.GONE);
                }
            }
        });

        btnChangePin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPinSetupDialog(new Runnable() {
                    @Override
                    public void run() {
                        refreshPinButtons.run();
                    }
                }, null);
            }
        });

        btnRemovePin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Remove Password")
                        .setMessage("Are you sure you want to remove password protection? BOOTV will open without a PIN or fingerprint.")
                        .setPositiveButton("Remove", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                mSecurityManager.removePin();
                                switchPin.setChecked(false);
                                switchFingerprint.setChecked(false);
                                switchFingerprint.setEnabled(false);
                                refreshPinButtons.run();
                                mEnteredPin.setLength(0);
                                mIsAuthenticated = true;
                                mLockOverlay.setVisibility(View.GONE);
                                Toast.makeText(MainActivity.this, "Password protection removed", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        switchFingerprint.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && !mSecurityManager.isBiometricSupported()) {
                    Toast.makeText(MainActivity.this, "Fingerprint hardware not available on this device", Toast.LENGTH_SHORT).show();
                    switchFingerprint.setChecked(false);
                    return;
                }
                mSecurityManager.setBiometricEnabled(isChecked);
            }
        });

        btnClearCache.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWebView.clearCache(true);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    CookieManager.getInstance().removeAllCookies(null);
                }
                Toast.makeText(MainActivity.this, "Cache and Cookies cleared successfully!", Toast.LENGTH_SHORT).show();
            }
        });

        Button btnCheckUpdate = dialogView.findViewById(R.id.btn_check_update);
        if (btnCheckUpdate != null) {
            btnCheckUpdate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AppUpdateManager.checkForUpdates(MainActivity.this, true);
                }
            });
        }

        Button btnTelegram = dialogView.findViewById(R.id.btn_telegram_owner);
        if (btnTelegram != null) {
            btnTelegram.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/BOOTLOOP1"));
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Unable to open Telegram link", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        View.OnClickListener dismissListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                applyEdgeToEdgeImmersive();
            }
        };

        if (btnClose != null) btnClose.setOnClickListener(dismissListener);
        if (btnDone != null) btnDone.setOnClickListener(dismissListener);

        dialog.show();

        if (dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(window.getAttributes());
            lp.dimAmount = 0.65f;

            int displayWidth = getResources().getDisplayMetrics().widthPixels;
            int maxDialogWidth = (int) (480 * getResources().getDisplayMetrics().density);
            lp.width = Math.min((int) (displayWidth * 0.92f), maxDialogWidth);
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity = Gravity.CENTER;
            window.setAttributes(lp);
        }
    }

    private void showPinSetupDialog(final Runnable onSuccess, final Runnable onCancel) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_pin_setup, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        final EditText edtPin = dialogView.findViewById(R.id.edt_pin_input);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel_pin);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm_pin);

        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pin = edtPin.getText().toString().trim();
                if (pin.length() < 4) {
                    Toast.makeText(MainActivity.this, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show();
                    return;
                }
                mSecurityManager.savePin(pin);
                Toast.makeText(MainActivity.this, "PIN saved successfully!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                if (onSuccess != null) onSuccess.run();
                applyEdgeToEdgeImmersive();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if (onCancel != null) onCancel.run();
                applyEdgeToEdgeImmersive();
            }
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(window.getAttributes());
            lp.dimAmount = 0.65f;

            int displayWidth = getResources().getDisplayMetrics().widthPixels;
            int maxDialogWidth = (int) (400 * getResources().getDisplayMetrics().density);
            lp.width = Math.min((int) (displayWidth * 0.88f), maxDialogWidth);
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity = Gravity.CENTER;
            window.setAttributes(lp);
        }

        edtPin.requestFocus();
        edtPin.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(edtPin, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        }, 150);
    }

    private void applyEdgeToEdgeImmersive() {
        int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            applyEdgeToEdgeImmersive();
        }
    }

    private void setupGestureController() {
        View hudBrightness = findViewById(R.id.hud_brightness);
        TextView txtBrightness = findViewById(R.id.txt_brightness);
        ProgressBar pbBrightness = findViewById(R.id.pb_brightness);

        View hudVolume = findViewById(R.id.hud_volume);
        TextView txtVolume = findViewById(R.id.txt_volume);
        ProgressBar pbVolume = findViewById(R.id.pb_volume);

        View hudSeek = findViewById(R.id.hud_seek);
        TextView txtSeekDelta = findViewById(R.id.txt_seek_delta);
        TextView hudFeedback = findViewById(R.id.hud_feedback);

        mGestureController = new GestureController(
                this,
                mGestureTouchView,
                mPlayerTopBar,
                mBtnRewind10,
                mBtnPlayPause,
                mBtnForward10,
                hudBrightness, txtBrightness, pbBrightness,
                hudVolume, txtVolume, pbVolume,
                hudSeek, txtSeekDelta,
                hudFeedback,
                new GestureController.VideoActionListener() {
                    @Override
                    public void onSeek(int secondsDelta) {
                        executeVideoSeek(secondsDelta);
                    }

                    @Override
                    public void onPlaybackControl(boolean shouldPlay) {
                        executeVideoPlaybackState(shouldPlay);
                    }

                    @Override
                    public void onSpeedChange(float speed) {
                        executeVideoSpeed(speed);
                        mBtnSpeed.setText("⚡ " + speed + "x");
                    }

                    @Override
                    public void onAspectRatioChange(int mode) {
                        applyAspectRatio(mode);
                    }
                }
        );

        mBtnPlayerClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitFullscreenVideo();
            }
        });

        mBtnAspectRatio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int mode = mGestureController.cycleAspectRatio();
                switch (mode) {
                    case 1: mBtnAspectRatio.setText("⤢ Fill"); break;
                    case 2: mBtnAspectRatio.setText("⤢ Stretch"); break;
                    default: mBtnAspectRatio.setText("⤢ Fit"); break;
                }
            }
        });

        mBtnSpeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGestureController.cycleSpeed();
            }
        });

        mBtnLock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean locked = mGestureController.toggleLock();
                mBtnLock.setText(locked ? "🔒 Locked" : "🔓 Lock");
            }
        });
    }

    private void applyAspectRatio(int mode) {
        if (mVideoSurfaceHolder == null) return;
        if (mode == 1) {
            // Fill / Zoom to Notch: scale to cover full width/height
            mVideoSurfaceHolder.setScaleX(1.18f);
            mVideoSurfaceHolder.setScaleY(1.18f);
            executeJs("try { document.querySelectorAll('video').forEach(function(v){ v.style.objectFit='cover'; }); } catch(e){}");
        } else if (mode == 2) {
            // Stretch mode
            mVideoSurfaceHolder.setScaleX(1.10f);
            mVideoSurfaceHolder.setScaleY(1.0f);
            executeJs("try { document.querySelectorAll('video').forEach(function(v){ v.style.objectFit='fill'; }); } catch(e){}");
        } else {
            // Default Fit mode
            mVideoSurfaceHolder.setScaleX(1.0f);
            mVideoSurfaceHolder.setScaleY(1.0f);
            executeJs("try { document.querySelectorAll('video').forEach(function(v){ v.style.objectFit='contain'; }); } catch(e){}");
        }
    }

    private void executeJs(String script) {
        if (mWebView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mWebView.evaluateJavascript(script, null);
        }
    }

    private void executeVideoSeek(int secondsDelta) {
        String script =
            "(function(delta) {" +
            "  function seek(win) {" +
            "    if (!win) return;" +
            "    try {" +
            "      if (win.jwplayer) {" +
            "        var p = (typeof win.jwplayer === 'function') ? win.jwplayer() : null;" +
            "        if (p && typeof p.seek === 'function' && typeof p.getPosition === 'function') {" +
            "          var cur = p.getPosition() || 0;" +
            "          var dur = (typeof p.getDuration === 'function' ? p.getDuration() : 999999) || 999999;" +
            "          p.seek(Math.max(0, Math.min(dur, cur + delta)));" +
            "        }" +
            "      }" +
            "    } catch(e) {}" +
            "    try {" +
            "      var vs = win.document.querySelectorAll('video');" +
            "      for (var i = 0; i < vs.length; i++) {" +
            "        var v = vs[i];" +
            "        var target = Math.max(0, v.currentTime + delta);" +
            "        if (v.duration) target = Math.min(v.duration, target);" +
            "        v.currentTime = target;" +
            "      }" +
            "    } catch(e) {}" +
            "    try {" +
            "      win.postMessage({ type: 'SISKA_VIDEO_SEEK', delta: delta }, '*');" +
            "    } catch(e) {}" +
            "    try {" +
            "      for (var j = 0; j < win.frames.length; j++) {" +
            "        seek(win.frames[j]);" +
            "      }" +
            "    } catch(e) {}" +
            "  }" +
            "  seek(window);" +
            "})(" + secondsDelta + ");";
        executeJs(script);

        // Also dispatch keyboard arrow keys directly to active video custom view
        if (mCustomView != null) {
            int keyCode = secondsDelta > 0 ? KeyEvent.KEYCODE_DPAD_RIGHT : KeyEvent.KEYCODE_DPAD_LEFT;
            int repeat = Math.max(1, Math.min(6, Math.abs(secondsDelta) / 5));
            for (int i = 0; i < repeat; i++) {
                mCustomView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
                mCustomView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
            }
        }
    }

    private void executeVideoPlaybackState(boolean shouldPlay) {
        String script =
            "(function(shouldPlay) {" +
            "  function applyState(win) {" +
            "    if (!win) return;" +
            "    try {" +
            "      if (win.jwplayer) {" +
            "        var p = (typeof win.jwplayer === 'function') ? win.jwplayer() : null;" +
            "        if (p) {" +
            "          if (shouldPlay) {" +
            "            if (typeof p.play === 'function') p.play(true);" +
            "          } else {" +
            "            if (typeof p.pause === 'function') p.pause(true);" +
            "            else if (typeof p.play === 'function') p.play(false);" +
            "          }" +
            "        }" +
            "      }" +
            "    } catch(e) {}" +
            "    try {" +
            "      if (win.videojs && typeof win.videojs.getAllPlayers === 'function') {" +
            "        var vjsList = win.videojs.getAllPlayers();" +
            "        for (var k = 0; k < vjsList.length; k++) {" +
            "          var pl = vjsList[k];" +
            "          if (pl) {" +
            "            if (shouldPlay) { if (typeof pl.play === 'function') pl.play(); }" +
            "            else { if (typeof pl.pause === 'function') pl.pause(); }" +
            "          }" +
            "        }" +
            "      }" +
            "    } catch(e) {}" +
            "    try {" +
            "      var vs = win.document.querySelectorAll('video');" +
            "      for (var i = 0; i < vs.length; i++) {" +
            "        var v = vs[i];" +
            "        if (shouldPlay) { var pr = v.play(); if (pr !== undefined) pr.catch(function(e){}); }" +
            "        else { v.pause(); }" +
            "      }" +
            "    } catch(e) {}" +
            "    try {" +
            "      win.postMessage({ type: 'SISKA_VIDEO_SET_PLAY', play: shouldPlay }, '*');" +
            "    } catch(e) {}" +
            "    try {" +
            "      for (var j = 0; j < win.frames.length; j++) {" +
            "        applyState(win.frames[j]);" +
            "      }" +
            "    } catch(e) {}" +
            "  }" +
            "  applyState(window);" +
            "})(" + shouldPlay + ");";
        executeJs(script);

        int targetKey = shouldPlay ? KeyEvent.KEYCODE_MEDIA_PLAY : KeyEvent.KEYCODE_MEDIA_PAUSE;
        if (mCustomView != null) {
            mCustomView.requestFocus();
            mCustomView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, targetKey));
            mCustomView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, targetKey));
        }
        if (mWebView != null) {
            mWebView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, targetKey));
            mWebView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, targetKey));
        }
    }

    private void executeVideoSpeed(float speed) {
        String script =
            "(function(s) {" +
            "  function speed(win) {" +
            "    if (!win) return;" +
            "    try {" +
            "      if (win.jwplayer) {" +
            "        var p = (typeof win.jwplayer === 'function') ? win.jwplayer() : null;" +
            "        if (p && typeof p.setPlaybackRate === 'function') p.setPlaybackRate(s);" +
            "      }" +
            "    } catch(e) {}" +
            "    try {" +
            "      var vs = win.document.querySelectorAll('video');" +
            "      for (var i = 0; i < vs.length; i++) vs[i].playbackRate = s;" +
            "    } catch(e) {}" +
            "    try {" +
            "      win.postMessage({ type: 'SISKA_VIDEO_SPEED', speed: s }, '*');" +
            "    } catch(e) {}" +
            "    try {" +
            "      for (var j = 0; j < win.frames.length; j++) {" +
            "        speed(win.frames[j]);" +
            "      }" +
            "    } catch(e) {}" +
            "  }" +
            "  speed(window);" +
            "})(" + speed + ");";
        executeJs(script);
    }

    private boolean isAdOrTracker(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();
        for (String keyword : AD_HOST_KEYWORDS) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private void injectAdBlockScript(WebView view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            view.evaluateJavascript(ADBLOCK_JS, null);
        } else {
            view.loadUrl("javascript:" + ADBLOCK_JS);
        }
    }

    private WebResourceResponse injectBridgeIntoEmbed(String urlString) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", mUserAgentString);
            conn.setRequestProperty("Referer", "https://siska.tv/");
            conn.setInstanceFollowRedirects(true);

            int status = conn.getResponseCode();
            if (status >= 300 && status < 400) {
                String redirect = conn.getHeaderField("Location");
                if (redirect != null) return injectBridgeIntoEmbed(redirect);
            }
            if (status != 200) return null;

            String contentType = conn.getContentType();
            if (contentType == null || !contentType.contains("text/html")) return null;

            InputStream in = conn.getInputStream();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[8192];
            int nRead;
            while ((nRead = in.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            String html = new String(buffer.toByteArray(), "UTF-8");

            String injectedHtml;
            if (html.contains("<head>")) {
                injectedHtml = html.replace("<head>", "<head>" + EMBED_BRIDGE_SCRIPT);
            } else if (html.contains("<HEAD>")) {
                injectedHtml = html.replace("<HEAD>", "<HEAD>" + EMBED_BRIDGE_SCRIPT);
            } else {
                injectedHtml = EMBED_BRIDGE_SCRIPT + html;
            }

            byte[] modifiedBytes = injectedHtml.getBytes("UTF-8");
            return new WebResourceResponse("text/html", "UTF-8", new ByteArrayInputStream(modifiedBytes));
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) {
                try { conn.disconnect(); } catch (Exception ignored) {}
            }
        }
    }

    private void setupWebView() {
        WebSettings settings = mWebView.getSettings();

        // Core capabilities & streaming stability
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        // Anti-redirect & anti-popup settings
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);

        // Media streaming optimizations & render priority
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);
            cookieManager.setAcceptThirdPartyCookies(mWebView, true);
        }

        // Clean user agent
        String defaultUa = settings.getUserAgentString();
        if (defaultUa != null) {
            mUserAgentString = defaultUa.replace("; wv", "");
            settings.setUserAgentString(mUserAgentString);
        } else {
            mUserAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36";
        }

        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                mProgressBar.setVisibility(View.VISIBLE);
                mErrorLayout.setVisibility(View.GONE);
                injectAdBlockScript(view);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                mProgressBar.setVisibility(View.GONE);
                injectAdBlockScript(view);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request != null && request.isForMainFrame()) {
                    mProgressBar.setVisibility(View.GONE);
                    mWebView.setVisibility(View.GONE);
                    mErrorLayout.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                // Recover gracefully without crashing if an embedded player exhausts GPU memory
                if (mCustomView != null) {
                    exitFullscreenVideo();
                }
                if (view != null) {
                    view.destroy();
                    setupWebView();
                    mWebView.loadUrl(TARGET_URL);
                }
                return true;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (request != null && request.getUrl() != null) {
                    String url = request.getUrl().toString();
                    if (isAdOrTracker(url)) {
                        return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(new byte[0]));
                    }
                    boolean isEmbedCandidate = (url.contains("/e/") || url.contains("/embed/") || url.contains("/stream/") || url.contains("/v/") || url.contains("/player/"))
                            && !url.contains(".js") && !url.contains(".css") && !url.contains(".png") && !url.contains(".jpg") && !url.contains(".m3u8") && !url.contains(".ts") && !url.contains(".mp4");
                    if (isEmbedCandidate) {
                        WebResourceResponse injected = injectBridgeIntoEmbed(url);
                        if (injected != null) return injected;
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @SuppressWarnings("deprecation")
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                if (isAdOrTracker(url)) {
                    return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(new byte[0]));
                }
                if (url != null) {
                    boolean isEmbedCandidate = (url.contains("/e/") || url.contains("/embed/") || url.contains("/stream/") || url.contains("/v/") || url.contains("/player/"))
                            && !url.contains(".js") && !url.contains(".css") && !url.contains(".png") && !url.contains(".jpg") && !url.contains(".m3u8") && !url.contains(".ts") && !url.contains(".mp4");
                    if (isEmbedCandidate) {
                        WebResourceResponse injected = injectBridgeIntoEmbed(url);
                        if (injected != null) return injected;
                    }
                }
                return super.shouldInterceptRequest(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request == null || request.getUrl() == null) return false;
                Uri uri = request.getUrl();
                return handleUrlNavigation(view, uri, request.isForMainFrame());
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url == null) return false;
                return handleUrlNavigation(view, Uri.parse(url), true);
            }

            private boolean handleUrlNavigation(WebView view, Uri uri, boolean isMainFrame) {
                String scheme = uri.getScheme();
                if (scheme == null) return true;

                if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
                    return true;
                }

                String urlString = uri.toString();
                if (isAdOrTracker(urlString)) {
                    return true;
                }

                String host = uri.getHost();
                if (host == null) return true;

                if (isMainFrame) {
                    if (host.endsWith(MAIN_DOMAIN) || host.equals(MAIN_DOMAIN)) {
                        return false;
                    }
                    return true;
                }

                return false;
            }
        });

        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    mProgressBar.setVisibility(View.VISIBLE);
                } else {
                    mProgressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                return false;
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (mCustomView != null) {
                    callback.onCustomViewHidden();
                    return;
                }

                mCustomView = view;
                mCustomViewCallback = callback;
                mOriginalOrientation = getRequestedOrientation();

                mWebView.setVisibility(View.GONE);
                mFabSettings.setVisibility(View.GONE); // Hide FAB in fullscreen video
                mFullscreenContainer.setVisibility(View.VISIBLE);

                mVideoSurfaceHolder.removeAllViews();
                mVideoSurfaceHolder.addView(view, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                ));

                applyEdgeToEdgeImmersive();
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

                mGestureController.setCustomView(view);
                mGestureController.reset();
            }

            @Override
            public void onHideCustomView() {
                exitFullscreenVideo();
            }
        });
    }

    private void exitFullscreenVideo() {
        if (mCustomView == null) return;

        mVideoSurfaceHolder.removeAllViews();
        mVideoSurfaceHolder.setScaleX(1.0f);
        mVideoSurfaceHolder.setScaleY(1.0f);
        mFullscreenContainer.setVisibility(View.GONE);
        mFabSettings.setVisibility(View.VISIBLE); // Restore FAB
        mWebView.setVisibility(View.VISIBLE);

        // Reset brightness to system default when exiting fullscreen!
        if (mGestureController != null) {
            mGestureController.restoreDefaultBrightness();
        }

        applyEdgeToEdgeImmersive();
        setRequestedOrientation(mOriginalOrientation);

        if (mCustomViewCallback != null) {
            mCustomViewCallback.onCustomViewHidden();
        }

        mGestureController.setCustomView(null);
        mCustomView = null;
        mCustomViewCallback = null;
    }

    @Override
    public void onBackPressed() {
        // If locked, back button should not bypass lock
        if (mLockOverlay.getVisibility() == View.VISIBLE) {
            finish();
            return;
        }

        if (mCustomView != null) {
            exitFullscreenVideo();
            return;
        }

        if (mWebView != null && mWebView.canGoBack()) {
            mWebView.goBack();
            return;
        }

        super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mWebView != null) {
            mWebView.saveState(outState);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsAuthenticated = false;
        if (mGestureController != null) {
            mGestureController.restoreDefaultBrightness();
        }
        if (mWebView != null) {
            mWebView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyEdgeToEdgeImmersive();
        if (mCustomView == null && mGestureController != null) {
            mGestureController.restoreDefaultBrightness();
        }
        checkSecurityLock();
        if (mWebView != null) {
            mWebView.onResume();
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= TRIM_MEMORY_RUNNING_LOW && mWebView != null) {
            mWebView.freeMemory();
        }
    }

    @Override
    protected void onDestroy() {
        if (mWebView != null) {
            mWebView.destroy();
            mWebView = null;
        }
        super.onDestroy();
    }
}
