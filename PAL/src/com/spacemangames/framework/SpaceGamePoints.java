package com.spacemangames.framework;

public class SpaceGamePoints {
    @SuppressWarnings("unused")
    private static final String TAG               = "SpaceGamePoints";

    public static final int     POINTS_STARTING   = 10000;
    public static final int     POINTS_PER_SECOND = 500;

    private int                 mPoints;
    private int                 mBonusPoints;
    private long                mTotalElapsed;

    public SpaceGamePoints() {
        reset();
    }

    public synchronized void reset() {
        mPoints = POINTS_STARTING;
        mBonusPoints = 0;
        mTotalElapsed = 0;
    }

    public int getCurrentPoints() {
        return mPoints;
    }

    public synchronized void elapse(int aElapsed /* milliseconds */) {
        mTotalElapsed += aElapsed;

        int mPointsDiff = (int) ((mTotalElapsed / 1000f) * POINTS_PER_SECOND);

        mPoints = (POINTS_STARTING + mBonusPoints) - mPointsDiff;

        if (mPoints < 0)
            mPoints = 0;
    }

    public synchronized void bonus(int aPoints) {
        mBonusPoints += aPoints;
    }
}
