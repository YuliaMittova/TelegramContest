package org.telegram.ui.Components.voip;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.ColorInt;

import org.telegram.ui.Components.RLottieDrawable;
import org.telegram.ui.Stories.recorder.HintView2;

@SuppressLint("ViewConstructor")
public class BotButtonHintView extends HintView2 {

    public BotButtonHintView(Context context, int direction, @ColorInt int backgroundColor, int textSizeDp) {
        super(context, direction);
        backgroundPaint.setColor(backgroundColor);
        setTextSize(textSizeDp);
    }

    public HintView2 setIconWithSize(int resId, int iconSize) {
        RLottieDrawable icon = new RLottieDrawable(resId, "" + resId, dp(iconSize), dp(iconSize));
        icon.start();
        return setIcon(icon);
    }
}
