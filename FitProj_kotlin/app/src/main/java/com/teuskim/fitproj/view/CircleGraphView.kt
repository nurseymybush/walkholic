package com.teuskim.fitproj.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.view.View

import java.util.ArrayList

/**
 * 원형 그래프 뷰
 */
class CircleGraphView : View {

    private var paintInnerCircle: Paint? = null
    private var paintBG: Paint? = null
    private var layoutArea: RectF? = null
    private var listGraph: MutableList<Graph>? = null
    private var innerCircleRate: Float = 0.toFloat()
    private var graphWidthRate: Float = 0.toFloat()
    private var spacingRate: Float = 0.toFloat()
    private var graphBgAlpha: Int = 0

    private val invalidateHandler = object : Handler() {
        override fun handleMessage(msg: Message) {
            invalidate()
        }
    }

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
        paintInnerCircle = Paint()
        paintBG = Paint()
        listGraph = ArrayList()
        layoutArea = RectF()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = View.resolveSize(0, widthMeasureSpec)
        val h = View.resolveSize(0, heightMeasureSpec)
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        layoutArea!!.set(0f, 0f, width.toFloat(), height.toFloat())
        val cx = layoutArea!!.centerX()
        val cy = layoutArea!!.centerY()
        val maxRadius = Math.min(cx, cy)
        val layoutSize = Math.min(width, height).toFloat()
        val insetX = (width - layoutSize) / 2
        val insetY = (height - layoutSize) / 2
        layoutArea!!.inset(insetX, insetY)

        // 작은원 반지름
        val innerCircleRadius = maxRadius * innerCircleRate
        // 그래프 두께
        val graphWidth = maxRadius * graphWidthRate
        // 간격 크기
        val spacingSize = maxRadius * spacingRate

        var needAnimate = false

        if (listGraph!!.size > 0) {
            // 가장큰원의 inset
            val inset = maxRadius - (innerCircleRadius + (graphWidth + spacingSize) * listGraph!!.size)
            layoutArea!!.inset(inset, inset)
            var r = maxRadius - inset - graphWidth / 2

            // 가장 큰 그래프부터 그리기
            for (i in listGraph!!.indices.reversed()) {
                // 그래프 사이 간격을 위해 원그리고 크기 줄이기
                canvas.drawCircle(cx, cy, r + graphWidth / 2, paintBG!!)
                layoutArea!!.inset(spacingSize, spacingSize)
                r -= spacingSize

                val g = listGraph!![i]

                // 그래프 끝부분 둥근 효과를 위해 작은원 그리기
                // 시작지점
                canvas.drawCircle(cx, cy - r, graphWidth / 2, g.getStartPaint())

                // 끝지점
                canvas.drawCircle(g.getEndX(cx, r), g.getEndY(cy, r), graphWidth / 2, g.getEndPaint())

                // 그래프 그리기
                val pt = g.getGraphPaint(cx, cy)
                pt.alpha = graphBgAlpha
                canvas.drawCircle(cx, cy, r + graphWidth / 2, pt)
                pt.alpha = 255
                canvas.drawArc(layoutArea!!, g.startAngle, g.angle, true, pt)

                // 원크기 줄이기
                layoutArea!!.inset(graphWidth, graphWidth)
                r -= graphWidth
            }

            // 애니메이션 관련
            for (g in listGraph!!) {
                if (g.needAnimateMore()) {
                    needAnimate = true
                    g.incrementAnimVal()
                    break
                }
            }
        }

        // 중앙에 마스크 덮기
        canvas.drawCircle(cx, cy, innerCircleRadius, paintInnerCircle!!)

        // 애니메이션
        if (needAnimate) {
            invalidateHandler.sendEmptyMessageDelayed(0, 5)
        }
    }

    /**
     * 그래프 추가
     * @param colors 그래프 색상들. 2개이상일 경우 그라데이션 효과
     * @param maxVal 그래프의 최대값
     * @return pos 그래프의 포지션
     */
    fun addGraph(colors: IntArray, maxVal: Int): Int {
        listGraph!!.add(Graph(colors, maxVal))
        return listGraph!!.size - 1
    }

    /**
     * 그래프 초기화
     */
    fun resetGraph() {
        listGraph!!.clear()
    }

    /**
     * 그래프 값 셋팅
     * @param pos 그래프의 포지션
     * @param val 그래프 값
     */
    fun setValue(pos: Int, `val`: Float, animate: Boolean) {
        val graph = listGraph!![pos]
        graph?.setValue(`val`.toInt(), animate)
        invalidate()
    }

    /**
     * 그래프 그리는데 필요한 값들
     * @param innerCircleRate 안쪽 원형이미지의 크기비율
     * @param graphWidthRate 그래프의 두께크기비율
     * @param spacingRate 그래프들 사이의 간격
     */
    fun setFactors(innerCircleRate: Float, graphWidthRate: Float, spacingRate: Float, bgColor: Int, innerCircleColor: Int, graphBgAlpha: Int) {
        this.innerCircleRate = innerCircleRate
        this.graphWidthRate = graphWidthRate
        this.spacingRate = spacingRate
        paintBG!!.color = bgColor
        paintInnerCircle!!.color = innerCircleColor
        this.graphBgAlpha = graphBgAlpha
    }


    internal inner class Graph(private val colors: IntArray  // 크기 1이상의 배열이어야 한다.
                               , private val maxVal: Int) {
        private var shader: Shader? = null
        private val graphPaint: Paint
        private val startPaint: Paint
        private val endPaint: Paint
        private var animVal: Int = 0
        private var currVal: Int = 0
        val startAngle: Float

        val angle: Float
            get() = animVal * 360f / maxVal

        init {
            startAngle = -90f
            graphPaint = Paint()
            graphPaint.color = colors[0]
            startPaint = Paint()
            startPaint.color = colors[0]
            endPaint = Paint()
            endPaint.color = colors[0]
        }

        fun setValue(`val`: Int, animate: Boolean) {
            var `val` = `val`
            if (`val` > maxVal) {
                `val` = maxVal
            }
            if (animate) {
                animVal = currVal
            } else {
                animVal = `val`
            }
            currVal = `val`
        }

        fun getEndX(cx: Float, r: Float): Float {
            return (cx + r * Math.sin(Math.toRadians(angle.toDouble()))).toFloat()
        }

        fun getEndY(cy: Float, r: Float): Float {
            return (cy - r * Math.cos(Math.toRadians(angle.toDouble()))).toFloat()
        }

        fun getStartPaint(): Paint {
            if (colors.size == 1) {
                return startPaint
            }
            startPaint.color = getColor(0f)
            return startPaint
        }

        fun getEndPaint(): Paint {
            if (colors.size == 1) {
                return endPaint
            }
            endPaint.color = getColor(angle)
            return endPaint
        }

        private fun getColor(diffAngle: Float): Int {
            if (colors.size == 1) {
                return colors[0]
            }
            val rangeAngle = (360 / (colors.size - 1)).toFloat()
            var angle = startAngle + diffAngle
            if (angle < 0) {
                angle += 360f
            }
            val pos = (angle / rangeAngle).toInt()
            if (pos == colors.size - 1) {
                return colors[pos]
            }
            val sColor = colors[pos]
            val sBlue = sColor and 0x000000ff
            val sGreen = sColor and 0x0000ff00
            val sRed = sColor and 0x00ff0000

            val eColor = colors[pos + 1]
            val eBlue = eColor and 0x000000ff
            val eGreen = eColor and 0x0000ff00
            val eRed = eColor and 0x00ff0000

            val ratio1 = (angle - rangeAngle * pos) / rangeAngle
            val ratio2 = 1 - ratio1

            val blue = (sBlue * ratio2 + eBlue * ratio1).toInt()
            val green = (sGreen * ratio2 + eGreen * ratio1).toInt()
            val red = (sRed * ratio2 + eRed * ratio1).toInt()

            return -0x1000000 or (blue and 0x000000ff) or (green and 0x0000ff00) or (red and 0x00ff0000)
        }

        fun getGraphPaint(cx: Float, cy: Float): Paint {
            if (shader == null && colors.size > 1) {
                shader = SweepGradient(cx, cy, colors, null)
                graphPaint.shader = shader
            }
            return graphPaint
        }

        fun needAnimateMore(): Boolean {
            return animVal < currVal
        }

        fun incrementAnimVal() {
            animVal++
        }
    }
}
