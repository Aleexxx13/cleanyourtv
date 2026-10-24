package com.gathin.tvhome;

/** TCL opt-in: the firmware supplies the first step before accessibility; only repeat a held key here. */
final class VolumeRepeat {
    boolean active;
    int delay;
    private int key;
    private long downTime, next;

    boolean press(int key, long downTime) {
        if (this.key == key && this.downTime == downTime) return false;
        this.key = key;
        this.downTime = downTime;
        delay = 500;
        next = downTime + delay;
        active = true;
        return true;
    }

    int step(long now) {
        if (!active || now < next) return 0;
        long held = now - downTime;
        delay = (int) Math.max(50, 150 - held / 40);
        next = now + delay; // A busy main thread must never catch up with a burst of volume changes.
        return key == 24 ? 1 : -1;
    }

    void release(int key) { if (this.key == key) stop(); }
    void stop() { active = false; }
}
