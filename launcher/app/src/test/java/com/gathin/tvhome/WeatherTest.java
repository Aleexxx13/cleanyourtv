package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class WeatherTest {
    private static final long NOW = 1792800000000L;

    @Test public void clearSkyUsesNightInsteadOfSunAfterDark() {
        assertEquals(Weather.Condition.SUN, Weather.condition(0, true));
        assertEquals(Weather.Condition.CLEAR_NIGHT, Weather.condition(0, false));
        assertEquals(Weather.Condition.CLEAR_NIGHT, Weather.condition(1, false));
    }
    @Test public void codesKeepFogSnowAndStormDistinctFromRain() {
        assertEquals(Weather.Condition.PARTLY_CLOUDY, Weather.condition(2, true));
        assertEquals(Weather.Condition.CLOUDY, Weather.condition(3, true));
        for (int c : new int[]{45,48}) assertEquals(Weather.Condition.FOG, Weather.condition(c,true));
        for (int c : new int[]{51,53,55,56,57}) assertEquals(Weather.Condition.DRIZZLE, Weather.condition(c,true));
        for (int c : new int[]{61,63,65,66,67}) assertEquals(Weather.Condition.RAIN, Weather.condition(c,true));
        for (int c : new int[]{80,81,82}) assertEquals(Weather.Condition.SHOWERS, Weather.condition(c,true));
        for (int c : new int[]{71,73,75,77,85,86}) assertEquals(Weather.Condition.SNOW, Weather.condition(c,true));
        for (int c : new int[]{95,96,99}) assertEquals(Weather.Condition.STORM, Weather.condition(c,true));
        assertEquals(Weather.Condition.UNKNOWN, Weather.condition(4,true));
    }
    @Test public void invalidOrMissingValuesCannotBecomeZeroDegreesAndSun() {
        assertNull(Weather.report(null,0,1,NOW));
        assertNull(Weather.report(Double.NaN,0,1,NOW));
        assertNull(Weather.report(Double.POSITIVE_INFINITY,0,1,NOW));
        assertNull(Weather.report(100.0,0,1,NOW));
        assertNull(Weather.report(12.0,null,1,NOW));
        assertNull(Weather.report(12.0,4,1,NOW));
        assertNull(Weather.report(12.0,0,null,NOW));
        assertNull(Weather.report(12.0,0,2,NOW));
        assertNull(Weather.report(12.0,0,1,0));
    }
    @Test public void temperaturePreservesNegativeAndRoundsForDisplay() {
        assertEquals(-3, Weather.report(-2.8,3,0,NOW).degrees());
        assertEquals(19, Weather.report(18.6,0,1,NOW).degrees());
        assertEquals(0, Weather.report(-0.2,0,1,NOW).degrees());
    }
    @Test public void staleOrFutureReportIsNotCurrent() {
        assertTrue(Weather.fresh(Weather.report(18.0,0,1,NOW),NOW));
        assertTrue(Weather.fresh(Weather.report(18.0,0,1,NOW-89*60000L),NOW));
        assertFalse(Weather.fresh(Weather.report(18.0,0,1,NOW-91*60000L),NOW));
        assertFalse(Weather.fresh(Weather.report(18.0,0,1,NOW+6*60000L),NOW));
        assertFalse(Weather.fresh(Weather.report(18.0,0,1,NOW),1000));
        assertFalse(Weather.fresh(null,NOW));
    }
    @Test public void placeRejectsInvalidCoordinatesAndDoesNotShareCityCache() {
        Weather.Place aix = new Weather.Place(true,"Aix-en-Provence",43.5297,5.4474);
        assertTrue(aix.enabled);
        assertFalse(new Weather.Place(true,"Aix",Double.NaN,5).enabled);
        assertFalse(new Weather.Place(true,"Aix",91,5).enabled);
        assertFalse(new Weather.Place(true,"Aix",43,181).enabled);
        assertFalse(new Weather.Place(true,"",43,5).enabled);
        assertFalse(new Weather.Place(true,"Aix\nFake",43,5).enabled);
        assertNotEquals(aix.key(),new Weather.Place(true,"Marseille",43.3,5.4).key());
        assertNotEquals(aix.key(),new Weather.Place(true,"Aix-en-Provence",43.52,5.44).key());
    }
    @Test public void rapidHomeReturnsAndFailuresDoNotFetchRepeatedly() {
        assertEquals(1800000L,Weather.delayUntilFetch(NOW,NOW));
        assertEquals(1200000L,Weather.delayUntilFetch(NOW,NOW+600000L));
        assertEquals(0,Weather.delayUntilFetch(NOW,NOW+1800000L));
        assertEquals(0,Weather.delayUntilFetch(0,NOW));
        assertEquals(0,Weather.delayUntilFetch(NOW,NOW-600000L));
    }
    @Test public void expirationRendersBeforeNextFetch() {
        Weather.Report old = Weather.report(18.0,0,1,NOW-89*60000L);
        assertEquals(60001L, Weather.nextUpdateDelay(old,NOW,1800000L));
        assertEquals(1000L, Weather.nextUpdateDelay(old,NOW,1000L));
        assertEquals(1800000L, Weather.nextUpdateDelay(null,NOW,1800000L));
    }
    @Test public void monotonicClockThrottlesEvenBeforeNetworkTimeIsSet() {
        assertEquals(1799000L, Weather.delayUntilFetch(1000L,2000L));
    }
    @Test public void compactGridReservesSpaceOnlyForEnabledWeather() {
        assertEquals(12, Weather.gridTopPadding(false,false,12,1));
        assertEquals(84, Weather.gridTopPadding(true,false,12,1));
        assertEquals(156, Weather.gridTopPadding(true,false,12,2));
        assertEquals(12, Weather.gridTopPadding(true,true,12,1));
    }
}
