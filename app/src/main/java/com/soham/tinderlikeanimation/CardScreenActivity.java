package com.soham.tinderlikeanimation;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import com.soham.tinderlikeanimation.adapter.FeedListAdapter;
import com.soham.tinderlikeanimation.views.CardStackView;
import com.soham.tinderlikeanimation.views.FeedItemView;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by Soham Banerjee on 28/08/15.
 */
public class CardScreenActivity extends Activity {

    @InjectView(R.id.mCardStack)
    CardStackView mCardStack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);

        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);
        doInitialize();

        mCardStack.setCardStackListener(new CardStackView.CardStackListener() {
            @Override
            public void onUpdateProgress(boolean choice, float percent, View view) {
                FeedItemView item = (FeedItemView) view;
                item.onUpdateProgress(choice, percent, view);
            }

            @Override
            public void onCancelled(View beingDragged) {
                FeedItemView item = (FeedItemView) beingDragged;
                item.onCancelled(beingDragged);
            }

            @Override
            public void onChoiceMade(boolean choice, View beingDragged) {
                FeedItemView item = (FeedItemView) beingDragged;
                item.onChoiceMade(choice, beingDragged);
            }
        });

        return;
    }

    private void doInitialize() {
        mCardStack.setAdapter(new FeedListAdapter(this));
    }
}
