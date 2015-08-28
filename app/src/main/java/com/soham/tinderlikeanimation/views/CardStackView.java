package com.soham.tinderlikeanimation.views;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.ValueAnimator;
import com.soham.tinderlikeanimation.R;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Soham Banerjee on 28/08/14.
 * <p/>
 * An implementation of a tinder like cardstack that can be swiped left or right.
 * The implmentation use the http://nineoldandroids.com/ implentation to be compatible
 * with pre ICS.
 */
public class CardStackView extends RelativeLayout {

    private static int STACK_SIZE = 4;
    private static int MAX_ANGLE_DEGREE = 20;
    final private int swipeLeft=1;
    final private int swipeRight=2;
    protected LinkedList<View> mCards = new LinkedList<View>();
    protected LinkedList<View> mRecycledCards = new LinkedList<View>();
    protected LinkedList<Object> mCardStack = new LinkedList<Object>();
    private BaseAdapter mAdapter;
    private int mCurrentPosition;
    private int mMinDragDistance;
    private int mMinAcceptDistance;
    private int mXDelta;
    private int mYDelta;
    private CardStackListener mCardStackListener;
    private int mXStart;
    private int mYStart;
    private View mBeingDragged;    private Context context;
    private MyOnTouchListener mMyTouchListener;

    public CardStackView(Context context) {
        super(context);
        this.context = context;
        setup();
    }

    public CardStackView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        setup();
    }

    public CardStackView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        setup();
    }

    private void setup() {

        Resources r = getContext().getResources();
        mMinDragDistance = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, r.getDisplayMetrics());
        mMinAcceptDistance = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, r.getDisplayMetrics());

        if (isInEditMode()) {
            mAdapter = new MockListAdapter(getContext());
        }

        mCurrentPosition = 0;
    }

    public void setAdapter(BaseAdapter adapter) {
        mAdapter = adapter;
        mRecycledCards.clear();
        mCards.clear();
        removeAllViews();
        mCurrentPosition = 0;

        initializeStack();
    }

    private void initializeStack() {
        int position = 0;
        for (; position < mCurrentPosition + STACK_SIZE;
             position++) {

            if (position >= mAdapter.getCount()) {
                break;
            }

            Object item = mAdapter.getItem(position);
            mCardStack.offer(item);
            View card = mAdapter.getView(position, null, null);

            mCards.offer(card);
            Button leftButton = new Button(context);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.CENTER_IN_PARENT);
            RelativeLayout.LayoutParams params1 = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            params1.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            params1.addRule(BELOW, card.getId());
            RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            params2.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            params2.addRule(ALIGN_PARENT_RIGHT, leftButton.getId());

            addView(card, 0, params);

            leftButton.setText("left");
            addView(leftButton, 1, params1);
            Button rightButton = new Button(context);
            rightButton.setText("right");
            addView(rightButton, 2, params2);

            mMyTouchListener = new MyOnTouchListener();
            leftButton.setOnTouchListener(mMyTouchListener);
            rightButton.setOnTouchListener(mMyTouchListener);

            rightButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    startSwipeAnimation(swipeRight);
                }
            });
            leftButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    startSwipeAnimation(swipeLeft);
                }
            });
        }

        mCurrentPosition += position;
    }
    private void startSwipeAnimation(int which){
        final View last = mCards.poll();
        ObjectAnimator xTranslation = null;
        switch (which){
            case swipeLeft:
                xTranslation = ObjectAnimator.ofFloat(last, "translationX", -1 * 1000);
                break;
            case swipeRight:
                xTranslation = ObjectAnimator.ofFloat(last, "translationX", 1 * 1000);
                break;
        }

        if (last != null) {
            AnimatorSet set = new AnimatorSet();
            ObjectAnimator imageViewObjectAnimator = ObjectAnimator.ofFloat(last,
                    "rotation", 0f, 90f);
            set.playTogether(
                    xTranslation,

                    imageViewObjectAnimator
            );

            set.setDuration(300).start();
            xTranslation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {

                    if (mCardStackListener != null) {
                        boolean choice = true;
                        mCardStackListener.onChoiceMade(choice, last);
                    }

                    recycleView(last);

                    final ViewGroup parent = (ViewGroup) last.getParent();
                    if (null != parent) {
                        parent.removeView(last);
                        parent.addView(last, 0);
                    }

                    last.setScaleX(1);
                    last.setScaleY(1);

                    setTranslationY(0);
                    setTranslationX(0);
                    requestLayout();
                }
            });
        }
    }
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mBeingDragged != null) {
            mXDelta = (int) mBeingDragged.getTranslationX();
            mYDelta = (int) mBeingDragged.getTranslationY();
        }

        int index = 0;
        Iterator<View> it = mCards.descendingIterator();
        while (it.hasNext()) {
            View card = it.next();
            if (card == null) {
                break;
            }

            if (isTopCard(card)) {
                card.setOnTouchListener(mMyTouchListener);
            } else {
                card.setOnTouchListener(null);
            }

            if (index == 0 && adapterHasMoreItems()) {
                if (mBeingDragged != null) {
                    index++;
                    continue;
                }

                scaleAndTranslate(1, card);
            } else {
                scaleAndTranslate(index, card);
            }

            index++;
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private boolean adapterHasMoreItems() {
        return mCurrentPosition < mAdapter.getCount();
    }

    private boolean isTopCard(View card) {
        return card == mCards.peek();
    }

    private boolean canAcceptChoice() {
        return Math.abs(mXDelta) > mMinAcceptDistance;
    }

    private void scaleAndTranslate(int cardIndex, View view) {
        LinearInterpolator interpolator = new LinearInterpolator();

        if (view == mBeingDragged) {
            int sign = 1;
            if (mXDelta > 0) {
                sign = -1;
            }
            float progress = Math.min(Math.abs(mXDelta) / ((float) mMinAcceptDistance * 5), 1);
            float angleDegree = MAX_ANGLE_DEGREE * interpolator.getInterpolation(progress);

            view.setRotation(sign * angleDegree);

            return;
        }

        float zoomFactor = 0;
        if (mBeingDragged != null) {
            float interpolation;
            float distance = (float) Math.sqrt(mXDelta * mXDelta + mYDelta * mYDelta);
            float progress = Math.min(distance / mMinDragDistance, 1);
            interpolation = interpolator.getInterpolation(progress);
            interpolation = Math.min(interpolation, 1);
            zoomFactor = interpolation;
        }

        int position = STACK_SIZE - cardIndex;

        float step = 0.025f;

        Resources r = getContext().getResources();
        float translateStep = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, r.getDisplayMetrics());

        float scale = step * (position - zoomFactor);
        float translate = translateStep * (position - zoomFactor);
        view.setTranslationY(translate);
        view.setTranslationX(0);
        view.setRotation(0);
        view.setScaleX(1 - scale);
        view.setScaleY(1 - scale);


        return;
    }

    public void setCardStackListener(CardStackListener cardStackListener) {
        mCardStackListener = cardStackListener;
    }

    private void recycleView(View last) {
        ((ViewGroup) last.getParent()).removeView(last);
        mRecycledCards.offer(last);
    }

    private View getRecycledOrNew() {
        if (adapterHasMoreItems()) {
            View view = mRecycledCards.poll();
            view = mAdapter.getView(mCurrentPosition++, view, null);

            return view;
        } else {
            return null;
        }
    }

    private boolean getStackChoice() {
        boolean choiceBoolean = false;
        if (mXDelta > 0) {
            choiceBoolean = true;
        }
        return choiceBoolean;
    }

    private float getStackProgress() {
        LinearInterpolator interpolator = new LinearInterpolator();
        float progress = Math.min(Math.abs(mXDelta) / ((float) mMinAcceptDistance * 5), 1);
        progress = interpolator.getInterpolation(progress);
        return progress;
    }

    public interface CardStackListener {
        void onUpdateProgress(boolean positif, float percent, View view);

        void onCancelled(View beingDragged);

        void onChoiceMade(boolean choice, View beingDragged);
    }

    private static class MockListAdapter extends BaseAdapter {

        List<String> mItems;

        Context mContext;

        public MockListAdapter(Context context) {
            mContext = context;
            mItems = new ArrayList<>();
            for (int i = 1; i < 15; i++) {
                mItems.add(i + "");
            }
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public Object getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView view = new ImageView(mContext);
            view.setImageResource(R.drawable.content_card_x_00);
            return view;
        }
    }

    private class MyOnTouchListener implements OnTouchListener {
        @Override
        public boolean onTouch(final View view, MotionEvent event) {
            if (!isTopCard(view)) {
                return false;
            }

            final int X = (int) event.getRawX();
            final int Y = (int) event.getRawY();

            final int action = event.getAction();
            switch (action & MotionEvent.ACTION_MASK) {

                case MotionEvent.ACTION_DOWN: {
                    mXStart = X;
                    mYStart = Y;

                    break;
                }
                case MotionEvent.ACTION_UP:
                    if (mBeingDragged == null) {
                        return false;
                    }

                    if (!canAcceptChoice()) {
                        requestLayout();

                        AnimatorSet set = new AnimatorSet();

                        ObjectAnimator yTranslation = ObjectAnimator.ofFloat(mBeingDragged, "translationY", 0);
                        ObjectAnimator xTranslation = ObjectAnimator.ofFloat(mBeingDragged, "translationX", 0);
                        set.playTogether(
                                xTranslation,
                                yTranslation
                        );

                        set.setDuration(100).start();
                        set.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {

                                View finalView = mBeingDragged;
                                mBeingDragged = null;
                                mXDelta = 0;
                                mYDelta = 0;
                                mXStart = 0;
                                mYStart = 0;
                                requestLayout();

                                if (mCardStackListener != null) {
                                    mCardStackListener.onCancelled(finalView);
                                }
                            }
                        });

                        ValueAnimator.AnimatorUpdateListener onUpdate = new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                mXDelta = (int) view.getTranslationX();
                                mYDelta = (int) view.getTranslationY();
                                requestLayout();
                            }
                        };

                        yTranslation.addUpdateListener(onUpdate);
                        xTranslation.addUpdateListener(onUpdate);

                        set.start();

                    } else {
                        final View last = mCards.poll();

                        View recycled = getRecycledOrNew();
                        if (recycled != null) {
                            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
                            params.addRule(RelativeLayout.CENTER_IN_PARENT);

                            mCards.offer(recycled);
                            addView(recycled, 0, params);
                        }

                        int sign = mXDelta > 0 ? +1 : -1;
                        final boolean finalChoice = mXDelta > 0;

                        mBeingDragged = null;
                        mXDelta = 0;
                        mYDelta = 0;
                        mXStart = 0;
                        mYStart = 0;

                        ObjectAnimator animation = ObjectAnimator.ofFloat(last, "translationX", sign * 1000)
                                .setDuration(300);
                        animation.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {

                                if (mCardStackListener != null) {
                                    boolean choice = finalChoice;
                                    mCardStackListener.onChoiceMade(choice, last);
                                }

                                recycleView(last);

                                final ViewGroup parent = (ViewGroup) view.getParent();
                                if (null != parent) {
                                    parent.removeView(view);
                                    parent.addView(view, 0);
                                }

                                last.setScaleX(1);
                                last.setScaleY(1);
                                setTranslationY(0);
                                setTranslationX(0);
                                requestLayout();
                            }
                        });
                        animation.start();
                    }

                    break;
                case MotionEvent.ACTION_MOVE:

                    boolean choiceBoolean = getStackChoice();
                    float progress = getStackProgress();

                    view.setTranslationX(X - mXStart);
                    view.setTranslationY(Y - mYStart);

                    mXDelta = X - mXStart;
                    mYDelta = Y - mYStart;

                    mBeingDragged = view;
                    requestLayout();

                    if (mCardStackListener != null) {
                        mCardStackListener.onUpdateProgress(choiceBoolean, progress, mBeingDragged);
                    }

                    break;
            }
            return true;
        }

    }
}
