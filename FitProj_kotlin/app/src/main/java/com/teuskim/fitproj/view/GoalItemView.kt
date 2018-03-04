package com.teuskim.fitproj.view

import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.os.Handler
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

import com.teuskim.fitproj.R
import com.teuskim.fitproj.common.Goal

/**
 * 목표뷰
 */
open class GoalItemView : LinearLayout {

    private var iconView: ImageView? = null
    private var text1View: TextView? = null
    private var text2View: TextView? = null
    private var graphView: CircleGraphView? = null
    private var textPercentView: TextView? = null
    private var clickArea: View? = null

    private var myHandler: Handler? = null

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    private fun init(context: Context) {
        LayoutInflater.from(context).inflate(R.layout.goal_item_view, this)
        iconView = findViewById<View>(R.id.icon) as ImageView
        text1View = findViewById<View>(R.id.text1) as TextView
        text2View = findViewById<View>(R.id.text2) as TextView
        graphView = findViewById<View>(R.id.graph) as CircleGraphView
        textPercentView = findViewById<View>(R.id.text_percent) as TextView
        clickArea = findViewById(R.id.click_area)
    }

    override fun setOnClickListener(l: View.OnClickListener?) {
        clickArea!!.setOnClickListener { l!!.onClick(this@GoalItemView) }
    }

    fun setIcon(g: Goal) {
        when (g.type) {
            Goal.Type.WALK -> iconView!!.setBackgroundResource(R.drawable.walk_anim_icon)
            Goal.Type.RUN -> iconView!!.setBackgroundResource(R.drawable.run_anim_icon)
            Goal.Type.CYCLE -> iconView!!.setBackgroundResource(R.drawable.cycle_anim_icon)
        }
    }

    fun startIconAnimate(delay: Int) {
        if (myHandler == null) {
            myHandler = Handler()
        }
        myHandler!!.postDelayed({
            val ad = iconView!!.background as AnimationDrawable
            ad.start()
        }, delay.toLong())
    }

    fun setText(text1: String, text2: String) {
        text1View!!.text = text1
        text2View!!.text = text2
    }

    fun showAndSetGraph(bgColor: Int, graphColor: Int, curr: Float, dest: Float) {
        graphView!!.visibility = View.VISIBLE
        graphView!!.resetGraph()
        graphView!!.setFactors(0.7f, 0.15f, 0.02f, bgColor, bgColor, 77)
        val maxVal = 100
        val pos = graphView!!.addGraph(intArrayOf(graphColor), maxVal)
        val `val` = maxVal * curr / dest
        graphView!!.setValue(pos, `val`, false)
        textPercentView!!.text = `val`.toInt().toString() + "%"
    }

    fun hideGraph() {
        graphView!!.visibility = View.GONE
    }

}
