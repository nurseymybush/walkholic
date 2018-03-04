package com.teuskim.fitproj;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.teuskim.fitproj.common.DistanceSet;
import com.teuskim.fitproj.common.FitPreference;
import com.teuskim.fitproj.common.FitUtil;
import com.teuskim.fitproj.common.Goal;
import com.teuskim.fitproj.view.CircleGraphView;
import com.teuskim.fitproj.view.CircleStateView;
import com.teuskim.fitproj.view.DashboardGoalItemView;
import com.teuskim.fitproj.view.HorizontalListView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 목표 상세화면
 * 대시보드의 목표를 클릭하면 펼쳐지는 화면
 */
public class GoalDetailFragment extends BaseFragment {

    private static final String KEY_GOAL = "goal";
    private static final String KEY_START_Y = "start_y";

    private LayoutInflater inflater;

    private DashboardGoalItemView topLayout;
    private View bodyView;
    private View colorPain;
    private ViewPager graphPager;

    private Goal goal;
    private int startY;
    private HorizontalListView historyListView;
    private HistoryAdapter historyAdapter;
    private GraphPagerAdapter graphPagerAdapter;
    private Handler handler = new Handler();
    private int animDuration = 200;
    private float maxScaleY;
    private int statusBarColor;
    private boolean isConvertUnitOn;
    private int historyItemWidth;

    public static GoalDetailFragment newInstance(Goal goal, int locationY) {
        GoalDetailFragment fr = new GoalDetailFragment();
        fr.statusBarColor = goal.getColor();
        Bundle args = new Bundle();
        args.putParcelable(KEY_GOAL, goal);
        args.putInt(KEY_START_Y, locationY);
        fr.setArguments(args);
        return fr;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            goal = (Goal)args.get(KEY_GOAL);
            startY = args.getInt(KEY_START_Y);
        }
        isConvertUnitOn = FitPreference.getInstance(getActivity()).isConvertUnitOn();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.inflater = inflater;
        View v = inflater.inflate(R.layout.goal_detail_fragment, container, false);
        initViews(v);
        loadData();
        return v;
    }

    @Override
    protected int getStatusBarColor() {
        return statusBarColor;
    }

    private void initViews(View v) {
        topLayout = (DashboardGoalItemView)v.findViewById(R.id.goal_top_layout);
        topLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        bodyView = v.findViewById(R.id.body);
        colorPain = v.findViewById(R.id.color_pain);
        graphPager = (ViewPager)v.findViewById(R.id.pager_graph);
        graphPagerAdapter = new GraphPagerAdapter();
        graphPager.setAdapter(graphPagerAdapter);
        graphPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
            @Override public void onPageScrollStateChanged(int state) {}

            @Override
            public void onPageSelected(int position) {
                if (historyItemWidth == 0 && historyListView.getChildCount() > 0) {
                    historyItemWidth = historyListView.getChildAt(0).getWidth();
                }
                int dx = (position - 2) * historyItemWidth;
                if (dx >= 0) {
                    historyListView.scrollTo(dx);
                }
            }
        });
        historyListView = (HorizontalListView)v.findViewById(R.id.history_list);
        historyAdapter = new HistoryAdapter();
        historyListView.setAdapter(historyAdapter);
        historyListView.setOnScrollStateChangedListener(new HorizontalListView.OnScrollStateChangedListener() {
            @Override
            public void onScrollStateChanged(ScrollState scrollState) {
                if (scrollState == ScrollState.SCROLL_STATE_IDLE) {
                    findNextAndLoadData();
                }
            }
        });
        historyListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                graphPager.setCurrentItem(position-2, true);  // historyListView의 0,1번째 아이템은 가짜다.
            }
        });

        ViewTreeObserver observer = topLayout.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                topLayout.getViewTreeObserver().removeOnPreDrawListener(this);
                runEnterAnimation();
                return true;
            }
        });
    }

    private void runEnterAnimation() {
        if (getView() != null) {
            maxScaleY = getView().getHeight() / (float)topLayout.getHeight();
        } else {
            maxScaleY = 1;
        }
        topLayout.setElevationWithShow(false);
        colorPain.setPivotY(0);
        bodyView.setAlpha(0);
        DecelerateInterpolator dint = new DecelerateInterpolator();
        AccelerateInterpolator aint = new AccelerateInterpolator();

        ObjectAnimator anim1 = ObjectAnimator.ofFloat(getView(), "translationY", startY, 0);
        anim1.setInterpolator(dint);
        anim1.setDuration(animDuration);
        ObjectAnimator anim2 = ObjectAnimator.ofFloat(colorPain, "scaleY", 1, maxScaleY);
        anim2.setInterpolator(dint);
        anim2.setDuration(animDuration);
        ObjectAnimator anim3 = ObjectAnimator.ofFloat(colorPain, "scaleY", maxScaleY, 1);
        anim3.setInterpolator(aint);
        anim3.setDuration(animDuration);
        ObjectAnimator anim4 = ObjectAnimator.ofFloat(bodyView, "alpha", 0, 1);
        anim4.setDuration(animDuration);
        ObjectAnimator anim5 = ObjectAnimator.ofFloat(colorPain, "alpha", 1, 0);
        anim5.setDuration(animDuration);

        AnimatorSet animSet = new AnimatorSet();
        animSet.play(anim1).with(anim2).with(anim4).before(anim3).before(anim5);
        animSet.start();
    }

    @Override
    protected void finish() {
        runExitAnimation(new Runnable() {
            @Override
            public void run() {
                GoalDetailFragment.super.finish();
            }
        });
    }

    private void runExitAnimation(final Runnable finish) {
        DecelerateInterpolator dint = new DecelerateInterpolator();
        AccelerateInterpolator aint = new AccelerateInterpolator();
        colorPain.setAlpha(1);

        ObjectAnimator anim1 = ObjectAnimator.ofFloat(colorPain, "scaleY", 1, maxScaleY);
        anim1.setInterpolator(dint);
        anim1.setDuration(animDuration);
        anim1.addListener(new Animator.AnimatorListener() {
            @Override public void onAnimationStart(Animator animation) {}
            @Override public void onAnimationCancel(Animator animation) {}
            @Override public void onAnimationRepeat(Animator animation) {}

            @Override
            public void onAnimationEnd(Animator animation) {
                if (getActivity() != null) {
                    ((MainActivity)getActivity()).resetStatusBarColor();
                }
            }
        });

        ObjectAnimator anim2 = ObjectAnimator.ofFloat(bodyView, "alpha", 1, 0);
        anim2.setDuration(animDuration);

        ObjectAnimator anim3 = ObjectAnimator.ofFloat(colorPain, "scaleY", maxScaleY, 1);
        anim3.setInterpolator(aint);
        anim3.setDuration(animDuration);

        ObjectAnimator anim4 = ObjectAnimator.ofFloat(getView(), "translationY", 0, startY);
        anim4.setInterpolator(aint);
        anim4.setDuration(animDuration);

        AnimatorSet animSet = new AnimatorSet();
        animSet.play(anim1).with(anim2).before(anim3).before(anim4);
        animSet.addListener(new Animator.AnimatorListener() {
            @Override public void onAnimationStart(Animator animation) {}
            @Override public void onAnimationCancel(Animator animation) {}
            @Override public void onAnimationRepeat(Animator animation) {}

            @Override
            public void onAnimationEnd(Animator animation) {
                finish.run();
            }
        });
        animSet.start();
    }

    private void loadData() {
        // 상단 목표내용
        topLayout.setGoal(goal, isConvertUnitOn);
        topLayout.startIconAnimate(500);
        colorPain.setBackgroundColor(goal.getColor());

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        long endTime = cal.getTimeInMillis();

        cal.setTimeInMillis(goal.getCrtDt()-(86400000L*100));
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startTime = cal.getTimeInMillis();

        // 하단 히스토리
        long oneDayMillis = 86400000L; // 1000 * 60 * 60 * 24
        List<History> hlist = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM.dd");
        hlist.add(new History(dateFormat.format(endTime+(oneDayMillis*2)), false, 0, 0, goal.getAmount(isConvertUnitOn)));
        hlist.add(new History(dateFormat.format(endTime+oneDayMillis), false, 0, 0, goal.getAmount(isConvertUnitOn)));

        List<History> hlist2 = new ArrayList<>();

        History h;
        for (long dayStart=endTime; dayStart>=startTime; dayStart -= oneDayMillis) {
            h = new History(dateFormat.format(dayStart), true, dayStart, (dayStart+oneDayMillis), goal.getAmount(isConvertUnitOn));
            h.setToday(dayStart == endTime);
            hlist.add(h);
            hlist2.add(h);
        }
        if (hlist2.size() > 0) {
            hlist2.get(0).setValues(goal.getCurrAmount(isConvertUnitOn), goal.getAmount(isConvertUnitOn));
        }
        historyAdapter.setList(hlist);
        historyAdapter.notifyDataSetChanged();
        graphPagerAdapter.setList(hlist2);
        graphPagerAdapter.notifyDataSetChanged();

        findNextAndLoadData();
    }

    private Runnable loadDataRunnable = new Runnable() {
        @Override
        public void run() {
            int firstPos = historyListView.getFirstVisiblePosition();
            int lastPos = historyListView.getLastVisiblePosition();
            History h = null;
            History tmp;
            for (int pos=firstPos; pos<=lastPos; pos++) {
                tmp = historyAdapter.getItem(pos);
                if (tmp.isEnabled() && tmp.isSet() == false) {
                    h = tmp;
                    break;
                }
            }
            if (h == null) {
                return;
            }
            final History h2 = h;
            FitUtil.getDistanceSet(getContext(), h.getStartTime(), h.getEndTime(), new FitUtil.RecvDistanceSet(){
                @Override
                public void onRecvDistanceSet(DistanceSet distanceSet) {
                    float dest = goal.getAmount(isConvertUnitOn);
                    float dist;
                    switch (goal.getType()) {
                        default:
                        case WALK: dist = distanceSet.getWalk(isConvertUnitOn); break;
                        case RUN: dist = distanceSet.getRun(isConvertUnitOn); break;
                        case CYCLE: dist = distanceSet.getCycle(isConvertUnitOn); break;
                    }
                    h2.setValues(dist, dest);
                    h2.setShouldAnimate(true);

                    historyAdapter.notifyDataSetChanged();
                    graphPagerAdapter.notifyDataSetChanged();
                    findNextAndLoadData();
                }
            });
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(loadDataRunnable);
    }

    private void findNextAndLoadData() {
        handler.removeCallbacks(loadDataRunnable);
        handler.postDelayed(loadDataRunnable, 100);
    }


    class History {

        private boolean isEnabled;
        private boolean isSet;
        private boolean shouldAnimate;
        private float currAmount;
        private float amount;
        private float filledRate;
        private String dateText;
        private long startTime;
        private long endTime;
        private boolean isToday;

        public History(String dateText, boolean isEnabled, long startTime, long endTime, float amount) {
            this.dateText = dateText;
            this.isEnabled = isEnabled;
            this.startTime = startTime;
            this.endTime = endTime;
            this.amount = amount;
        }

        public boolean isEnabled() {
            return isEnabled;
        }

        public boolean isSet() {
            return isSet;
        }

        public boolean shouldAnimate() {
            return shouldAnimate;
        }

        public void setShouldAnimate(boolean shouldAnimate) {
            this.shouldAnimate = shouldAnimate;
        }

        public float getCurrAmount() {
            return currAmount;
        }

        public String getAmountUnit(boolean isConvertUnitOn) {
            return (isConvertUnitOn ? "mi" : "km");
        }

        public float getAmount() {
            return amount;
        }

        public float getFilledRate() {
            return filledRate;
        }

        public void setValues(float currAmount, float amount) {
            this.currAmount = currAmount;
            this.amount = amount;
            this.filledRate = currAmount / amount;
            this.isSet = true;
        }

        public String getDateText() {
            if (isToday) {
                return getString(R.string.text_today);
            }
            return dateText;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getEndTime() {
            return endTime;
        }

        public boolean isToday() {
            return isToday;
        }

        public void setToday(boolean isToday) {
            this.isToday = isToday;
        }
    }

    class HistoryViewHolder {
        CircleStateView csView;
        TextView dateView;
    }

    class HistoryAdapter extends BaseAdapter {

        private List<History> list = new ArrayList<>();

        public void setList(List<History> list) {
            this.list = list;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public History getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            HistoryViewHolder vh;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.history_list_item, parent, false);
                vh = new HistoryViewHolder();
                vh.csView = (CircleStateView)convertView.findViewById(R.id.circle_state);
                vh.csView.setColor(goal.getColor());
                vh.dateView = (TextView)convertView.findViewById(R.id.date);
                convertView.setTag(vh);
            } else {
                vh = (HistoryViewHolder)convertView.getTag();
            }

            History h = getItem(position);
            vh.csView.setEnabled(h.isEnabled());
            if (h.shouldAnimate() && h.getFilledRate() > 0) {
                ObjectAnimator anim = ObjectAnimator.ofFloat(vh.csView, "filledRate", 0, h.getFilledRate());
                anim.setDuration(animDuration);
                anim.start();
                h.setShouldAnimate(false);
            } else {
                vh.csView.setFilledRate(h.getFilledRate());
            }
            vh.dateView.setText(h.getDateText());
            if (h.isToday()) {
                vh.dateView.setTextColor(goal.getColor());
            } else {
                vh.dateView.setTextColor(0xffffffff);
            }
            return convertView;
        }
    }

    class GraphPagerAdapter extends PagerAdapter {

        private List<History> list = new ArrayList<>();

        public void setList(List<History> list) {
            this.list = list;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View v = inflater.inflate(R.layout.goal_detail_graph_item, container, false);

            CircleGraphView graphView = (CircleGraphView)v.findViewById(R.id.graph_big);
            TextView currAmountView = (TextView)v.findViewById(R.id.curr_amount);
            TextView todayAmountView = (TextView)v.findViewById(R.id.today_amount);
            TextView textDateView = (TextView)v.findViewById(R.id.text_date);

            History h = list.get(position);

            // 그래프
            int maxVal = 100;
            int bgColor = getResources().getColor(R.color.bg_window);
            graphView.setFactors(0.6f, 0.11f, 0.02f, bgColor, bgColor, 20);
            int pos = graphView.addGraph(new int[]{goal.getColor()}, maxVal);
            graphView.setValue(pos, maxVal*h.getFilledRate(), false);

            // 그래프 중앙의 현재값 표시
            currAmountView.setTextColor(goal.getColor());
            currAmountView.setText(String.format("%.2f %s", h.getCurrAmount(), h.getAmountUnit(isConvertUnitOn)));
            todayAmountView.setText(String.format("%.2f %s", h.getAmount(), h.getAmountUnit(isConvertUnitOn)));
            textDateView.setText(h.getDateText());

            container.addView(v);
            return v;
        }
    }
}
