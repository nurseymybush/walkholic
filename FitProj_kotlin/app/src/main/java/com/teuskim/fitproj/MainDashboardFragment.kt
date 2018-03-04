package com.teuskim.fitproj

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import com.teuskim.fitproj.common.DistanceSet
import com.teuskim.fitproj.common.FitDao
import com.teuskim.fitproj.common.FitPreference
import com.teuskim.fitproj.common.FitUtil
import com.teuskim.fitproj.common.Goal
import com.teuskim.fitproj.common.RefreshListener
import com.teuskim.fitproj.view.CircleGraphView
import com.teuskim.fitproj.view.DashboardGoalItemView

import java.util.Calendar


/**
 * 메인탭의 대시보드 프레그먼트
 * 현재 수행해야하는 목표와 오늘 기준 지금까지의 운동량을 보여준다.
 */
class MainDashboardFragment : BaseFragment() {

    private var loadingView: View? = null
    private var hasGoalLayout: View? = null
    private var graphView: CircleGraphView? = null
    private var iconMoodView: ImageView? = null
    private var dashboardMsgView: TextView? = null
    private var walkView: DashboardGoalItemView? = null
    private var runView: DashboardGoalItemView? = null
    private var cycleView: DashboardGoalItemView? = null
    private var noGoalLayout: View? = null

    private var dao: FitDao? = null
    private var pref: FitPreference? = null
    private val animDuration = 200
    private var isConvertUnitOn: Boolean = false

    private val clickListener = object : View.OnClickListener {

        val statusBarHeight: Int
            get() {
                var result = 0
                val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
                if (resourceId > 0) {
                    result = resources.getDimensionPixelSize(resourceId)
                }
                return result
            }

        override fun onClick(v: View) {
            when (v.id) {
                R.id.goal_walk_layout, R.id.goal_run_layout, R.id.goal_cycle_layout -> {
                    val g = (v as DashboardGoalItemView).goal
                    val screenLocation = IntArray(2)
                    v.getLocationOnScreen(screenLocation)
                    openFragment(GoalDetailFragment.newInstance(g!!, screenLocation[1] - statusBarHeight))
                }

                R.id.btn_add_goal -> openFragment(CreateGoalFragment.newInstance(null))
            }
        }
    }

    private val refreshListener = object: RefreshListener {
        override fun refresh() {
            loadData()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dao = FitDao.getInstance(activity)
        pref = FitPreference.getInstance(activity)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        isConvertUnitOn = pref!!.isConvertUnitOn
        val v = inflater.inflate(R.layout.main_dashboard_fragment, container, false)
        initViews(v)
        loadData()
        (activity as MainActivity).addRefreshListener(refreshListener)
        return v
    }

    override fun onDestroyView() {
        (activity as MainActivity).removeRefreshListener(refreshListener)
        super.onDestroyView()
    }

    private fun initViews(v: View) {
        loadingView = v.findViewById(R.id.loading)
        hasGoalLayout = v.findViewById(R.id.has_goal_layout)
        graphView = v.findViewById<View>(R.id.graph_big) as CircleGraphView
        iconMoodView = v.findViewById<View>(R.id.icon_mood) as ImageView
        dashboardMsgView = v.findViewById<View>(R.id.dashboard_msg) as TextView
        walkView = v.findViewById<View>(R.id.goal_walk_layout) as DashboardGoalItemView
        walkView!!.setOnClickListener(clickListener)
        runView = v.findViewById<View>(R.id.goal_run_layout) as DashboardGoalItemView
        runView!!.setOnClickListener(clickListener)
        cycleView = v.findViewById<View>(R.id.goal_cycle_layout) as DashboardGoalItemView
        cycleView!!.setOnClickListener(clickListener)
        noGoalLayout = v.findViewById(R.id.no_goal_layout)
        val btnAddGoal = v.findViewById<View>(R.id.btn_add_goal)
        btnAddGoal.setOnClickListener(clickListener)
    }

    fun loadData() {
        // 구간 날짜 설정
        val cal = Calendar.getInstance()
        val endTime = cal.timeInMillis
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        cal.set(year, month, day, 0, 0, 0)
        val startTime = cal.timeInMillis

        FitUtil.getDistanceSet(context, startTime, endTime, object: FitUtil.RecvDistanceSet {
            override fun onRecvDistanceSet(distanceSet: DistanceSet) {
                // 오늘 달성해야하는 목표들 가져오기
                val cal = Calendar.getInstance()
                val todayPosition = cal.get(Calendar.DAY_OF_WEEK) - 1
                val glist = dao!!.getGoalList(todayPosition)

                var walkGoal: Goal? = null
                var runGoal: Goal? = null
                var cycleGoal: Goal? = null
                var msgResId = 0
                var imgResId = 0

                if (glist.size > 0) {
                    // 가져와야할 데이터 파악
                    for (g in glist) {
                        when (g.type) {
                            Goal.Type.WALK -> walkGoal = g
                            Goal.Type.RUN -> runGoal = g
                            Goal.Type.CYCLE -> cycleGoal = g
                        }
                    }

                    // 데이터 가져오기
                    var tmp = 0f
                    var tmpCnt = 0
                    if (walkGoal != null) {
                        walkGoal.setCurrAmount(distanceSet.getWalk(isConvertUnitOn))
                        tmp += walkGoal.currAmountRatio
                        tmpCnt++
                    }
                    if (runGoal != null) {
                        runGoal.setCurrAmount(distanceSet.getRun(isConvertUnitOn))
                        tmp += runGoal.currAmountRatio
                        tmpCnt++
                    }
                    if (cycleGoal != null) {
                        cycleGoal.setCurrAmount(distanceSet.getCycle(isConvertUnitOn))
                        tmp += cycleGoal.currAmountRatio
                        tmpCnt++
                    }
                    if (tmpCnt > 1) {
                        tmp /= (tmpCnt - 1).toFloat()
                    }

                    cal.timeInMillis = endTime
                    val hour = cal.get(Calendar.HOUR_OF_DAY)
                    if (hour < 5) {
                        msgResId = R.string.dashboard_msg_common
                        imgResId = R.drawable.ic_mood_sun
                    } else if (hour < 10) {
                        msgResId = R.string.dashboard_msg_1_1
                        imgResId = R.drawable.ic_mood_sun
                    } else if (hour < 13) {
                        msgResId = R.string.dashboard_msg_1_2
                        imgResId = R.drawable.ic_mood_sun
                    } else if (tmp > 0.8) {
                        msgResId = R.string.dashboard_msg_3_1
                        imgResId = R.drawable.ic_mood_sun
                    } else if (tmp > 0.4) {
                        if (hour < 17) {
                            msgResId = R.string.dashboard_msg_2_4
                            imgResId = R.drawable.ic_mood_sun
                        } else {
                            msgResId = R.string.dashboard_msg_2_5
                            imgResId = R.drawable.ic_mood_sun
                        }
                    } else if (tmp < 0.2) {
                        msgResId = R.string.dashboard_msg_2_1
                        imgResId = R.drawable.ic_mood_cloud
                    } else if (hour < 16) {
                        msgResId = R.string.dashboard_msg_2_2
                        imgResId = R.drawable.ic_mood_cloud
                    } else if (hour < 17) {
                        msgResId = R.string.dashboard_msg_2_3
                        imgResId = R.drawable.ic_mood_cloud
                    } else if (hour < 19) {
                        msgResId = R.string.dashboard_msg_3_2
                        imgResId = R.drawable.ic_mood_cloud
                    } else if (hour < 21) {
                        msgResId = R.string.dashboard_msg_3_3
                        imgResId = R.drawable.ic_mood_rain
                    } else {
                        msgResId = R.string.dashboard_msg_common
                        imgResId = R.drawable.ic_mood_sun
                    }
                }

                loadingView!!.visibility = View.GONE
                if (walkGoal == null && runGoal == null && cycleGoal == null) {
                    // 오늘 목표가 없으면 목표 등록하라고 보여준다.
                    noGoalLayout!!.visibility = View.VISIBLE
                    hasGoalLayout!!.visibility = View.GONE
                } else {
                    hasGoalLayout!!.visibility = View.VISIBLE
                    noGoalLayout!!.visibility = View.GONE
                    val maxVal = 100
                    var pos: Int
                    var delay = 0
                    var itemHeight = 0

                    graphView!!.resetGraph()
                    var goalCnt = 0

                    if (cycleGoal != null) {
                        goalCnt++
                        pos = graphView!!.addGraph(intArrayOf(cycleGoal.color), maxVal)
                        graphView!!.setValue(pos, maxVal * cycleGoal.currAmountRatio, true)
                        cycleView!!.visibility = View.VISIBLE
                        cycleView!!.setGoal(cycleGoal, isConvertUnitOn)

                        itemHeight += FitUtil.convertDpToPx(72f, resources)
                        cycleView!!.translationY = itemHeight.toFloat()
                        cycleView!!.animate().setDuration(animDuration.toLong()).translationY(0f)
                        cycleView!!.startIconAnimate(animDuration)
                        delay += animDuration
                    } else {
                        cycleView!!.visibility = View.GONE
                    }
                    if (runGoal != null) {
                        goalCnt++
                        pos = graphView!!.addGraph(intArrayOf(runGoal.color), maxVal)
                        graphView!!.setValue(pos, maxVal * runGoal.currAmountRatio, true)
                        runView!!.visibility = View.VISIBLE
                        runView!!.setGoal(runGoal, isConvertUnitOn)

                        itemHeight += FitUtil.convertDpToPx(72f, resources)
                        runView!!.translationY = itemHeight.toFloat()
                        runView!!.animate().setStartDelay(delay.toLong()).setDuration(animDuration.toLong()).translationY(0f)
                        runView!!.startIconAnimate(delay + animDuration)
                        delay += animDuration
                    } else {
                        runView!!.visibility = View.GONE
                    }
                    if (walkGoal != null) {
                        goalCnt++
                        pos = graphView!!.addGraph(intArrayOf(walkGoal.color), maxVal)
                        graphView!!.setValue(pos, maxVal * walkGoal.currAmountRatio, true)
                        walkView!!.visibility = View.VISIBLE
                        walkView!!.setGoal(walkGoal, isConvertUnitOn)

                        itemHeight += FitUtil.convertDpToPx(72f, resources)
                        walkView!!.translationY = itemHeight.toFloat()
                        walkView!!.animate().setStartDelay(delay.toLong()).setDuration(animDuration.toLong()).translationY(0f)
                        walkView!!.startIconAnimate(delay + animDuration)
                    } else {
                        walkView!!.visibility = View.GONE
                    }

                    val innerCircleRate: Float
                    if (goalCnt < 2) {
                        innerCircleRate = 0.6f
                    } else {
                        innerCircleRate = 0.5f
                    }
                    graphView!!.setFactors(innerCircleRate, 0.11f, 0.02f, -0xc2ad93, -0xc2ad93, 20)
                    graphView!!.alpha = 0f
                    graphView!!.animate().setDuration(animDuration.toLong()).alpha(1f)

                    iconMoodView!!.setImageResource(imgResId)
                    dashboardMsgView!!.setText(msgResId)
                }
            }
        })
    }

    companion object {

        fun newInstance(): MainDashboardFragment {
            return MainDashboardFragment()
        }
    }

}
