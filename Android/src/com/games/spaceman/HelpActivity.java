package com.games.spaceman;

import tv.ouya.console.api.OuyaController;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.spacemangames.library.SpaceData;
import com.spacemangames.library.SpaceLevel;
import com.spacemangames.pal.PALManager;

public class HelpActivity extends Activity {
    private final class PreviousClickedListener implements OnClickListener {
        public void onClick(View v) {
            PALManager.getLog().v(TAG, "OnClick PrevButton");
            if (SpaceData.getInstance().mCurrentLevel.mId == SpaceLevel.ID_HELP1) {
                SpaceView spaceview = (SpaceView) findViewById(R.id.space);
                spaceview.mIgnoreFocusChange = true;
                finish();
            } else {
                GameThreadHolder.getThread().loadPrevLevel(true);
            }
        }
    }

    private final class NextClickedListener implements OnClickListener {
        public void onClick(View v) {
            PALManager.getLog().v(TAG, "OnClick NextButton");
            if (SpaceData.getInstance().mCurrentLevel.mId == SpaceLevel.ID_HELP4) {
                GameThreadHolder.getThread().postSyncRunnable(new Runnable() {
                    public void run() {
                        GameThreadHolder.getThread().freeze();
                    }
                });
                Intent i = new Intent();
                i.putExtra("action", HELP_ACTION_START_GAME);
                setResult(Activity.RESULT_OK, i);
                finish();
            } else {
                GameThreadHolder.getThread().loadNextLevel(true);
            }
        }
    }

    private static final String TAG                           = "HelpActivity";

    public static final int     HELP_ACTION_START_GAME        = 0;

    private Button              mNextButton;
    private Button              mPrevButton;

    public static final String  HAS_SEEN_HELP_SHARED_PREF_KEY = "hasSeenHelp";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            setContentView(R.layout.help_layout);

            // add button handlers
            mNextButton = (Button) findViewById(R.id.button_next);
            mNextButton.setOnClickListener(new NextClickedListener());

            mPrevButton = (Button) findViewById(R.id.button_prev);
            mPrevButton.setOnClickListener(new PreviousClickedListener());

            // set the shared pref that the help has been shown
            SharedPreferences.Editor sp = getSharedPreferences(getPackageName(), MODE_PRIVATE).edit();
            sp.putBoolean(HAS_SEEN_HELP_SHARED_PREF_KEY, true);
            sp.commit();
        } else {
            // we are being restored: restart the app
            Intent i = new Intent(this, com.games.spaceman.LoadingActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(i);
            finish();
        }
    }

    @Override
    protected void onResume() {
        GoogleAnalyticsTracker.getInstance().trackPageView("/help");

        PALManager.getLog().v(TAG, "onResume");
        super.onResume();

        SpaceView spaceView = (SpaceView) findViewById(R.id.space);
        spaceView.ignoreInput(true);

        GameThreadHolder.getThread().setSurfaceHolder(spaceView.getHolder());
        GameThreadHolder.getThread().changeLevel(SpaceLevel.ID_HELP1, true);
        GameThreadHolder.getThread().postSyncRunnable(new Runnable() {
            public void run() {
                GameThreadHolder.getThread().unfreeze();
            }
        });
    }

    @Override
    protected void onPause() {
        PALManager.getLog().v(TAG, "onPause");
        super.onPause();
        GameThreadHolder.getThread().postSyncRunnable(new Runnable() {
            public void run() {
                GameThreadHolder.getThread().freeze();
            }
        });
    }

    @Override
    public boolean onKeyDown(final int keyCode, KeyEvent event) {
        boolean handled = false;

        switch (keyCode) {
        case OuyaController.BUTTON_O:
        case OuyaController.BUTTON_DPAD_RIGHT:
        case OuyaController.BUTTON_R1:
            new NextClickedListener().onClick(null);
            handled = true;
            break;
        case OuyaController.BUTTON_A:
        case OuyaController.BUTTON_DPAD_LEFT:
        case OuyaController.BUTTON_L1:
            new PreviousClickedListener().onClick(null);
            handled = true;
            break;
        }

        return handled || super.onKeyDown(keyCode, event);
    }
}
