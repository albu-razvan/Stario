/*
 * Copyright (C) 2025 Răzvan Albu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>
 */

// reference https://github.com/quiph/RecyclerView-FastScroller/blob/master/recyclerviewfastscroller/src/main/java/com/qtalk/recyclerviewfastscroller/RecyclerViewFastScroller.kt

package com.stario.launcher.ui.recyclers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.R;
import com.stario.launcher.utils.animation.Animation;

public class FastScroller extends RelativeLayout {
    private RecyclerView recyclerView;
    private TextView popup;
    private View trackRight;
    private View trackLeft;
    private Float x;
    private Float y;
    private float alphaLeft;
    private float alphaRight;
    private int currentPosition;
    private int trackLength;
    private int popupHeight;
    private int swipeSlop;
    private boolean isEngaged;
    private int lastPositionScrolled;

    public FastScroller(Context context) {
        super(context);

        init(context);
    }

    public FastScroller(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context);
    }

    public FastScroller(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context);
    }

    private void init(Context context) {
        this.x = null;
        this.y = null;
        this.alphaLeft = 0;
        this.alphaRight = 0;
        this.currentPosition = -1;
        this.isEngaged = false;
        this.swipeSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        this.lastPositionScrolled = -1;

        addPopupLayout();
        addTrack();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        trackLength = trackRight.getHeight();
        popupHeight = popup.getHeight();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        updateTrackWidth();
    }

    public void setTopOffset(int topOffset) {
        ((MarginLayoutParams) trackRight
                .getLayoutParams()).topMargin = topOffset;
        ((MarginLayoutParams) trackLeft
                .getLayoutParams()).topMargin = topOffset;
    }

    public void setBottomOffset(int bottomOffset) {
        ((MarginLayoutParams) trackRight
                .getLayoutParams()).bottomMargin = bottomOffset;
        ((MarginLayoutParams) trackLeft
                .getLayoutParams()).bottomMargin = bottomOffset;
    }

    private void addPopupLayout() {
        View.inflate(getContext(), R.layout.fastscroller_popup, this);

        popup = findViewById(R.id.fast_scroller_pop_up);
    }

    private void addTrack() {
        View.inflate(getContext(), R.layout.fastscroller_track_thumb, this);

        trackRight = findViewById(R.id.track_right);
        trackLeft = findViewById(R.id.track_left);
    }

    @Override
    protected void onDetachedFromWindow() {
        detachFastScrollerFromRecyclerView();

        super.onDetachedFromWindow();
    }

    @SuppressLint("ClickableViewAccessibility")
    public void detachFastScrollerFromRecyclerView() {
        popup.setOnTouchListener(null);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        // two tracks + popup view which are inflated before everything else
        if (getChildCount() > 3) {
            View child = getChildAt(3);

            // move to top
            removeView(child);
            addView(child, 0);

            addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                if (recyclerView == null ||
                        recyclerView.getMeasuredHeight() < getMeasuredHeight()) {
                    trackLeft.setVisibility(GONE);
                    trackRight.setVisibility(GONE);
                } else {
                    trackLeft.setVisibility(VISIBLE);
                    trackRight.setVisibility(VISIBLE);
                }
            });

            this.recyclerView = findRecycler(child);
            if (recyclerView != null) {
                updateTrackWidth();
            }

            post(() -> {
                OnTouchListener listener = (view, motionEvent) -> {
                    if (recyclerView != null &&
                            recyclerView.getAdapter() != null) {
                        if ((x == null && y == null) ||
                                motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                            x = motionEvent.getRawX();
                            y = motionEvent.getRawY();
                        } else {
                            float absDeltaX = Math.abs(motionEvent.getRawX() - x);
                            float absDeltaY = Math.abs(motionEvent.getRawY() - y);

                            if (absDeltaX > absDeltaY && absDeltaX >= swipeSlop) {
                                x = Float.MIN_VALUE;

                                if (motionEvent.getAction() == MotionEvent.ACTION_UP ||
                                        motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {
                                    x = null;
                                    y = null;
                                }

                                return false;
                            } else if (absDeltaY > absDeltaX && absDeltaY >= swipeSlop) {
                                y = Float.MIN_VALUE;

                                recyclerView.stopScroll();

                                switch (motionEvent.getAction()) {
                                    case MotionEvent.ACTION_MOVE: {
                                        if (!isEngaged) {
                                            isEngaged = true;

                                            animatePopupVisibility(true, view.equals(trackRight));
                                        }

                                        float currentRelativePos = motionEvent.getY();

                                        moveViewToRelativePositionWithBounds(currentRelativePos -
                                                (float) popupHeight / 2);

                                        int position = Math.min(recyclerView.getAdapter().getItemCount() - 1,
                                                computePositionForOffsetAndScroll(currentRelativePos));

                                        updateTextInPopup(position);

                                        return true;
                                    }

                                    case MotionEvent.ACTION_UP:
                                    case MotionEvent.ACTION_CANCEL: {
                                        isEngaged = false;

                                        post(() -> animatePopupVisibility(false, view.equals(trackRight)));

                                        if (recyclerView.getAdapter() instanceof OnPopupViewReset) {
                                            ((OnPopupViewReset) recyclerView.getAdapter())
                                                    .onReset(currentPosition);
                                        }

                                        x = null;
                                        y = null;

                                        return FastScroller.super.onTouchEvent(motionEvent);
                                    }

                                    default: {
                                        return false;
                                    }
                                }
                            }

                            return true;
                        }
                    } else {
                        if (motionEvent.getAction() == MotionEvent.ACTION_UP ||
                                motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {
                            x = null;
                            y = null;
                        }
                    }

                    return true;
                };

                trackLeft.setOnTouchListener(listener);
                trackRight.setOnTouchListener(listener);

                invalidate();
            });
        }
    }

    private RecyclerView findRecycler(View view) {
        if (view instanceof RecyclerView) {
            return (RecyclerView) view;
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;

            for (int index = 0; index < group.getChildCount(); index++) {
                RecyclerView recycler = findRecycler(group.getChildAt(index));

                if (recycler != null) {
                    return recycler;
                }
            }
        }

        return null;
    }

    private int computePositionForOffsetAndScroll(float rawPosition) {
        if (recyclerView != null) {
            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            RecyclerView.Adapter<?> adapter = recyclerView.getAdapter();

            int recyclerViewItemCount = adapter != null ? adapter.getItemCount() : 0;
            var newOffset = rawPosition / trackLength;

            newOffset = Math.max(Math.min(newOffset, 1), 0);

            if (layoutManager instanceof LinearLayoutManager) {
                int last;

                if (((LinearLayoutManager) layoutManager).getReverseLayout()) {
                    last = recyclerViewItemCount - (int) (newOffset * recyclerViewItemCount);
                } else {
                    last = (int) (newOffset * recyclerViewItemCount);
                }

                int position = Math.min(recyclerViewItemCount, Math.max(0, last));

                safeScrollToPosition(Math.min(adapter != null ? adapter.getItemCount() : 0, position));

                return position;
            } else {
                int position = (int) (newOffset * recyclerViewItemCount);

                safeScrollToPosition(position);

                return position;
            }
        }

        return 0;
    }

    private void safeScrollToPosition(int position) {
        if (position != lastPositionScrolled && recyclerView != null) {
            recyclerView.scrollToPosition(position);

            lastPositionScrolled = position;
        }
    }

    private void updateTextInPopup(int position) {
        if (recyclerView != null && currentPosition != position) {
            RecyclerView.Adapter<?> adapter = recyclerView.getAdapter();

            if (adapter == null ||
                    position < 0 ||
                    position >= adapter.getItemCount()) {
                return;
            }

            currentPosition = position;

            if (adapter instanceof OnPopupViewUpdate) {
                ((OnPopupViewUpdate) adapter).onUpdate(position, popup);
            } else {
                popup.setVisibility(GONE);
            }
        }
    }

    private void alignPopupLayout(boolean rightSide) {
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);

        if (rightSide) {
            params.addRule(LEFT_OF, trackRight.getId());

            post(() -> popup.setX(trackRight.getX() - popup.getWidth()));
        } else {
            params.addRule(RIGHT_OF, trackLeft.getId());

            post(() -> popup.setX(trackLeft.getX() + trackLeft.getWidth()));
        }

        popup.setLayoutParams(params);
    }

    public void animateVisibility(boolean makeVisible) {
        animateVisibility(makeVisible, false, 0.2f, 0f);
        animateVisibility(makeVisible, true, 0.2f, 0f);
    }

    private void animateVisibility(boolean makeVisible, boolean rightSide,
                                   float maxAlpha, float minAlpha) {
        float alpha = makeVisible ? maxAlpha : minAlpha;

        if (rightSide) {
            if (alphaRight != alpha) {
                trackRight.animate()
                        .alpha(alpha)
                        .setDuration(Animation.SHORT.getDuration());

                alphaRight = alpha;
            }
        } else {
            if (alphaLeft != alpha) {
                trackLeft.animate()
                        .alpha(alpha)
                        .setDuration(Animation.SHORT.getDuration());

                alphaLeft = alpha;
            }
        }
    }

    private void animatePopupVisibility(boolean makeVisible, boolean rightSide) {
        float scaleFactor = makeVisible ? 1f : 0.5f;
        float alpha = makeVisible ? 1f : 0f;

        alignPopupLayout(rightSide);
        popup.setPivotX(popup.getWidth() * (rightSide ? 0.75f : 0.25f));

        popup.animate()
                .scaleX(scaleFactor)
                .scaleY(scaleFactor)
                .alpha(alpha)
                .setDuration(Animation.MEDIUM.getDuration());

        animateVisibility(makeVisible, rightSide, 0.6f, 0.2f);
    }

    private void moveViewToRelativePositionWithBounds(float offset) {
        if (Float.isNaN(offset)) {
            offset = 0f;
        }

        popup.setY(trackRight.getY() + Math.min(Math.max(offset, 0),
                (trackLength - popupHeight)));
    }

    private void updateTrackWidth() {
        if (recyclerView != null) {
            trackLeft.getLayoutParams().width = recyclerView.getPaddingLeft();
            trackRight.getLayoutParams().width = recyclerView.getPaddingRight();

            invalidate();
        }
    }

    public interface OnPopupViewReset {
        void onReset(int position);
    }

    public interface OnPopupViewUpdate {
        void onUpdate(int index, @NonNull TextView textView);
    }
}



