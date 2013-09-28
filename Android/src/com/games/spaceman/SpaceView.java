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
    private static final String MTAG               = "SpaceView";

    private static final int    ACCUMULATE_COUNT   = 3;
    private Vector<Vector2>     mPreviousLocations;

    // if this is true all input is ignored
    private boolean             mIgnoreInput       = false;

    public boolean              mIgnoreFocusChange = false;

    public SpaceView(Context aContext, AttributeSet aAttrs) {
        super(aContext, aAttrs);

        // variable init
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
    public boolean onGenericMotionEvent(final MotionEvent event) {
        boolean handled = OuyaController.onGenericMotionEvent(event);
        return handled || super.onGenericMotionEvent(event);
    }

    private boolean handleLeftStick(int lState) {
        OuyaController c = OuyaController.getControllerByPlayer(0);

        float x = c.getAxisValue(OuyaController.AXIS_LS_X);
        float y = c.getAxisValue(OuyaController.AXIS_LS_Y);
        if (x * x + y * y < OuyaController.STICK_DEADZONE * OuyaController.STICK_DEADZONE) {
            x = y = 0.0f;
        }

        boolean lResult = false;
        if (lState == SpaceGameState.STATE_NOT_STARTED) {
            x = SpaceUtil.resolutionScale(x);
            y = SpaceUtil.resolutionScale(y);
            SpaceGameState.getInstance().setState(SpaceGameState.STATE_CHARGING);
            SpaceGameState.getInstance().mChargingState.setChargingStart(0, 0);
            SpaceGameState.getInstance().mChargingState.deltaChargingCurrent(x, y);
            lResult = true;
        } else if (lState == SpaceGameState.STATE_CHARGING) {
            x = SpaceUtil.resolutionScale(x);
            y = SpaceUtil.resolutionScale(y);
            SpaceGameState.getInstance().mChargingState.deltaChargingCurrent(x, y);
            lResult = true;
        }
        return lResult;
    }

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

        handleLeftStick(SpaceGameState.getInstance().getState());
    }
}
