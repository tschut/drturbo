package com.games.spaceman;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class EndLevelDialogFragment extends DialogFragment {
    private final class OnRetryClickListener implements View.OnClickListener {
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
            GameThreadHolder.getThread().reloadCurrentLevel();
            Intent intent = new Intent(activity, LevelSelect.class);
            activity.startActivityForResult(intent, SpaceApp.ACTIVITY_LEVELSELECT);
            dismiss();
        }
    }

    private final class OnNextLevelClickListener implements View.OnClickListener {
        public void onClick(View v) {
            GameThreadHolder.getThread().loadNextLevel();
            GameThreadHolder.getThread().redrawOnce();
            dismiss();
        }
    }

    private Activity activity;
    private int      textResource;
    private int      titleResource;
    private int      imageResource;
    private int      best;
    private int      points;
    private boolean  nextLevelUnlocked;

    public void setStartingActivity(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setCancelable(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.end_level_layout, container);

        getDialog().setTitle(titleResource);
        ((TextView) view.findViewById(R.id.end_level_subtitle)).setText(textResource);
        ((ImageView) view.findViewById(R.id.end_level_star)).setImageResource(imageResource);
        ((TextView) view.findViewById(R.id.end_level_points)).setText(Integer.toString(points));
        ((TextView) view.findViewById(R.id.end_level_points_best)).setText(Integer.toString(best));

        ImageButton next = (ImageButton) view.findViewById(R.id.end_level_button_next);
        ImageButton list = (ImageButton) view.findViewById(R.id.end_level_button_list);
        ImageButton retry = (ImageButton) view.findViewById(R.id.end_level_button_retry);

        if (!nextLevelUnlocked) {
            next.setImageResource(R.drawable.nextlevelbutton_disabled);
            next.setEnabled(false);
        }

        next.setOnClickListener(new OnNextLevelClickListener());
        list.setOnClickListener(new OnLevelListClickListener());
        retry.setOnClickListener(new OnRetryClickListener());

        return view;
    }

    public void setProperties(int points, int best, int imageResource, int titleResource, int textResource, boolean nextLevelUnlocked) {
        this.points = points;
        this.best = best;
        this.imageResource = imageResource;
        this.titleResource = titleResource;
        this.textResource = textResource;
        this.nextLevelUnlocked = nextLevelUnlocked;

    }
}
