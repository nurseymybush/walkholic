package com.teuskim.fitproj;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.teuskim.fitproj.common.FitDao;
import com.teuskim.fitproj.common.FitPreference;
import com.teuskim.fitproj.common.FitUtil;
import com.teuskim.fitproj.common.Goal;


/**
 * 목표 추가 화면
 */
public class CreateGoalFragment extends BaseFragment {

    private static final String KEY_GOAL = "goal";

    private Spinner whatKindView;
    private EditText howLongView;
    private ViewGroup whatDayContainer;

    private FitDao dao;
    private Goal goal;
    private boolean isConvertUnitOn;

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_save:
                    save();
                    break;

                case R.id.btn_delete:
                    delete();
                    break;
            }
        }
    };

    public static CreateGoalFragment newInstance(Goal g) {
        CreateGoalFragment fr = new CreateGoalFragment();
        if (g != null) {
            Bundle args = new Bundle();
            args.putParcelable(KEY_GOAL, g);
            fr.setArguments(args);
        }
        return fr;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dao = FitDao.getInstance(getActivity());
        Bundle bundle = getArguments();
        if (bundle != null) {
            goal = (Goal)bundle.get(KEY_GOAL);
        }
        isConvertUnitOn = FitPreference.getInstance(getActivity()).isConvertUnitOn();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.create_goal_fragment, container, false);
        v.setBackgroundColor(getResources().getColor(R.color.bg_window));
        initViews(v);
        return v;
    }

    @Override
    protected int getStatusBarColor() {
        return 0xfff34880;
    }

    private void initViews(View v) {
        TextView titleView = (TextView)v.findViewById(R.id.title);
        whatKindView = (Spinner)v.findViewById(R.id.what_kind);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(), R.array.what_kind_array, R.layout.spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        whatKindView.setAdapter(adapter);

        howLongView = (EditText)v.findViewById(R.id.how_long);
        howLongView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT && getActivity() != null) {
                    InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(howLongView.getWindowToken(), 0);
                }
                return false;
            }
        });
        TextView howLongUnitView = (TextView)v.findViewById(R.id.how_long_unit);
        howLongUnitView.setText(getString(R.string.distance, isConvertUnitOn ? "mi" : "km"));

        whatDayContainer = (ViewGroup)v.findViewById(R.id.what_day);
        View.OnClickListener l = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setSelected(!v.isSelected());
            }
        };
        for (int i=0; i<7; i++) {
            whatDayContainer.getChildAt(i).setOnClickListener(l);
        }
        v.findViewById(R.id.btn_save).setOnClickListener(clickListener);

        if (goal != null) {
            titleView.setText(R.string.title_modify_goal);
            whatKindView.setSelection(goal.getTypeInt());
            howLongView.setText(String.format("%.2f", goal.getAmount(isConvertUnitOn)));
            for (int i=0; i<7; i++) {
                whatDayContainer.getChildAt(i).setSelected(goal.isCheckedDay(i));
            }
            v.findViewById(R.id.btn_delete_wrapper).setVisibility(View.VISIBLE);
            v.findViewById(R.id.btn_delete).setOnClickListener(clickListener);
        } else {
            titleView.setText(R.string.title_create_goal);
        }
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        if (enter) {
            return AnimationUtils.loadAnimation(getActivity(), R.anim.bottom_in);
        } else {
            return AnimationUtils.loadAnimation(getActivity(), R.anim.bottom_out);
        }
    }

    private void save() {
        // 목표 타입
        int whatKindType;
        switch (whatKindView.getSelectedItemPosition()) {
            case 0:default: whatKindType = FitDao.TYPE_GOAL_WALKING; break;
            case 1: whatKindType = FitDao.TYPE_GOAL_RUNNING; break;
            case 2: whatKindType = FitDao.TYPE_GOAL_CYCLING; break;
        }

        // 목표 거리
        String howLongStr = howLongView.getText().toString();
        if (TextUtils.isEmpty(howLongStr)) {
            howLongView.setError(getString(R.string.empty_distance));
            showToast(R.string.empty_distance);
            return;
        }
        float howLong = Float.parseFloat(howLongStr);

        // 목표 요일들
        boolean[] whatDayArray = new boolean[7];
        boolean check = false;
        for (int i=0; i<7; i++) {
            whatDayArray[i] = whatDayContainer.getChildAt(i).isSelected();
            if (whatDayArray[i]) {
                check = true;
            }
        }
        if (check == false) {
            showToast(R.string.empty_days);
            return;
        }

        boolean result;
        int resultMsg;
        if (isConvertUnitOn) {
            howLong = FitUtil.convertToKm(howLong);
        }
        if (goal != null) {
            goal.setType(whatKindType);
            goal.setAmount(howLong);
            goal.setWhatDays(whatDayArray);
            result = dao.updateGoal(goal, isConvertUnitOn);
            resultMsg = R.string.goal_modified;
        } else {
            result = dao.insertGoal(whatKindType, howLong, "km", whatDayArray);
            resultMsg = R.string.goal_created;
        }
        if (result) {
            showToast(resultMsg);
            ((MainActivity)getActivity()).refreshAll();
            finish();
        } else {
            showToast(R.string.common_error);
        }
    }

    private void delete() {
        if (goal == null) {
            return;
        }
        if (dao.deleteGoal(goal.getId())) {
            showToast(R.string.goal_deleted);
            ((MainActivity)getActivity()).refreshAll();
            finish();
        } else {
            showToast(R.string.common_error);
        }
    }

}
