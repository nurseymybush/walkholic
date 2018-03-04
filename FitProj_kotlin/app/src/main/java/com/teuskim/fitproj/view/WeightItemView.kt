package com.teuskim.fitproj.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

import com.teuskim.fitproj.common.FitUtil

/**
 * 몸무게 그래프 아이템뷰
 */
class WeightItemView : View {

    private var paint: Paint? = null
    private var dp1: Float = 0.toFloat()
    private var dp4: Float = 0.toFloat()
    private var dp8: Float = 0.toFloat()
    private var dp12: Float = 0.toFloat()
    private var dp30: Float = 0.toFloat()
    private var dateText: String? = null
    private var weightText: String? = null
    private var maxWeight: Float = 0.toFloat()
    private var minWeight: Float = 0.toFloat()
    private var weightRate: Float = 0.toFloat()
    private var prevWeightRate: Float = 0.toFloat()
    private var nextWeightRate: Float = 0.toFloat()
    private var hasPrev: Boolean = false
    private var hasNext: Boolean = false

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
        paint = Paint()
        dp1 = FitUtil.convertDpToPx(1f, resources).toFloat()
        dp4 = FitUtil.convertDpToPx(4f, resources).toFloat()
        dp8 = FitUtil.convertDpToPx(8f, resources).toFloat()
        dp12 = FitUtil.convertDpToPx(12f, resources).toFloat()
        dp30 = FitUtil.convertDpToPx(30f, resources).toFloat()
    }

    fun setDateText(dateText: String) {
        this.dateText = dateText
    }

    fun setRange(maxWeight: Float, minWeight: Float) {
        this.maxWeight = maxWeight
        this.minWeight = minWeight
    }

    fun setWeight(weight: Float, isConvertedUnit: Boolean) {
        if (isConvertedUnit) {
            weightText = String.format("%.1f lbs", weight)
        } else {
            weightText = String.format("%.1f kg", weight)
        }
        weightRate = getWeightRate(weight)
    }

    fun setPrevWeight(prevWeight: Float) {
        if (prevWeight > 0) {
            prevWeightRate = getWeightRate(prevWeight)
            hasPrev = true
        } else {
            hasPrev = false
        }
    }

    fun setNextWeight(nextWeight: Float) {
        if (nextWeight > 0) {
            nextWeightRate = getWeightRate(nextWeight)
            hasNext = true
        } else {
            hasNext = false
        }
    }

    private fun getWeightRate(weight: Float): Float {
        return if (maxWeight == minWeight) {
            0.5f
        } else {
            (maxWeight - weight) / (maxWeight - minWeight)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val h = height.toFloat()
        val currX = (width / 2).toFloat()
        val tmp = dp30 + dp12 + dp8 + dp8
        val currY = (h - tmp) * weightRate + dp8

        // 이전몸무게에서 이어지는 선그리기
        paint!!.color = COLOR_CYAN
        paint!!.strokeWidth = dp4
        if (hasPrev) {
            val prevX = currX - width
            val prevY = (h - tmp) * prevWeightRate + dp8
            canvas.drawLine(currX, currY, prevX, prevY, paint!!)
        }
        // 다음몸무게로 이어지는 선그리기
        if (hasNext) {
            val nextX = currX + width
            val nextY = (h - tmp) * nextWeightRate + dp8
            canvas.drawLine(currX, currY, nextX, nextY, paint!!)
        }

        // 아래로 내려가는 기준선 그리기
        paint!!.color = COLOR_LINE_DARK
        paint!!.strokeWidth = dp1
        canvas.drawLine(currX, currY, currX, h - dp12 - dp12 - dp4, paint!!)

        // 몸무게 해당 높이에 원그리기
        paint!!.color = COLOR_WHITE
        canvas.drawCircle(currX, currY, dp8, paint!!)
        paint!!.color = COLOR_CYAN
        canvas.drawCircle(currX, currY, dp4, paint!!)

        // 날짜텍스트 출력
        paint!!.color = COLOR_WHITE
        paint!!.textSize = dp12
        paint!!.textAlign = Paint.Align.CENTER
        canvas.drawText(dateText!!, (width / 2).toFloat(), h - dp12 - dp4, paint!!)

        // 몸무게 출력
        paint!!.color = COLOR_CYAN
        paint!!.textSize = dp12
        paint!!.textAlign = Paint.Align.CENTER
        canvas.drawText(weightText!!, (width / 2).toFloat(), h - dp4, paint!!)
    }

    companion object {

        private val COLOR_WHITE = -0x1
        private val COLOR_CYAN = -0xa81d01
        private val COLOR_LINE_DARK = -0xae9b84
    }
}
