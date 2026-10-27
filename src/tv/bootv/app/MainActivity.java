package tv.bootv.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PictureInPictureParams;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Rational;
import android.webkit.JavascriptInterface;
import android.widget.SeekBar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.json.JSONObject;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {

    public MainActivity() {
        super();
    }

    private static final String TARGET_URL = AppGuard.getTargetUrl();
    private static final String MAIN_DOMAIN = AppGuard.getMainDomain();

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
        "  function getAllVideos(root) {" +
        "    var res = [];" +
        "    try {" +
        "      var walker = document.createTreeWalker(root || document.documentElement || document.body, NodeFilter.SHOW_ELEMENT, null, false);" +
        "      var n = walker.currentNode;" +
        "      while (n) {" +
        "        if (n.tagName === 'VIDEO') res.push(n);" +
        "        if (n.shadowRoot) res = res.concat(getAllVideos(n.shadowRoot));" +
        "        n = walker.nextNode();" +
        "      }" +
        "    } catch(e) {}" +
        "    return res;" +
        "  }" +
        "  function getPrimary() {" +
        "    var fs = document.fullscreenElement || document.webkitFullscreenElement;" +
        "    if (fs) {" +
        "      if (fs.tagName === 'VIDEO') return fs;" +
        "      var fsv = fs.querySelector ? fs.querySelector('video') : null;" +
        "      if (fsv) return fsv;" +
        "    }" +
        "    var vs = getAllVideos(document);" +
        "    for (var i = 0; i < vs.length; i++) {" +
        "      if (!vs[i].paused && vs[i].currentTime > 0) return vs[i];" +
        "    }" +
        "    return vs.length > 0 ? vs[0] : null;" +
        "  }" +
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
        "      var v = getPrimary();" +
        "      if (v) {" +
        "        var d = (v.duration && isFinite(v.duration)) ? v.duration : (v.seekable && v.seekable.length > 0 ? v.seekable.end(v.seekable.length - 1) : 999999);" +
        "        v.currentTime = Math.max(0, Math.min(d, (v.currentTime || 0) + delta));" +
        "      }" +
        "    } catch(e) {}" +
        "  }" +
        "  function applyPlayState(shouldPlay) {" +
        "    try {" +
        "      if (window.jwplayer) {" +
        "        var p = (typeof window.jwplayer === 'function') ? window.jwplayer() : null;" +
        "        if (p) { if (shouldPlay) p.play(true); else p.pause(true); return; }" +
        "      }" +
        "    } catch(e) {}" +
        "    try {" +
        "      var vs = getAllVideos(document);" +
        "      for (var i = 0; i < vs.length; i++) {" +
        "        if (shouldPlay) { var pr = vs[i].play(); if (pr !== undefined) pr.catch(function(e){}); }" +
        "        else vs[i].pause();" +
        "      }" +
        "    } catch(e) {}" +
        "  }" +
        "  function seekTo(target) {" +
        "    try {" +
        "      if (window.jwplayer) {" +
        "        var p = (typeof window.jwplayer === 'function') ? window.jwplayer() : null;" +
        "        if (p && typeof p.seek === 'function') { p.seek(target); return; }" +
        "      }" +
        "    } catch(e) {}" +
        "    try {" +
        "      var v = getPrimary();" +
        "      if (v) v.currentTime = target;" +
        "    } catch(e) {}" +
        "  }" +
        "  function seekRatio(ratio) {" +
        "    try {" +
        "      if (window.jwplayer) {" +
        "        var p = (typeof window.jwplayer === 'function') ? window.jwplayer() : null;" +
        "        if (p && typeof p.seek === 'function') {" +
        "          var dur = (typeof p.getDuration === 'function' ? p.getDuration() : 0) || 0;" +
        "          if (dur > 0) { p.seek(dur * ratio); return; }" +
        "        }" +
        "      }" +
        "    } catch(e) {}" +
        "    try {" +
        "      var v = getPrimary();" +
        "      if (v) {" +
        "        var d = (v.duration && isFinite(v.duration)) ? v.duration : (v.seekable && v.seekable.length > 0 ? v.seekable.end(v.seekable.length - 1) : 0);" +
        "        if (d > 0) v.currentTime = d * ratio;" +
        "      }" +
        "    } catch(e) {}" +
        "  }" +
        "  function setSpeed(speed) {" +
        "    try {" +
        "      if (window.jwplayer) {" +
        "        var p = (typeof window.jwplayer === 'function') ? window.jwplayer() : null;" +
        "        if (p && typeof p.setPlaybackRate === 'function') p.setPlaybackRate(speed);" +
        "      }" +
        "    } catch(e) {}" +
        "    try {" +
        "      var vs = getAllVideos(document);" +
        "      for (var i = 0; i < vs.length; i++) vs[i].playbackRate = speed;" +
        "    } catch(e) {}" +
        "  }" +
        "  function notify(cur, dur, playing) {" +
        "    if (dur > 0) {" +
        "      if (window.AndroidBridge && typeof window.AndroidBridge.onVideoTimeUpdate === 'function') {" +
        "        window.AndroidBridge.onVideoTimeUpdate(cur, dur, playing);" +
        "      } else {" +
        "        try { window.top.postMessage({ type: 'SISKA_VIDEO_TIME_UPDATE', current: cur, duration: dur, playing: playing }, '*'); } catch(err) {}" +
        "        try { window.parent.postMessage({ type: 'SISKA_VIDEO_TIME_UPDATE', current: cur, duration: dur, playing: playing }, '*'); } catch(err) {}" +
        "      }" +
        "    }" +
        "  }" +
        "  function hookEmbedVideo(v) {" +
        "    if (!v || v.__siska_hooked) return;" +
        "    v.__siska_hooked = true;" +
        "    var evts = ['timeupdate', 'play', 'pause', 'seeking', 'seeked', 'durationchange'];" +
        "    for (var i = 0; i < evts.length; i++) {" +
        "      v.addEventListener(evts[i], function() {" +
        "        var d = (v.duration && isFinite(v.duration)) ? v.duration : (v.seekable && v.seekable.length > 0 ? v.seekable.end(v.seekable.length - 1) : 0);" +
        "        if (d > 0) notify(v.currentTime || 0, d, !v.paused);" +
        "      }, true);" +
        "    }" +
        "  }" +
        "  function hookAll() {" +
        "    var vs = getAllVideos(document);" +
        "    for (var i = 0; i < vs.length; i++) hookEmbedVideo(vs[i]);" +
        "  }" +
        "  hookAll();" +
        "  try {" +
        "    var obs = new MutationObserver(function() { hookAll(); });" +
        "    obs.observe(document.documentElement || document.body, { childList: true, subtree: true });" +
        "  } catch(e) {}" +
        "  setInterval(function() {" +
        "    try {" +
        "      var v = getPrimary();" +
        "      if (v) {" +
        "        var d = (v.duration && isFinite(v.duration)) ? v.duration : (v.seekable && v.seekable.length > 0 ? v.seekable.end(v.seekable.length - 1) : 0);" +
        "        if (d > 0) notify(v.currentTime || 0, d, !v.paused);" +
        "      }" +
        "    } catch(e) {}" +
        "  }, 350);" +
        "  window.addEventListener('message', function(e) {" +
        "    if (!e || !e.data || typeof e.data !== 'object') return;" +
        "    if (e.data.type === 'SISKA_VIDEO_SEEK' || e.data.type === 'SISKA_VIDEO_SEEK_DELTA') seek(e.data.delta);" +
        "    else if (e.data.type === 'SISKA_VIDEO_SEEK_TO') seekTo(e.data.target);" +
        "    else if (e.data.type === 'SISKA_VIDEO_SEEK_RATIO') seekRatio(e.data.ratio);" +
        "    else if (e.data.type === 'SISKA_VIDEO_SET_PLAY' || e.data.type === 'SISKA_VIDEO_TOGGLE') applyPlayState(e.data.play !== undefined ? e.data.play : false);" +
        "    else if (e.data.type === 'SISKA_VIDEO_SPEED') setSpeed(e.data.speed);" +
        "    try {" +
        "      var cFrames = document.querySelectorAll('iframe');" +
        "      for (var ci = 0; ci < cFrames.length; ci++) {" +
        "        try { cFrames[ci].contentWindow.postMessage(e.data, '*'); } catch(err) {}" +
        "      }" +
        "    } catch(err) {}" +
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
        "          if (!href.includes('" + MAIN_DOMAIN + "')) {" +
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

    // YouTube Dark Theme CSS injected into web views
    private static final String YOUTUBE_THEME_CSS =
        "(function() {" +
        "  try {" +
        "    if (document.getElementById('__yt_theme_style')) return;" +
        "    var style = document.createElement('style');" +
        "    style.id = '__yt_theme_style';" +
        "    style.innerHTML = '" +
        "      body { background-color: #0f0f0f !important; color: #ffffff !important; } " +
        "      a { color: #ffffff !important; text-decoration: none !important; } " +
        "      img, video { border-radius: 8px !important; } " +
        "      .item, .card, .thumb, .video-card { border-radius: 12px !important; background: #212121 !important; } " +
        "      header, nav, .header, .navbar { background-color: #0f0f0f !important; border-bottom: 1px solid #282828 !important; } " +
        "    ';" +
        "    document.head.appendChild(style);" +
        "  } catch(e) {}" +
        "})();";

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private SecurityManager mSecurityManager;
    private static boolean sIsAuthenticated = false;
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
    private TextView mBtnPip;
    private TextView mBtnFullscreenExtPlayer;
    private LinearLayout mPillExternalPlayer;
    private String mLastStreamUrl = null;
    private String mLastStreamFormat = "HLS";

    // Soul Browser / Via Browser Player Controls & Timeline
    private View mPlayerBottomBar;
    private TextView mTxtPlayerTitle;
    private TextView mTxtPlayerClock;
    private TextView mTxtPlayerBattery;
    private TextView mTxtTimeCurrent;
    private TextView mTxtTimeTotal;
    private SeekBar mPlayerSeekBar;
    private boolean mIsUserSeeking = false;
    private double mVideoDuration = 0;


    // Battery Receiver & Clock Updater
    private BroadcastReceiver mBatteryReceiver;
    private boolean mIsBatteryReceiverRegistered = false;
    private final Runnable mClockUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (mTxtPlayerClock != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                mTxtPlayerClock.setText(sdf.format(new Date()));
            }
            mHandler.postDelayed(this, 10000);
        }
    };

    // UI & Security Elements
    private FrameLayout mFabSettings;
    private LinearLayout mPopupBootloop;
    private View mLockOverlay;
    private View mDot1, mDot2, mDot3, mDot4;

    // Mandatory Update Lockout
    private View mLayoutUpdateLockout;
    private TextView mTxtLockoutVersionInfo;
    private Button mBtnLockoutUpdate;
    private Button mBtnLockoutExit;

    // YouTube App Shell Elements
    private View mYtAppHeader;
    private View mLayoutSearchBox;
    private EditText mEditSearchInput;
    private View mBtnSearchSubmit;
    private TextView mBtnHeaderCast;
    private TextView mBtnHeaderSearch;
    private TextView mBtnHeaderProfile;

    private View mYtChipsScroll;
    private TextView mChipAll, mChipTrending, mChipMovies, mChipSeries, mChipAnime, mChipAction, mChipRecently;

    private RelativeLayout mMiniPlayerContainer;
    private FrameLayout mMiniVideoSurface;
    private TextView mTxtMiniTitle;
    private TextView mBtnMiniPlayPause;
    private TextView mBtnMiniClose;
    private ProgressBar mPbMiniProgress;
    private boolean mIsMiniPlayerActive = false;

    private View mYtBottomNav;
    private View mTabHome, mTabTrending, mTabLibrary, mTabYou;
    private TextView mTxtTabHome, mTxtTabTrending, mTxtTabLibrary, mTxtTabYou;

    private View mYtCenterControls;

    // Active Settings Dialog
    private AlertDialog mActiveSettingsDialog = null;

    private int mOriginalOrientation;
    private String mUserAgentString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enterprise runtime integrity, signature & anti-cloning verification
        if (!AppGuard.verifyIntegrity(this)) {
            return;
        }

        mSecurityManager = new SecurityManager(this);

        // Enable edge-to-edge into display cutout / notch area
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }

        // Keep screen awake while streaming & ensure hardware acceleration
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

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
        mBtnPip = findViewById(R.id.btn_pip);
        mBtnFullscreenExtPlayer = findViewById(R.id.btn_fullscreen_ext_player);

        // Soul Browser Top & Bottom Bars
        mPlayerBottomBar = findViewById(R.id.player_bottom_bar);
        mTxtPlayerTitle = findViewById(R.id.txt_player_title);
        mTxtPlayerClock = findViewById(R.id.txt_player_clock);
        mTxtPlayerBattery = findViewById(R.id.txt_player_battery);
        mTxtTimeCurrent = findViewById(R.id.txt_time_current);
        mTxtTimeTotal = findViewById(R.id.txt_time_total);
        mPlayerSeekBar = findViewById(R.id.player_seekbar);

        if (mBtnPip != null) {
            mBtnPip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    enterPipMode();
                }
            });
        }

        View.OnClickListener extPlayerClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchExternalPlayer(mLastStreamUrl, mLastStreamFormat);
            }
        };

        if (mBtnFullscreenExtPlayer != null) {
            mBtnFullscreenExtPlayer.setOnClickListener(extPlayerClickListener);
        }

        if (mPlayerSeekBar != null) {
            mPlayerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        double ratio = progress / 1000.0;
                        if (mVideoDuration > 0) {
                            double targetSec = ratio * mVideoDuration;
                            if (mTxtTimeCurrent != null) {
                                mTxtTimeCurrent.setText(formatTime((long) targetSec));
                            }
                        } else {
                            if (mTxtTimeCurrent != null) {
                                mTxtTimeCurrent.setText(String.format(Locale.getDefault(), "%d%%", (int)(ratio * 100)));
                            }
                        }
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    mIsUserSeeking = true;
                    if (mGestureController != null) {
                        mGestureController.cancelAutoHideControls();
                    }
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    mIsUserSeeking = false;
                    double ratio = seekBar.getProgress() / 1000.0;
                    if (mVideoDuration > 0) {
                        double targetSec = ratio * mVideoDuration;
                        seekVideoTo(targetSec);
                    } else {
                        seekVideoRatio(ratio);
                    }
                    if (mGestureController != null) {
                        mGestureController.scheduleAutoHideControls();
                    }
                }
            });
        }


        mBatteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null && Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                    int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                    if (level >= 0 && scale > 0) {
                        int pct = (int) ((level / (float) scale) * 100);
                        if (mTxtPlayerBattery != null) {
                            mTxtPlayerBattery.setText(" 🔋 " + pct + "%");
                        }
                    }
                }
            }
        };

        // UI & Security Elements
        mFabSettings = findViewById(R.id.fab_settings);
        mPopupBootloop = findViewById(R.id.popup_bootloop);
        mLockOverlay = findViewById(R.id.lock_overlay);
        mDot1 = findViewById(R.id.dot_1);
        mDot2 = findViewById(R.id.dot_2);
        mDot3 = findViewById(R.id.dot_3);
        mDot4 = findViewById(R.id.dot_4);

        // Mandatory Update Lockout
        mLayoutUpdateLockout = findViewById(R.id.layout_update_lockout);
        mTxtLockoutVersionInfo = findViewById(R.id.txt_lockout_version_info);
        mBtnLockoutUpdate = findViewById(R.id.btn_lockout_update);
        mBtnLockoutExit = findViewById(R.id.btn_lockout_exit);

        setupGestureController();
        setupYouTubeAppShell();
        setupWebView();
        setupSettingsAndKeypad();

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

        // Start periodic background OTA update checker
        AppUpdateManager.startPeriodicUpdateChecker(this);
        handleUpdateIntent(getIntent());
    }

    private boolean mHasShownPopup = false;

    private void showBootloopSignaturePopup() {
        if (mHasShownPopup || mPopupBootloop == null) return;
        mHasShownPopup = true;
        mPopupBootloop.bringToFront();
        mPopupBootloop.setVisibility(View.VISIBLE);
        mPopupBootloop.setAlpha(0f);
        mPopupBootloop.setTranslationY(40f);
        mPopupBootloop.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .start();

        // Auto dismiss after 3.5 seconds
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mPopupBootloop != null) {
                    mPopupBootloop.animate()
                            .alpha(0f)
                            .translationY(40f)
                            .setDuration(500)
                            .withEndAction(new Runnable() {
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
                    public void onClick(final View v) {
                        v.animate().scaleX(0.88f).scaleY(0.88f).setDuration(50).withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(70).start();
                            }
                        }).start();
                        onKeypadDigit(key.getText().toString());
                    }
                });
            }
        }

        View keyDel = findViewById(R.id.key_del);
        if (keyDel != null) {
            keyDel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    v.animate().scaleX(0.88f).scaleY(0.88f).setDuration(50).withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(70).start();
                        }
                    }).start();
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
            sIsAuthenticated = true;
            showBootloopSignaturePopup();
            return;
        }

        if (!sIsAuthenticated) {
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
            showBootloopSignaturePopup();
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
        sIsAuthenticated = true;
        mLockOverlay.setVisibility(View.GONE);
        applyEdgeToEdgeImmersive();
        showBootloopSignaturePopup();
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_settings, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();
        mActiveSettingsDialog = dialog;
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        View btnClose = dialogView.findViewById(R.id.btn_close_settings);
        Button btnDone = dialogView.findViewById(R.id.btn_done_settings);
        final View rowPinLock = dialogView.findViewById(R.id.row_pin_lock);
        final Switch switchPin = dialogView.findViewById(R.id.switch_pin_lock);
        final Button btnChangePin = dialogView.findViewById(R.id.btn_change_pin);
        final Button btnRemovePin = dialogView.findViewById(R.id.btn_remove_pin);
        final Button btnLockNow = dialogView.findViewById(R.id.btn_lock_now);
        final View rowFingerprint = dialogView.findViewById(R.id.row_fingerprint);
        final Switch switchFingerprint = dialogView.findViewById(R.id.switch_fingerprint);
        final View rowAutoUpdate = dialogView.findViewById(R.id.row_auto_update);
        final Switch switchAutoUpdate = dialogView.findViewById(R.id.switch_auto_update);
        Button btnClearCache = dialogView.findViewById(R.id.btn_clear_cache);

        if (rowPinLock != null) {
            rowPinLock.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchPin.toggle();
                }
            });
        }

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
                    sIsAuthenticated = false;
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
                    sIsAuthenticated = true;
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
                                sIsAuthenticated = true;
                                mLockOverlay.setVisibility(View.GONE);
                                Toast.makeText(MainActivity.this, "Password protection removed", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        if (rowFingerprint != null) {
            rowFingerprint.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!mSecurityManager.isBiometricSupported()) {
                        Toast.makeText(MainActivity.this, "Fingerprint not available or not enrolled in device settings", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!mSecurityManager.isPinSet() || !mSecurityManager.isLockEnabled()) {
                        Toast.makeText(MainActivity.this, "Enable PIN lock first to use Fingerprint unlock", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    switchFingerprint.toggle();
                }
            });
        }

        switchFingerprint.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!mSecurityManager.isBiometricSupported()) {
                        Toast.makeText(MainActivity.this, "Fingerprint not available or not enrolled in device settings", Toast.LENGTH_SHORT).show();
                        switchFingerprint.setChecked(false);
                        return;
                    }
                    if (!mSecurityManager.isPinSet() || !mSecurityManager.isLockEnabled()) {
                        Toast.makeText(MainActivity.this, "Enable PIN lock first to use Fingerprint unlock", Toast.LENGTH_SHORT).show();
                        switchFingerprint.setChecked(false);
                        return;
                    }
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
                    CookieManager.getInstance().flush();
                }
                Toast.makeText(MainActivity.this, "Cache and Cookies cleared successfully!", Toast.LENGTH_SHORT).show();
            }
        });

        if (switchAutoUpdate != null) {
            switchAutoUpdate.setChecked(AppUpdateManager.isAutoUpdateEnabled(MainActivity.this));
            switchAutoUpdate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    AppUpdateManager.setAutoUpdateEnabled(MainActivity.this, isChecked);
                    if (isChecked) {
                        AppUpdateManager.startPeriodicUpdateChecker(MainActivity.this);
                        Toast.makeText(MainActivity.this, "Auto-update background checker enabled", Toast.LENGTH_SHORT).show();
                    } else {
                        AppUpdateManager.stopPeriodicUpdateChecker();
                        Toast.makeText(MainActivity.this, "Auto-update background checker disabled", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        if (rowAutoUpdate != null) {
            rowAutoUpdate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (switchAutoUpdate != null) {
                        switchAutoUpdate.toggle();
                    }
                }
            });
        }

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
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(AppGuard.getTelegramUrl()));
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
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
                applyEdgeToEdgeImmersive();
            }
        };

        if (btnClose != null) btnClose.setOnClickListener(dismissListener);
        if (btnDone != null) btnDone.setOnClickListener(dismissListener);

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface d) {
                mActiveSettingsDialog = null;
                applyEdgeToEdgeImmersive();
            }
        });

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface d) {
                mActiveSettingsDialog = null;
                applyEdgeToEdgeImmersive();
            }
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setWindowAnimations(android.R.style.Animation_Dialog);
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
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
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

    public void applyEdgeToEdgeImmersive() {
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

        View seekZoneLeft = findViewById(R.id.seek_zone_left);
        View seekZoneRight = findViewById(R.id.seek_zone_right);
        View badgeSpeedBoost = findViewById(R.id.badge_speed_boost);
        TextView txtSeekTarget = findViewById(R.id.txt_seek_target);
        TextView txtVolumeIcon = findViewById(R.id.txt_volume_icon);

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

                    @Override
                    public void onMinimize() {
                        minimizeToMiniPlayer();
                    }
                }
        );

        mGestureController.setSeekZones(seekZoneLeft, seekZoneRight);
        mGestureController.setSpeedBoostBadge(badgeSpeedBoost);
        mGestureController.setSeekTargetView(txtSeekTarget);
        mGestureController.setVolumeIconView(txtVolumeIcon);
        mGestureController.setBottomBar(mPlayerBottomBar);

        mBtnPlayerClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                minimizeToMiniPlayer();
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

    private void setupYouTubeAppShell() {
        mYtAppHeader = findViewById(R.id.yt_app_header);
        mLayoutSearchBox = findViewById(R.id.layout_search_box);
        mEditSearchInput = findViewById(R.id.edit_search_input);
        mBtnSearchSubmit = findViewById(R.id.btn_search_submit);
        mBtnHeaderCast = findViewById(R.id.btn_header_cast);
        mBtnHeaderSearch = findViewById(R.id.btn_header_search);
        mBtnHeaderProfile = findViewById(R.id.btn_header_profile);

        mYtChipsScroll = findViewById(R.id.yt_chips_scroll);
        mChipAll = findViewById(R.id.chip_all);
        mChipTrending = findViewById(R.id.chip_trending);
        mChipMovies = findViewById(R.id.chip_movies);
        mChipSeries = findViewById(R.id.chip_series);
        mChipAnime = findViewById(R.id.chip_anime);
        mChipAction = findViewById(R.id.chip_action);
        mChipRecently = findViewById(R.id.chip_recently);

        mMiniPlayerContainer = findViewById(R.id.mini_player_container);
        mMiniVideoSurface = findViewById(R.id.mini_video_surface);
        mTxtMiniTitle = findViewById(R.id.txt_mini_title);
        mBtnMiniPlayPause = findViewById(R.id.btn_mini_play_pause);
        mBtnMiniClose = findViewById(R.id.btn_mini_close);
        mPbMiniProgress = findViewById(R.id.pb_mini_progress);

        mYtBottomNav = findViewById(R.id.yt_bottom_nav);
        mTabHome = findViewById(R.id.tab_home);
        mTabTrending = findViewById(R.id.tab_trending);
        mTabLibrary = findViewById(R.id.tab_library);
        mTabYou = findViewById(R.id.tab_you);
        mTxtTabHome = findViewById(R.id.txt_tab_home);
        mTxtTabTrending = findViewById(R.id.txt_tab_trending);
        mTxtTabLibrary = findViewById(R.id.txt_tab_library);
        mTxtTabYou = findViewById(R.id.txt_tab_you);

        mYtCenterControls = findViewById(R.id.yt_center_controls);
        if (mGestureController != null) {
            mGestureController.setCenterControls(mYtCenterControls);
        }

        if (mBtnHeaderCast != null) {
            mBtnHeaderCast.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mLastStreamUrl != null) {
                        launchExternalPlayer(mLastStreamUrl, mLastStreamFormat);
                    } else {
                        Toast.makeText(MainActivity.this, "Play a video first to cast to VLC/MPV", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        if (mBtnHeaderSearch != null) {
            mBtnHeaderSearch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mLayoutSearchBox != null) {
                        if (mLayoutSearchBox.getVisibility() == View.VISIBLE) {
                            mLayoutSearchBox.setVisibility(View.GONE);
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            if (imm != null && mEditSearchInput != null) {
                                imm.hideSoftInputFromWindow(mEditSearchInput.getWindowToken(), 0);
                            }
                        } else {
                            mLayoutSearchBox.setVisibility(View.VISIBLE);
                            if (mEditSearchInput != null) {
                                mEditSearchInput.requestFocus();
                                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                if (imm != null) {
                                    imm.showSoftInput(mEditSearchInput, InputMethodManager.SHOW_IMPLICIT);
                                }
                            }
                        }
                    }
                }
            });
        }

        View.OnClickListener performSearchListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEditSearchInput != null) {
                    String query = mEditSearchInput.getText().toString().trim();
                    if (!query.isEmpty()) {
                        performWebSearch(query);
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(mEditSearchInput.getWindowToken(), 0);
                        }
                    }
                }
            }
        };

        if (mBtnSearchSubmit != null) {
            mBtnSearchSubmit.setOnClickListener(performSearchListener);
        }

        if (mEditSearchInput != null) {
            mEditSearchInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    String query = v.getText().toString().trim();
                    if (!query.isEmpty()) {
                        performWebSearch(query);
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                        }
                        return true;
                    }
                    return false;
                }
            });
        }

        if (mBtnHeaderProfile != null) {
            mBtnHeaderProfile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showSettingsDialog();
                }
            });
        }

        setupCategoryChips();

        if (mMiniPlayerContainer != null) {
            mMiniPlayerContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    expandFromMiniPlayer();
                }
            });
        }

        if (mBtnMiniPlayPause != null) {
            mBtnMiniPlayPause.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mGestureController != null) {
                        mGestureController.togglePlayPause();
                    }
                }
            });
        }

        if (mBtnMiniClose != null) {
            mBtnMiniClose.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismissMiniPlayer();
                }
            });
        }

        setupBottomNavTabs();
    }

    private void selectChip(TextView selected) {
        TextView[] chips = {mChipAll, mChipTrending, mChipMovies, mChipSeries, mChipAnime, mChipAction, mChipRecently};
        for (TextView chip : chips) {
            if (chip != null) {
                if (chip == selected) {
                    chip.setBackgroundResource(R.drawable.yt_chip_active);
                    chip.setTextColor(getResources().getColor(R.color.yt_chip_selected_text));
                } else {
                    chip.setBackgroundResource(R.drawable.yt_chip_inactive);
                    chip.setTextColor(getResources().getColor(R.color.yt_text_primary));
                }
            }
        }
    }

    private void setupCategoryChips() {
        if (mChipAll != null) {
            mChipAll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectChip(mChipAll);
                    if (mWebView != null) mWebView.loadUrl(TARGET_URL);
                }
            });
        }
        if (mChipTrending != null) {
            mChipTrending.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectChip(mChipTrending);
                    performWebSearch("Trending");
                }
            });
        }
        if (mChipMovies != null) {
            mChipMovies.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectChip(mChipMovies);
                    performWebSearch("Movies");
                }
            });
        }
        if (mChipSeries != null) {
            mChipSeries.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectChip(mChipSeries);
                    performWebSearch("Series");
                }
            });
        }
        if (mChipAnime != null) {
            mChipAnime.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectChip(mChipAnime);
                    performWebSearch("Anime");
                }
            });
        }
        if (mChipAction != null) {
            mChipAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectChip(mChipAction);
                    performWebSearch("Action");
                }
            });
        }
        if (mChipRecently != null) {
            mChipRecently.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectChip(mChipRecently);
                    performWebSearch("Latest");
                }
            });
        }
    }

    private void setBottomNavActive(int index) {
        TextView[] tabTexts = {mTxtTabHome, mTxtTabTrending, mTxtTabLibrary, mTxtTabYou};
        int activeColor = getResources().getColor(R.color.white);
        int inactiveColor = getResources().getColor(R.color.yt_text_secondary);

        for (int i = 0; i < tabTexts.length; i++) {
            if (tabTexts[i] != null) {
                tabTexts[i].setTextColor(i == index ? activeColor : inactiveColor);
            }
        }
    }

    private void setupBottomNavTabs() {
        if (mTabHome != null) {
            mTabHome.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setBottomNavActive(0);
                    selectChip(mChipAll);
                    if (mWebView != null) mWebView.loadUrl(TARGET_URL);
                }
            });
        }
        if (mTabTrending != null) {
            mTabTrending.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setBottomNavActive(1);
                    selectChip(mChipTrending);
                    performWebSearch("Trending");
                }
            });
        }
        if (mTabLibrary != null) {
            mTabLibrary.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setBottomNavActive(2);
                    showLibraryDialog();
                }
            });
        }
        if (mTabYou != null) {
            mTabYou.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setBottomNavActive(3);
                    showSettingsDialog();
                }
            });
        }
    }

    private void performWebSearch(String query) {
        if (mWebView == null || query == null || query.isEmpty()) return;
        String searchScript =
            "(function() {" +
            "  var q = '" + query.replace("'", "\\'") + "';" +
            "  var inp = document.querySelector('input[type=\"search\"], input[name=\"q\"], input[name=\"search\"], input[name=\"keyword\"], input[placeholder*=\"search\" i], input[placeholder*=\"Search\" i]');" +
            "  if (inp) {" +
            "    inp.value = q;" +
            "    var form = inp.form;" +
            "    if (form) { form.submit(); return; }" +
            "    var evt = new Event('input', { bubbles: true });" +
            "    inp.dispatchEvent(evt);" +
            "  }" +
            "  window.location.href = '" + TARGET_URL + "/search?q=' + encodeURIComponent(q);" +
            "})();";
        executeJs(searchScript);
    }

    private void minimizeToMiniPlayer() {
        if (mCustomView == null) {
            exitFullscreenVideo();
            return;
        }

        mIsMiniPlayerActive = true;
        mVideoSurfaceHolder.removeAllViews();
        if (mMiniVideoSurface != null) {
            mMiniVideoSurface.removeAllViews();
            mMiniVideoSurface.addView(mCustomView, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
        }

        mFullscreenContainer.setVisibility(View.GONE);
        if (mPlayerBottomBar != null) mPlayerBottomBar.setVisibility(View.GONE);
        if (mPlayerTopBar != null) mPlayerTopBar.setVisibility(View.GONE);

        if (mMiniPlayerContainer != null) {
            mMiniPlayerContainer.setVisibility(View.VISIBLE);
            if (mTxtMiniTitle != null && mTxtPlayerTitle != null) {
                mTxtMiniTitle.setText(mTxtPlayerTitle.getText());
            }
        }

        if (mYtAppHeader != null) mYtAppHeader.setVisibility(View.VISIBLE);
        if (mYtChipsScroll != null) mYtChipsScroll.setVisibility(View.VISIBLE);
        if (mYtBottomNav != null) mYtBottomNav.setVisibility(View.VISIBLE);
        mWebView.setVisibility(View.VISIBLE);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        applyEdgeToEdgeImmersive();
    }

    private void expandFromMiniPlayer() {
        if (mCustomView == null) return;

        mIsMiniPlayerActive = false;
        if (mMiniVideoSurface != null) {
            mMiniVideoSurface.removeAllViews();
        }
        if (mMiniPlayerContainer != null) {
            mMiniPlayerContainer.setVisibility(View.GONE);
        }

        mVideoSurfaceHolder.removeAllViews();
        mVideoSurfaceHolder.addView(mCustomView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        mFullscreenContainer.setVisibility(View.VISIBLE);
        if (mPlayerBottomBar != null) mPlayerBottomBar.setVisibility(View.VISIBLE);
        if (mPlayerTopBar != null) mPlayerTopBar.setVisibility(View.VISIBLE);

        if (mYtAppHeader != null) mYtAppHeader.setVisibility(View.GONE);
        if (mYtChipsScroll != null) mYtChipsScroll.setVisibility(View.GONE);
        if (mYtBottomNav != null) mYtBottomNav.setVisibility(View.GONE);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        applyEdgeToEdgeImmersive();

        if (mGestureController != null) {
            mGestureController.reset();
            mGestureController.scheduleAutoHideControls();
        }
    }

    private void dismissMiniPlayer() {
        mIsMiniPlayerActive = false;
        if (mMiniVideoSurface != null) {
            mMiniVideoSurface.removeAllViews();
        }
        if (mMiniPlayerContainer != null) {
            mMiniPlayerContainer.setVisibility(View.GONE);
        }
        exitFullscreenVideo();
    }

    private void showLibraryDialog() {
        if (isFinishing() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed())) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_library, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setGravity(Gravity.BOTTOM);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        View btnClose = dialogView.findViewById(R.id.btn_close_library);
        TextView txtLastTitle = dialogView.findViewById(R.id.txt_lib_last_title);
        TextView txtLastUrl = dialogView.findViewById(R.id.txt_lib_last_url);
        Button btnResume = dialogView.findViewById(R.id.btn_lib_resume);
        Button btnExternal = dialogView.findViewById(R.id.btn_lib_external);
        View btnClear = dialogView.findViewById(R.id.btn_lib_clear);
        View btnReload = dialogView.findViewById(R.id.btn_lib_reload);

        if (mLastStreamUrl != null) {
            if (txtLastTitle != null && mTxtPlayerTitle != null) {
                txtLastTitle.setText(mTxtPlayerTitle.getText());
            }
            if (txtLastUrl != null) {
                txtLastUrl.setText(mLastStreamUrl);
            }
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
        }

        if (btnResume != null) {
            btnResume.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    if (mIsMiniPlayerActive) {
                        expandFromMiniPlayer();
                    } else if (mLastStreamUrl != null) {
                        mWebView.loadUrl(mLastStreamUrl);
                    }
                }
            });
        }

        if (btnExternal != null) {
            btnExternal.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    launchExternalPlayer(mLastStreamUrl, mLastStreamFormat);
                }
            });
        }

        if (btnClear != null) {
            btnClear.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mWebView != null) mWebView.clearCache(true);
                    Toast.makeText(MainActivity.this, "Cache cleared successfully", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            });
        }

        if (btnReload != null) {
            btnReload.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mWebView != null) mWebView.reload();
                    dialog.dismiss();
                }
            });
        }

        dialog.show();
    }

    private void injectYouTubeThemeCss(WebView view) {
        if (view == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            view.evaluateJavascript(YOUTUBE_THEME_CSS, null);
        } else {
            view.loadUrl("javascript:" + YOUTUBE_THEME_CSS);
        }
    }

    private void applyAspectRatio(int mode) {
        if (mVideoSurfaceHolder != null) {
            if (mode == 1) { // Fill / Crop
                mVideoSurfaceHolder.setScaleX(1.15f);
                mVideoSurfaceHolder.setScaleY(1.15f);
            } else if (mode == 2) { // Stretch
                mVideoSurfaceHolder.setScaleX(1.30f);
                mVideoSurfaceHolder.setScaleY(1.0f);
            } else { // Fit
                mVideoSurfaceHolder.setScaleX(1.0f);
                mVideoSurfaceHolder.setScaleY(1.0f);
            }
        }
        String fit = mode == 1 ? "cover" : (mode == 2 ? "fill" : "contain");
        executeJs("try { var vs = document.querySelectorAll('video'); for (var i = 0; i < vs.length; i++) vs[i].style.objectFit = '" + fit + "'; } catch(e){}" +
                  "try { window.postMessage({ type: 'SISKA_VIDEO_RATIO', mode: " + mode + " }, '*'); } catch(e){}");
    }

    private void executeJs(String script) {
        if (mWebView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mWebView.evaluateJavascript(script, null);
        }
    }

    private void executeVideoSeek(int secondsDelta) {
        seekVideoDelta(secondsDelta);
    }

    private void executeVideoPlaybackState(boolean shouldPlay) {
        setVideoPlayState(shouldPlay);
    }

    private void executeVideoSpeed(float speed) {
        setVideoSpeed(speed);
    }

    private void seekVideoDelta(int secondsDelta) {
        String script =
            "(function(delta) {" +
            "  function getAllVideos(root) {" +
            "    var res = [];" +
            "    try {" +
            "      var walker = document.createTreeWalker(root || document.documentElement || document.body, NodeFilter.SHOW_ELEMENT, null, false);" +
            "      var n = walker.currentNode;" +
            "      while (n) {" +
            "        if (n.tagName === 'VIDEO') res.push(n);" +
            "        if (n.shadowRoot) res = res.concat(getAllVideos(n.shadowRoot));" +
            "        n = walker.nextNode();" +
            "      }" +
            "    } catch(e) {}" +
            "    return res;" +
            "  }" +
            "  function seekInWin(win) {" +
            "    if (!win) return;" +
            "    try {" +
            "      var vs = getAllVideos(win.document);" +
            "      var targetVideo = null;" +
            "      for (var i = 0; i < vs.length; i++) {" +
            "        var v = vs[i];" +
            "        if (!v.paused || v.duration > 30 || vs.length === 1) {" +
            "          targetVideo = v; break;" +
            "        }" +
            "      }" +
            "      if (!targetVideo && vs.length > 0) targetVideo = vs[0];" +
            "      if (targetVideo) {" +
            "        var d = (targetVideo.duration && isFinite(targetVideo.duration)) ? targetVideo.duration : (targetVideo.seekable && targetVideo.seekable.length > 0 ? targetVideo.seekable.end(targetVideo.seekable.length - 1) : 999999);" +
            "        targetVideo.currentTime = Math.max(0, Math.min(d, (targetVideo.currentTime || 0) + delta));" +
            "      }" +
            "    } catch(e) {}" +
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
            "    try { win.postMessage({ type: 'SISKA_VIDEO_SEEK_DELTA', delta: delta }, '*'); } catch(e) {}" +
            "    try {" +
            "      for (var j = 0; j < win.frames.length; j++) {" +
            "        try { seekInWin(win.frames[j]); } catch(err) {}" +
            "      }" +
            "    } catch(e) {}" +
            "    try {" +
            "      var iframes = win.document ? win.document.querySelectorAll('iframe') : [];" +
            "      for (var k = 0; k < iframes.length; k++) {" +
            "        try { if (iframes[k].contentWindow) seekInWin(iframes[k].contentWindow); } catch(err) {}" +
            "      }" +
            "    } catch(e) {}" +
            "  }" +
            "  seekInWin(window);" +
            "})(" + secondsDelta + ");";
        executeJs(script);
    }

    private void seekVideoTo(double targetSeconds) {
        String script =
            "(function(target) {" +
            "  function getAllVideos(root) {" +
            "    var res = [];" +
            "    try {" +
            "      var walker = document.createTreeWalker(root || document.documentElement || document.body, NodeFilter.SHOW_ELEMENT, null, false);" +
            "      var n = walker.currentNode;" +
            "      while (n) {" +
            "        if (n.tagName === 'VIDEO') res.push(n);" +
            "        if (n.shadowRoot) res = res.concat(getAllVideos(n.shadowRoot));" +
            "        n = walker.nextNode();" +
            "      }" +
            "    } catch(e) {}" +
            "    return res;" +
            "  }" +
            "  function seekInWin(win) {" +
            "    if (!win) return;" +
            "    try {" +
            "      var vs = getAllVideos(win.document);" +
            "      var targetVideo = null;" +
            "      for (var i = 0; i < vs.length; i++) {" +
            "        var v = vs[i];" +
            "        if (!v.paused || v.duration > 30 || vs.length === 1) {" +
            "          targetVideo = v; break;" +
            "        }" +
            "      }" +
            "      if (!targetVideo && vs.length > 0) targetVideo = vs[0];" +
            "      if (targetVideo) targetVideo.currentTime = target;" +
            "    } catch(e) {}" +
            "    try {" +
            "      if (win.jwplayer) {" +
            "        var p = (typeof win.jwplayer === 'function') ? win.jwplayer() : null;" +
            "        if (p && typeof p.seek === 'function') p.seek(target);" +
            "      }" +
            "    } catch(e) {}" +
            "    try {" +
            "      if (win.videojs && typeof win.videojs.getAllPlayers === 'function') {" +
            "        var ps = win.videojs.getAllPlayers();" +
            "        for (var k = 0; k < ps.length; k++) {" +
            "          if (ps[k] && typeof ps[k].currentTime === 'function') ps[k].currentTime(target);" +
            "        }" +
            "      }" +
            "    } catch(e) {}" +
            "    try { win.postMessage({ type: 'SISKA_VIDEO_SEEK_TO', target: target }, '*'); } catch(e) {}" +
            "    try {" +
            "      for (var j = 0; j < win.frames.length; j++) {" +
            "        try { seekInWin(win.frames[j]); } catch(err) {}" +
            "      }" +
            "    } catch(e) {}" +
            "    try {" +
            "      var iframes = win.document ? win.document.querySelectorAll('iframe') : [];" +
            "      for (var k = 0; k < iframes.length; k++) {" +
            "        try { if (iframes[k].contentWindow) seekInWin(iframes[k].contentWindow); } catch(err) {}" +
            "      }" +
            "    } catch(e) {}" +
            "  }" +
            "  seekInWin(window);" +
            "})(" + targetSeconds + ");";
        executeJs(script);
    }

    private void seekVideoRatio(double ratio) {
        String script =
            "(function(r) {" +
            "  function getAllVideos(root) {" +
            "    var res = [];" +
            "    try {" +
            "      var walker = document.createTreeWalker(root || document.documentElement || document.body, NodeFilter.SHOW_ELEMENT, null, false);" +
            "      var n = walker.currentNode;" +
            "      while (n) {" +
            "        if (n.tagName === 'VIDEO') res.push(n);" +
            "        if (n.shadowRoot) res = res.concat(getAllVideos(n.shadowRoot));" +
            "        n = walker.nextNode();" +
            "      }" +
            "    } catch(e) {}" +
            "    return res;" +
            "  }" +
            "  function seekInWin(win) {" +
            "    if (!win) return;" +
            "    try {" +
            "      var vs = getAllVideos(win.document);" +
            "      for (var i = 0; i < vs.length; i++) {" +
            "        var v = vs[i];" +
            "        var d = v.duration;" +
            "        if ((!d || !isFinite(d)) && v.seekable && v.seekable.length > 0) d = v.seekable.end(v.seekable.length - 1);" +
            "        if (d && isFinite(d) && d > 0) { v.currentTime = d * r; break; }" +
            "      }" +
            "    } catch(e) {}" +
            "    try {" +
            "      if (win.jwplayer) {" +
            "        var p = (typeof win.jwplayer === 'function') ? win.jwplayer() : null;" +
            "        if (p && typeof p.seek === 'function') {" +
            "          var dur = (typeof p.getDuration === 'function' ? p.getDuration() : 0) || 0;" +
            "          if (dur > 0) p.seek(dur * r);" +
            "        }" +
            "      }" +
            "    } catch(e) {}" +
            "    try { win.postMessage({ type: 'SISKA_VIDEO_SEEK_RATIO', ratio: r }, '*'); } catch(e) {}" +
            "    try {" +
            "      for (var j = 0; j < win.frames.length; j++) {" +
            "        try { seekInWin(win.frames[j]); } catch(err) {}" +
            "      }" +
            "    } catch(e) {}" +
            "    try {" +
            "      var iframes = win.document ? win.document.querySelectorAll('iframe') : [];" +
            "      for (var k = 0; k < iframes.length; k++) {" +
            "        try { if (iframes[k].contentWindow) seekInWin(iframes[k].contentWindow); } catch(err) {}" +
            "      }" +
            "    } catch(e) {}" +
            "  }" +
            "  seekInWin(window);" +
            "})(" + ratio + ");";
        executeJs(script);
    }

    private void setVideoPlayState(boolean shouldPlay) {
        String script =
            "(function(play) {" +
            "  function getAllVideos(root) {" +
            "    var res = [];" +
            "    try {" +
            "      var walker = document.createTreeWalker(root || document.documentElement || document.body, NodeFilter.SHOW_ELEMENT, null, false);" +
            "      var n = walker.currentNode;" +
            "      while (n) {" +
            "        if (n.tagName === 'VIDEO') res.push(n);" +
            "        if (n.shadowRoot) res = res.concat(getAllVideos(n.shadowRoot));" +
            "        n = walker.nextNode();" +
            "      }" +
            "    } catch(e) {}" +
            "    return res;" +
            "  }" +
            "  function apply(win) {" +
            "    if (!win) return;" +
            "    try {" +
            "      if (win.jwplayer) {" +
            "        var p = (typeof win.jwplayer === 'function') ? win.jwplayer() : null;" +
            "        if (p) { if (play) p.play(); else p.pause(); }" +
            "      }" +
            "    } catch(e) {}" +
            "    try {" +
            "      var vs = getAllVideos(win.document);" +
            "      for (var i = 0; i < vs.length; i++) {" +
            "        if (play) { var pr = vs[i].play(); if (pr !== undefined) pr.catch(function(e){}); }" +
            "        else vs[i].pause();" +
            "      }" +
            "    } catch(e) {}" +
            "    try { win.postMessage({ type: 'SISKA_VIDEO_SET_PLAY', play: play }, '*'); } catch(e) {}" +
            "    try {" +
            "      for (var j = 0; j < win.frames.length; j++) {" +
            "        try { apply(win.frames[j]); } catch(err) {}" +
            "      }" +
            "    } catch(e) {}" +
            "    try {" +
            "      var iframes = win.document ? win.document.querySelectorAll('iframe') : [];" +
            "      for (var k = 0; k < iframes.length; k++) {" +
            "        try { if (iframes[k].contentWindow) apply(iframes[k].contentWindow); } catch(err) {}" +
            "      }" +
            "    } catch(e) {}" +
            "  }" +
            "  apply(window);" +
            "})(" + shouldPlay + ");";
        executeJs(script);

        if (mCustomView != null) {
            mCustomView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
            mCustomView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
        }

        if (mBtnPlayPause != null) {
            mBtnPlayPause.setText(shouldPlay ? "⏸" : "▶");
        }
    }

    private void setVideoSpeed(float speed) {
        String script =
            "(function(spd) {" +
            "  function getAllVideos(root) {" +
            "    var res = [];" +
            "    try {" +
            "      var walker = document.createTreeWalker(root || document.documentElement || document.body, NodeFilter.SHOW_ELEMENT, null, false);" +
            "      var n = walker.currentNode;" +
            "      while (n) {" +
            "        if (n.tagName === 'VIDEO') res.push(n);" +
            "        if (n.shadowRoot) res = res.concat(getAllVideos(n.shadowRoot));" +
            "        n = walker.nextNode();" +
            "      }" +
            "    } catch(e) {}" +
            "    return res;" +
            "  }" +
            "  function apply(win) {" +
            "    if (!win) return;" +
            "    try {" +
            "      if (win.jwplayer) {" +
            "        var p = (typeof win.jwplayer === 'function') ? win.jwplayer() : null;" +
            "        if (p && typeof p.setPlaybackRate === 'function') p.setPlaybackRate(spd);" +
            "      }" +
            "    } catch(e) {}" +
            "    try {" +
            "      var vs = getAllVideos(win.document);" +
            "      for (var i = 0; i < vs.length; i++) vs[i].playbackRate = spd;" +
            "    } catch(e) {}" +
            "    try { win.postMessage({ type: 'SISKA_VIDEO_SPEED', speed: spd }, '*'); } catch(e) {}" +
            "    try {" +
            "      for (var j = 0; j < win.frames.length; j++) {" +
            "        try { apply(win.frames[j]); } catch(err) {}" +
            "      }" +
            "    } catch(e) {}" +
            "    try {" +
            "      var iframes = win.document ? win.document.querySelectorAll('iframe') : [];" +
            "      for (var k = 0; k < iframes.length; k++) {" +
            "        try { if (iframes[k].contentWindow) apply(iframes[k].contentWindow); } catch(err) {}" +
            "      }" +
            "    } catch(e) {}" +
            "  }" +
            "  apply(window);" +
            "})(" + speed + ");";
        executeJs(script);
    }

    private boolean isAdOrTracker(String url) {
        if (url == null || url.isEmpty()) return false;
        if (url.contains(MAIN_DOMAIN)) return false;
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
            conn.setRequestProperty("Referer", TARGET_URL);
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

        // Media streaming optimizations & hardware accelerated render priority
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);

        // Maximum network & resource caching (Fast back-forward navigation & offline chunking)
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // Enable HTML5 Database Storage
        try {
            java.io.File dbDir = new java.io.File(getFilesDir(), "databases");
            if (!dbDir.exists()) dbDir.mkdirs();
            settings.setDatabasePath(dbDir.getAbsolutePath());
        } catch (Exception ignored) {}

        // Performance & latency optimizations
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(false);
        }
        settings.setGeolocationEnabled(true);
        settings.setSaveFormData(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);
            cookieManager.setAcceptThirdPartyCookies(mWebView, true);
        }

        // Clean, modern desktop-class Chrome mobile user agent (prevents degraded mobile fallback video players)
        String defaultUa = settings.getUserAgentString();
        if (defaultUa != null) {
            mUserAgentString = defaultUa.replace("; wv", "");
            settings.setUserAgentString(mUserAgentString);
        } else {
            mUserAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36";
        }

        mWebView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        mWebView.setScrollbarFadingEnabled(true);
        mWebView.setVerticalScrollBarEnabled(false);
        mWebView.setHorizontalScrollBarEnabled(false);
        mWebView.setDrawingCacheEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }

        mWebView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                mProgressBar.setVisibility(View.VISIBLE);
                mErrorLayout.setVisibility(View.GONE);
                injectAdBlockScript(view);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    CookieManager.getInstance().flush();
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                mProgressBar.setVisibility(View.GONE);
                injectAdBlockScript(view);
                injectVideoSyncScript(view);
                injectYouTubeThemeCss(view);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    CookieManager.getInstance().flush();
                }
                String title = view.getTitle();
                if (title != null && !title.isEmpty()) {
                    if (mTxtPlayerTitle != null) {
                        mTxtPlayerTitle.setText(title);
                    }
                    if (mTxtMiniTitle != null) {
                        mTxtMiniTitle.setText(title);
                    }
                }
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
                    checkAndCaptureStreamUrl(url);
                    boolean isEmbedCandidate = (url.contains("/e/") || url.contains("/embed/") || url.contains("/stream/") || url.contains("/v/") || url.contains("/player/"))
                            && !url.contains(".js") && !url.contains(".css") && !url.contains(".png") && !url.contains(".jpg") && !url.contains(".m3u8") && !url.contains(".ts") && !url.contains(".mp4") && !url.contains(".mpd") && !url.contains(".webm") && !url.contains(".mkv") && !url.contains(".flv") && !url.contains(".mov") && !url.contains(".m4v");
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
                    checkAndCaptureStreamUrl(url);
                    boolean isEmbedCandidate = (url.contains("/e/") || url.contains("/embed/") || url.contains("/stream/") || url.contains("/v/") || url.contains("/player/"))
                            && !url.contains(".js") && !url.contains(".css") && !url.contains(".png") && !url.contains(".jpg") && !url.contains(".m3u8") && !url.contains(".ts") && !url.contains(".mp4") && !url.contains(".mpd") && !url.contains(".webm") && !url.contains(".mkv") && !url.contains(".flv") && !url.contains(".mov") && !url.contains(".m4v");
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
            public void onPermissionRequest(final PermissionRequest request) {
                if (request != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    try {
                        request.grant(request.getResources());
                    } catch (Exception e) {
                        try { request.deny(); } catch (Exception ignored) {}
                    }
                }
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                if (callback != null) {
                    callback.invoke(origin, false, false);
                }
            }

            @Override
            public Bitmap getDefaultVideoPoster() {
                // Return a 1x1 transparent bitmap to prevent grey placeholder boxes while loading media
                return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
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

                // Keep WebView VISIBLE behind mFullscreenContainer so Chromium never throttles rendering or pauses DOM timers!
                mWebView.setVisibility(View.VISIBLE);
                mFabSettings.setVisibility(View.GONE); // Hide FAB in fullscreen video
                if (mYtAppHeader != null) mYtAppHeader.setVisibility(View.GONE);
                if (mYtChipsScroll != null) mYtChipsScroll.setVisibility(View.GONE);
                if (mYtBottomNav != null) mYtBottomNav.setVisibility(View.GONE);
                if (mMiniPlayerContainer != null) mMiniPlayerContainer.setVisibility(View.GONE);
                mFullscreenContainer.setVisibility(View.VISIBLE);

                mVideoSurfaceHolder.removeAllViews();
                mVideoSurfaceHolder.addView(view, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                ));

                if (mPlayerBottomBar != null) {
                    mPlayerBottomBar.setVisibility(View.VISIBLE);
                }
                if (mPlayerTopBar != null) {
                    mPlayerTopBar.setVisibility(View.VISIBLE);
                }
                if (mBtnFullscreenExtPlayer != null && mLastStreamUrl != null) {
                    mBtnFullscreenExtPlayer.setVisibility(View.VISIBLE);
                    mBtnFullscreenExtPlayer.setText("📺 " + mLastStreamFormat);
                }

                applyEdgeToEdgeImmersive();
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

                mGestureController.setCustomView(view);
                mGestureController.reset();
                mGestureController.scheduleAutoHideControls();
                injectVideoSyncScript(mWebView);
            }

            @Override
            public void onHideCustomView() {
                exitFullscreenVideo();
            }
        });
    }

    private void exitFullscreenVideo() {
        if (mCustomView == null && (mFullscreenContainer == null || mFullscreenContainer.getVisibility() != View.VISIBLE) && !mIsMiniPlayerActive) return;

        mIsUserSeeking = false;
        mIsMiniPlayerActive = false;

        if (mMiniVideoSurface != null) {
            mMiniVideoSurface.removeAllViews();
        }
        if (mMiniPlayerContainer != null) {
            mMiniPlayerContainer.setVisibility(View.GONE);
        }

        mVideoSurfaceHolder.removeAllViews();
        mFullscreenContainer.setVisibility(View.GONE);
        mWebView.setVisibility(View.VISIBLE);

        if (mYtAppHeader != null) mYtAppHeader.setVisibility(View.VISIBLE);
        if (mYtChipsScroll != null) mYtChipsScroll.setVisibility(View.VISIBLE);
        if (mYtBottomNav != null) mYtBottomNav.setVisibility(View.VISIBLE);

        if (mPlayerBottomBar != null) {
            mPlayerBottomBar.setVisibility(View.GONE);
        }
        if (mPlayerTopBar != null) {
            mPlayerTopBar.setVisibility(View.GONE);
        }

        // Reset brightness to system default when exiting fullscreen!
        if (mGestureController != null) {
            mGestureController.restoreDefaultBrightness();
        }

        applyEdgeToEdgeImmersive();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if (mCustomViewCallback != null) {
            mCustomViewCallback.onCustomViewHidden();
        }

        mGestureController.setCustomView(null);
        mCustomView = null;
        mCustomViewCallback = null;
    }

    public void enforceUpdateLockout(final String remoteVersionName, final String changelog, final String apkUrl) {
        if (isFinishing() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed())) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mLayoutUpdateLockout != null) {
                    mLayoutUpdateLockout.setVisibility(View.VISIBLE);
                    mLayoutUpdateLockout.bringToFront();
                    if (mTxtLockoutVersionInfo != null) {
                        mTxtLockoutVersionInfo.setText("BOOTV v" + remoteVersionName + " is required to continue.");
                    }
                    if (mBtnLockoutUpdate != null) {
                        mBtnLockoutUpdate.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                AppUpdateManager.checkForUpdates(MainActivity.this, true);
                            }
                        });
                    }
                    if (mBtnLockoutExit != null) {
                        mBtnLockoutExit.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                finishAffinity();
                                System.exit(0);
                            }
                        });
                    }
                }
                if (mWebView != null) {
                    mWebView.onPause();
                    mWebView.pauseTimers();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (mLayoutUpdateLockout != null && mLayoutUpdateLockout.getVisibility() == View.VISIBLE) {
            finishAffinity();
            System.exit(0);
            return;
        }

        // If settings menu dialog is open, cleanly dismiss it
        if (mActiveSettingsDialog != null && mActiveSettingsDialog.isShowing()) {
            mActiveSettingsDialog.dismiss();
            mActiveSettingsDialog = null;
            return;
        }

        // If locked, back button should not bypass lock
        if (mLockOverlay != null && mLockOverlay.getVisibility() == View.VISIBLE) {
            finish();
            return;
        }

        if (mFullscreenContainer != null && mFullscreenContainer.getVisibility() == View.VISIBLE) {
            minimizeToMiniPlayer();
            return;
        }

        if (mIsMiniPlayerActive) {
            dismissMiniPlayer();
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

    private void checkAndCaptureStreamUrl(String url) {
        if (url == null || url.isEmpty()) return;
        String lower = url.toLowerCase();
        String format = null;
        if (lower.contains(".m3u8") || lower.contains("/hls/") || lower.contains("format=hls") || lower.contains("format=m3u8") || lower.contains("m3u8?") || lower.contains("playlist.m3u8")) {
            format = "HLS";
        } else if (lower.contains(".mpd") || lower.contains("/dash/") || lower.contains("format=mpd") || lower.contains("mpd?") || lower.contains("manifest.mpd")) {
            format = "DASH";
        } else if (lower.contains(".mp4") || lower.contains("video/mp4") || lower.contains("mime=video%2fmp4") || lower.contains("mime=video/mp4") || lower.contains("mp4?")) {
            format = "MP4";
        } else if (lower.contains(".webm") || lower.contains("video/webm") || lower.contains("mime=video%2fwebm") || lower.contains("webm?")) {
            format = "WEBM";
        } else if (lower.contains(".mkv") || lower.contains("video/x-matroska") || lower.contains("mkv?")) {
            format = "MKV";
        } else if (lower.contains(".flv") || lower.contains("/flv/") || lower.contains("video/x-flv") || lower.contains("flv?")) {
            format = "FLV";
        } else if (lower.contains(".mov") || lower.contains("video/quicktime") || lower.contains("mov?")) {
            format = "MOV";
        } else if (lower.contains(".m4v") || lower.contains("video/x-m4v") || lower.contains("m4v?")) {
            format = "M4V";
        }

        if (format != null && !isAdOrTracker(url)) {
            final String finalUrl = url;
            final String finalFormat = format;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (isFinishing() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed())) {
                        return;
                    }
                    mLastStreamUrl = finalUrl;
                    mLastStreamFormat = finalFormat;
                    if (mBtnFullscreenExtPlayer != null) {
                        mBtnFullscreenExtPlayer.setVisibility(View.VISIBLE);
                        mBtnFullscreenExtPlayer.setText("📺 " + finalFormat);
                    }
                }
            });
        }
    }

    private void launchExternalPlayer(String url, String format) {
        if (url == null || url.isEmpty()) {
            Toast.makeText(this, "No video stream captured yet. Play a video first!", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.parse(url);
            String mimeType = "video/*";
            if (url.contains(".m3u8") || "HLS".equals(format)) mimeType = "application/x-mpegURL";
            else if (url.contains(".mpd") || "DASH".equals(format)) mimeType = "application/dash+xml";
            else if (url.contains(".mp4") || "MP4".equals(format)) mimeType = "video/mp4";
            else if (url.contains(".webm") || "WEBM".equals(format)) mimeType = "video/webm";
            else if (url.contains(".mkv") || "MKV".equals(format)) mimeType = "video/x-matroska";
            else if (url.contains(".flv") || "FLV".equals(format)) mimeType = "video/x-flv";
            else if (url.contains(".mov") || "MOV".equals(format)) mimeType = "video/quicktime";
            else if (url.contains(".m4v") || "M4V".equals(format)) mimeType = "video/x-m4v";

            intent.setDataAndType(uri, mimeType);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            Bundle headers = new Bundle();
            headers.putString("User-Agent", mUserAgentString);
            headers.putString("Referer", TARGET_URL);
            intent.putExtra("android.media.intent.extra.HTTP_HEADERS", headers);

            startActivity(Intent.createChooser(intent, "Play " + format + " in External Player"));
        } catch (Exception e) {
            Toast.makeText(this, "No external video player found. Install VLC, MX Player, or Just Player.", Toast.LENGTH_LONG).show();
        }
    }

    public void enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder();
                builder.setAspectRatio(new Rational(16, 9));
                enterPictureInPictureMode(builder.build());
            } catch (Exception e) {
                try {
                    enterPictureInPictureMode();
                } catch (Exception ignored) {}
            }
        } else {
            Toast.makeText(this, "Picture-in-Picture requires Android 8.0+", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (mFullscreenContainer != null && mFullscreenContainer.getVisibility() == View.VISIBLE) {
                enterPipMode();
            } else if (mLastStreamUrl != null) {
                enterPipMode();
            }
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        if (isInPictureInPictureMode) {
            if (mPlayerTopBar != null) mPlayerTopBar.setVisibility(View.GONE);
            if (mPlayerBottomBar != null) mPlayerBottomBar.setVisibility(View.GONE);
            if (mFabSettings != null) mFabSettings.setVisibility(View.GONE);
            if (mPillExternalPlayer != null) mPillExternalPlayer.setVisibility(View.GONE);
            if (mPopupBootloop != null) mPopupBootloop.setVisibility(View.GONE);
            if (mLockOverlay != null) mLockOverlay.setVisibility(View.GONE);
            if (mGestureTouchView != null) mGestureTouchView.setVisibility(View.GONE);
        } else {
            if (mFullscreenContainer != null && mFullscreenContainer.getVisibility() == View.VISIBLE) {
                if (mPlayerTopBar != null) mPlayerTopBar.setVisibility(View.VISIBLE);
                if (mPlayerBottomBar != null) mPlayerBottomBar.setVisibility(View.VISIBLE);
                if (mGestureController != null) mGestureController.scheduleAutoHideControls();
            }
            if (mGestureTouchView != null) mGestureTouchView.setVisibility(View.VISIBLE);
            applyEdgeToEdgeImmersive();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mIsBatteryReceiverRegistered) {
            try {
                if (mBatteryReceiver != null) unregisterReceiver(mBatteryReceiver);
            } catch (Exception ignored) {}
            mIsBatteryReceiverRegistered = false;
        }
        mHandler.removeCallbacks(mClockUpdateRunnable);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode()) {
            return;
        }
        // Authentication persists while running in background; force-close or manual lock resets it
        if (mGestureController != null) {
            mGestureController.restoreDefaultBrightness();
        }
        if (mWebView != null) {
            mWebView.onPause();
            mWebView.pauseTimers();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyEdgeToEdgeImmersive();
        if (!mIsBatteryReceiverRegistered) {
            try {
                registerReceiver(mBatteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                mIsBatteryReceiverRegistered = true;
            } catch (Exception ignored) {}
        }
        mHandler.post(mClockUpdateRunnable);

        if (mCustomView == null && mGestureController != null) {
            mGestureController.restoreDefaultBrightness();
        }
        checkSecurityLock();
        if (mWebView != null) {
            mWebView.onResume();
            mWebView.resumeTimers();
        }
        AppUpdateManager.startPeriodicUpdateChecker(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleUpdateIntent(intent);
    }

    private void handleUpdateIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra(AppUpdateManager.EXTRA_SHOW_UPDATE, false)) {
            AppUpdateManager.checkForUpdates(this, true);
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (mWebView != null) {
            mWebView.freeMemory();
        }
        if (level >= TRIM_MEMORY_MODERATE) {
            System.gc();
        }
    }

    @Override
    protected void onDestroy() {
        if (mIsBatteryReceiverRegistered) {
            try {
                if (mBatteryReceiver != null) unregisterReceiver(mBatteryReceiver);
            } catch (Exception ignored) {}
            mIsBatteryReceiverRegistered = false;
        }
        mHandler.removeCallbacks(mClockUpdateRunnable);
        mHandler.removeCallbacksAndMessages(null);
        AppUpdateManager.stopPeriodicUpdateChecker();
        if (mWebView != null) {
            try {
                mWebView.stopLoading();
                mWebView.loadUrl("about:blank");
                mWebView.clearHistory();
                mWebView.removeAllViews();
                mWebView.destroy();
            } catch (Exception ignored) {}
            mWebView = null;
        }
        super.onDestroy();
    }

    private void updatePlaybackProgress(final double currentSec, final double totalSec, final boolean isPlaying) {
        if (isFinishing() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed())) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (totalSec > 0 && !Double.isInfinite(totalSec) && !Double.isNaN(totalSec)) {
                    mVideoDuration = totalSec;
                }
                if (!mIsUserSeeking) {
                    if (mTxtTimeCurrent != null) {
                        mTxtTimeCurrent.setText(formatTime((long) currentSec));
                    }
                    if (mTxtTimeTotal != null && mVideoDuration > 0) {
                        mTxtTimeTotal.setText(formatTime((long) mVideoDuration));
                    }
                    if (mPlayerSeekBar != null && mVideoDuration > 0) {
                        int progress = (int) ((currentSec / mVideoDuration) * 1000);
                        mPlayerSeekBar.setProgress(progress);
                    }
                }
                if (mBtnPlayPause != null) {
                    mBtnPlayPause.setText(isPlaying ? "⏸" : "▶");
                }
                if (mBtnMiniPlayPause != null) {
                    mBtnMiniPlayPause.setText(isPlaying ? "⏸" : "▶");
                }
                if (mPbMiniProgress != null && mVideoDuration > 0) {
                    int progress = (int) ((currentSec / mVideoDuration) * 1000);
                    mPbMiniProgress.setProgress(progress);
                }
                if (mGestureController != null) {
                    mGestureController.updatePlaybackTime((long) currentSec, (long) mVideoDuration);
                    mGestureController.updatePlaybackState(isPlaying);
                }
            }
        });
    }

    private String formatTime(long totalSec) {
        if (totalSec < 0) totalSec = 0;
        long h = totalSec / 3600;
        long m = (totalSec % 3600) / 60;
        long s = totalSec % 60;
        if (h > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", m, s);
        }
    }

    private void triggerFullscreenVideo() {
        String script =
            "(function() {" +
            "  function reqFs(win) {" +
            "    if (!win) return false;" +
            "    try {" +
            "      if (win.jwplayer) {" +
            "        var p = (typeof win.jwplayer === 'function') ? win.jwplayer() : null;" +
            "        if (p && typeof p.setFullscreen === 'function') { p.setFullscreen(true); return true; }" +
            "      }" +
            "    } catch(e) {}" +
            "    try {" +
            "      var vs = win.document.querySelectorAll('video');" +
            "      for (var i = 0; i < vs.length; i++) {" +
            "        var v = vs[i];" +
            "        if (v.requestFullscreen) { v.requestFullscreen(); return true; }" +
            "        else if (v.webkitRequestFullscreen) { v.webkitRequestFullscreen(); return true; }" +
            "        else if (v.webkitEnterFullscreen) { v.webkitEnterFullscreen(); return true; }" +
            "      }" +
            "    } catch(e) {}" +
            "    try {" +
            "      for (var j = 0; j < win.frames.length; j++) {" +
            "        if (reqFs(win.frames[j])) return true;" +
            "      }" +
            "    } catch(e) {}" +
            "    try {" +
            "      var iframes = win.document.querySelectorAll('iframe');" +
            "      for (var k = 0; k < iframes.length; k++) {" +
            "        try { if (iframes[k].contentWindow && reqFs(iframes[k].contentWindow)) return true; } catch(e) {}" +
            "      }" +
            "    } catch(e) {}" +
            "    return false;" +
            "  }" +
            "  reqFs(window);" +
            "})();";
        executeJs(script);
    }

    private void injectVideoSyncScript(WebView view) {
        String script =
            "(function() {" +
            "  if (window.__siska_sync_injected) return;" +
            "  window.__siska_sync_injected = true;" +
            "  function getAllVideos(root) {" +
            "    try {" +
            "      return Array.prototype.slice.call((root || document).querySelectorAll('video'));" +
            "    } catch(e) { return []; }" +
            "  }" +
            "  var _cachedDur = 0;" +
            "  function parsePageDuration() {" +
            "    if (_cachedDur > 0) return _cachedDur;" +
            "    try {" +
            "      var els = document.querySelectorAll('.duration, [class*=\"duration\"], [id*=\"duration\"], time');" +
            "      for (var i = 0; i < els.length; i++) {" +
            "        var txt = els[i].textContent || '';" +
            "        var m = txt.match(/(\\d+)\\s*:\\s*(\\d+)(?:\\s*:\\s*(\\d+))?/);" +
            "        if (m) {" +
            "          _cachedDur = m[3] !== undefined ? parseInt(m[1], 10) * 3600 + parseInt(m[2], 10) * 60 + parseInt(m[3], 10) : parseInt(m[1], 10) * 60 + parseInt(m[2], 10);" +
            "          if (_cachedDur > 0) return _cachedDur;" +
            "        }" +
            "      }" +
            "    } catch(e) {}" +
            "    return 0;" +
            "  }" +
            "  function getValidDuration(v) {" +
            "    if (!v) return 0;" +
            "    var d = v.duration;" +
            "    if (typeof d === 'number' && isFinite(d) && d > 0) return d;" +
            "    try {" +
            "      if (v.seekable && v.seekable.length > 0) {" +
            "        var s = v.seekable.end(v.seekable.length - 1);" +
            "        if (typeof s === 'number' && isFinite(s) && s > 0) return s;" +
            "      }" +
            "    } catch(e) {}" +
            "    try {" +
            "      if (v.buffered && v.buffered.length > 0) {" +
            "        var b = v.buffered.end(v.buffered.length - 1);" +
            "        if (typeof b === 'number' && isFinite(b) && b > 0) return b;" +
            "      }" +
            "    } catch(e) {}" +
            "    return parsePageDuration();" +
            "  }" +
            "  function findPrimaryVideo() {" +
            "    var fs = document.fullscreenElement || document.webkitFullscreenElement || document.mozFullScreenElement;" +
            "    if (fs) {" +
            "      if (fs.tagName === 'VIDEO') return fs;" +
            "      var fv = fs.querySelector ? fs.querySelector('video') : null;" +
            "      if (fv) return fv;" +
            "    }" +
            "    var vs = getAllVideos(document);" +
            "    if (!vs || vs.length === 0) return null;" +
            "    if (vs.length === 1) return vs[0];" +
            "    var best = null;" +
            "    var bestScore = -1;" +
            "    for (var i = 0; i < vs.length; i++) {" +
            "      var v = vs[i];" +
            "      var d = getValidDuration(v);" +
            "      var playing = !v.paused && !v.ended && v.currentTime > 0;" +
            "      var area = (v.offsetWidth || 0) * (v.offsetHeight || 0);" +
            "      var score = (playing ? 100000 : 0) + (d > 30 ? d : 0) + (area > 10000 ? 5000 : 0);" +
            "      if (score > bestScore) {" +
            "        bestScore = score;" +
            "        best = v;" +
            "      }" +
            "    }" +
            "    return best || vs[0];" +
            "  }" +
            "  function sendUpdate(cur, dur, playing) {" +
            "    if (dur <= 0) dur = parsePageDuration();" +
            "    if (dur > 0 && window.AndroidBridge && typeof window.AndroidBridge.onVideoTimeUpdate === 'function') {" +
            "      window.AndroidBridge.onVideoTimeUpdate(cur, dur, playing);" +
            "    }" +
            "  }" +
            "  function syncNow() {" +
            "    try {" +
            "      var v = findPrimaryVideo();" +
            "      if (v) {" +
            "        var d = getValidDuration(v);" +
            "        sendUpdate(v.currentTime || 0, d, !v.paused);" +
            "        return;" +
            "      }" +
            "      if (window.jwplayer) {" +
            "        var p = (typeof window.jwplayer === 'function') ? window.jwplayer() : null;" +
            "        if (p && typeof p.getPosition === 'function') {" +
            "          var cur = p.getPosition() || 0;" +
            "          var dur = (typeof p.getDuration === 'function' ? p.getDuration() : 0) || 0;" +
            "          sendUpdate(cur, dur, p.getState && p.getState() === 'playing');" +
            "          return;" +
            "        }" +
            "      }" +
            "    } catch(e) {}" +
            "  }" +
            "  window.addEventListener('message', function(e) {" +
            "    if (e.data && e.data.type === 'SISKA_VIDEO_TIME_UPDATE') {" +
            "      sendUpdate(e.data.current, e.data.duration, e.data.playing);" +
            "    }" +
            "  });" +
            "  var evts = ['timeupdate', 'play', 'pause', 'seeking', 'seeked', 'durationchange', 'loadedmetadata'];" +
            "  for (var k = 0; k < evts.length; k++) {" +
            "    window.addEventListener(evts[k], function(e) {" +
            "      var t = e.target;" +
            "      if (t && t.tagName === 'VIDEO') {" +
            "        var d = getValidDuration(t);" +
            "        sendUpdate(t.currentTime || 0, d, !t.paused);" +
            "      }" +
            "    }, true);" +
            "  }" +
            "  function hookVideo(v) {" +
            "    if (!v || v.__siska_hooked) return;" +
            "    v.__siska_hooked = true;" +
            "    for (var i = 0; i < evts.length; i++) {" +
            "      v.addEventListener(evts[i], function() {" +
            "        if (v === findPrimaryVideo() || !v.paused) {" +
            "          var d = getValidDuration(v);" +
            "          sendUpdate(v.currentTime || 0, d, !v.paused);" +
            "        }" +
            "      }, true);" +
            "    }" +
            "  }" +
            "  function hookAll() {" +
            "    try {" +
            "      var vs = getAllVideos(document);" +
            "      for (var i = 0; i < vs.length; i++) hookVideo(vs[i]);" +
            "    } catch(e) {}" +
            "    try {" +
            "      if (window.jwplayer) {" +
            "        var p = (typeof window.jwplayer === 'function') ? window.jwplayer() : null;" +
            "        if (p && typeof p.on === 'function' && !p.__siska_hooked) {" +
            "          p.__siska_hooked = true;" +
            "          p.on('time', function(e) { sendUpdate(e.position, e.duration || parsePageDuration(), true); });" +
            "          p.on('play', function() { sendUpdate(p.getPosition() || 0, p.getDuration() || parsePageDuration(), true); });" +
            "          p.on('pause', function() { sendUpdate(p.getPosition() || 0, p.getDuration() || parsePageDuration(), false); });" +
            "          p.on('seek', function(e) { sendUpdate(e.offset || 0, p.getDuration() || parsePageDuration(), true); });" +
            "        }" +
            "      }" +
            "    } catch(e) {}" +
            "  }" +
            "  hookAll();" +
            "  try {" +
            "    var _debTimer = null;" +
            "    var obs = new MutationObserver(function() {" +
            "      if (_debTimer) return;" +
            "      _debTimer = setTimeout(function() { _debTimer = null; hookAll(); syncNow(); }, 400);" +
            "    });" +
            "    obs.observe(document.documentElement || document.body, { childList: true, subtree: true });" +
            "  } catch(e) {}" +
            "  setInterval(syncNow, 500);" +
            "})();";
        executeJs(script);
    }

    public class AndroidBridge {
        @JavascriptInterface
        public void onVideoTimeUpdate(final double currentSec, final double totalSec, final boolean isPlaying) {
            updatePlaybackProgress(currentSec, totalSec, isPlaying);
        }

        @JavascriptInterface
        public void onVideoTitle(final String title) {
            if (title != null && !title.isEmpty()) {
                if (isFinishing() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed())) {
                    return;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mTxtPlayerTitle != null) {
                            mTxtPlayerTitle.setText(title);
                        }
                        if (mTxtMiniTitle != null) {
                            mTxtMiniTitle.setText(title);
                        }
                    }
                });
            }
        }
    }
}
