package com.teuskim.fitproj

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView

import com.teuskim.fitproj.common.FitDao
import com.teuskim.fitproj.common.FitPreference
import com.teuskim.fitproj.common.Goal
import com.teuskim.fitproj.common.RefreshListener
import com.teuskim.fitproj.view.GoalItemView

import java.util.ArrayList


/**
 * 메인탭의 목표 프레그먼트
 * 추가한 목표들을 보여주고 목표를 추가할 수 있다.
 */
class MainGoalsFragment : BaseFragment() {

    private var listView: ListView? = null
    private var noGoalView: View? = null
    private var adapter: GoalsAdapter? = null
    private var dao: FitDao? = null
    private var pref: FitPreference? = null
    private var isConvertUnitOn: Boolean = false

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
        val v = inflater.inflate(R.layout.main_goals_fragment, container, false)
        initViews(v)
        loadData()
        (activity as MainActivity).addRefreshListener(refreshListener)
        return v
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isVisibleToUser && pref != null && pref!!.isConvertUnitOn != isConvertUnitOn) {
            isConvertUnitOn = pref!!.isConvertUnitOn
            adapter!!.notifyDataSetChanged()
        }
    }

    override fun onDestroyView() {
        (activity as MainActivity).removeRefreshListener(refreshListener)
        super.onDestroyView()
    }

    private fun initViews(v: View) {
        listView = v.findViewById<View>(R.id.list) as ListView
        noGoalView = v.findViewById(R.id.no_goal_view)
        adapter = GoalsAdapter()
        listView!!.adapter = adapter
        v.findViewById<View>(R.id.btn_add_goal).setOnClickListener { openFragment(CreateGoalFragment.newInstance(null)) }
    }

    fun loadData() {
        adapter!!.setList(dao!!.goalList)
        adapter!!.notifyDataSetChanged()
        if (adapter!!.count == 0) {
            noGoalView!!.visibility = View.VISIBLE
        } else {
            noGoalView!!.visibility = View.GONE
        }
    }


    internal inner class GoalsAdapter : BaseAdapter() {

        private val clickListener = View.OnClickListener { v ->
            val g = v.tag as Goal
            openFragment(CreateGoalFragment.newInstance(g))
        }

        private var list: List<Goal> = ArrayList()

        fun setList(list: List<Goal>) {
            this.list = list
        }

        override fun getCount(): Int {
            return list.size
        }

        override fun getItem(position: Int): Goal {
            return list[position]
        }

        override fun getItemId(position: Int): Long {
            return 0
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

            val v: GoalItemView
            if (convertView == null) {
                v = GoalItemView(activity)
            } else {
                v = (convertView as GoalItemView)!!
            }

            val g = getItem(position)
            v.setIcon(g)
            val text1 = String.format("%s %.2f %s", g.getTypeText(activity), g.getAmount(isConvertUnitOn), g.getAmountUnit(isConvertUnitOn))
            val text2 = g.getWhatDaysText(activity)
            v.setText(text1, text2)
            v.tag = g
            v.setOnClickListener(clickListener)

            return v
        }
    }

    companion object {

        fun newInstance(): MainGoalsFragment {
            return MainGoalsFragment()
        }
    }

}
