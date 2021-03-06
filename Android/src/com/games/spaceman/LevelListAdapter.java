package com.games.spaceman;

import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.spacemangames.framework.SpaceGameState;
import com.spacemangames.library.SpaceData;
import com.spacemangames.pal.PALManager;

public class LevelListAdapter extends BaseAdapter {
    private static final String TAG = "LevelListAdapter";

    private static final int TYPE_LEVEL = 0;
    private static final int TYPE_AD = 1;
    private static final int TYPE_MAX_COUNT = TYPE_AD + 1;

    private final LayoutInflater mInflater;
    private final Cursor mCursor;

    public LevelListAdapter(Cursor aCursor, LayoutInflater aInflater) {
        mInflater = aInflater;
        mCursor = aCursor;
    }

    @Override
    public int getItemViewType(int position) {
        return TYPE_LEVEL;
    }

    @Override
    public int getViewTypeCount() {
        return TYPE_MAX_COUNT;
    }

    public int getCount() {
        return SpaceData.getInstance().mLevels.size();
    }

    public Object getItem(int position) {
        return SpaceData.getInstance().mLevels.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        mCursor.moveToPosition((int) getItemId(position));
        String lLevelTitle = mCursor.getString(mCursor.getColumnIndex(LevelDbAdapter.KEY_TITLE));
        String lLevelNumber = mCursor.getString(mCursor.getColumnIndex(LevelDbAdapter.KEY_LEVELNUMBER));
        String lLevelHighScore = mCursor.getString(mCursor.getColumnIndex(LevelDbAdapter.KEY_HIGHSCORE));

        PALManager.getLog().i(TAG, "Level: " + lLevelNumber + " " + lLevelHighScore + " points");
        convertView = mInflater.inflate(R.layout.levelselect_item, null);
        TextView lLevelTitleTextView = (TextView) convertView.findViewById(R.id.level_title);
        TextView lLevelNumberTextView = (TextView) convertView.findViewById(R.id.level_number);
        TextView lLevelHighScoreTextView = (TextView) convertView.findViewById(R.id.level_points);
        TextView lLevelHighScoreTitleView = (TextView) convertView.findViewById(R.id.level_points_text);
        ImageView lStarImageView = (ImageView) convertView.findViewById(R.id.star_image);

        lLevelTitleTextView.setText(lLevelTitle);
        lLevelNumberTextView.setText(lLevelNumber);
        lLevelHighScoreTextView.setText(lLevelHighScore);

        lLevelHighScoreTextView.setVisibility(View.VISIBLE);
        lLevelHighScoreTitleView.setVisibility(View.VISIBLE);
        if (!LevelDbAdapter.getInstance().levelIsUnlocked(Integer.parseInt(lLevelNumber))) {
            lStarImageView.setImageResource(R.drawable.star_disabled);
            lLevelHighScoreTextView.setVisibility(View.INVISIBLE);
            lLevelHighScoreTitleView.setVisibility(View.INVISIBLE);
        } else if (Integer.parseInt(lLevelHighScore) == 0) {
            lStarImageView.setImageResource(R.drawable.star_enabled);
        } else {
            int lStarColor = SpaceData.getInstance().levelStarColor(Integer.parseInt(lLevelNumber),
                    Integer.parseInt(lLevelHighScore));
            switch (lStarColor) {
            case SpaceGameState.WON_BRONZE:
                lStarImageView.setImageResource(R.drawable.star_bronze);
                break;
            case SpaceGameState.WON_SILVER:
                lStarImageView.setImageResource(R.drawable.star_silver);
                break;
            case SpaceGameState.WON_GOLD:
                lStarImageView.setImageResource(R.drawable.star_gold);
                break;
            }
        }

        return convertView;
    }
}
