package com.gathin.tvhome;

/** Weather values and policy, independent of Android, the network and wall-clock reads. */
final class Weather {
    static final long REFRESH_MS = 30 * 60_000L;
    static final long MAX_AGE_MS = 90 * 60_000L;
    static final long MIN_TIME = 1_735_689_600_000L; // 2025; TVs may boot with a 1970 clock

    enum Condition {
        SUN("Ensoleillé"), CLEAR_NIGHT("Ciel dégagé"), PARTLY_CLOUDY("Quelques nuages"), CLOUDY("Nuageux"),
        FOG("Brouillard"), DRIZZLE("Bruine"), RAIN("Pluvieux"), SHOWERS("Averses"), SNOW("Neige"),
        STORM("Orageux"), UNKNOWN("Météo indisponible");
        final String label;
        Condition(String label) { this.label = label; }
    }

    static final class Place {
        final boolean enabled;
        final String name;
        final double latitude, longitude;

        Place(boolean enabled, String name, double latitude, double longitude) {
            this.name = name == null ? "" : name.trim();
            this.latitude = latitude;
            this.longitude = longitude;
            this.enabled = enabled && !this.name.isEmpty() && this.name.length() <= 40
                    && !this.name.matches(".*[\\p{Cntrl}].*")
                    && Math.abs(latitude) <= 90 && Math.abs(longitude) <= 180;
        }

        String key() { return name + ":" + latitude + ":" + longitude; }
    }

    static final class Report {
        final double temperature;
        final int code;
        final boolean day;
        final long observed;

        private Report(double temperature, int code, boolean day, long observed) {
            this.temperature = temperature;
            this.code = code;
            this.day = day;
            this.observed = observed;
        }

        int degrees() { return (int) Math.round(temperature); }
    }

    static Report report(Double temperature, Integer code, Integer day, long observed) {
        if (temperature == null || !Double.isFinite(temperature) || temperature < -90 || temperature > 65
                || code == null || condition(code, true) == Condition.UNKNOWN
                || day == null || (day != 0 && day != 1) || observed < MIN_TIME
                || observed > 4_102_444_800_000L) return null;
        return new Report(temperature, code, day == 1, observed);
    }

    static boolean fresh(Report report, long now) {
        return report != null && now >= MIN_TIME && report.observed <= now + 5 * 60_000L
                && now - report.observed <= MAX_AGE_MS;
    }

    static long delayUntilFetch(long lastAttempt, long now) {
        if (lastAttempt <= 0 || now < lastAttempt) return 0;
        return Math.max(0, REFRESH_MS - (now - lastAttempt));
    }

    /** Render at expiry even when another network attempt is not due yet. */
    static long nextUpdateDelay(Report report, long now, long fetchDelay) {
        return fresh(report, now) ? Math.min(fetchDelay, Math.max(1, report.observed + MAX_AGE_MS - now + 1)) : fetchDelay;
    }

    static int gridTopPadding(boolean enabled, boolean dock, int padding, float density) {
        return padding + (enabled && !dock ? Math.round(72 * density) : 0);
    }

    static Condition condition(int code, boolean day) {
        switch (code) {
            case 0: case 1: return day ? Condition.SUN : Condition.CLEAR_NIGHT;
            case 2: return Condition.PARTLY_CLOUDY;
            case 3: return Condition.CLOUDY;
            case 45: case 48: return Condition.FOG;
            case 51: case 53: case 55: case 56: case 57: return Condition.DRIZZLE;
            case 61: case 63: case 65: case 66: case 67: return Condition.RAIN;
            case 80: case 81: case 82: return Condition.SHOWERS;
            case 71: case 73: case 75: case 77: case 85: case 86: return Condition.SNOW;
            case 95: case 96: case 99: return Condition.STORM;
            default: return Condition.UNKNOWN;
        }
    }
}
