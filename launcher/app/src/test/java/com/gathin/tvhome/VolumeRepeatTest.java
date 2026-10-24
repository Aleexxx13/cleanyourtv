package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class VolumeRepeatTest {
    @Test public void shortTapNeverAddsAnotherStep() {
        VolumeRepeat volume = new VolumeRepeat();
        volume.press(24, 1000);
        assertEquals(0, volume.step(1499));
        volume.release(24);
        assertEquals(0, volume.step(1600));
        assertFalse(volume.active);
    }

    @Test public void holdAcceleratesWithoutCatchingUpAfterAStall() {
        VolumeRepeat volume = new VolumeRepeat();
        volume.press(24, 1000);
        assertEquals(1, volume.step(1500));
        assertEquals(0, volume.step(1500));
        assertEquals(0, volume.step(1637));
        assertEquals(1, volume.step(1638));
        assertEquals(1, volume.step(2500)); // 1.5 seconds held: faster cadence.
        assertEquals(0, volume.step(2612));
        assertEquals(1, volume.step(2613));
        assertEquals(1, volume.step(4000)); // Three seconds: faster again.
        assertEquals(0, volume.step(4074));
        assertEquals(1, volume.step(4075));
        assertEquals(1, volume.step(6000)); // Five seconds: fastest cadence, still only one step.
        assertEquals(0, volume.step(6000));
        assertEquals(0, volume.step(6049));
        assertEquals(1, volume.step(6050));
    }

    @Test public void releaseStopsTheHeldDirectionImmediately() {
        VolumeRepeat volume = new VolumeRepeat();
        volume.press(25, 1000);
        assertEquals(-1, volume.step(1500));
        volume.release(25);
        assertEquals(0, volume.step(1700));
    }

    @Test public void longHoldHasNoArtificialStepLimitButReleaseStillStopsIt() {
        VolumeRepeat volume = new VolumeRepeat();
        volume.press(24, 1000);
        assertEquals(1, volume.step(1500));
        assertEquals(1, volume.step(3000));
        assertTrue(volume.active);
        assertFalse(volume.press(24, 1000)); // Duplicate DOWN must not reset acceleration.
        assertEquals(1, volume.step(10000));
        volume.release(24);
        assertEquals(0, volume.step(10000));
        assertTrue(volume.press(24, 10001));
        assertEquals(1, volume.step(10501));
    }

    @Test public void changingDirectionReplacesThePendingHold() {
        VolumeRepeat volume = new VolumeRepeat();
        volume.press(24, 1000);
        volume.press(25, 1300);
        volume.release(24);
        assertEquals(0, volume.step(1500));
        assertEquals(-1, volume.step(1800));
    }

    @Test public void stoppingTheServiceCancelsPendingSteps() {
        VolumeRepeat volume = new VolumeRepeat();
        volume.press(25, 1000);
        volume.stop();
        assertEquals(0, volume.step(1500));
        assertFalse(volume.active);
    }
}
