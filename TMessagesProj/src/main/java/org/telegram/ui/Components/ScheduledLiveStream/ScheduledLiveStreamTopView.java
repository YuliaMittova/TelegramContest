package org.telegram.ui.Components.ScheduledLiveStream;

import static java.time.temporal.ChronoUnit.MILLIS;

import android.content.Context;
import android.os.CountDownTimer;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.telegram.messenger.R;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class ScheduledLiveStreamTopView extends FrameLayout {

    private final int MILLIS_IN_HOUR = 3600000;
    private final int MILLIS_IN_MIN = 60000;
    private final int MILLIS_IN_SEC = 1000;
    private final int HOURS_IN_DAY = 24;
    private final int MINUTES_IN_HOUR = 60;
    private final int SECONDS_IN_MINUTE = 60;
    private final int COUNTDOWN_UPDATE_INTERVAL_IN_MILLIS = 1000;
    private final String FINISHED_COUNTDOWN = "00:00:00";
    private final String STARTS_ON_FORMAT_DATE = "dd MMM yyyy";
    private final String STARTS_ON_FORMAT_TIME = "HH:mm";

    private final NumberFormat numberFormat = new DecimalFormat("00");
    private final TextView countDownView;
    private OnClickListener clickListener;
    private boolean isCountdownStarted = false;

    //TimeZone.getDefault():
    public ScheduledLiveStreamTopView(Context context, OffsetDateTime startDateTime) {
        super(context);

        inflate(context, R.layout.scheduled_live_stream_top_view, this);
        final String countdownStringFormat = getResources().getString(R.string.ScheduledLiveStream_countdownFormat);
        countDownView = findViewById(R.id.scheduledLiveStream_countdown);
        final TextView startsOnView = findViewById(R.id.scheduledLiveStream_startsOnText);

        final String dateString = DateTimeFormatter.ofPattern(STARTS_ON_FORMAT_DATE).format(startDateTime);
        final String timeString = DateTimeFormatter.ofPattern(STARTS_ON_FORMAT_TIME).format(startDateTime);
        startsOnView.setText(getResources().getString(R.string.ScheduledLiveStream_startsOnFormat, dateString, timeString));

        countDownView.setOnClickListener(v -> {
            if (isCountdownStarted) return;
            isCountdownStarted = true;
            clickListener.onClick(this);

            final long millisInFuture = startDateTime.minus(System.currentTimeMillis(), MILLIS).toEpochSecond() * MILLIS_IN_SEC;
            new CountDownTimer(millisInFuture, COUNTDOWN_UPDATE_INTERVAL_IN_MILLIS) {
                @Override
                public void onTick(long millisUntilFinished) {
                    // Used for formatting digits to be in 2 digits only
                    final long hour = (millisUntilFinished / MILLIS_IN_HOUR) % HOURS_IN_DAY;
                    final long min = (millisUntilFinished / MILLIS_IN_MIN) % MINUTES_IN_HOUR;
                    final long sec = (millisUntilFinished / MILLIS_IN_SEC) % SECONDS_IN_MINUTE;
                    countDownView.setText(String.format(countdownStringFormat, numberFormat.format(hour), numberFormat.format(min), numberFormat.format(sec)));
                }

                @Override
                public void onFinish() {
                    countDownView.setText(FINISHED_COUNTDOWN);
                }
            }.start();
        });
    }

    public void setOnNotifyClickListener(final OnClickListener clickListener) {
        this.clickListener = clickListener;
    }
}
