package com.games.spaceman;

import tv.ouya.console.api.OuyaController;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.spacemangames.framework.SpaceGameState;
import com.spacemangames.library.SpaceLevel;
import com.spacemangames.pal.PALManager;

public class MainMenu extends Activity {
    private final class HelpButtonClickListener implements OnClickListener {
        public void onClick(View v) {
            PALManager.getLog().v(TAG, "OnClick helpButton");
            GameThreadHolder.getThread().postSyncRunnable(new Runnable() {
                public void run() {
                    GameThreadHolder.getThread().freeze();
                }
            });
            Intent intent = new Intent(SpaceApp.mAppContext, HelpActivity.class);
            startActivityForResult(intent, SpaceApp.ACTIVITY_HELP);
        }
    }

    private final class LevelsButtonClickListener implements OnClickListener {
        public void onClick(View v) {
            PALManager.getLog().v(TAG, "OnClick listButton");
            GameThreadHolder.getThread().postSyncRunnable(new Runnable() {
                public void run() {
                    GameThreadHolder.getThread().freeze();
                }
            });
            Intent intent = new Intent(SpaceApp.mAppContext, LevelSelect.class);
            startActivityForResult(intent, SpaceApp.ACTIVITY_LEVELSELECT);
        }
    }

    private final class PlayButtonClickListener implements OnClickListener {
        public void onClick(View v) {
            PALManager.getLog().v(TAG, "OnClick playButton");
            GameThreadHolder.getThread().postSyncRunnable(new Runnable() {
                public void run() {
                    GameThreadHolder.getThread().freeze();
                }
            });
            Intent i = new Intent(SpaceApp.mAppContext, SpaceApp.class);
            i.putExtra("level", SpaceApp.LAST_UNLOCKED_LEVEL);
            startActivity(i);
        }
    }

    private static final String TAG = "MainMenu";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            setContentView(R.layout.mainmenu_layout);

            // add button handlers
            ImageButton playButton = (ImageButton) findViewById(R.id.button_play);
            playButton.setOnClickListener(new PlayButtonClickListener());

            ImageButton listButton = (ImageButton) findViewById(R.id.button_list);
            listButton.setOnClickListener(new LevelsButtonClickListener());

            ImageButton helpButton = (ImageButton) findViewById(R.id.button_help);
            helpButton.setOnClickListener(new HelpButtonClickListener());
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
        PALManager.getLog().v(TAG, "onResume");

        GoogleAnalyticsTracker.getInstance().trackPageView("/mainmenu");

        super.onResume();

        if (goToHelpImmediately()) {
            GameThreadHolder.getThread().postSyncRunnable(new Runnable() {
                public void run() {
                    GameThreadHolder.getThread().freeze();
                }
            });
            Intent intent = new Intent(SpaceApp.mAppContext, HelpActivity.class);
            startActivityForResult(intent, SpaceApp.ACTIVITY_HELP);
        } else {
            SpaceView spaceView = (SpaceView) findViewById(R.id.space);
            spaceView.ignoreInput(true);

            GameThreadHolder.getThread().setSurfaceHolder(spaceView.getHolder());
            GameThreadHolder.getThread().changeLevel(SpaceLevel.ID_LOADING_SCREEN, true);
            GameThreadHolder.getThread().postSyncRunnable(new Runnable() {
                public void run() {
                    GameThreadHolder.getThread().unfreeze();
                }
            });
            SpaceGameState.getInstance().setPaused(false);
        }
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
    public void onBackPressed() {
        PALManager.getLog().d(TAG, "onBackPressed Called");
        Intent setIntent = new Intent(Intent.ACTION_MAIN);
        setIntent.addCategory(Intent.CATEGORY_HOME);
        setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(setIntent);
    }

    @Override
    public void onActivityResult(int aRequestCode, int aResultCode, Intent aData) {
        PALManager.getLog().v(TAG, "onActivityResult");
        switch (aRequestCode) {
        case SpaceApp.ACTIVITY_LEVELSELECT:
            if (aResultCode == Activity.RESULT_OK) {
                GameThreadHolder.getThread().postSyncRunnable(new Runnable() {
                    public void run() {
                        GameThreadHolder.getThread().freeze();
                    }
                });

                int level = aData.getIntExtra(SpaceApp.LEVEL_ID_STRING, 0);
                Intent i = new Intent(SpaceApp.mAppContext, SpaceApp.class);
                i.putExtra("level", level);
                startActivity(i);
            }
            break;
        case SpaceApp.ACTIVITY_HELP:
            if (aResultCode == Activity.RESULT_OK) {
                int action = aData.getIntExtra("action", HelpActivity.HELP_ACTION_START_GAME);
                if (action == HelpActivity.HELP_ACTION_START_GAME) {
                    GameThreadHolder.getThread().postSyncRunnable(new Runnable() {
                        public void run() {
                            GameThreadHolder.getThread().freeze();
                        }
                    });
                    startActivity(new Intent(SpaceApp.mAppContext, SpaceApp.class));
                }
            }
            break;
        }
    }

    @Override
    public boolean onKeyDown(final int keyCode, KeyEvent event) {
        boolean handled = false;

        switch (keyCode) {
        case OuyaController.BUTTON_O:
            new PlayButtonClickListener().onClick(null);
            handled = true;
            break;
        case OuyaController.BUTTON_MENU:
            new LevelsButtonClickListener().onClick(null);
            handled = true;
            break;
        case OuyaController.BUTTON_U:
            new HelpButtonClickListener().onClick(null);
            handled = true;
            break;
        }
        return handled || super.onKeyDown(keyCode, event);
    }

    // if this returns true we should skip the menu and go to the help
    // immediately
    private boolean goToHelpImmediately() {
        // return true if the first level is never completed...
        if (LevelDbAdapter.getInstance().highScore(0) > 0)
            return false;

        // ... and the shared preferences indicate we've never showed the help
        // before
        SharedPreferences sp = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        boolean hasSeenHelp = sp.getBoolean(HelpActivity.HAS_SEEN_HELP_SHARED_PREF_KEY, false);

        return !hasSeenHelp;
    }
}
