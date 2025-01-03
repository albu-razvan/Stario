package com.stario.launcher.ui.common.collapsible;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.stario.launcher.utils.animation.Animation;

public class CollapsibleTitleBar extends RelativeLayout {
    private OffsetListener offsetListener;
    private ViewPropertyAnimator animator;
    private int collapsedHeight;
    private int expandedDelta;

    public CollapsibleTitleBar(Context context) {
        super(context);

        init();
    }

    public CollapsibleTitleBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    public CollapsibleTitleBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    private void init() {
        setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        collapsedHeight = 0;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        updateLayout();
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (getChildCount() == 1) {
            throw new RuntimeException("CollapsibleTitleView does not support multiple children.");
        }

        super.addView(child, index, params);

        child.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            collapsedHeight = child.getMeasuredHeight();

            updateLayout();
        });
    }

    private void updateLayout() {
        expandedDelta = getMeasuredHeight() - collapsedHeight;

        update(0);
    }

    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
        super.setLayoutParams(params);

        if (!(params instanceof CoordinatorLayout.LayoutParams)) {
            throw new RuntimeException("CollapsibleTitleView has to be attached to a CoordinatorLayout.");
        }

        ((CoordinatorLayout.LayoutParams) params).setBehavior(new CollapsibleTitleBehavior());
    }

    private int update(int scrolled) {
        if (animator != null) {
            animator.cancel();

            animator = null;
        }

        int newTranslation = (int) Math.max(-expandedDelta, Math.min(0, getTranslationY() - scrolled));
        int consumed = (int) (getTranslationY() - newTranslation);

        setTranslationY(newTranslation);

        View child = getChildAt(0);
        if (child != null) {
            float scale;

            if (expandedDelta != 0) {
                scale = 1 - 0.3f * -newTranslation / expandedDelta;
            } else {
                scale = 1;
            }

            child.setScaleY(scale);
            child.setScaleX(scale);
        }

        if (offsetListener != null) {
            offsetListener.onChange(newTranslation);
        }

        return consumed;
    }

    private void settle() {
        float translation = getTranslationY();

        if (-expandedDelta != translation && 0 != translation) {
            animate().translationY(-expandedDelta / 2f < getTranslationY() ? 0 : -expandedDelta)
                    .setDuration(Animation.MEDIUM.getDuration())
                    .setInterpolator(new FastOutSlowInInterpolator())
                    .setUpdateListener(animation -> {
                        if (offsetListener != null) {
                            offsetListener.onChange((int) getTranslationY());
                        }
                    })
                    .withEndAction(() -> animator = null);
        }
    }

    public int getCollapsedHeight() {
        return collapsedHeight;
    }

    public void setOnOffsetChangeListener(OffsetListener listener) {
        this.offsetListener = listener;

        if (listener != null) {
            listener.onChange((int) getTranslationY());
        }
    }

    public interface OffsetListener {
        void onChange(int offset);
    }

    private class CollapsibleTitleBehavior extends CoordinatorLayout.Behavior<CollapsibleTitleBar> {

        @Override
        public void onNestedPreScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull CollapsibleTitleBar child, @NonNull View target, int dx, int dy, @NonNull int[] consumed, int type) {
            if (dy > 0 || getTranslationY() > -expandedDelta) {
                consumed[1] = update(dy);
            }

            super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type);
        }

        @Override
        public void onNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull CollapsibleTitleBar child, @NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type, @NonNull int[] consumed) {
            update(dyUnconsumed);

            super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, consumed);
        }

        @Override
        public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull CollapsibleTitleBar child, @NonNull View directTargetChild, @NonNull View target, int axes, int type) {
            return true;
        }

        /*@Override
        public void onStopNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull CollapsibleTitleBarView child, @NonNull View target, int type) {
            settle();

            super.onStopNestedScroll(coordinatorLayout, child, target, type);
        }*/
    }
}
