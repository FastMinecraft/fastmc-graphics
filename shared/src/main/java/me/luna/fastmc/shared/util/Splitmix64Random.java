package dev.fastmc.common;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class Splitmix64Random extends Random {
    private static final long STATE_DELTA = 0x9E3779B97F4A7C15L;
    private static final long MULTIPLIER_1 = 0xBF58476D1CE4E5B9L;
    private static final long MULTIPLIER_2 = 0x94D049BB133111EBL;
    private static final AtomicLong SEED_STATE = new AtomicLong(System.nanoTime());
    private long state;

    public Splitmix64Random(long seed) {
        state = seed;
    }

    public Splitmix64Random() {
        state = nextSeed();
    }

    private static long nextSeed() {
        return next(SEED_STATE.addAndGet(STATE_DELTA));
    }

    private static long next(long z) {
        z = (z ^ (z >>> 30)) * MULTIPLIER_1;
        z = (z ^ (z >>> 27)) * MULTIPLIER_2;
        return z ^ (z >>> 31);
    }

    @Override
    public void setSeed(long seed) {
        state = seed;
    }

    @Override
    public long nextLong() {
        state += STATE_DELTA;
        return next(state);
    }

    @Override
    protected int next(int bits) {
        return (int) ((nextLong()) >> (64 - bits));
    }
}
