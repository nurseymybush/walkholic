package com.teuskim.fitproj;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.teuskim.fitproj.common.DistanceSet;
import com.teuskim.fitproj.common.FitDao;
import com.teuskim.fitproj.common.FitPreference;
import com.teuskim.fitproj.common.FitUtil;
import com.teuskim.fitproj.common.Goal;
import com.teuskim.fitproj.common.RefreshListener;
import com.teuskim.fitproj.view.CircleGraphView;
import com.teuskim.fitproj.view.DashboardGoalItemView;

import java.util.Calendar;
import java.util.List;


/**
 * 메인탭의 대시보드 프레그먼트
 * 현재 수행해야하는 목표와 오늘 기준 지금까지의 운동량을 보여준다.
 */
public class MainDashboardFragment extends BaseFragment {

    private View loadingView;
    private View hasGoalLayout;
    private CircleGraphView graphView;
    private ImageView iconMoodView;
    private TextView dashboardMsgView;
    private DashboardGoalItemView walkView;
    private DashboardGoalItemView runView;
    private DashboardGoalItemView cycleView;
    private View noGoalLayout;

    private FitDao dao;
    private FitPreference pref;
    private int animDuration = 200;
    private boolean isConvertUnitOn;

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.goal_walk_layout:
                case R.id.goal_run_layout:
                case R.id.goal_cycle_layout:
                    Goal g = ((DashboardGoalItemView)v).getGoal();
                    int[] screenLocation = new int[2];
                    v.getLocationOnScreen(screenLocation);
                    openFragment(GoalDetailFragment.newInstance(g, screenLocation[1]-getStatusBarHeight()));
                    break;

                case R.id.btn_add_goal:
                    openFragment(CreateGoalFragment.newInstance(null));
                    break;
            }
        }

        public int getStatusBarHeight() {
            int result = 0;
            int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                result = getResources().getDimensionPixelSize(resourceId);
            }
            return result;
        }
    };

    private RefreshListener refreshListener = new RefreshListener() {
        @Override
        public void refresh() {
            loadData();
        }
    };

    public static MainDashboardFragment newInstance() {
        return new MainDashboardFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dao = FitDao.getInstance(getActivity());
        pref = FitPreference.getInstance(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        isConvertUnitOn = pref.isConvertUnitOn();
        View v = inflater.inflate(R.layout.main_dashboard_fragment, container, false);
        initViews(v);
        loadData();
        ((MainActivity)getActivity()).addRefreshListener(refreshListener);
        return v;
    }

    @Override
    public void onDestroyView() {
        ((MainActivity)getActivity()).removeRefreshListener(refreshListener);
        super.onDestroyView();
    }

    private void initViews(View v) {
        loadingView = v.findViewById(R.id.loading);
        hasGoalLayout = v.findViewById(R.id.has_goal_layout);
        graphView = (CircleGraphView)v.findViewById(R.id.graph_big);
        iconMoodView = (ImageView)v.findViewById(R.id.icon_mood);
        dashboardMsgView = (TextView)v.findViewById(R.id.dashboard_msg);
        walkView = (DashboardGoalItemView)v.findViewById(R.id.goal_walk_layout);
        walkView.setOnClickListener(clickListener);
        runView = (DashboardGoalItemView)v.findViewById(R.id.goal_run_layout);
        runView.setOnClickListener(clickListener);
        cycleView = (DashboardGoalItemView)v.findViewById(R.id.goal_cycle_layout);
        cycleView.setOnClickListener(clickListener);
        noGoalLayout = v.findViewById(R.id.no_goal_layout);
        View btnAddGoal = v.findViewById(R.id.btn_add_goal);
        btnAddGoal.setOnClickListener(clickListener);
    }

    public void loadData() {
        // 구간 날짜 설정
        Calendar cal = Calendar.getInstance();
        final long endTime = cal.getTimeInMillis();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        cal.set(year, month, day, 0, 0, 0);
        final long startTime = cal.getTimeInMillis();

        FitUtil.getDistanceSet(getContext(), startTime, endTime, new FitUtil.RecvDistanceSet() {
            @Override
            public void onRecvDistanceSet(DistanceSet distanceSet) {
                // 오늘 달성해야하는 목표들 가져오기
                Calendar cal = Calendar.getInstance();
                int todayPosition = cal.get(Calendar.DAY_OF_WEEK)-1;
                List<Goal> glist = dao.getGoalList(todayPosition);

                Goal walkGoal = null;
                Goal runGoal = null;
                Goal cycleGoal = null;
                int msgResId = 0;
                int imgResId = 0;

                if (glist.size() > 0) {
                    // 가져와야할 데이터 파악
                    for (Goal g : glist) {
                        switch (g.getType()) {
                            case WALK: walkGoal=g; break;
                            case RUN: runGoal=g; break;
                            case CYCLE: cycleGoal=g; break;
                        }
                    }

                    // 데이터 가져오기
                    float tmp = 0;
                    int tmpCnt = 0;
                    if (walkGoal != null) {
                        walkGoal.setCurrAmount(distanceSet.getWalk(isConvertUnitOn));
                        tmp += walkGoal.getCurrAmountRatio();
                        tmpCnt++;
                    }
                    if (runGoal != null) {
                        runGoal.setCurrAmount(distanceSet.getRun(isConvertUnitOn));
                        tmp += runGoal.getCurrAmountRatio();
                        tmpCnt++;
                    }
                    if (cycleGoal != null) {
                        cycleGoal.setCurrAmount(distanceSet.getCycle(isConvertUnitOn));
                        tmp += cycleGoal.getCurrAmountRatio();
                        tmpCnt++;
                    }
                    if (tmpCnt > 1) {
                        tmp /= (tmpCnt-1);
                    }

                    cal.setTimeInMillis(endTime);
                    int hour = cal.get(Calendar.HOUR_OF_DAY);
                    if (hour < 5) {
                        msgResId = R.string.dashboard_msg_common;
                        imgResId = R.drawable.ic_mood_sun;
                    } else if (hour < 10) {
                        msgResId = R.string.dashboard_msg_1_1;
                        imgResId = R.drawable.ic_mood_sun;
                    } else if (hour < 13) {
                        msgResId = R.string.dashboard_msg_1_2;
                        imgResId = R.drawable.ic_mood_sun;
                    } else if (tmp > 0.8) {
                        msgResId = R.string.dashboard_msg_3_1;
                        imgResId = R.drawable.ic_mood_sun;
                    } else if (tmp > 0.4) {
                        if (hour < 17) {
                            msgResId = R.string.dashboard_msg_2_4;
                            imgResId = R.drawable.ic_mood_sun;
                        } else {
                            msgResId = R.string.dashboard_msg_2_5;
                            imgResId = R.drawable.ic_mood_sun;
                        }
                    } else if (tmp < 0.2) {
                        msgResId = R.string.dashboard_msg_2_1;
                        imgResId = R.drawable.ic_mood_cloud;
                    } else if (hour < 16) {
                        msgResId = R.string.dashboard_msg_2_2;
                        imgResId = R.drawable.ic_mood_cloud;
                    } else if (hour < 17) {
                        msgResId = R.string.dashboard_msg_2_3;
                        imgResId = R.drawable.ic_mood_cloud;
                    } else if (hour < 19) {
                        msgResId = R.string.dashboard_msg_3_2;
                        imgResId = R.drawable.ic_mood_cloud;
                    } else if (hour < 21) {
                        msgResId = R.string.dashboard_msg_3_3;
                        imgResId = R.drawable.ic_mood_rain;
                    } else {
                        msgResId = R.string.dashboard_msg_common;
                        imgResId = R.drawable.ic_mood_sun;
                    }
                }

                loadingView.setVisibility(View.GONE);
                if (walkGoal == null && runGoal == null && cycleGoal == null) {
                    // 오늘 목표가 없으면 목표 등록하라고 보여준다.
                    noGoalLayout.setVisibility(View.VISIBLE);
                    hasGoalLayout.setVisibility(View.GONE);
                } else {
                    hasGoalLayout.setVisibility(View.VISIBLE);
                    noGoalLayout.setVisibility(View.GONE);
                    int maxVal = 100;
                    int pos;
                    int delay = 0;
                    int itemHeight = 0;

                    graphView.resetGraph();
                    int goalCnt = 0;

                    if (cycleGoal != null) {
                        goalCnt++;
                        pos = graphView.addGraph(new int[]{cycleGoal.getColor()}, maxVal);
                        graphView.setValue(pos, maxVal * cycleGoal.getCurrAmountRatio(), true);
                        cycleView.setVisibility(View.VISIBLE);
                        cycleView.setGoal(cycleGoal, isConvertUnitOn);

                        itemHeight += FitUtil.convertDpToPx(72, getResources());
                        cycleView.setTranslationY(itemHeight);
                        cycleView.animate().setDuration(animDuration).translationY(0);
                        cycleView.startIconAnimate(animDuration);
                        delay += animDuration;
                    } else {
                        cycleView.setVisibility(View.GONE);
                    }
                    if (runGoal != null) {
                        goalCnt++;
                        pos = graphView.addGraph(new int[]{runGoal.getColor()}, maxVal);
                        graphView.setValue(pos, maxVal * runGoal.getCurrAmountRatio(), true);
                        runView.setVisibility(View.VISIBLE);
                        runView.setGoal(runGoal, isConvertUnitOn);

                        itemHeight += FitUtil.convertDpToPx(72, getResources());
                        runView.setTranslationY(itemHeight);
                        runView.animate().setStartDelay(delay).setDuration(animDuration).translationY(0);
                        runView.startIconAnimate(delay+animDuration);
                        delay += animDuration;
                    } else {
                        runView.setVisibility(View.GONE);
                    }
                    if (walkGoal != null) {
                        goalCnt++;
                        pos = graphView.addGraph(new int[]{walkGoal.getColor()}, maxVal);
                        graphView.setValue(pos, maxVal * walkGoal.getCurrAmountRatio(), true);
                        walkView.setVisibility(View.VISIBLE);
                        walkView.setGoal(walkGoal, isConvertUnitOn);

                        itemHeight += FitUtil.convertDpToPx(72, getResources());
                        walkView.setTranslationY(itemHeight);
                        walkView.animate().setStartDelay(delay).setDuration(animDuration).translationY(0);
                        walkView.startIconAnimate(delay+animDuration);
                    } else {
                        walkView.setVisibility(View.GONE);
                    }

                    float innerCircleRate;
                    if (goalCnt < 2) {
                        innerCircleRate = 0.6f;
                    } else {
                        innerCircleRate = 0.5f;
                    }
                    graphView.setFactors(innerCircleRate, 0.11f, 0.02f, 0xff3d526d, 0xff3d526d, 20);
                    graphView.setAlpha(0);
                    graphView.animate().setDuration(animDuration).alpha(1);

                    iconMoodView.setImageResource(imgResId);
                    dashboardMsgView.setText(msgResId);
                }
            }
        });
    }

}
