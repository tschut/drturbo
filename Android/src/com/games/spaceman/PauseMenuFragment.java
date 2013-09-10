package com.games.spaceman;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.spacemangames.framework.SpaceGameState;

public class PauseMenuFragment extends DialogFragment {
    private final class OnRestartClickListener implements View.OnClickListener {
        public void onClick(View v) {
            GameThreadHolder.getThread().reloadCurrentLevel();
            GameThreadHolder.getThread().redrawOnce();
            dismiss();
        }
    }

    private final class OnLevelListClickListener implements View.OnClickListener {
        public void onClick(View v) {
            // Reload the current level. If we don't do that, the flow makes it
            // possible to get back to
            // the level in the state it's in now because you can press the
            // 'back'button in the level selector
            int lState = SpaceGameState.getInstance().endState();
            if (lState != SpaceGameState.NOT_YET_ENDED) {
                GameThreadHolder.getThread().reloadCurrentLevel();
            }
            Intent intent = new Intent(activity, LevelSelect.class);
            activity.startActivityForResult(intent, SpaceApp.ACTIVITY_LEVELSELECT);
            dismiss();
        }
    }

    private final class OnContinueClickListener implements View.OnClickListener {
        public void onClick(View v) {
            SpaceGameState.getInstance().setPaused(false);
            dismiss();
        }
    }

    private Activity activity;

    public void setStartingActivity(Activity activity) {
        this.activity = activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.pause_layout, container);

        getDialog().setTitle(R.string.pause_title);

        Button pauseList = (Button) view.findViewById(R.id.pause_button_list);
        Button pauseRestart = (Button) view.findViewById(R.id.pause_button_restart);
        Button pauseContinue = (Button) view.findViewById(R.id.pause_button_continue);

        pauseContinue.setOnClickListener(new OnContinueClickListener());
        pauseList.setOnClickListener(new OnLevelListClickListener());
        pauseRestart.setOnClickListener(new OnRestartClickListener());

        return view;
    }
}
