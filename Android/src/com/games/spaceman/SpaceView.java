package com.games.spaceman;

import java.util.Vector;

import tv.ouya.console.api.OuyaController;
import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.badlogic.gdx.math.Vector2;
import com.spacemangames.framework.SpaceGameState;
import com.spacemangames.framework.SpaceUtil;

class SpaceView extends SurfaceView implements SurfaceHolder.Callback, IInputHandler {
    private static final String MTAG                 = "SpaceView";

    /** Used to have an offset while dragging the view around */
    private Vector2             mDragStart;
    private boolean             mDragging            = false;
    private static final int    MIN_MOVE_BEFORE_DRAG = 20;               // pixels
    /** Variables used to implement flinging */
    private static final int    MIN_SPEED_FOR_FLING  = 100;              // 100
                                                                          // pixels/second
    private static final int    MAX_FLING_SPEED      = 2000;
    private static final long   MAX_TIME_FOR_FLING   = 300 * 1000 * 1000; // 300
                                                                          // milliseconds
    private static final int    ACCUMULATE_COUNT     = 3;
    private long                mPreviousTime;
    private Vector<Vector2>     mPreviousLocations;
    private int                 mIndexInVector;
    /** Variables used to implement pinch-zoom */
    private static final int    MIN_MOVE_BEFORE_ZOOM = 20;               // pixels
    private float               mPreviousDist;
    private boolean             mZooming;

    // if this is true all input is ignored
    private boolean             mIgnoreInput         = false;

    public boolean              mIgnoreFocusChange   = false;

    private float               axisX;

    private float               axisY;

    public SpaceView(Context aContext, AttributeSet aAttrs) {
        super(aContext, aAttrs);

        // variable init
        mDragStart = new Vector2();
        mPreviousLocations = new Vector<Vector2>();
        for (int i = 0; i < ACCUMULATE_COUNT; i++) {
            mPreviousLocations.add(new Vector2(0, 0));
        }

        setFocusable(true); // make sure we get key events

        SurfaceHolder lHolder = getHolder();
        lHolder.addCallback(this);
    }

    // Put the game on pause if the window loses focus
    @Override
    public void onWindowFocusChanged(boolean aHasWindowFocus) {
        if (mIgnoreFocusChange)
            return;

        SpaceGameState aState = SpaceGameState.getInstance();
        if (!aHasWindowFocus) {
            aState.setPaused(true);
            aState.mChargingState.reset();
        } else {
            aState.setPaused(false);
        }
    }

    public void ignoreInput(boolean aIgnore) {
        mIgnoreInput = aIgnore;
    }

    @Override
    public boolean onKeyDown(final int keyCode, KeyEvent event) {
        if (ignoreInput()) {
            return false;
        }

        int lState = SpaceGameState.getInstance().getState();

        switch (keyCode) {
        case OuyaController.BUTTON_O:
            if (lState == SpaceGameState.STATE_CHARGING) {
                GameThreadHolder.getThread().requestFireSpaceman();
                return true;
            }
        }

        return false;
    }

    @Override
    // TODO Use OuyController class!
    public boolean onGenericMotionEvent(final MotionEvent event) {
        float x = event.getAxisValue(OuyaController.AXIS_LS_X);
        float y = event.getAxisValue(OuyaController.AXIS_LS_Y);
        if (x * x + y * y < OuyaController.STICK_DEADZONE * OuyaController.STICK_DEADZONE) {
            x = y = 0.0f;
        }
        axisX = x;
        axisY = y;

        return true;
    }

    private boolean handleLeftStick(int lState, float lX, float lY) {
        boolean lResult = false;
        if (lState == SpaceGameState.STATE_NOT_STARTED) {
            lX = SpaceUtil.resolutionScale(lX);
            lY = SpaceUtil.resolutionScale(lY);
            SpaceGameState.getInstance().setState(SpaceGameState.STATE_CHARGING);
            SpaceGameState.getInstance().mChargingState.setChargingStart(0, 0);
            SpaceGameState.getInstance().mChargingState.deltaChargingCurrent(lX, lY);
            lResult = true;
        } else if (lState == SpaceGameState.STATE_CHARGING) {
            lX = SpaceUtil.resolutionScale(lX);
            lY = SpaceUtil.resolutionScale(lY);
            SpaceGameState.getInstance().mChargingState.deltaChargingCurrent(lX, lY);
            lResult = true;
        }
        return lResult;
    }

    /*
     * // Capture touch screen input
     * 
     * @Override public boolean onTouchEvent(MotionEvent aEvent) {
     * 
     * int lAction = aEvent.getAction(); float lX = aEvent.getX(); float lY =
     * aEvent.getY(); // Y is inverted boolean lResult = false; boolean
     * lHitsSpaceMan = lThread.hitsSpaceMan(lX, lY); boolean lHitsArrow =
     * lThread.hitsSpaceManArrow(lX, lY);
     * 
     * // This means we are starting with the charging (if this is on the //
     * location of spaceman) if (lState == SpaceGameState.STATE_NOT_STARTED &&
     * lAction == MotionEvent.ACTION_DOWN && lHitsSpaceMan) { lX =
     * SpaceUtil.resolutionScale(lX); lY = SpaceUtil.resolutionScale(lY);
     * SpaceGameState.getInstance().setState(SpaceGameState.STATE_CHARGING);
     * SpaceGameState.getInstance().mChargingState.setChargingStart(lX, lY);
     * SpaceGameState.getInstance().mChargingState.setChargingCurrent(lX, lY);
     * lResult = true; } else if (lState == SpaceGameState.STATE_CHARGING) { lX
     * = SpaceUtil.resolutionScale(lX); lY = SpaceUtil.resolutionScale(lY); if
     * (lAction == MotionEvent.ACTION_MOVE) {
     * SpaceGameState.getInstance().mChargingState.setChargingCurrent(lX, lY); }
     * else if (lAction == MotionEvent.ACTION_UP) {
     * SpaceGameState.getInstance().mChargingState.setChargingCurrent(lX, lY);
     * lThread.requestFireSpaceman(); } lResult = true; } else if (lState ==
     * SpaceGameState.STATE_NOT_STARTED && lAction == MotionEvent.ACTION_DOWN &&
     * lHitsArrow) { lThread.mViewport.focusViewportOnSpaceman(); } else if
     * (lAction == MotionEvent.ACTION_DOWN && lHitsArrow) { // recenter // on //
     * spaceman lThread.mViewport.resetFocusViewportStatus(true); } else if
     * (lAction == MotionEvent.ACTION_DOWN) { // about to move the // viewport
     * lThread.mViewport.setFlinging(false); mDragStart.set(lX, lY);
     * lThread.mViewport.startViewportDrag(lX, lY); mIndexInVector =
     * ACCUMULATE_COUNT - 1; mPreviousLocations.get(mIndexInVector).set(lX, lY);
     * lThread.mViewport.getFlingSpeed().set(0, 0); lResult = true; } else if
     * (lAction == MotionEvent.ACTION_MOVE) { // moving the viewport if
     * (mDragStart.dst(lX, lY) > MIN_MOVE_BEFORE_DRAG || mDragging) { mDragging
     * = true; lThread.mViewport.dragViewport(lX, lY); long lCurrentTime =
     * System.nanoTime(); float ldT = (float) ((lCurrentTime - mPreviousTime) /
     * 1000000000d); lThread.mViewport.getFlingSpeed().x =
     * (mPreviousLocations.get(mIndexInVector).x - lX) / ldT;
     * lThread.mViewport.getFlingSpeed().y =
     * (mPreviousLocations.get(mIndexInVector).y - lY) / ldT; // shift one left
     * for (int i = 0; i < ACCUMULATE_COUNT - 1; i++)
     * mPreviousLocations.get(i).set(mPreviousLocations.get(i + 1));
     * mPreviousLocations.get(mIndexInVector).set(lX, lY); mPreviousTime =
     * lCurrentTime; mIndexInVector--; if (mIndexInVector < 0) mIndexInVector =
     * 0; } lResult = true; } else if (lAction == MotionEvent.ACTION_UP) { //
     * done moving the // viewport mDragging = false;
     * lThread.mViewport.stopViewportDrag(); long lCurrentTime =
     * System.nanoTime(); float lLen = lThread.mViewport.getFlingSpeed().len();
     * if (lLen > MIN_SPEED_FOR_FLING && lCurrentTime - mPreviousTime <
     * MAX_TIME_FOR_FLING) { if (lLen > MAX_FLING_SPEED) {
     * lThread.mViewport.getFlingSpeed().x =
     * (lThread.mViewport.getFlingSpeed().x / lLen) * MAX_FLING_SPEED;
     * lThread.mViewport.getFlingSpeed().y =
     * (lThread.mViewport.getFlingSpeed().y / lLen) * MAX_FLING_SPEED; }
     * lThread.mViewport.setFlinging(true); } lResult = true; }
     * 
     * return lResult; }
     * 
     * private void handleMultitouchEvent(MotionEvent aEvent) { int pointerCount
     * = aEvent.getPointerCount(); PALManager.getLog().i(MTAG,
     * "Multitouch event. Pointers: " + pointerCount);
     * 
     * if (pointerCount != 2) return;
     * 
     * float dx = Math.abs(aEvent.getX(0) - aEvent.getX(1)); float dy =
     * Math.abs(aEvent.getY(0) - aEvent.getY(1)); float lDist =
     * FloatMath.sqrt(dx * dx + dy * dy);
     * 
     * switch (aEvent.getAction()) { case MotionEvent.ACTION_POINTER_1_DOWN: //
     * either the first case MotionEvent.ACTION_POINTER_2_DOWN: // or second
     * pointer has gone // down PALManager.getLog().i(MTAG, "Action down");
     * mZooming = false; mPreviousDist = lDist; break; case
     * MotionEvent.ACTION_MOVE: PALManager.getLog().i(MTAG, "Action move");
     * float lZoom = mPreviousDist - lDist; if (Math.abs(lZoom) >
     * MIN_MOVE_BEFORE_ZOOM || mZooming) { lZoom = (lZoom /
     * GameThreadHolder.getThread().canvasDiagonal());
     * GameThreadHolder.getThread().mViewport.zoomViewport(lZoom); mPreviousDist
     * = lDist; mZooming = true; } default: PALManager.getLog().i(MTAG,
     * "Action: " + aEvent.getAction()); } }
     */

    private boolean ignoreInput() {
        return mIgnoreInput || GameThreadHolder.getThread() == null;
    }

    /* Callback invoked when the surface dimensions change. */
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        GameThreadHolder.getThread().setSurfaceSize(width, height);
    }

    // Called when the surface has been destroyed
    public void surfaceDestroyed(SurfaceHolder holder) {
        GameThreadHolder.getThread().removeInputHandler(this);
        // TODO need to do something here?
    }

    public void surfaceCreated(SurfaceHolder holder) {
        GameThreadHolder.getThread().setInputHandler(this);
        // TODO need to do something here?
    }

    public void tick() {
        if (ignoreInput()) {
            return;
        }

        int lState = SpaceGameState.getInstance().getState();

        if (lState == SpaceGameState.STATE_LOADED) {
            SpaceGameState.getInstance().setState(SpaceGameState.STATE_NOT_STARTED);
            return;
        }

        // only process input if we're in the right state
        if (lState != SpaceGameState.STATE_CHARGING && lState != SpaceGameState.STATE_FLYING && lState != SpaceGameState.STATE_LOADED
                && lState != SpaceGameState.STATE_NOT_STARTED) {
            return;
        }

        handleLeftStick(SpaceGameState.getInstance().getState(), axisX, axisY);
    }
}
