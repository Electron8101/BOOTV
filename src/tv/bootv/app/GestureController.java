package tv.bootv.app;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

public class GestureController {

    public interface VideoActionListener {
        void onSeek(int secondsDelta);
        void onPlaybackControl(boolean shouldPlay);
        void onSpeedChange(float speed);
        void onAspectRatioChange(int mode);
    }

    private final Activity mActivity;
    private final AudioManager mAudioManager;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final VideoActionListener mListener;

    private final View mTouchView;
    private final View mTopBar;
    private View mBottomBar;
    private final View mHudBrightness;
    private final TextView mTxtBrightness;
    private final ProgressBar mPbBrightness;
    private final View mHudVolume;
    private final TextView mTxtVolume;
    private final ProgressBar mPbVolume;
    private final View mHudSeek;
    private final TextView mTxtSeekDelta;
    private final TextView mHudFeedback;

    private View mCustomView; // Active video view reference

    private boolean mIsLocked = false;
    private int mCurrentAspectMode = 0; // 0: Fit, 1: Fill (Notch Zoom), 2: Stretch
    private float mCurrentSpeed = 1.0f;
    private boolean mIsLongPressSpeedBoost = false;
    private float mPreBoostSpeed = 1.0f;

    // Gesture tracking state
    private static final int GESTURE_NONE = 0;
    private static final int GESTURE_BRIGHTNESS = 1;
    private static final int GESTURE_VOLUME = 2;
    private static final int GESTURE_SEEK = 3;
    private int mActiveGesture = GESTURE_NONE;

    private float mInitialBrightness = 0.5f;
    private int mInitialVolume = 0;
    private int mMaxVolume = 15;
    private int mCurrentSeekDelta = 0;

    private final GestureDetector mGestureDetector;
    private final ScaleGestureDetector mScaleDetector;

    private final TextView mBtnPlayPause;
    private boolean mIsPlaying = true;

    public GestureController(Activity activity,
                             View touchView,
                             View topBar,
                             View btnRewind10,
                             TextView btnPlayPause,
                             View btnForward10,
                             View hudBrightness, TextView txtBrightness, ProgressBar pbBrightness,
                             View hudVolume, TextView txtVolume, ProgressBar pbVolume,
                             View hudSeek, TextView txtSeekDelta,
                             TextView hudFeedback,
                             VideoActionListener listener) {
        mActivity = activity;
        mListener = listener;
        mBtnPlayPause = btnPlayPause;
        mAudioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        if (mAudioManager != null) {
            mMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        }

        mTouchView = touchView;
        mTopBar = topBar;
        mHudBrightness = hudBrightness;
        mTxtBrightness = txtBrightness;
        mPbBrightness = pbBrightness;
        mHudVolume = hudVolume;
        mTxtVolume = txtVolume;
        mPbVolume = pbVolume;
        mHudSeek = hudSeek;
        mTxtSeekDelta = txtSeekDelta;
        mHudFeedback = hudFeedback;

        if (btnRewind10 != null) {
            btnRewind10.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) mListener.onSeek(-10);
                    showFeedback("⏪ -10s");
                }
            });
        }

        if (mBtnPlayPause != null) {
            mBtnPlayPause.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    togglePlayPause();
                }
            });
        }

        if (btnForward10 != null) {
            btnForward10.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) mListener.onSeek(10);
                    showFeedback("⏩ +10s");
                }
            });
        }

        mGestureDetector = new GestureDetector(activity, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                mActiveGesture = GESTURE_NONE;
                mCurrentSeekDelta = 0;

                // Capture initial brightness
                float curBrightness = mActivity.getWindow().getAttributes().screenBrightness;
                if (curBrightness < 0) curBrightness = 0.5f;
                mInitialBrightness = curBrightness;

                // Capture initial volume
                if (mAudioManager != null) {
                    mInitialVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                }
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (mIsLocked || e1 == null || e2 == null) return false;

                float width = mTouchView.getWidth();
                float height = mTouchView.getHeight();
                if (width <= 0 || height <= 0) return false;

                float deltaX = e2.getX() - e1.getX();
                float deltaY = e2.getY() - e1.getY();

                if (mActiveGesture == GESTURE_NONE) {
                    if (Math.abs(deltaY) > Math.abs(deltaX) && Math.abs(deltaY) > 20) {
                        if (e1.getX() < width * 0.45f) {
                            mActiveGesture = GESTURE_BRIGHTNESS;
                        } else if (e1.getX() > width * 0.55f) {
                            mActiveGesture = GESTURE_VOLUME;
                        }
                    } else if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > 25) {
                        mActiveGesture = GESTURE_SEEK;
                    }
                }

                if (mActiveGesture == GESTURE_BRIGHTNESS) {
                    handleBrightnessScroll(deltaY, height);
                    return true;
                } else if (mActiveGesture == GESTURE_VOLUME) {
                    handleVolumeScroll(deltaY, height);
                    return true;
                } else if (mActiveGesture == GESTURE_SEEK) {
                    handleSeekScroll(deltaX, width);
                    return true;
                }

                return false;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (mIsLocked) return false;
                float width = mTouchView.getWidth();
                if (width <= 0) return false;

                if (e.getX() < width * 0.35f) {
                    if (mListener != null) mListener.onSeek(-10);
                    showFeedback("⏪ -10s");
                    return true;
                } else if (e.getX() > width * 0.65f) {
                    if (mListener != null) mListener.onSeek(10);
                    showFeedback("⏩ +10s");
                    return true;
                } else {
                    togglePlayPause();
                    return true;
                }
            }

            @Override
            public void onLongPress(MotionEvent e) {
                if (mIsLocked) return;
                mIsLongPressSpeedBoost = true;
                mPreBoostSpeed = mCurrentSpeed;
                if (mListener != null) {
                    mListener.onSpeedChange(2.0f);
                }
                showFeedback("⚡ 2x Speed (Hold)");
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (mTopBar != null && mTopBar.getVisibility() == View.VISIBLE) {
                    hideControls();
                } else {
                    showControls();
                }
                return true;
            }
        });

        mScaleDetector = new ScaleGestureDetector(activity, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (mIsLocked) return false;
                float factor = detector.getScaleFactor();
                if (factor > 1.12f) {
                    if (mCurrentAspectMode != 1) {
                        mCurrentAspectMode = 1;
                        showFeedback("⤢ Zoom to Notch (Fill)");
                        if (mListener != null) mListener.onAspectRatioChange(mCurrentAspectMode);
                    }
                    return true;
                } else if (factor < 0.88f) {
                    if (mCurrentAspectMode != 0) {
                        mCurrentAspectMode = 0;
                        showFeedback("⤢ Fit to Screen");
                        if (mListener != null) mListener.onAspectRatioChange(mCurrentAspectMode);
                    }
                    return true;
                }
                return false;
            }
        });

        mTouchView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getPointerCount() > 1) {
                    mScaleDetector.onTouchEvent(event);
                    return true;
                }

                boolean handled = mGestureDetector.onTouchEvent(event);
                if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    if (mIsLongPressSpeedBoost) {
                        mIsLongPressSpeedBoost = false;
                        if (mListener != null) {
                            mListener.onSpeedChange(mPreBoostSpeed);
                        }
                        showFeedback("⚡ Speed: " + mPreBoostSpeed + "x");
                    }
                    if (mActiveGesture == GESTURE_SEEK && mCurrentSeekDelta != 0) {
                        if (mListener != null) {
                            mListener.onSeek(mCurrentSeekDelta);
                        }
                        showFeedback((mCurrentSeekDelta > 0 ? "⏩ +" : "⏪ ") + mCurrentSeekDelta + "s");
                    }
                    mActiveGesture = GESTURE_NONE;
                    mCurrentSeekDelta = 0;
                    scheduleAutoHideHuds();
                }
                return true;
            }
        });
    }

    public void setCustomView(View customView) {
        mCustomView = customView;
    }

    private void handleBrightnessScroll(float deltaY, float height) {
        float percentChange = -(deltaY / height) * 1.2f;
        float newBrightness = Math.max(0.01f, Math.min(1.0f, mInitialBrightness + percentChange));

        WindowManager.LayoutParams lp = mActivity.getWindow().getAttributes();
        lp.screenBrightness = newBrightness;
        mActivity.getWindow().setAttributes(lp);

        int percent = (int) (newBrightness * 100);
        mTxtBrightness.setText("☀️ " + percent + "%");
        mPbBrightness.setProgress(percent);
        mHudBrightness.setVisibility(View.VISIBLE);
        mHudVolume.setVisibility(View.GONE);
        mHudSeek.setVisibility(View.GONE);
    }

    private void handleVolumeScroll(float deltaY, float height) {
        if (mAudioManager == null || mMaxVolume <= 0) return;

        float percentChange = -(deltaY / height) * 1.5f;
        int deltaVol = (int) (percentChange * mMaxVolume);
        int newVolume = Math.max(0, Math.min(mMaxVolume, mInitialVolume + deltaVol));

        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0);

        int percent = (int) (((float) newVolume / mMaxVolume) * 100);
        mTxtVolume.setText("🔊 " + percent + "%");
        mPbVolume.setProgress(percent);
        mHudVolume.setVisibility(View.VISIBLE);
        mHudBrightness.setVisibility(View.GONE);
        mHudSeek.setVisibility(View.GONE);
    }

    private void handleSeekScroll(float deltaX, float width) {
        // Swipe across width calculates up to +/- 120 seconds seek
        float percentX = deltaX / width;
        mCurrentSeekDelta = (int) (percentX * 120);

        if (mCurrentSeekDelta == 0) return;

        String prefix = mCurrentSeekDelta > 0 ? "⏩ +" : "⏪ ";
        mTxtSeekDelta.setText(prefix + Math.abs(mCurrentSeekDelta) + "s");
        mHudSeek.setVisibility(View.VISIBLE);
        mHudBrightness.setVisibility(View.GONE);
        mHudVolume.setVisibility(View.GONE);
    }

    private void scheduleAutoHideHuds() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                fadeOutHud(mHudBrightness);
                fadeOutHud(mHudVolume);
                fadeOutHud(mHudSeek);
            }
        }, 700);
    }

    private void fadeOutHud(final View hud) {
        if (hud != null && hud.getVisibility() == View.VISIBLE) {
            hud.animate().alpha(0f).setDuration(200).withEndAction(new Runnable() {
                @Override
                public void run() {
                    hud.setVisibility(View.GONE);
                    hud.setAlpha(1f);
                }
            }).start();
        }
    }

    public void setBottomBar(View bottomBar) {
        mBottomBar = bottomBar;
    }

    private final Runnable mAutoHideRunnable = new Runnable() {
        @Override
        public void run() {
            hideControls();
        }
    };

    public void scheduleAutoHideTopBar() {
        scheduleAutoHideControls();
    }

    public void cancelAutoHideControls() {
        mHandler.removeCallbacks(mAutoHideRunnable);
    }

    public void scheduleAutoHideControls() {
        mHandler.removeCallbacks(mAutoHideRunnable);
        mHandler.postDelayed(mAutoHideRunnable, 4500);
    }

    public void showControls() {
        if (mTopBar != null) {
            mTopBar.setAlpha(0f);
            mTopBar.setVisibility(View.VISIBLE);
            mTopBar.animate().alpha(1f).setDuration(220).start();
        }
        if (mBottomBar != null) {
            mBottomBar.setAlpha(0f);
            mBottomBar.setVisibility(View.VISIBLE);
            mBottomBar.animate().alpha(1f).setDuration(220).start();
        }
        scheduleAutoHideControls();
    }

    public void hideControls() {
        if (mTopBar != null && mTopBar.getVisibility() == View.VISIBLE) {
            mTopBar.animate().alpha(0f).setDuration(220).withEndAction(new Runnable() {
                @Override
                public void run() {
                    mTopBar.setVisibility(View.GONE);
                    mTopBar.setAlpha(1f);
                }
            }).start();
        }
        if (mBottomBar != null && mBottomBar.getVisibility() == View.VISIBLE) {
            mBottomBar.animate().alpha(0f).setDuration(220).withEndAction(new Runnable() {
                @Override
                public void run() {
                    mBottomBar.setVisibility(View.GONE);
                    mBottomBar.setAlpha(1f);
                }
            }).start();
        }
    }

    public void showFeedback(String message) {
        mHudFeedback.setText(message);
        mHudFeedback.setAlpha(0f);
        mHudFeedback.setVisibility(View.VISIBLE);
        mHudFeedback.animate().alpha(1f).setDuration(160).start();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mHudFeedback != null && mHudFeedback.getVisibility() == View.VISIBLE) {
                    mHudFeedback.animate().alpha(0f).setDuration(220).withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            mHudFeedback.setVisibility(View.GONE);
                            mHudFeedback.setAlpha(1f);
                        }
                    }).start();
                }
            }
        }, 1500);
    }

    public boolean toggleLock() {
        mIsLocked = !mIsLocked;
        showFeedback(mIsLocked ? "🔒 Gestures Locked" : "🔓 Gestures Unlocked");
        return mIsLocked;
    }

    public int cycleAspectRatio() {
        mCurrentAspectMode = (mCurrentAspectMode + 1) % 3;
        String modeName;
        switch (mCurrentAspectMode) {
            case 1: modeName = "⤢ Zoom to Notch (Fill)"; break;
            case 2: modeName = "⤢ Stretch"; break;
            default: modeName = "⤢ Fit to Screen"; break;
        }
        showFeedback(modeName);
        if (mListener != null) {
            mListener.onAspectRatioChange(mCurrentAspectMode);
        }
        return mCurrentAspectMode;
    }

    public float cycleSpeed() {
        if (mCurrentSpeed == 1.0f) mCurrentSpeed = 1.25f;
        else if (mCurrentSpeed == 1.25f) mCurrentSpeed = 1.5f;
        else if (mCurrentSpeed == 1.5f) mCurrentSpeed = 2.0f;
        else if (mCurrentSpeed == 2.0f) mCurrentSpeed = 3.0f;
        else if (mCurrentSpeed == 3.0f) mCurrentSpeed = 0.5f;
        else if (mCurrentSpeed == 0.5f) mCurrentSpeed = 0.75f;
        else mCurrentSpeed = 1.0f;

        showFeedback("⚡ Speed: " + mCurrentSpeed + "x");
        if (mListener != null) {
            mListener.onSpeedChange(mCurrentSpeed);
        }
        return mCurrentSpeed;
    }

    public void restoreDefaultBrightness() {
        WindowManager.LayoutParams lp = mActivity.getWindow().getAttributes();
        lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        mActivity.getWindow().setAttributes(lp);
    }

    public void togglePlayPause() {
        mIsPlaying = !mIsPlaying;
        if (mListener != null) {
            mListener.onPlaybackControl(mIsPlaying);
        }
        if (mBtnPlayPause != null) {
            mBtnPlayPause.setText(mIsPlaying ? "⏸ Pause" : "▶ Play");
        }
        showFeedback(mIsPlaying ? "▶ Playing" : "⏸ Paused");
    }

    public void reset() {
        mIsLocked = false;
        mIsPlaying = true;
        if (mBtnPlayPause != null) {
            mBtnPlayPause.setText("⏸ Pause");
        }
        mActiveGesture = GESTURE_NONE;
        mCurrentSeekDelta = 0;
        mHudBrightness.setVisibility(View.GONE);
        mHudVolume.setVisibility(View.GONE);
        mHudSeek.setVisibility(View.GONE);
        mHudFeedback.setVisibility(View.GONE);
        mTopBar.setVisibility(View.VISIBLE);
        if (mBottomBar != null) mBottomBar.setVisibility(View.VISIBLE);
        scheduleAutoHideControls();
    }
}
