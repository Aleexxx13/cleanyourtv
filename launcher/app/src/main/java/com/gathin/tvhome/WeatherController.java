package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.net.ssl.HttpsURLConnection;

/** Opt-in fork feature: no service, no location lookup, no work scheduled while Home is paused. */
final class WeatherController implements AutoCloseable {
    private final Context context;
    private final Weather.Place place;
    private final TextView line;
    private final SharedPreferences cache;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Runnable tick = this::refresh;
    private Weather.Report report;
    private Future<?> request;
    private long elapsedAttempt;
    private volatile HttpsURLConnection connection;
    private volatile boolean active;
    private volatile int generation;

    WeatherController(Context context, Weather.Place place, FrameLayout host) {
        this.context = context;
        this.place = place;
        LinearLayout panel = new LinearLayout(context);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setFocusable(false);
        float density = context.getResources().getDisplayMetrics().density;
        TextView city = text(11, 0x8CFFFFFF, R.string.font_medium);
        city.setLetterSpacing(0.16f);
        city.setAllCaps(true);
        city.setText(place.name);
        panel.addView(city);
        line = text(23, 0xE6FFFFFF, R.string.font_light);
        LinearLayout.LayoutParams row = new LinearLayout.LayoutParams(-1, -2);
        row.topMargin = Math.round(6 * density);
        panel.addView(line, row);
        FrameLayout.LayoutParams position = new FrameLayout.LayoutParams(Math.round(340 * density), -2, Gravity.TOP | Gravity.LEFT);
        position.leftMargin = Math.round(60 * density);
        position.topMargin = Math.round(116 * density);
        host.addView(panel, host.indexOfChild(host.findViewById(R.id.hero)), position);
        cache = context.getSharedPreferences("home_weather", Context.MODE_PRIVATE);
        if (!place.key().equals(cache.getString("place", ""))) {
            cache.edit().clear().putString("place", place.key()).apply();
        } else if (cache.contains("temperature")) {
            report = Weather.report(Double.longBitsToDouble(cache.getLong("temperature", 0)),
                    cache.getInt("code", -1), cache.getInt("day", -1), cache.getLong("observed", 0));
        }
        panel.setVisibility(place.enabled ? View.VISIBLE : View.GONE);
    }

    private TextView text(int size, int colour, int font) {
        TextView text = new TextView(context);
        text.setTextSize(size);
        text.setTextColor(colour);
        text.setTypeface(UiFont.get(context, font));
        text.setSingleLine(true);
        text.setEllipsize(TextUtils.TruncateAt.END);
        text.setIncludeFontPadding(false);
        text.setFocusable(false);
        return text;
    }

    void start() {
        if (!place.enabled || active) return;
        active = true;
        generation++;
        refresh();
    }

    void stop() {
        active = false;
        generation++;
        main.removeCallbacksAndMessages(null);
        if (request != null) request.cancel(true);
        HttpsURLConnection running = connection;
        if (running != null) running.disconnect();
    }

    @Override public void close() {
        stop();
        io.shutdownNow();
    }

    private void refresh() {
        if (!active) return;
        main.removeCallbacks(tick);
        render();
        long now = System.currentTimeMillis();
        long delay = elapsedAttempt > 0
                ? Weather.delayUntilFetch(elapsedAttempt, SystemClock.elapsedRealtime())
                : Weather.delayUntilFetch(cache.getLong("attempt", 0), now);
        if (delay > 0) {
            main.postDelayed(tick, Weather.nextUpdateDelay(report, now, delay));
            return;
        }
        // Persist attempts as well as successes: opening Home repeatedly while offline must not hammer the API.
        elapsedAttempt = SystemClock.elapsedRealtime();
        cache.edit().putLong("attempt", now >= Weather.MIN_TIME ? now : 0).apply();
        final int token = generation;
        request = io.submit(() -> {
            Weather.Report fetched = null;
            try {
                if (active && token == generation && !Thread.currentThread().isInterrupted()) fetched = fetch(token);
            } catch (Exception ignored) {
                // Home remains usable. The next foreground tick retries; never substitute zero degrees.
            }
            final Weather.Report result = fetched;
            if (!active || token != generation) return;
            main.post(() -> {
                if (!active || token != generation) return;
                if (Weather.fresh(result, System.currentTimeMillis())) {
                    report = result;
                    cache.edit().putLong("temperature", Double.doubleToLongBits(result.temperature))
                            .putInt("code", result.code).putInt("day", result.day ? 1 : 0)
                            .putLong("observed", result.observed).apply();
                }
                render();
                main.removeCallbacks(tick);
                long remaining = Weather.delayUntilFetch(elapsedAttempt, SystemClock.elapsedRealtime());
                main.postDelayed(tick, Weather.nextUpdateDelay(report, System.currentTimeMillis(), remaining));
            });
        });
        main.postDelayed(tick, Weather.nextUpdateDelay(report, now, Weather.REFRESH_MS));
    }

    private Weather.Report fetch(int token) throws Exception {
        URL url = new URL("https://api.open-meteo.com/v1/forecast?latitude=" + place.latitude
                + "&longitude=" + place.longitude
                + "&current=temperature_2m,weather_code,is_day&temperature_unit=celsius&timeformat=unixtime&forecast_days=1");
        HttpsURLConnection c = (HttpsURLConnection) url.openConnection();
        c.setConnectTimeout(8_000);
        c.setReadTimeout(8_000);
        c.setInstanceFollowRedirects(false);
        connection = c;
        try {
            if (!active || token != generation || Thread.currentThread().isInterrupted()) return null;
            if (c.getResponseCode() != 200 || c.getContentLengthLong() > 16_384) return null;
            try (InputStream in = c.getInputStream(); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[1024];
                int n;
                while ((n = in.read(buffer)) != -1) {
                    if (!active || token != generation || Thread.currentThread().isInterrupted()) return null;
                    if (bytes.size() + n > 16_384) throw new IOException("Weather response too large");
                    bytes.write(buffer, 0, n);
                }
                JSONObject json = new JSONObject(bytes.toString(StandardCharsets.UTF_8.name()));
                if (!"°C".equals(json.getJSONObject("current_units").optString("temperature_2m"))) return null;
                JSONObject current = json.getJSONObject("current");
                Object temperature = current.opt("temperature_2m");
                Object time = current.opt("time");
                if (!(temperature instanceof Number) || !(time instanceof Number)) return null;
                double seconds = ((Number) time).doubleValue();
                if (!Double.isFinite(seconds) || seconds < Weather.MIN_TIME / 1000L || seconds > 4_102_444_800L) return null;
                return Weather.report(((Number) temperature).doubleValue(), integer(current, "weather_code"),
                        integer(current, "is_day"), (long) (seconds * 1000));
            }
        } finally {
            c.disconnect();
            connection = null;
        }
    }

    private static Integer integer(JSONObject json, String key) {
        Object value = json.opt(key);
        if (!(value instanceof Number)) return null;
        double number = ((Number) value).doubleValue();
        return Double.isFinite(number) && number == (int) number ? (int) number : null;
    }

    @android.annotation.SuppressLint("SetTextI18n") // Fixed French display in this personal fork.
    private void render() {
        if (!Weather.fresh(report, System.currentTimeMillis())) {
            line.setText(Weather.Condition.UNKNOWN.label);
            return;
        }
        String condition = Weather.condition(report.code, report.day).label;
        line.setText(report.degrees() + " °C · " + condition);
    }
}
