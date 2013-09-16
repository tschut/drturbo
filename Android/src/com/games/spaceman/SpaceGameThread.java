package com.games.spaceman;

import java.util.Queue;

import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.FloatMath;
import android.view.SurfaceHolder;

import com.badlogic.gdx.math.Vector2;
import com.games.spaceman.pal.AndroidRenderer;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.spacemangames.framework.GameThread;
import com.spacemangames.framework.Rect;
import com.spacemangames.framework.SpaceGameState;
import com.spacemangames.framework.Viewport;
import com.spacemangames.library.SpaceData;
import com.spacemangames.library.SpaceWorldEventBuffer;
import com.spacemangames.pal.PALManager;

public class SpaceGameThread extends GameThread {
    public static final String           TAG                 = "SpaceGameThread";
    // maximum frame rate
    public static final float            MIN_FRAME_TIME      = 0.033f;           // in
                                                                                  // seconds
                                                                                  // (0.033
                                                                                  // =
                                                                                  // 30
                                                                                  // fps)
    public static final float            MAX_FRAME_TIME      = 0.100f;

    private final Rect                   mViewportScratch;

    /** Handle to the surface manager object we interact with */
    private SurfaceHolder                mSurfaceHolder;
    private final Object                 mDummySurfaceHolder = new Object();

    // used to message the ui thread
    private Handler                      mMsgHandler;

    // The rendering engine
    private final AndroidRenderer        mRenderer;

    private boolean                      mFrozen             = false;

    private final GoogleAnalyticsTracker tracker;
    private IInputHandler                inputHandler;

    public SpaceGameThread() {

        super();
        // Start in STATE_LOADING
        SpaceGameState.getInstance().setState(SpaceGameState.STATE_LOADING);

        // speed for flinging the screen
        mViewport.setFlingSpeed(new Vector2(0, 0));

        // rects
        mViewportScratch = new Rect();

        // rendering engine
        mRenderer = new AndroidRenderer();

        tracker = GoogleAnalyticsTracker.getInstance();
    }

    public void setSurfaceHolder(SurfaceHolder aSurfaceHolder) {
        if (mSurfaceHolder != null) {
            synchronized (mSurfaceHolder) {
                mSurfaceHolder = aSurfaceHolder;
            }
        } else {
            mSurfaceHolder = aSurfaceHolder;
        }
    }

    @Override
    public Object getSurfaceLocker() {
        if (mSurfaceHolder != null) {
            return (Object) mSurfaceHolder;
        } else {
            return mDummySurfaceHolder;
        }
    }

    public void freeze() {
        PALManager.getLog().v(TAG, "Freezing thread");
        mFrozen = true;
    }

    public void unfreeze() {
        PALManager.getLog().v(TAG, "Unfreezing thread");
        mFrozen = false;
    }

    @Override
    public void run() {
        SpaceGameState lGameState = SpaceGameState.getInstance();
        long lFpsHelper = 0;
        while (mRun) {
            // handle events that need to run on this thread
            runQueue();
            if (inputHandler != null) {
                inputHandler.tick();
            }

            Canvas c = null;

            boolean viewportValid = false;
            synchronized (mViewport.getViewport()) {
                if (mViewport.isValid()) {
                    viewportValid = true;
                    mViewportScratch.set(mViewport.getViewport());
                }
            }

            if (!lGameState.paused() && viewportValid && !mFrozen) {
                long lLoopStart = System.nanoTime();
                float lElapsed = lGameState.getElapsedTime();
                lGameState.updateTimeTick();

                // Are we flinging the canvas?
                if (mViewport.isFlinging()) {
                    mViewport.moveViewport(mViewport.getFlingSpeed().x * lElapsed, mViewport.getFlingSpeed().y * lElapsed);
                    mViewport.getFlingSpeed().mul(Viewport.FLING_DAMPING_FACTOR);
                    if (mViewport.getFlingSpeed().len() < Viewport.FLING_STOP_THRESHOLD)
                        mViewport.setFlinging(false);
                }

                if (mRequestFireSpaceman) {
                    fireSpaceMan();
                    mRequestFireSpaceman = false;
                }

                parseGameEvents();
                updatePhysics(lElapsed);
                if (mViewport.isFocusOnSpaceman())
                    mViewport.viewportFollowSpaceman();
                if (lGameState.mChargingState.chargingPower() > DRAW_PREDICTION_THRESHOLD
                        && lGameState.getState() == SpaceGameState.STATE_CHARGING) {
                    lGameState.setPredicting(true);
                    mSpaceData.calculatePredictionData(SpaceGameState.getInstance().mChargingState.getSpaceManSpeed());
                    lGameState.setPredicting(false);
                }
                c = mSurfaceHolder.lockCanvas(null);
                if (c != null) {
                    synchronized (mSurfaceHolder) {
                        try {
                            doDraw(c);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                // if we have any time to spare in this frame we can sleep now
                double upToHere = (System.nanoTime() - lLoopStart) / 1000000000d;
                if (upToHere < MIN_FRAME_TIME)
                    SystemClock.sleep((long) ((MIN_FRAME_TIME - upToHere) * 1000));

                // PALManager.getLog().v (TAG, "FPS: " + 1f /
                // ((System.nanoTime() - lFpsHelper) / 1000000000d));
                lFpsHelper = System.nanoTime();
                // now blit to the screen
                if (c != null) {
                    mSurfaceHolder.unlockCanvasAndPost(c);
                }
            } else if (mRedrawOnce && mSurfaceHolder != null && !mFrozen) {
                c = mSurfaceHolder.lockCanvas(null);
                if (c != null) {
                    mRedrawOnce = false;
                    synchronized (mSurfaceHolder) {
                        doDraw(c);
                        mSurfaceHolder.unlockCanvasAndPost(c);
                    }
                } else {
                    try {
                        sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                try {
                    sleep(100); // sleep 50 ms to save battery
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        PALManager.getLog().i(TAG, "Thread ended...");
    }

    // Save game state
    public Bundle saveState(Bundle map) {
        if (map != null) {
            // TODO Implement this ???
        }
        return map;
    }

    /* Callback invoked when the surface dimensions change. */
    public void setSurfaceSize(int width, int height) {
        PALManager.getLog().i(TAG, "surface size " + width + "x" + height);
        // synchronized to make sure these all change atomically
        synchronized (mSurfaceHolder) {
            mCanvasWidth = width;
            mCanvasHeight = height;
            if (mSpaceData.mCurrentLevel != null) {
                mViewport.reset(mSpaceData.mCurrentLevel.startCenterX(), mSpaceData.mCurrentLevel.startCenterY(), width, height);
            } else {
                mViewport.reset(mViewport.getViewport().centerX(), mViewport.getViewport().centerY(), width, height);
            }
            redrawOnce();
        }
    }

    // The actual drawing happens here :)
    private void doDraw(Canvas aCanvas) {
        if (SpaceGameState.getInstance().getState() >= SpaceGameState.STATE_LOADED) {
            mRenderer.initialize(aCanvas, mViewportScratch, mViewport.mScreenRect);
            mSpaceData.mCurrentLevel.draw(mRenderer);
        }
    }

    private void parseGameEvents() {
        // check if points have reached zero
        if (SpaceData.getInstance().mPoints.getCurrentPoints() == 0) {
            tracker.trackEvent("out-of-time", String.valueOf(SpaceData.getInstance().getCurrentLevelId()), "", 0);
            SpaceGameState.getInstance().setPaused(true);
            SpaceGameState.getInstance().setEndState(SpaceGameState.LOST_LOST);
            mMsgHandler.sendEmptyMessage(0);
        }

        // check world events
        Queue<Integer> lQueue = SpaceWorldEventBuffer.getInstance().mEvents;
        while (lQueue.size() > 0) {
            Integer lEvent = lQueue.remove();
            switch (lEvent) {
            case (SpaceWorldEventBuffer.EVENT_HIT_ROCKET):
                SpaceGameState.getInstance().setPaused(true);
                int lCurrentLevelID = SpaceData.getInstance().getCurrentLevelId();
                int lHighScore = LevelDbAdapter.getInstance().highScore(lCurrentLevelID);
                int lCurScore = SpaceData.getInstance().mPoints.getCurrentPoints();
                SpaceGameState.getInstance().setEndState(SpaceData.getInstance().currentLevelWinState(lCurScore));
                tracker.trackEvent("win", String.valueOf(lCurrentLevelID), String.valueOf(lCurScore), 0);

                if (lCurScore > lHighScore)
                    LevelDbAdapter.getInstance().updateHighScore(lCurrentLevelID, lCurScore);
                mMsgHandler.sendEmptyMessage(0);
                break;
            case (SpaceWorldEventBuffer.EVENT_HIT_DOI_OBJECT):
                tracker.trackEvent("die", String.valueOf(SpaceData.getInstance().getCurrentLevelId()), "", 0);
                SpaceGameState.getInstance().setPaused(true);
                SpaceGameState.getInstance().setEndState(SpaceGameState.LOST_DIE);
                mMsgHandler.sendEmptyMessage(0);
                break;
            case (SpaceWorldEventBuffer.EVENT_SCORE_BONUS):
                SpaceData.getInstance().mPoints.bonus(BONUS_POINTS);
                break;
            default:
                PALManager.getLog().e(TAG, "Unexpected game event");
            }
        }
    }

    public float canvasDiagonal() {
        int lW = mCanvasWidth;
        int lH = mCanvasHeight;
        return FloatMath.sqrt(lW * lW + lH * lH);
    }

    public void setMsgHandler(Handler aMsgHandler) {
        mMsgHandler = aMsgHandler;
    }

    public void setInputHandler(IInputHandler inputHandler) {
        this.inputHandler = inputHandler;
    }

    public void removeInputHandler(IInputHandler inputHandler) {
        if (this.inputHandler == inputHandler) {
            this.inputHandler = null;
        }
    }
}