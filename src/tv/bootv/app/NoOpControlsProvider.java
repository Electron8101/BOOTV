package tv.bootv.app;

import android.content.Context;
import com.devbrackets.android.exomedia.ui.widget.controls.VideoControls;
import com.devbrackets.android.exomedia.ui.widget.controls.VideoControlsProvider;

/**
 * Custom VideoControlsProvider that suppresses default ExoMedia controls
 * since BOOTV provides its own Material 3 & Soul Browser gesture controls.
 */
public class NoOpControlsProvider extends VideoControlsProvider {
    public NoOpControlsProvider() {
        super();
    }

    @Override
    public VideoControls getControls(Context context) {
        return null;
    }
}
