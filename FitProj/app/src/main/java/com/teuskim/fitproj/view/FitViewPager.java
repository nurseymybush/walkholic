package com.teuskim.fitproj.view;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class FitViewPager extends ViewPager {

    public static interface OnChildMoveListener {
        public boolean onMove(float diffX);
    }
    private OnChildMoveListener onChildMoveListener;
    private float lastX;

    public FitViewPager(Context context) {
        super(context);
    }

    public FitViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setOnChildMoveListener(OnChildMoveListener l) {
        this.onChildMoveListener = l;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            lastX = ev.getRawX();
        } else if (ev.getAction() == MotionEvent.ACTION_MOVE) {
            float diffX = ev.getRawX() - lastX;
            if (onChildMoveListener != null && onChildMoveListener.onMove(diffX)) {
                return false;
            }
        }
        return super.onInterceptTouchEvent(ev);
    }
}
