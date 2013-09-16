package com.spacemangames.framework;

import com.badlogic.gdx.math.Vector2;
import com.spacemangames.pal.PALManager;

/** Singleton class describing the game state */
public class SpaceGameState {
    public class ChargingState {
        public static final int   MAX_CHARGING_POWER  = 150;
        public static final float CHARGING_MULTIPLIER = 5f;

        /** Start of charging gesture */
        private float             mChargingStartX;
        private float             mChargingStartY;

        /** Current pos of charging gesture */
        private float             mChargingPower      = 0;
        private float             mChargingAngle      = 0;

        private Vector2           mSpaceManSpeed;
        private float             mChargingCurrentX   = 0;
        private float             mChargingCurrentY   = 0;

        public ChargingState() {
            mSpaceManSpeed = new Vector2(0, 0);
        }

        public float chargingPower() {
            return mChargingPower;
        }

        public void setChargingStart(float aX, float aY) {
            mChargingStartX = aX;
            mChargingStartY = aY;
            mChargingCurrentX = mChargingCurrentY = 0;
        }

        public boolean setChargingCurrent(float aX, float aY) {
            boolean updated = true;
            float lX = aX - mChargingStartX;
            float lY = aY - mChargingStartY;
            mChargingPower = calculateChargingPower(lX, lY);
            mChargingAngle = (float) Math.atan2(lX, lY);
            // if length is longer than this we have to recalculate x,y
            // coordinates because we're overcharging
            if (chargingPowerOverMax(mChargingPower)) {
                updated = false;
                mChargingPower = MAX_CHARGING_POWER;
            }

            // we fire in the opposite direction :)
            lY = -1.0f * (float) Math.cos(mChargingAngle) * mChargingPower;
            lX = -1.0f * (float) Math.sin(mChargingAngle) * mChargingPower;
            mSpaceManSpeed.set(lX, lY);
            // PALManager.getLog().i (TAG, "Speed: " + lX + " " + lY);
            return updated;
        }

        private boolean chargingPowerOverMax(float power) {
            return power > MAX_CHARGING_POWER;
        }

        private float calculateChargingPower(float lX, float lY) {
            return (float) (Math.sqrt(lX * lX + lY * lY) * CHARGING_MULTIPLIER);
        }

        public void deltaChargingCurrent(float x, float y) {
            if (setChargingCurrent(mChargingCurrentX + x, mChargingCurrentY + y)) {
                mChargingCurrentX += x;
                mChargingCurrentY += y;
            }
        }

        public Vector2 getSpaceManSpeed() {
            return mSpaceManSpeed;
        }

        public float getAngle() {
            return mChargingAngle;
        }

        public void reset() {
            mChargingPower = 0;
            mChargingAngle = 0;
            mSpaceManSpeed.set(0, 0);
        }
    }

    private static final String TAG = "SpaceGameState";

    // private constructor
    private SpaceGameState() {
        mState = STATE_INVALID;
    }

    // Singleton holder
    private static class SingletonHolder {
        public static final SpaceGameState INSTANCE = new SpaceGameState();
    }

    // Singleton access
    public static SpaceGameState getInstance() {
        return SingletonHolder.INSTANCE;
    }

    // Interesting stuff follows
    // state for charging stuff
    public ChargingState    mChargingState    = new ChargingState();

    /** Used to figure out elapsed time between frames */
    private long            mLastTime;

    // Possible states
    public static final int STATE_INVALID     = -1;
    public static final int STATE_LOADING     = 0;
    public static final int STATE_LOADED      = 1;
    public static final int STATE_NOT_STARTED = 2;
    public static final int STATE_CHARGING    = 3;
    public static final int STATE_FLYING      = 4;
    public static final int STATE_PAUSED      = 5;

    public static final int NOT_YET_ENDED     = -1;
    public static final int WON_BRONZE        = 0;
    public static final int WON_SILVER        = 1;
    public static final int WON_GOLD          = 2;
    public static final int LOST_DIE          = 3;
    public static final int LOST_LOST         = 4;

    private int             mEndState         = NOT_YET_ENDED;

    private int             mState;
    private int             mLastState;

    private boolean         mPredicting       = false;

    public void setPredicting(boolean aPredicting) {
        mPredicting = aPredicting;
    }

    public boolean isPredicting() {
        return mPredicting;
    }

    public synchronized int getState() {
        return mState;
    }

    public synchronized int getLastState() {
        return mLastState;
    }

    public synchronized void setState(int aState) {
        if (aState == STATE_PAUSED) { // use setPaused (true) for this
            PALManager.getLog().e(TAG, "Trying to setState(STATE_PAUSED). Use setPaused(true) instead");
        }

        PALManager.getLog().i(TAG, "Changing state from " + getStateString(mState) + " to " + getStateString(aState));

        updateTimeTick();

        if (mState == STATE_PAUSED) {
            mLastState = aState;
        } else {
            mState = aState;
        }
    }

    public synchronized void setPaused(boolean aPause) {
        assert mState >= STATE_LOADED;

        if (aPause) {
            PALManager.getLog().i(TAG, "Pausing. Current state: " + getStateString(mState));
            if (mState != STATE_PAUSED)
                mLastState = mState;
            mState = STATE_PAUSED;
        } else {
            updateTimeTick();
            if (mState != STATE_PAUSED) {
                PALManager.getLog().i(TAG, "Resuming while not paused, ignoring.");
            } else {
                PALManager.getLog().i(TAG, "Resuming. Setting state to: " + getStateString(mLastState));
                mState = mLastState;
            }
        }
    }

    public synchronized boolean paused() {
        return (mState == STATE_PAUSED);
    }

    public void togglePause() {
        if (mState == STATE_PAUSED)
            setPaused(false);
        else
            setPaused(true);
    }

    public synchronized String getStateString(int aState) {
        String lRes = "";

        switch (aState) {
        case STATE_LOADING:
            lRes = "STATE_LOADING";
            break;
        case STATE_LOADED:
            lRes = "STATE_LOADED";
            break;
        case STATE_NOT_STARTED:
            lRes = "STATE_NOT_STARTED";
            break;
        case STATE_CHARGING:
            lRes = "STATE_CHARGING";
            break;
        case STATE_FLYING:
            lRes = "STATE_FLYING";
            break;
        case STATE_PAUSED:
            lRes = "STATE_PAUSED";
            break;
        default:
            lRes = "Unknown state!";
            break;
        }
        return lRes;
    }

    public synchronized float getElapsedTime() {
        long lNow = System.nanoTime();
        return (lNow - mLastTime) / 1000000000f;
    }

    public synchronized void updateTimeTick() {
        mLastTime = System.nanoTime();
    }

    public void setEndState(int aState) {
        mEndState = aState;
    }

    public int endState() {
        return mEndState;
    }
}
