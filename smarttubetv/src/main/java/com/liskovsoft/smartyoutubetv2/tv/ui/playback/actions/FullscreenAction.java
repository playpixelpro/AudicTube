package com.liskovsoft.smartyoutubetv2.tv.ui.playback.actions;

import android.content.Context;
import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * An action for toggling between portrait (strip) and landscape (fullscreen) video modes.
 * Off state = enter fullscreen, On state = exit fullscreen (tinted).
 */
public class FullscreenAction extends TwoStateAction {
    public FullscreenAction(Context context) {
        super(context, R.id.action_fullscreen, R.drawable.action_fullscreen);

        String[] labels = new String[2];
        labels[INDEX_OFF] = context.getString(R.string.action_fullscreen);
        labels[INDEX_ON] = context.getString(R.string.action_fullscreen_exit);
        setLabels(labels);
    }
}