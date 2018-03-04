package com.teuskim.fitproj.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

import com.teuskim.fitproj.R

/**
 * 목표상세보기 하단의 원형상태뷰
 */
class CircleStateView : View {

    private var maskBm: Bitmap? = null
    private var filledPaint: Paint? = null
    private var filledRate: Float = 0.toFloat()

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        maskBm = BitmapFactory.decodeResource(resources, R.drawable.mask_circle)
        filledPaint = Paint()
    }

    fun setColor(color: Int) {
        filledPaint!!.color = color
    }

    fun setFilledRate(filledRate: Float) {
        this.filledRate = filledRate
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 배경색
        if (isEnabled) {
            canvas.drawColor(-0xcebda9)

            // 채우는색
            val filledTop = height * (1 - filledRate)
            canvas.drawRect(0f, filledTop, width.toFloat(), height.toFloat(), filledPaint!!)
        } else {
            canvas.drawColor(-0x9b8a76)
        }

        // 원형 마스크
        canvas.save()
        canvas.scale(width / maskBm!!.width.toFloat(), height / maskBm!!.height.toFloat())
        canvas.drawBitmap(maskBm!!, 0f, 0f, null)
        canvas.restore()
    }
}
