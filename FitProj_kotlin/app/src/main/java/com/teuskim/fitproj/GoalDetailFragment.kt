package com.teuskim.fitproj

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.TextView

import com.teuskim.fitproj.common.DistanceSet
import com.teuskim.fitproj.common.FitPreference
import com.teuskim.fitproj.common.FitUtil
import com.teuskim.fitproj.common.Goal
import com.teuskim.fitproj.view.CircleGraphView
import com.teuskim.fitproj.view.CircleStateView
import com.teuskim.fitproj.view.DashboardGoalItemView
import com.teuskim.fitproj.view.HorizontalListView

import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Calendar

/**
 * 목표 상세화면
 * 대시보드의 목표를 클릭하면 펼쳐지는 화면
 */
class GoalDetailFragment : BaseFragment() {

    private var inflater: LayoutInflater? = null

    private var topLayout: DashboardGoalItemView? = null
    private var bodyView: View? = null
    private var colorPain: View? = null
    private var graphPager: ViewPager? = null

    private var goal: Goal? = null
    private var startY: Float = 0f
    private var historyListView: HorizontalListView? = null
    private var historyAdapter: HistoryAdapter? = null
    private var graphPagerAdapter: GraphPagerAdapter? = null
    private val handler = Handler()
    private val animDuration = 200
    private var maxScaleY: Float = 0.toFloat()
    override var statusBarColor: Int = 0
        set(value: Int) {
            super.statusBarColor = value
        }
    private var isConvertUnitOn: Boolean = false
    private var historyItemWidth: Int = 0

    private val loadDataRunnable = Runnable {
        val firstPos = historyListView!!.firstVisiblePosition
        val lastPos = historyListView!!.lastVisiblePosition
        var h: History? = null
        var tmp: History
        for (pos in firstPos..lastPos) {
            tmp = historyAdapter!!.getItem(pos)
            if (tmp.isEnabled && tmp.isSet == false) {
                h = tmp
                break
            }
        }
        if (h == null) {
            return@Runnable
        }
        val h2 = h
        FitUtil.getDistanceSet(context, h.startTime, h.endTime, object: FitUtil.RecvDistanceSet {
            override fun onRecvDistanceSet(distanceSet: DistanceSet) {
                val dest = goal!!.getAmount(isConvertUnitOn)
                val dist: Float
                when (goal!!.type) {
                    Goal.Type.WALK -> dist = distanceSet.getWalk(isConvertUnitOn)
                    Goal.Type.RUN -> dist = distanceSet.getRun(isConvertUnitOn)
                    Goal.Type.CYCLE -> dist = distanceSet.getCycle(isConvertUnitOn)
                    else -> dist = distanceSet.getWalk(isConvertUnitOn)
                }
                h2.setValues(dist, dest)
                h2.setShouldAnimate(true)

                historyAdapter!!.notifyDataSetChanged()
                graphPagerAdapter!!.notifyDataSetChanged()
                findNextAndLoadData()
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments
        if (args != null) {
            goal = args.get(KEY_GOAL) as Goal
            startY = args.getFloat(KEY_START_Y)
        }
        isConvertUnitOn = FitPreference.getInstance(activity).isConvertUnitOn
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        this.inflater = inflater
        val v = inflater.inflate(R.layout.goal_detail_fragment, container, false)
        initViews(v)
        loadData()
        return v
    }

    private fun initViews(v: View) {
        topLayout = v.findViewById<View>(R.id.goal_top_layout) as DashboardGoalItemView
        topLayout!!.setOnClickListener { finish() }
        bodyView = v.findViewById(R.id.body)
        colorPain = v.findViewById(R.id.color_pain)
        graphPager = v.findViewById<View>(R.id.pager_graph) as ViewPager
        graphPagerAdapter = GraphPagerAdapter()
        graphPager!!.adapter = graphPagerAdapter
        graphPager!!.setOnPageChangeListener(object : ViewPager.OnPageChangeListener {

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageScrollStateChanged(state: Int) {}

            override fun onPageSelected(position: Int) {
                if (historyItemWidth == 0 && historyListView!!.childCount > 0) {
                    historyItemWidth = historyListView!!.getChildAt(0).width
                }
                val dx = (position - 2) * historyItemWidth
                if (dx >= 0) {
                    historyListView!!.scrollTo(dx)
                }
            }
        })
        historyListView = v.findViewById<View>(R.id.history_list) as HorizontalListView
        historyAdapter = HistoryAdapter()
        historyListView!!.adapter = historyAdapter
        historyListView!!.setOnScrollStateChangedListener(object: HorizontalListView.OnScrollStateChangedListener {
            override fun onScrollStateChanged(scrollState: HorizontalListView.OnScrollStateChangedListener.ScrollState) {
                if (scrollState == HorizontalListView.OnScrollStateChangedListener.ScrollState.SCROLL_STATE_IDLE) {
                    findNextAndLoadData()
                }
            }
        })
        historyListView!!.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            graphPager!!.setCurrentItem(position - 2, true)  // historyListView의 0,1번째 아이템은 가짜다.
        }

        val observer = topLayout!!.viewTreeObserver
        observer.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                topLayout!!.viewTreeObserver.removeOnPreDrawListener(this)
                runEnterAnimation()
                return true
            }
        })
    }

    @SuppressLint("ObjectAnimatorBinding")
    private fun runEnterAnimation() {
        if (view != null) {
            maxScaleY = view!!.height / topLayout!!.height.toFloat()
        } else {
            maxScaleY = 1f
        }
        topLayout!!.setElevationWithShow(false)
        colorPain!!.pivotY = 0f
        bodyView!!.alpha = 0f
        val dint = DecelerateInterpolator()
        val aint = AccelerateInterpolator()

        val anim1 = ObjectAnimator.ofFloat(view, "translationY", startY, 0f)
        anim1.setInterpolator(dint)
        anim1.setDuration(animDuration.toLong())
        val anim2 = ObjectAnimator.ofFloat(colorPain, "scaleY", 1f, maxScaleY)
        anim2.setInterpolator(dint)
        anim2.setDuration(animDuration.toLong())
        val anim3 = ObjectAnimator.ofFloat(colorPain, "scaleY", maxScaleY, 1f)
        anim3.setInterpolator(aint)
        anim3.setDuration(animDuration.toLong())
        val anim4 = ObjectAnimator.ofFloat(bodyView, "alpha", 0f, 1f)
        anim4.setDuration(animDuration.toLong())
        val anim5 = ObjectAnimator.ofFloat(colorPain, "alpha", 1f, 0f)
        anim5.setDuration(animDuration.toLong())

        val animSet = AnimatorSet()
        animSet.play(anim1).with(anim2).with(anim4).before(anim3).before(anim5)
        animSet.start()
    }

    override fun finish() {
        runExitAnimation(Runnable { super@GoalDetailFragment.finish() })
    }

    @SuppressLint("ObjectAnimatorBinding")
    private fun runExitAnimation(finish: Runnable) {
        val dint = DecelerateInterpolator()
        val aint = AccelerateInterpolator()
        colorPain!!.alpha = 1f

        val anim1 = ObjectAnimator.ofFloat(colorPain, "scaleY", 1f, maxScaleY)
        anim1.setInterpolator(dint)
        anim1.setDuration(animDuration.toLong())
        anim1.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {
                if (activity != null) {
                    (activity as MainActivity).resetStatusBarColor()
                }
            }
        })

        val anim2 = ObjectAnimator.ofFloat(bodyView, "alpha", 1f, 0f)
        anim2.setDuration(animDuration.toLong())

        val anim3 = ObjectAnimator.ofFloat(colorPain, "scaleY", maxScaleY, 1f)
        anim3.setInterpolator(aint)
        anim3.setDuration(animDuration.toLong())

        val anim4 = ObjectAnimator.ofFloat(view, "translationY", 0f, startY)
        anim4.setInterpolator(aint)
        anim4.setDuration(animDuration.toLong())

        val animSet = AnimatorSet()
        animSet.play(anim1).with(anim2).before(anim3).before(anim4)
        animSet.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {
                finish.run()
            }
        })
        animSet.start()
    }

    private fun loadData() {
        // 상단 목표내용
        topLayout!!.setGoal(goal!!, isConvertUnitOn)
        topLayout!!.startIconAnimate(500)
        colorPain!!.setBackgroundColor(goal!!.color)

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val endTime = cal.timeInMillis

        cal.timeInMillis = goal!!.crtDt - 86400000L * 100
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startTime = cal.timeInMillis

        // 하단 히스토리
        val oneDayMillis = 86400000L // 1000 * 60 * 60 * 24
        val hlist = ArrayList<History>()
        val dateFormat = SimpleDateFormat("MM.dd")
        hlist.add(History(dateFormat.format(endTime + oneDayMillis * 2), false, 0, 0, goal!!.getAmount(isConvertUnitOn)))
        hlist.add(History(dateFormat.format(endTime + oneDayMillis), false, 0, 0, goal!!.getAmount(isConvertUnitOn)))

        val hlist2 = ArrayList<History>()

        var h: History
        var dayStart = endTime
        while (dayStart >= startTime) {
            h = History(dateFormat.format(dayStart), true, dayStart, dayStart + oneDayMillis, goal!!.getAmount(isConvertUnitOn))
            h.isToday = dayStart == endTime
            hlist.add(h)
            hlist2.add(h)
            dayStart -= oneDayMillis
        }
        if (hlist2.size > 0) {
            hlist2[0].setValues(goal!!.getCurrAmount(isConvertUnitOn), goal!!.getAmount(isConvertUnitOn))
        }
        historyAdapter!!.setList(hlist)
        historyAdapter!!.notifyDataSetChanged()
        graphPagerAdapter!!.setList(hlist2)
        graphPagerAdapter!!.notifyDataSetChanged()

        findNextAndLoadData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(loadDataRunnable)
    }

    private fun findNextAndLoadData() {
        handler.removeCallbacks(loadDataRunnable)
        handler.postDelayed(loadDataRunnable, 100)
    }


    internal inner class History(private val dateText: String, val isEnabled: Boolean, val startTime: Long, val endTime: Long, amount: Float) {
        var isSet: Boolean = false
            private set
        private var shouldAnimate: Boolean = false
        var currAmount: Float = 0.toFloat()
            private set
        var amount: Float = 0.toFloat()
            private set
        var filledRate: Float = 0.toFloat()
            private set
        var isToday: Boolean = false

        init {
            this.amount = amount
        }

        fun shouldAnimate(): Boolean {
            return shouldAnimate
        }

        fun setShouldAnimate(shouldAnimate: Boolean) {
            this.shouldAnimate = shouldAnimate
        }

        fun getAmountUnit(isConvertUnitOn: Boolean): String {
            return if (isConvertUnitOn) "mi" else "km"
        }

        fun setValues(currAmount: Float, amount: Float) {
            this.currAmount = currAmount
            this.amount = amount
            this.filledRate = currAmount / amount
            this.isSet = true
        }

        fun getDateText(): String {
            return if (isToday) {
                getString(R.string.text_today)
            } else dateText
        }
    }

    internal inner class HistoryViewHolder {
        var csView: CircleStateView? = null
        var dateView: TextView? = null
    }

    internal inner class HistoryAdapter : BaseAdapter() {

        private var list: List<History> = ArrayList()

        fun setList(list: List<History>) {
            this.list = list
        }

        override fun getCount(): Int {
            return list.size
        }

        override fun getItem(position: Int): History {
            return list[position]
        }

        override fun getItemId(position: Int): Long {
            return 0
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var convertView = convertView
            val vh: HistoryViewHolder
            if (convertView == null) {
                convertView = inflater!!.inflate(R.layout.history_list_item, parent, false)
                vh = HistoryViewHolder()
                vh.csView = convertView!!.findViewById<View>(R.id.circle_state) as CircleStateView
                vh.csView!!.setColor(goal!!.color)
                vh.dateView = convertView.findViewById<View>(R.id.date) as TextView
                convertView.tag = vh
            } else {
                vh = convertView.tag as HistoryViewHolder
            }

            val h = getItem(position)
            vh.csView!!.isEnabled = h.isEnabled
            if (h.shouldAnimate() && h.filledRate > 0) {
                val anim = ObjectAnimator.ofFloat(vh.csView, "filledRate", 0f, h.filledRate)
                anim.setDuration(animDuration.toLong())
                anim.start()
                h.setShouldAnimate(false)
            } else {
                vh.csView!!.setFilledRate(h.filledRate)
            }
            vh.dateView!!.text = h.getDateText()
            if (h.isToday) {
                vh.dateView!!.setTextColor(goal!!.color)
            } else {
                vh.dateView!!.setTextColor(-0x1)
            }
            return convertView
        }
    }

    internal inner class GraphPagerAdapter : PagerAdapter() {

        private var list: List<History> = ArrayList()

        fun setList(list: List<History>) {
            this.list = list
        }

        override fun getCount(): Int {
            return list.size
        }

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view === `object`
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }

        override fun getItemPosition(`object`: Any): Int {
            return PagerAdapter.POSITION_NONE
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val v = inflater!!.inflate(R.layout.goal_detail_graph_item, container, false)

            val graphView = v.findViewById<View>(R.id.graph_big) as CircleGraphView
            val currAmountView = v.findViewById<View>(R.id.curr_amount) as TextView
            val todayAmountView = v.findViewById<View>(R.id.today_amount) as TextView
            val textDateView = v.findViewById<View>(R.id.text_date) as TextView

            val h = list[position]

            // 그래프
            val maxVal = 100
            val bgColor = resources.getColor(R.color.bg_window)
            graphView.setFactors(0.6f, 0.11f, 0.02f, bgColor, bgColor, 20)
            val pos = graphView.addGraph(intArrayOf(goal!!.color), maxVal)
            graphView.setValue(pos, maxVal * h.filledRate, false)

            // 그래프 중앙의 현재값 표시
            currAmountView.setTextColor(goal!!.color)
            currAmountView.setText(String.format("%.2f %s", h.currAmount, h.getAmountUnit(isConvertUnitOn)))
            todayAmountView.setText(String.format("%.2f %s", h.amount, h.getAmountUnit(isConvertUnitOn)))
            textDateView.text = h.getDateText()

            container.addView(v)
            return v
        }
    }

    companion object {

        private val KEY_GOAL = "goal"
        private val KEY_START_Y = "start_y"

        fun newInstance(goal: Goal, locationY: Int): GoalDetailFragment {
            val fr = GoalDetailFragment()
            fr.statusBarColor = goal.color
            val args = Bundle()
            args.putParcelable(KEY_GOAL, goal)
            args.putInt(KEY_START_Y, locationY)
            fr.arguments = args
            return fr
        }
    }
}
