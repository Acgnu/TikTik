package com.acgnu.dikdick;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;
import android.widget.ListView;

/** Horizontal swipe reveals delete; vertical gestures remain ordinary list scrolling. */
public class ScrollDelListView extends ListView {
    private LinearLayout row;
    private final int revealWidth;
    private final int touchSlop;
    private float downX, downY;
    private int initialScroll;
    private boolean swiping, vertical;

    public ScrollDelListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        revealWidth = Math.round(100 * getResources().getDisplayMetrics().density);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX(); downY = event.getY();
                swiping = false; vertical = false;
                int position = pointToPosition((int) downX, (int) downY);
                row = position == INVALID_POSITION ? null
                        : (LinearLayout) getChildAt(position - getFirstVisiblePosition());
                initialScroll = row == null ? 0 : row.getScrollX();
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - downX, dy = event.getY() - downY;
                if (!swiping && !vertical && Math.max(Math.abs(dx), Math.abs(dy)) > touchSlop) {
                    vertical = Math.abs(dy) >= Math.abs(dx);
                    swiping = !vertical && row != null;
                    if (swiping) {
                        MotionEvent cancel = MotionEvent.obtain(event);
                        cancel.setAction(MotionEvent.ACTION_CANCEL);
                        super.onTouchEvent(cancel);
                        cancel.recycle();
                    }
                }
                if (swiping) {
                    row.scrollTo(Math.max(0, Math.min(revealWidth, initialScroll - (int) dx)), 0);
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (swiping) {
                    row.scrollTo(event.getActionMasked() == MotionEvent.ACTION_UP
                            && row.getScrollX() > revealWidth / 2 ? revealWidth : 0, 0);
                    swiping = false;
                    row = null;
                    return true;
                }
                row = null;
                break;
        }
        return super.onTouchEvent(event);
    }
}
