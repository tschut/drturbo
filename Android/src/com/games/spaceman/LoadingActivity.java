package com.games.spaceman;

import tv.ouya.console.api.OuyaController;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;

import com.games.spaceman.pal.AndroidBitmapFactory;
import com.games.spaceman.pal.AndroidLog;
import com.games.spaceman.pal.AndroidResourceHandler;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.spacemangames.framework.ILoadingDoneListener;
import com.spacemangames.framework.Rect;
import com.spacemangames.framework.SpaceUtil;
import com.spacemangames.library.SpaceData;
import com.spacemangames.pal.EmptyLog;
import com.spacemangames.pal.PALManager;

public class LoadingActivity extends Activity implements ILoadingDoneListener {
    private final static String TAG = "LoadingActivity";

    static {
        System.loadLibrary("gdx");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        OuyaController.init(this);

        // Initialize the PALManager stuff
        PALManager.setResourceHandler(new AndroidResourceHandler());
        PALManager.setBitmapFactory(new AndroidBitmapFactory());
        if (DebugSettings.DEBUG_LOGGING) {
            PALManager.setLog(new AndroidLog());
        } else {
            PALManager.setLog(new EmptyLog());
        }

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        SpaceUtil.init(dm.xdpi, dm.ydpi);
        Rect resolution = new Rect(0, 0, dm.widthPixels, dm.heightPixels);
        SpaceUtil.setResolution(resolution);

        SpaceApp.mAppContext = this;

        PALManager.getLog().v(TAG, "onCreate");

        GoogleAnalyticsTracker.getInstance().startNewSession("UA-34397887-2", 30, this);

        // set the view
        setContentView(R.layout.loading_layout);

        SpaceData.getInstance().addLoadingDoneListener(this);

        // start the game thread
        final SpaceGameThread lThread = GameThreadHolder.createThread();
        lThread.setRunning(true);
        lThread.freeze();
        if (lThread.getState() == Thread.State.NEW)
            lThread.start();

        lThread.postRunnable(new Runnable() {
            public void run() {
                SpaceData.getInstance().preloadAllLevels();
                LevelDbAdapter.getInstance().insertAllLevelsIfEmpty();
                // load the fist level
                lThread.changeLevel(0, true);
                SpaceData.getInstance().setLoadingDone();
            }
        });
    }

    public void loadingDone() {
        SpaceData.getInstance().remLoadingDoneListener(this);
        // loading done, continue to the MainMenuActivity
        startActivity(new Intent(SpaceApp.mAppContext, com.games.spaceman.MainMenu.class));
    }
}
