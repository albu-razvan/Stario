/*
 * Copyright (C) 2026 Răzvan Albu
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

package com.stario.launcher.ui.common.text;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Layout;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

// Modification of https://stackoverflow.com/a/52373765
public class ClickableSpanTextView extends AppCompatTextView implements View.OnTouchListener {
    private OnSpanClickListener spanClickListener;
    private Runnable longPressRunnable;
    private boolean longPressTriggered;
    private ClickableSpan pressedSpan;
    private int moveSlop;
    private float downX;
    private float downY;

    public ClickableSpanTextView(@NonNull Context context) {
        super(context);

        init(context);
    }

    public ClickableSpanTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        init(context);
    }

    public ClickableSpanTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void init(Context context) {
        super.setOnTouchListener(this);

        longPressRunnable = () -> longPressTriggered = true;
        moveSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    public void setOnTouchListener(OnTouchListener l) {
        throw new RuntimeException("ClickableSpanTextView cannot set a touch listener.");
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (!(getText() instanceof Spanned)) {
            return false;
        }

        Spanned text = (Spanned) getText();
        int action = event.getAction();

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                pressedSpan = findSpan(event, text);

                if (pressedSpan != null) {
                    downX = event.getX();
                    downY = event.getY();
                    longPressTriggered = false;

                    postDelayed(longPressRunnable,
                            ViewConfiguration.getLongPressTimeout());

                    return true;
                }

                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (pressedSpan == null) break;

                float dx = Math.abs(event.getX() - downX);
                float dy = Math.abs(event.getY() - downY);

                if (dx > moveSlop || dy > moveSlop) {
                    cancelPressedSpan();
                }

                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                cancelPressedSpan();
                break;
            }

            case MotionEvent.ACTION_UP: {
                if (pressedSpan != null && !longPressTriggered) {
                    if (spanClickListener != null) {
                        spanClickListener.onSpanClick(this, pressedSpan);
                        cancelPressedSpan();

                        return true;
                    }
                }

                cancelPressedSpan();
                break;
            }
        }

        return false;
    }

    private void cancelPressedSpan() {
        removeCallbacks(longPressRunnable);

        pressedSpan = null;
        longPressTriggered = false;
    }

    private ClickableSpan findSpan(MotionEvent event, Spanned spannable) {
        int x = (int) event.getX();
        int y = (int) event.getY();

        x -= getTotalPaddingLeft();
        y -= getTotalPaddingTop();

        x += getScrollX();
        y += getScrollY();

        Layout layout = getLayout();
        if (layout == null) {
            return null;
        }

        int line = layout.getLineForVertical(y);
        int off = layout.getOffsetForHorizontal(line, x);

        ClickableSpan[] spans = spannable.getSpans(off, off, ClickableSpan.class);
        return spans.length > 0 ? spans[0] : null;
    }

    public void setOnSpanClickListener(OnSpanClickListener listener) {
        this.spanClickListener = listener;
    }

    public interface OnSpanClickListener {
        void onSpanClick(@NonNull ClickableSpanTextView view, @NonNull ClickableSpan span);
    }
}
