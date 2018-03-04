package com.teuskim.fitproj;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.teuskim.fitproj.common.FitDao;
import com.teuskim.fitproj.common.FitPreference;
import com.teuskim.fitproj.common.Goal;
import com.teuskim.fitproj.common.RefreshListener;
import com.teuskim.fitproj.view.GoalItemView;

import java.util.ArrayList;
import java.util.List;


/**
 * 메인탭의 목표 프레그먼트
 * 추가한 목표들을 보여주고 목표를 추가할 수 있다.
 */
public class MainGoalsFragment extends BaseFragment {

    private ListView listView;
    private View noGoalView;
    private GoalsAdapter adapter;
    private FitDao dao;
    private FitPreference pref;
    private boolean isConvertUnitOn;

    private RefreshListener refreshListener = new RefreshListener() {
        @Override
        public void refresh() {
            loadData();
        }
    };

    public static MainGoalsFragment newInstance() {
        return new MainGoalsFragment();
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
        View v = inflater.inflate(R.layout.main_goals_fragment, container, false);
        initViews(v);
        loadData();
        ((MainActivity)getActivity()).addRefreshListener(refreshListener);
        return v;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && pref != null && pref.isConvertUnitOn() != isConvertUnitOn) {
            isConvertUnitOn = pref.isConvertUnitOn();
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDestroyView() {
        ((MainActivity)getActivity()).removeRefreshListener(refreshListener);
        super.onDestroyView();
    }

    private void initViews(View v) {
        listView = (ListView)v.findViewById(R.id.list);
        noGoalView = v.findViewById(R.id.no_goal_view);
        adapter = new GoalsAdapter();
        listView.setAdapter(adapter);
        v.findViewById(R.id.btn_add_goal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFragment(CreateGoalFragment.newInstance(null));
            }
        });
    }

    public void loadData() {
        adapter.setList(dao.getGoalList());
        adapter.notifyDataSetChanged();
        if (adapter.getCount() == 0) {
            noGoalView.setVisibility(View.VISIBLE);
        } else {
            noGoalView.setVisibility(View.GONE);
        }
    }


    class GoalsAdapter extends BaseAdapter {

        private View.OnClickListener clickListener = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Goal g = (Goal)v.getTag();
                openFragment(CreateGoalFragment.newInstance(g));
            }
        };

        private List<Goal> list = new ArrayList<>();

        public void setList(List<Goal> list) {
            this.list = list;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Goal getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            GoalItemView v;
            if (convertView == null) {
                v = new GoalItemView(getActivity());
            } else {
                v = (GoalItemView) convertView;
            }

            Goal g = getItem(position);
            v.setIcon(g);
            String text1 = String.format("%s %.2f %s", g.getTypeText(getActivity()), g.getAmount(isConvertUnitOn), g.getAmountUnit(isConvertUnitOn));
            String text2 = g.getWhatDaysText(getActivity());
            v.setText(text1, text2);
            v.setTag(g);
            v.setOnClickListener(clickListener);

            return v;
        }
    }

}
