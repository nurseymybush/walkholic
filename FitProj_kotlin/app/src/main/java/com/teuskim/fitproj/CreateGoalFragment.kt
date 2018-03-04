package com.teuskim.fitproj

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import com.teuskim.fitproj.common.FitDao
import com.teuskim.fitproj.common.FitPreference
import com.teuskim.fitproj.common.FitUtil
import com.teuskim.fitproj.common.Goal


/**
 * 목표 추가 화면
 */
class CreateGoalFragment : BaseFragment() {

    private var whatKindView: Spinner? = null
    private var howLongView: EditText? = null
    private var whatDayContainer: ViewGroup? = null

    private var dao: FitDao? = null
    private var goal: Goal? = null
    private var isConvertUnitOn: Boolean = false

    private val clickListener = View.OnClickListener { v ->
        when (v.id) {
            R.id.btn_save -> save()

            R.id.btn_delete -> delete()
        }
    }

    override var statusBarColor: Int = 0
        get() = -0xcb780

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dao = FitDao.getInstance(activity)
        val bundle = arguments
        if (bundle != null) {
            goal = bundle.get(KEY_GOAL) as Goal
        }
        isConvertUnitOn = FitPreference.getInstance(activity).isConvertUnitOn
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.create_goal_fragment, container, false)
        v.setBackgroundColor(resources.getColor(R.color.bg_window))
        initViews(v)
        return v
    }

    private fun initViews(v: View) {
        val titleView = v.findViewById<View>(R.id.title) as TextView
        whatKindView = v.findViewById<View>(R.id.what_kind) as Spinner
        val adapter = ArrayAdapter.createFromResource(activity, R.array.what_kind_array, R.layout.spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        whatKindView!!.adapter = adapter

        howLongView = v.findViewById<View>(R.id.how_long) as EditText
        howLongView!!.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_NEXT && activity != null) {
                val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(howLongView!!.windowToken, 0)
            }
            false
        }
        val howLongUnitView = v.findViewById<View>(R.id.how_long_unit) as TextView
        howLongUnitView.text = getString(R.string.distance, if (isConvertUnitOn) "mi" else "km")

        whatDayContainer = v.findViewById<View>(R.id.what_day) as ViewGroup
        val l = View.OnClickListener { v -> v.isSelected = !v.isSelected }
        for (i in 0..6) {
            whatDayContainer!!.getChildAt(i).setOnClickListener(l)
        }
        v.findViewById<View>(R.id.btn_save).setOnClickListener(clickListener)

        if (goal != null) {
            titleView.setText(R.string.title_modify_goal)
            whatKindView!!.setSelection(goal!!.typeInt)
            howLongView!!.setText(String.format("%.2f", goal!!.getAmount(isConvertUnitOn)))
            for (i in 0..6) {
                whatDayContainer!!.getChildAt(i).isSelected = goal!!.isCheckedDay(i)
            }
            v.findViewById<View>(R.id.btn_delete_wrapper).visibility = View.VISIBLE
            v.findViewById<View>(R.id.btn_delete).setOnClickListener(clickListener)
        } else {
            titleView.setText(R.string.title_create_goal)
        }
    }

    override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation {
        return if (enter) {
            AnimationUtils.loadAnimation(activity, R.anim.bottom_in)
        } else {
            AnimationUtils.loadAnimation(activity, R.anim.bottom_out)
        }
    }

    private fun save() {
        // 목표 타입
        val whatKindType: Int
        when (whatKindView!!.selectedItemPosition) {
            0 -> whatKindType = FitDao.TYPE_GOAL_WALKING
            1 -> whatKindType = FitDao.TYPE_GOAL_RUNNING
            2 -> whatKindType = FitDao.TYPE_GOAL_CYCLING
            else -> whatKindType = FitDao.TYPE_GOAL_WALKING
        }

        // 목표 거리
        val howLongStr = howLongView!!.text.toString()
        if (TextUtils.isEmpty(howLongStr)) {
            howLongView!!.error = getString(R.string.empty_distance)
            showToast(R.string.empty_distance)
            return
        }
        var howLong = java.lang.Float.parseFloat(howLongStr)

        // 목표 요일들
        val whatDayArray = BooleanArray(7)
        var check = false
        for (i in 0..6) {
            whatDayArray[i] = whatDayContainer!!.getChildAt(i).isSelected
            if (whatDayArray[i]) {
                check = true
            }
        }
        if (check == false) {
            showToast(R.string.empty_days)
            return
        }

        val result: Boolean
        val resultMsg: Int
        if (isConvertUnitOn) {
            howLong = FitUtil.convertToKm(howLong)
        }
        if (goal != null) {
            goal!!.setType(whatKindType)
            goal!!.setAmount(howLong)
            goal!!.setWhatDays(whatDayArray)
            result = dao!!.updateGoal(goal!!, isConvertUnitOn)
            resultMsg = R.string.goal_modified
        } else {
            result = dao!!.insertGoal(whatKindType, howLong, "km", whatDayArray)
            resultMsg = R.string.goal_created
        }
        if (result) {
            showToast(resultMsg)
            (activity as MainActivity).refreshAll()
            finish()
        } else {
            showToast(R.string.common_error)
        }
    }

    private fun delete() {
        if (goal == null) {
            return
        }
        if (dao!!.deleteGoal(goal!!.id)) {
            showToast(R.string.goal_deleted)
            (activity as MainActivity).refreshAll()
            finish()
        } else {
            showToast(R.string.common_error)
        }
    }

    companion object {

        private val KEY_GOAL = "goal"

        fun newInstance(g: Goal?): CreateGoalFragment {
            val fr = CreateGoalFragment()
            if (g != null) {
                val args = Bundle()
                args.putParcelable(KEY_GOAL, g)
                fr.arguments = args
            }
            return fr
        }
    }

}
