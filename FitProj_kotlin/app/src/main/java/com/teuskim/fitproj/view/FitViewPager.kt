package com.teuskim.fitproj.view

import android.content.Context
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.view.MotionEvent

class FitViewPager : ViewPager {
    private var onChildMoveListener: OnChildMoveListener? = null
    private var lastX: Float = 0.toFloat()

    interface OnChildMoveListener {
        fun onMove(diffX: Float): Boolean
    }

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    fun setOnChildMoveListener(l: OnChildMoveListener) {
        this.onChildMoveListener = l
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            lastX = ev.rawX
        } else if (ev.action == MotionEvent.ACTION_MOVE) {
            val diffX = ev.rawX - lastX
            if (onChildMoveListener != null && onChildMoveListener!!.onMove(diffX)) {
                return false
            }
        }
        return super.onInterceptTouchEvent(ev)
    }
}
