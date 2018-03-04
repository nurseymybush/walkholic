package com.teuskim.fitproj;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.teuskim.fitproj.common.FitPreference;
import com.teuskim.fitproj.common.FitUtil;
import com.teuskim.fitproj.view.FitViewPager;
import com.teuskim.fitproj.view.HorizontalListView;
import com.teuskim.fitproj.view.WeightItemView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * 메인탭의 데이터 프레그먼트
 * 체중 등 사용자 정보 입력
 */
public class MainDataFragment extends BaseFragment {

    private static final String TAG = "WalkHolic";
    private EditText lastWeightView;
    private TextView lastWeightUnitView;
    private View btnModifyWrapper;
    private TextView lastDateView;
    private HorizontalListView weightListView;
    private View enterWeightMsgView;

    private WeightAdapter adapter;
    private Weight lastWeight;
    private FitPreference pref;
    private boolean isClickedWeight;
    private boolean isConvertUnitOn;

    public static MainDataFragment newInstance() {
        return new MainDataFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pref = FitPreference.getInstance(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        isClickedWeight = false;
        isConvertUnitOn = pref.isConvertUnitOn();
        View v = inflater.inflate(R.layout.main_data_fragment, container, false);
        initViews(v);
        loadData();
        ((MainActivity)getActivity()).setViewPagerOnChildMoveListener(new FitViewPager.OnChildMoveListener() {
            @Override
            public boolean onMove(float diffX) {
                int x = weightListView.getCurrentX();
                return !(x == 0 && diffX > 0);
            }
        });
        return v;
    }

    private void initViews(View v) {
        lastWeightView = v.findViewById(R.id.last_weight);
        lastWeightView.addTextChangedListener(new TextWatcher() {

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!isClickedWeight) {
                    return;
                }
                String wstr = s.toString().trim();
                try {
                    float w = Float.parseFloat(wstr);
                    if ((lastWeight == null || (w > 0 && w != lastWeight.weight))
                            && btnModifyWrapper.getVisibility() != View.VISIBLE) {
                        Animation anim = AnimationUtils.loadAnimation(getActivity(), R.anim.right_in);
                        btnModifyWrapper.startAnimation(anim);
                        btnModifyWrapper.setVisibility(View.VISIBLE);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "weight view text changed error", e);
                }
            }
        });
        lastWeightView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    isClickedWeight = true;
                }
            }
        });

        lastWeightUnitView = v.findViewById(R.id.last_weight_unit);

        View.OnClickListener l = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.btn_modify:
                        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                        }
                        saveWeight();
                        break;
                }
            }
        };
        btnModifyWrapper = v.findViewById(R.id.btn_modify_wrapper);
        v.findViewById(R.id.btn_modify).setOnClickListener(l);
        lastDateView = v.findViewById(R.id.last_date);
        weightListView = v.findViewById(R.id.weight_list);
        enterWeightMsgView = v.findViewById(R.id.enter_weight_img);
        adapter = new WeightAdapter();
        weightListView.setAdapter(adapter);

        Switch switchRecordingView = v.findViewById(R.id.switch_recording);
        switchRecordingView.setChecked(pref.isRecordingOn());
        switchRecordingView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                pref.setRecordingOn(isChecked);
                ((MainActivity)getActivity()).subscribeOrCancel();
            }
        });

        Switch switchAlertView = v.findViewById(R.id.switch_alert);
        switchAlertView.setChecked(pref.isAlarmOn());
        switchAlertView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                pref.setAlarmOn(isChecked);
                ((MainActivity)getActivity()).initAlarm();
            }
        });

        Switch switchConvertUnitView = v.findViewById(R.id.switch_convert_unit);
        switchConvertUnitView.setChecked(pref.isConvertUnitOn());
        switchConvertUnitView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                pref.setConvertUnitOn(isChecked);
                isConvertUnitOn = isChecked;
                loadData();
            }
        });
    }

    private void loadData() {
        if (isConvertUnitOn) {
            lastWeightUnitView.setText("lbs");
        } else {
            lastWeightUnitView.setText("kg");
        }

        // 요청 만들기
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        long endTime = cal.getTimeInMillis() + 86400000L;
        long startTime = endTime - (86400000L * 365);
        DataReadRequest req = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_WEIGHT, DataType.AGGREGATE_WEIGHT_SUMMARY)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();

        // 데이터 가져오기
        Fitness.getHistoryClient(getContext(), GoogleSignIn.getLastSignedInAccount(getContext()))
                .readData(req)
                .addOnSuccessListener(
                        new OnSuccessListener<DataReadResponse>() {
                            @SuppressLint("DefaultLocale")
                            @Override
                            public void onSuccess(DataReadResponse dataReadResponse) {
                                List<Weight> weightList = new ArrayList<>();
                                @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("MM.dd");
                                for (Bucket bk : dataReadResponse.getBuckets()) {
                                    List<DataSet> list = bk.getDataSets();
                                    for (DataSet ds : list) {
                                        for (DataPoint dp : ds.getDataPoints()) {
                                            if (dp.getDataType().getFields().contains(Field.FIELD_AVERAGE)) {
                                                long date = dp.getStartTime(TimeUnit.MILLISECONDS);
                                                String dateText = dateFormat.format(date);
                                                float weight = getConvertedWeight(dp.getValue(Field.FIELD_AVERAGE).asFloat());
                                                weightList.add(new Weight(weight, date, dateText));
                                            }
                                        }
                                    }
                                }
                                List<Weight> reverseWeightList = new ArrayList<>();
                                for (int i=weightList.size()-1; i>=0; i--) {
                                    reverseWeightList.add(weightList.get(i));
                                }
                                if (reverseWeightList.size() > 0) {
                                    Calendar cal = Calendar.getInstance();
                                    lastWeight = reverseWeightList.get(0);
                                    lastWeightView.setText(String.format("%.1f", lastWeight.weight));
                                    cal.setTimeInMillis(lastWeight.date);
                                    lastDateView.setText(DateFormat.getDateInstance(DateFormat.FULL).format(cal.getTime()));
                                    enterWeightMsgView.setVisibility(View.GONE);

                                    adapter.setList(reverseWeightList);
                                    adapter.notifyDataSetChanged();
                                } else {  // 몸무게 데이터가 없을때
                                    lastWeightView.setText("0.0");
                                    lastDateView.setText(R.string.enter_weight);
                                    enterWeightMsgView.setVisibility(View.VISIBLE);
                                }
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // There was a problem reading the data.
                            }
                        });
    }

    private float getConvertedWeight(float weightKg) {
        if (isConvertUnitOn) {
            return (float)(weightKg * 2.20462262);  // convert from kg to lbs
        }
        return weightKg;
    }

    private void saveWeight() {
        float w = 0;
        try {
            w = Float.parseFloat(lastWeightView.getText().toString().trim());
        } catch (Exception e) {
            Log.e(TAG, "save weight error", e);
        }

        if (w <= 0) {
            showToast(R.string.wrong_number);
            return;
        }

        if (isConvertUnitOn) {
            w = (float)(w * 0.45359237);  // convert from lbs to kg
        }

        DataSource dataSource = new DataSource.Builder()
                .setAppPackageName(getActivity())
                .setDataType(DataType.TYPE_WEIGHT)
                .setName("fitproj-weight")
                .setType(DataSource.TYPE_RAW)
                .build();

        DataSet dataSet = DataSet.create(dataSource);

        Calendar cal = Calendar.getInstance();
        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        long startTime = cal.getTimeInMillis();
        long endTime = startTime + 86400000L - 3600000L; // 0시에서 23시까지

        DataPoint dataPoint = dataSet.createDataPoint()
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS);
        dataPoint.getValue(Field.FIELD_WEIGHT).setFloat(w);
        dataSet.add(dataPoint);

        Fitness.getHistoryClient(getContext(), GoogleSignIn.getLastSignedInAccount(getContext()))
                .insertData(dataSet)
                .addOnCompleteListener(
                        new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    showToast(R.string.save_success);
                                    btnModifyWrapper.setVisibility(View.GONE);
                                    loadData();
                                } else {
                                    // There was a problem inserting the dataset.
                                    showToast(R.string.common_error);
                                    Log.e(TAG, "save weight error", task.getException());
                                }
                            }
                        });
    }


    class Weight {
        long date;
        String dateText;
        float weight;

        Weight(float weight, long date, String dateText) {
            this.date = date;
            this.dateText = dateText;
            this.weight = weight;
        }
    }

    class WeightAdapter extends BaseAdapter {

        private List<Weight> list = new ArrayList<>();
        private float maxWeight, minWeight;
        private int itemWidth;

        public void setList(List<Weight> list) {
            this.list = list;
            maxWeight = 0;
            minWeight = 1000;
            for (Weight w : list) {
                if (w.weight > maxWeight) {
                    maxWeight = w.weight;
                }
                if (w.weight < minWeight) {
                    minWeight = w.weight;
                }
            }
            if (list.size() < 6) {
                itemWidth = FitUtil.getScreenWidth(getActivity()) / list.size();
            } else {
                itemWidth = FitUtil.getScreenWidth(getActivity()) / 6;
            }
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Weight getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            WeightItemView v;
            if (convertView == null) {
                v = new WeightItemView(getActivity());
                HorizontalListView.LayoutParams lp = new HorizontalListView.LayoutParams(itemWidth, HorizontalListView.LayoutParams.MATCH_PARENT);
                v.setLayoutParams(lp);
                v.setRange(maxWeight, minWeight);
            } else {
                v = (WeightItemView)convertView;
            }

            Weight w = getItem(position);
            v.setDateText(w.dateText);
            v.setWeight(w.weight, isConvertUnitOn);
            if (position > 0) {
                Weight prev = getItem(position-1);
                v.setPrevWeight(prev.weight);
            } else {
                v.setPrevWeight(0);
            }
            if (position < getCount()-1) {
                Weight next = getItem(position+1);
                v.setNextWeight(next.weight);
            } else {
                v.setNextWeight(0);
            }

            return v;
        }
    }

}
