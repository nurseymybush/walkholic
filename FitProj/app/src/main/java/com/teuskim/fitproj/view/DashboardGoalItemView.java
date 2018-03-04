package com.teuskim.fitproj.view;

import android.content.Context;
import android.util.AttributeSet;

import com.teuskim.fitproj.R;
import com.teuskim.fitproj.common.CompatUtil;
import com.teuskim.fitproj.common.Goal;

/**
 * 대시보드 하단의 목표뷰
 */
public class DashboardGoalItemView extends GoalItemView {

    private Goal goal;

    public DashboardGoalItemView(Context context) {
        super(context);
    }

    public DashboardGoalItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DashboardGoalItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setGoal(Goal goal, boolean isConvertUnitOn) {
        this.goal = goal;
        setBackgroundColor(goal.getColor());
        showAndSetGraph(goal.getColor(), 0xffffffff, goal.getCurrAmount(isConvertUnitOn), goal.getAmount(isConvertUnitOn));
        setIcon(goal);
        switch (goal.getType()) {
            case WALK:
                setText(getContext().getString(R.string.dashboard_goal_text_walk, goal.getCurrAmount(isConvertUnitOn))+goal.getAmountUnit(isConvertUnitOn)
                        , getContext().getString(R.string.dashboard_todays_goal, goal.getAmount(isConvertUnitOn))+goal.getAmountUnit(isConvertUnitOn));
                CompatUtil.setElevation(this, 6);
                break;

            case RUN:
                setText(getContext().getString(R.string.dashboard_goal_text_run, goal.getCurrAmount(isConvertUnitOn))+goal.getAmountUnit(isConvertUnitOn)
                        , getContext().getString(R.string.dashboard_todays_goal, goal.getAmount(isConvertUnitOn))+goal.getAmountUnit(isConvertUnitOn));
                CompatUtil.setElevation(this, 5);
                break;

            case CYCLE:
                setText(getContext().getString(R.string.dashboard_goal_text_cycle, goal.getCurrAmount(isConvertUnitOn))+goal.getAmountUnit(isConvertUnitOn)
                        , getContext().getString(R.string.dashboard_todays_goal, goal.getAmount(isConvertUnitOn))+goal.getAmountUnit(isConvertUnitOn));
                CompatUtil.setElevation(this, 0);
                break;
        }
    }

    public Goal getGoal() {
        return goal;
    }

    public void setElevationWithShow(boolean show) {
        if (show && goal != null) {
            switch (goal.getType()) {
                case WALK:
                    CompatUtil.setElevation(this, 6);
                    break;

                case RUN:
                    CompatUtil.setElevation(this, 5);
                    break;

                case CYCLE:
                    CompatUtil.setElevation(this, 0);
                    break;
            }
        } else {
            CompatUtil.setElevation(this, 0);
        }
    }

}
