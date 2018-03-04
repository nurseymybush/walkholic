package com.teuskim.fitproj.view

import android.content.Context
import android.util.AttributeSet

import com.teuskim.fitproj.R
import com.teuskim.fitproj.common.CompatUtil
import com.teuskim.fitproj.common.Goal

/**
 * 대시보드 하단의 목표뷰
 */
class DashboardGoalItemView : GoalItemView {

    var goal: Goal? = null
        private set

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

    fun setGoal(goal: Goal, isConvertUnitOn: Boolean) {
        this.goal = goal
        setBackgroundColor(goal.color)
        showAndSetGraph(goal.color, -0x1, goal.getCurrAmount(isConvertUnitOn), goal.getAmount(isConvertUnitOn))
        setIcon(goal)
        when (goal.type) {
            Goal.Type.WALK -> {
                setText(context.getString(R.string.dashboard_goal_text_walk, goal.getCurrAmount(isConvertUnitOn)) + goal.getAmountUnit(isConvertUnitOn), context.getString(R.string.dashboard_todays_goal, goal.getAmount(isConvertUnitOn)) + goal.getAmountUnit(isConvertUnitOn))
                CompatUtil.setElevation(this, 6f)
            }

            Goal.Type.RUN -> {
                setText(context.getString(R.string.dashboard_goal_text_run, goal.getCurrAmount(isConvertUnitOn)) + goal.getAmountUnit(isConvertUnitOn), context.getString(R.string.dashboard_todays_goal, goal.getAmount(isConvertUnitOn)) + goal.getAmountUnit(isConvertUnitOn))
                CompatUtil.setElevation(this, 5f)
            }

            Goal.Type.CYCLE -> {
                setText(context.getString(R.string.dashboard_goal_text_cycle, goal.getCurrAmount(isConvertUnitOn)) + goal.getAmountUnit(isConvertUnitOn), context.getString(R.string.dashboard_todays_goal, goal.getAmount(isConvertUnitOn)) + goal.getAmountUnit(isConvertUnitOn))
                CompatUtil.setElevation(this, 0f)
            }
        }
    }

    fun setElevationWithShow(show: Boolean) {
        if (show && goal != null) {
            when (goal!!.type) {
                Goal.Type.WALK -> CompatUtil.setElevation(this, 6f)

                Goal.Type.RUN -> CompatUtil.setElevation(this, 5f)

                Goal.Type.CYCLE -> CompatUtil.setElevation(this, 0f)
            }
        } else {
            CompatUtil.setElevation(this, 0f)
        }
    }

}
