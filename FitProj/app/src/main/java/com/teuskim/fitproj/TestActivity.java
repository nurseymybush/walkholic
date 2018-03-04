package com.teuskim.fitproj;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessStatusCodes;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.data.Subscription;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.request.DataSourcesRequest;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.android.gms.fitness.request.SessionReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;
import com.google.android.gms.fitness.result.DataSourcesResult;
import com.google.android.gms.fitness.result.ListSubscriptionsResult;
import com.google.android.gms.fitness.result.SessionReadResult;
import com.teuskim.fitproj.common.Goal;
import com.teuskim.fitproj.view.CircleGraphView;
import com.teuskim.fitproj.view.CircleStateView;
import com.teuskim.fitproj.view.HorizontalListView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 2015년 2월 오픈 당시의 Test 전용 화면
 * 잊고 지내다가 앱이 실행되지 않는 것을 인지하고 기존 API가 하위호환을 하지 않는다는 것을 확인한 후,
 * 2018년 2월에 업데이트 하면서 Test2Activity를 만들었는데, 기존 코드를 남기고자 삭제하지 않음.
 */
public class TestActivity extends Activity {
    
    private static final String TAG = "WalkHolic";

    private TextView consoleLog;
    private View consoleLogWrapper;
    private Handler handler = new Handler();

    private GoogleApiClient client;
    private boolean authInProgress = false;
    private static final String AUTH_PENDING = "auth_state_pending";
    private static final int REQUEST_OAUTH = 1;
    private OnDataPointListener odpListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_activity);
        consoleLogWrapper = findViewById(R.id.console_log_wrapper);
        consoleLog = (TextView)findViewById(R.id.console_log);

        if (savedInstanceState != null) {
            authInProgress = savedInstanceState.getBoolean(AUTH_PENDING);
        }
        buildFitnessClient();
        initHorizontalListView();
    }

    private void log(String s) {
        log(s, null);
    }
    private void log(final String s, final Throwable e) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                consoleLog.append(s+"\n");
                if (e != null) {
                    Log.e("FitProj", s, e);
                } else {
                    Log.e("FitProj", s);
                }
            }
        });
    }

    /**
     * 콘솔창 닫기/열기
     */
    public void onClickBtnConsole(View arg) {
        if (consoleLog.isShown()) {
            consoleLogWrapper.setVisibility(View.GONE);
            ((Button)arg).setText("콘솔창 열기");
        } else {
            consoleLogWrapper.setVisibility(View.VISIBLE);
            ((Button)arg).setText("콘솔창 닫기");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        log("Connecting...");
        client.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (client.isConnected()) {
            client.disconnect();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_OAUTH) {
            authInProgress = false;
            if (resultCode == RESULT_OK) {
                if (!client.isConnecting() && !client.isConnected()) {
                    client.connect();
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(AUTH_PENDING, authInProgress);
    }

    /**
     * 메인화면으로 이동
     */
    public void onClickMoveToMain(View arg) {
        Intent i = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(i);
    }

    private void buildFitnessClient() {
        client = new GoogleApiClient.Builder(this)
//                .addApi(Fitness.API)
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                .addScope(new Scope(Scopes.FITNESS_BODY_READ_WRITE))
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        log("Connected!");
                        dumpSubscriptionsList();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                            log("Connection lost.  Cause: Network Lost.");
                        } else if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
                            log("Connection lost.  Reason: Service Disconnected");
                        }
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        log("Connection failed. Cause: " + result.toString());
                        if (!result.hasResolution()) {
                            GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), TestActivity.this, 0).show();
                            return;
                        }
                        if (!authInProgress) {
                            try {
                                log("Attempting to resolve failed connection");
                                authInProgress = true;
                                result.startResolutionForResult(TestActivity.this, REQUEST_OAUTH);
                            } catch (IntentSender.SendIntentException e) {
                                log("Exception while starting resolution activity", e);
                            }
                        }
                    }
                })
                .build();
    }

    /**
     * 데이터 불러오기
     */
    public void onClickLoadData(View arg) {
//        new AsyncTask<Void, Void, DataReadResult>() {
//            @Override
//            protected DataReadResult doInBackground(Void... params) {
////                DataReadRequest req = getReqDaily();
//                DataReadRequest req = getReqTodayData();
////                DataReadRequest req = getReqWeightData();
//                return Fitness.HistoryApi.readData(client, req).await(1, TimeUnit.MINUTES);
//            }
//
//            @Override
//            protected void onPostExecute(DataReadResult result) {
////                dumpDataReadResult(result);
//                simulateData2(result);
//            }
//        }.execute();

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
//                Calendar cal = Calendar.getInstance();
//                cal.set(2015, Calendar.FEBRUARY, 11, 0, 0, 0);
//                long startTime = cal.getTimeInMillis();
//                cal.set(2015, Calendar.FEBRUARY, 12, 0, 0, 0);
//                long endTime = cal.getTimeInMillis();
//                DistanceSet resultSet = FitUtil.getDistanceSet(client, startTime, endTime);
//                Log.e(TAG, "walk: "+resultSet.getWalk());

                // 요청 만들기
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                long endTime = cal.getTimeInMillis() + 86400000L;
                long startTime = endTime - (86400000L * 30);

//                DataReadRequest req = new DataReadRequest.Builder()
//                        .aggregate(DataType.TYPE_WEIGHT, DataType.AGGREGATE_WEIGHT_SUMMARY)
//                        .bucketByTime(1, TimeUnit.DAYS)
//                        .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
//                        .build();

                DataReadRequest req = new DataReadRequest.Builder()
                        .read(DataType.TYPE_WEIGHT)
                        .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                        .build();

                // 데이터 가져오기
                DataReadResult result = Fitness.HistoryApi.readData(client, req).await(1, TimeUnit.MINUTES);
                if (result.getStatus().isSuccess() == false) {
                    Log.e(TAG, "error: "+result.getStatus().getStatusMessage());
                    return null;
                }
                SimpleDateFormat dateFormat = new SimpleDateFormat("MM.dd");

                for (Bucket bk : result.getBuckets()) {
                    List<DataSet> list = bk.getDataSets();
                    for (DataSet ds : list) {
                        for (DataPoint dp : ds.getDataPoints()) {
                            if (dp.getDataType().getFields().contains(Field.FIELD_AVERAGE)) {
                                long date = dp.getStartTime(TimeUnit.MILLISECONDS);
                                String dateText = dateFormat.format(date);
                                float weight = dp.getValue(Field.FIELD_AVERAGE).asFloat();
                                Log.e(TAG, "1 dateText:"+dateText+" , weight:"+weight);
                            } else if (dp.getDataType().getFields().contains(Field.FIELD_WEIGHT)) {
                                long date = dp.getStartTime(TimeUnit.MILLISECONDS);
                                String dateText = dateFormat.format(date);
                                float weight = dp.getValue(Field.FIELD_WEIGHT).asFloat();
                                Log.e(TAG, "2 dateText:"+dateText+" , weight:"+weight);
                            }
                        }
                    }
                }

                for (DataSet ds : result.getDataSets()) {
                    Log.e(TAG, "111");
                    for (DataPoint dp : ds.getDataPoints()) {
                        Log.e(TAG, "222: "+dp.getDataType().getFields());
                        if (dp.getDataType().getFields().contains(Field.FIELD_WEIGHT)) {
                            long date = dp.getStartTime(TimeUnit.MILLISECONDS);
                            String dateText = dateFormat.format(date);
                            float weight = dp.getValue(Field.FIELD_WEIGHT).asFloat();
                            Log.e(TAG, "3 dateText:"+dateText+" , weight:"+weight);
                        }
                    }
                }

                return null;
            }
        }.execute();
    }

    private void dumpDataReadResult(DataReadResult result) {
        if (result.getBuckets().size() > 0) {
            log("\nbuckets!");
            for (Bucket bk : result.getBuckets()) {
                List<DataSet> list = bk.getDataSets();
                for (DataSet ds : list) {
                    dumpDataSet(ds);
                }
            }
        }
        if (result.getDataSets().size() > 0) {
            log("\ndata sets!");
            for (DataSet ds : result.getDataSets()) {
                dumpDataSet(ds);
            }
        }
    }

    private void simulateData(DataReadResult result) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

        float walkLow = 50;
        float walkHigh = 120;
        float runLow = 120;
        float runHigh = 400;
        float cycleLow = 400;
        float cycleHigh = 650;
        float carLow = 800;

        List<Long> startStepTimes = new ArrayList<>();
        List<Long> endStepTimes = new ArrayList<>();

        // steps time list 셋팅
        for (Bucket bk : result.getBuckets()) {
            List<DataSet> list = bk.getDataSets();
            for (DataSet ds : list) {
                for (DataPoint dp : ds.getDataPoints()) {
                    if (dp.getDataType().getFields().contains(Field.FIELD_STEPS)
                            && dp.getValue(Field.FIELD_STEPS).asInt() >= walkLow) {
                        startStepTimes.add(dp.getStartTime(TimeUnit.MINUTES));
                        endStepTimes.add(dp.getEndTime(TimeUnit.MINUTES));
                        Log.e(TAG, dp.getValue(Field.FIELD_STEPS)
                                +" ("+dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS))
                                +" ~ "+dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS))+")");
                    }
                }
            }
        }

        float walkDistance = 0;
        float runDistance = 0;
        float cycleDistance = 0;
        float cycleDistancePart = 0;

        int duration;
        long smin, emin;
        float val, avg;
        boolean inCycle = false;
        boolean inCar = false;
        Boolean hasStep;

        // 데이터 산출
        for (Bucket bk : result.getBuckets()) {
            List<DataSet> list = bk.getDataSets();
            for (DataSet ds : list) {
                for (DataPoint dp : ds.getDataPoints()) {
                    /*
                    Log.e(TAG, dp.getValue(Field.FIELD_DISTANCE)
                                    +" ("+dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS))
                                    +" ~ "+dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS))+")");
                    */
                    if (dp.getDataType().getFields().contains(Field.FIELD_DISTANCE) == false) {
                        continue;
                    }
                    smin = dp.getStartTime(TimeUnit.MINUTES);
                    emin = dp.getEndTime(TimeUnit.MINUTES);
                    duration = (int)(emin - smin);
                    val = dp.getValue(Field.FIELD_DISTANCE).asFloat();
                    avg = val / duration;
                    hasStep = null;

                    // 차를 타고 있나?
                    if (avg >= carLow) {
                        inCar = true;
                    } else if (inCar && avg < runHigh) {
                        hasStep = hasStep(smin, emin, startStepTimes, endStepTimes);
                        if (hasStep) {
                            inCar = false;
                        }
                    }
                    // 차 타고 있으면 패스
                    if (inCar) {
                        continue;
                    }
                    // 자전거 타고 있나?
                    if (avg >= cycleLow && avg < cycleHigh) {
                        inCycle = true;
                        cycleDistancePart += val;
                    } else if (inCycle) {
                        inCycle = false;
                        if (avg < walkHigh) {
                            cycleDistance += cycleDistancePart;
                            Log.e(TAG, "cycle data added! "+cycleDistancePart
                                    +" , ("+dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS))
                                    +" ~ "+dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS))+")");
                        } else if (avg >= cycleHigh) {
                            cycleDistancePart = 0;
                        }
                    }
                    // 자전거 타고 있으면 패스
                    if (inCycle) {
                        continue;
                    }
                    // 달리고 있나?
                    if (avg >= runLow && avg < runHigh) {
                        if (hasStep == null) {
                            hasStep = hasStep(smin, emin, startStepTimes, endStepTimes);
                        }
                        if (hasStep) {
                            runDistance += val;
                            Log.e(TAG, "run! duration: "+duration+" , val: "+val
                                    +" , ("+dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS))
                                    +" ~ "+dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS))+")");
                            continue;
                        }
                    }
                    // 걷고 있나?
                    if (avg >= walkLow && avg < walkHigh) {
                        if (hasStep == null) {
                            hasStep = hasStep(smin, emin, startStepTimes, endStepTimes);
                        }
                        if (hasStep) {
                            walkDistance += val;
                            Log.e(TAG, "walk! duration: "+duration+" , val: "+val
                                    +" , ("+dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS))
                                    +" ~ "+dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS))+")");
                        }
                    }
                }
            }
        }

        Log.e(TAG, "walk: "+walkDistance+" , run: "+runDistance+" , cycle: "+cycleDistance);
    }

    private boolean hasStep(long smin, long emin, List<Long> startStepTimes, List<Long> endStepTimes) {
        // smin 직전에 endtime이 있고(즉, starttime이 없고), [smin,emin] 구간에 starttime이 없으면, 해당 구간에 step이 없는 것이다.
        long stime = 0;
        long stime2 = 0;
        for (long t : startStepTimes) {
            if (t > smin) {
                if (t > emin) {
                    break;
                } else {
                    stime2 = t;
                }
            } else {
                stime = t;
            }
        }
        long etime = 0;
        for (long t : endStepTimes) {
            if (t > smin) {
                break;
            } else {
                etime = t;
            }
        }
        if (stime < etime && stime2 == 0) {
            return false;
        }
        return true;
    }

    private void simulateData2(DataReadResult result) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

        // 스텝수 범위
        int walkStepLow = 10;
        int walkStepHigh = 70;

        // 거리 범위
        float walkDistHigh = 100;
        float runDistHigh = 400;
        float cycleDistLow = 300;
        float cycleDistHigh = 650;
        float carDistLow = 800;

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 11); // TODO: for test
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        long todayStart = cal.getTimeInMillis();

        int[] stepPerMin = new int[1440];  // 분당스텝수
        float[] distPerMin = new float[1440];  // 분당이동거리
        boolean[] carMap = new boolean[1440];  // 각 분별 차량탑승여부
        List<Integer> carPosList = new ArrayList<>();  // 차 탑승 확신할 수 있는 시간대
        boolean[] checkedCar = new boolean[1440];  // 차 탑승 과련 체크된 내용

        // 분당스텝수, 분당이동거리, 차탑승시간대 셋팅
        for (Bucket bk : result.getBuckets()) {
            List<DataSet> list = bk.getDataSets();
            for (DataSet ds : list) {
                for (DataPoint dp : ds.getDataPoints()) {
                    int smin = getMin(dp.getStartTime(TimeUnit.MILLISECONDS), todayStart);
                    int emin = getMin(dp.getEndTime(TimeUnit.MILLISECONDS), todayStart);
                    int diff = emin - smin + 1;
                    for (int i=smin; i<=emin; i++) {
                        if (dp.getDataType().getFields().contains(Field.FIELD_STEPS)) {
                            stepPerMin[i] = dp.getValue(Field.FIELD_STEPS).asInt() / diff;
                        } else if (dp.getDataType().getFields().contains(Field.FIELD_DISTANCE)) {
                            distPerMin[i] = dp.getValue(Field.FIELD_DISTANCE).asFloat() / diff;
                            if (distPerMin[i] > carDistLow) {
                                carPosList.add(i);
                            }
                        }
                    }
                }
            }
        }

        // 각 분별 차량탑승여부 셋팅
        for (int pos : carPosList) {
            carMap[pos] = true;
            checkedCar[pos] = true;
            int i = pos - 1;
            while (i >= 0) {
                if (stepPerMin[i] < walkStepLow && checkedCar[i] == false) {
                    carMap[i] = true;
                    checkedCar[i] = true;
                } else {
                    if (carMap[i+1] == true && distPerMin[i] == distPerMin[i+1]) {
                        carMap[i] = true;
                    }
                    checkedCar[i] = true;
                    break;
                }
                i--;
            }
            i = pos + 1;
            while (i < 1440) {
                if (stepPerMin[i] < walkStepLow && checkedCar[i] == false) {
                    carMap[i] = true;
                    checkedCar[i] = true;
                } else {
                    if (carMap[i-1] == true && distPerMin[i] == distPerMin[i-1]) {
                        carMap[i] = true;
                    }
                    checkedCar[i] = true;
                    break;
                }
                i++;
            }
        }

        float walkDist = 0;
        float runDist = 0;
        float cycleDist = 0;

        int step;
        float dist;

        for (int i=0; i<1440; i++) {
            step = stepPerMin[i];
            dist = distPerMin[i];
            if (step >= walkStepLow && dist < runDistHigh) {
                if (step < walkStepHigh && dist < walkDistHigh) {
                    walkDist += dist;
                } else {
                    runDist += dist;
                }
            } else if (carMap[i] == false && dist >= cycleDistLow && dist < cycleDistHigh) {
                cycleDist += dist;
            }

            if (step > 0) {
                Log.e(TAG, "step: "+getHourMinStr(i)+", "+step);
            }
            if (dist > 0) {
                Log.e(TAG, "dist: "+getHourMinStr(i)+", "+dist);
            }
            if (carMap[i]) {
                Log.e(TAG, "in car: "+getHourMinStr(i));
            }
        }

        log("walk: "+walkDist+" , run: "+runDist+" , cycle: "+cycleDist);
    }

    private String getHourMinStr(int m) {
        int hh = m / 60;
        int mm = m % 60;
        return hh+":"+mm;
    }

    private int getMin(long time, long todayStart) {
        int min = (int)((time-todayStart) / 60000);
        if (min < 0) {
            return 0;
        } else if (min >= 1440) {
            return 1439;
        }
        return min;
    }

    /**
     * 일간 데이터 (걸음수, 거리)
     * 2014년 12월 1일부터 7일까지
     */
    private DataReadRequest getReqDaily() {
        Calendar cal = Calendar.getInstance();
        cal.set(2014, Calendar.DECEMBER, 1);
        long startTime = cal.getTimeInMillis();
        cal.add(Calendar.WEEK_OF_YEAR, 1);
        long endTime = cal.getTimeInMillis();

        return new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * 오늘 운동량
     */
    private DataReadRequest getReqTodayData() {
        Calendar cal = Calendar.getInstance();

//        long endTime = System.currentTimeMillis();
//        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
//        long startTime = cal.getTimeInMillis();

        cal.set(2015, Calendar.FEBRUARY, 11, 0, 0, 0);
        long startTime = cal.getTimeInMillis();
        cal.set(2015, Calendar.FEBRUARY, 12, 0, 0, 0);
        long endTime = cal.getTimeInMillis();

        return new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
//                .aggregate(DataType.TYPE_SPEED, DataType.AGGREGATE_SPEED_SUMMARY)
//                .aggregate(DataType.TYPE_ACTIVITY_SEGMENT, DataType.AGGREGATE_ACTIVITY_SUMMARY)
                .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
//                .aggregate(DataType.TYPE_LOCATION_SAMPLE, DataType.AGGREGATE_LOCATION_BOUNDING_BOX)
                .bucketByTime(1, TimeUnit.MINUTES)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();
    }

    private DataReadRequest getReqWeightData() {
        long endTime = System.currentTimeMillis();
        long startTime = endTime - (86400000L * 10);

        return new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_WEIGHT, DataType.AGGREGATE_WEIGHT_SUMMARY)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();
    }

    private void readSession() {
        Log.e(TAG, "readSession() called!");
//        long DAY_IN_MS = TimeUnit.DAYS.toMillis(7);
//        Date now = new Date();
//        // Set a range of the day, using a start time of 7 days before this moment.
//        long endTime = now.getTime();
//        long startTime = endTime - DAY_IN_MS;

        Calendar cal = Calendar.getInstance();
        cal.set(2014, Calendar.DECEMBER, 3, 9, 8, 35);
        long startTime = cal.getTimeInMillis();
        cal.set(2014, Calendar.DECEMBER, 3, 9, 12, 45);
        long endTime = cal.getTimeInMillis();

        SessionReadRequest request = new SessionReadRequest.Builder()
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
//                .read(dataTypeJumpHeight)
//                .read(dataTypeHustle)
//                .read(dataTypeQuickness)
                .read(DataType.TYPE_HEART_RATE_BPM)
                .read(DataType.TYPE_DISTANCE_DELTA)
                .read(DataType.TYPE_SPEED)
                .read(DataType.TYPE_STEP_COUNT_DELTA)
                .readSessionsFromAllApps()
                .build();

        PendingResult<SessionReadResult> pendingResult =
                Fitness.SessionsApi.readSession(client, request);


        // 3. Check the result
        pendingResult.setResultCallback(
                new ResultCallback<SessionReadResult>() {
                    @Override
                    public void onResult(SessionReadResult sessionReadResult) {
                        // Get a list of sessions that match the criteria
                        Log.e(TAG, "Sessions found: " + sessionReadResult.getSessions().size());

                        List<Session> mSessions = sessionReadResult.getSessions();
//                        mDemoApplication.setSessions(mSessions);
                        for (Session session : mSessions) {
                            String sessionName = session.getName();
                            Log.d(TAG, "Session: " + sessionName);

                            // Get the currentSessionDataSets for the time interval of this session
                            List<DataSet> currentSessionDataSets = sessionReadResult.getDataSet(session);
//                            mSessionDataSets.put(session.getIdentifier(), currentSessionDataSets);
                        }
//                        mDemoApplication.setSessionDataSets(mSessionDataSets);
                        Log.d(TAG, "The number of sessions is: " + mSessions.size());
//                        mSessionAdapter.setSessions(mSessions);
//                        mSessionAdapter.notifyDataSetChanged();
                    }
                }
        );
    }

    private void dumpDataSet(DataSet dataSet) {
        log("Data returned for Data type: " + dataSet.getDataType().getName());
//        log(dataSet.toString());

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

        for (DataPoint dp : dataSet.getDataPoints()) {
            log("Data point:");
            log("\tType: " + dp.getDataType().getName());
            log("\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
            log("\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));
            for(Field field : dp.getDataType().getFields()) {
                log("\tField: " + field.getName() + " Value: " + dp.getValue(field));
            }
        }
    }

    /**
     * 데이터 기록 시작 및 종료
     */
    public void onClickRecordData(View arg) {
        subscribe();
    }

    private void dumpSubscriptionsList() {
        Fitness.RecordingApi.listSubscriptions(client, DataType.TYPE_STEP_COUNT_DELTA).setResultCallback(new ResultCallback<ListSubscriptionsResult>() {
            @Override
            public void onResult(ListSubscriptionsResult listSubscriptionsResult) {
                for (Subscription sc : listSubscriptionsResult.getSubscriptions()) {
                    DataType dt = sc.getDataType();
                    log("Active subscription for data type: " + dt.getName());
                }
            }
        });
    }

    private void subscribe() {
        Fitness.RecordingApi.subscribe(client, DataType.TYPE_STEP_COUNT_DELTA).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) {
                    if (status.getStatusCode() == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
                        log("Existing subscription for activity detected. Unsubscribe..");
                        unsubscribe();
                    } else {
                        log("Successfully subscribed!");
                    }
                } else {
                    log("There was a problem subscribing.");
                }
            }
        });
    }

    private void unsubscribe() {
        final String dataTypeStr = DataType.TYPE_STEP_COUNT_DELTA.toString();
        log("Unsubscribing from data type: " + dataTypeStr);

        Fitness.RecordingApi.unsubscribe(client, DataType.TYPE_STEP_COUNT_DELTA).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) {
                    log("Successfully unsubscribed for data type: " + dataTypeStr);
                } else {
                    log("Failed to unsubscribe for data type: " + dataTypeStr);
                }
            }
        });
    }

    /**
     * 실시간 데이터 확인
     */
    public void onClickSensors(View arg) {
        findFitnessDataSources();
    }

    private void findFitnessDataSources() {
        final DataType dataType = DataType.TYPE_STEP_COUNT_CUMULATIVE;
        Fitness.SensorsApi.findDataSources(client, new DataSourcesRequest.Builder()
                .setDataTypes(dataType)
                .setDataSourceTypes(DataSource.TYPE_RAW)
                .build())
                .setResultCallback(new ResultCallback<DataSourcesResult>() {
                    @Override
                    public void onResult(DataSourcesResult dataSourcesResult) {
                        log("findDataSources result: " + dataSourcesResult.getStatus().toString());
                        for (DataSource dataSource : dataSourcesResult.getDataSources()) {
                            log("Data source found: " + dataSource.toString());
                            log("Data Source type: " + dataSource.getDataType().getName());

                            if (dataSource.getDataType().equals(dataType)) {
                                if (odpListener == null) {
                                    log("Data source registering..");
                                    registerFitnessDataListener(dataSource, dataType);
                                } else {
                                    log("Data source unregistering..");
                                    unregisterFitnessDataListener();
                                }
                            }
                        }
                    }
                });
    }

    private void registerFitnessDataListener(DataSource dataSource, DataType dataType) {
        odpListener = new OnDataPointListener() {
            @Override
            public void onDataPoint(DataPoint dataPoint) {
                for (Field field : dataPoint.getDataType().getFields()) {
                    Value val = dataPoint.getValue(field);
                    log("Detected DataPoint field: " + field.getName());
                    log("Detected DataPoint value: " + val);
                }
            }
        };

        Fitness.SensorsApi.add(
                client,
                new SensorRequest.Builder()
                        .setDataSource(dataSource)
                        .setDataType(dataType) // Can't be omitted.
                        .setSamplingRate(5, TimeUnit.SECONDS)
                        .build(),
                odpListener)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            log("odpListener registered!");
                        } else {
                            log("odpListener not registered.");
                        }
                    }
                });
    }

    private void unregisterFitnessDataListener() {
        if (odpListener == null) {
            return;
        }

        Fitness.SensorsApi.remove(
                client,
                odpListener)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            log("odpListener was removed!");
                        } else {
                            log("odpListener was not removed.");
                        }
                    }
                });
    }

    /**
     * 원형 그래프뷰 클릭했을때
     */
    public void onClickCircleGraphView(View arg) {
        CircleGraphView v = (CircleGraphView)arg;
        v.setFactors(0.5f, 0.1f, 0.02f, 0xffeeeeee, 0xffeeeeee, 20);
        int pos = v.addGraph(new int[]{Color.RED, Color.YELLOW, Color.RED}, 100);
        v.setValue(pos, 200, true);
        pos = v.addGraph(new int[]{Color.BLUE, Color.GREEN, Color.RED, Color.BLUE}, 100);
        v.setValue(pos, 30, true);

        log("onClickCircleGraphView!");
    }

    /**
     * 횡스크롤 리스트뷰 테스트
     */
    private void initHorizontalListView() {
        HorizontalListView listView = (HorizontalListView)findViewById(R.id.hlist);
        BaseAdapter adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return 10;
            }

            @Override
            public Object getItem(int position) {
                return null;
            }

            @Override
            public long getItemId(int position) {
                return 0;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.history_list_item, parent, false);
                }
                CircleStateView csv = (CircleStateView)convertView.findViewById(R.id.circle_state);
                csv.setColor(Goal.getColor(Goal.Type.WALK));
                csv.setFilledRate(0.5f);
                ((TextView)convertView.findViewById(R.id.date)).setText("1." + (position + 1));
                return convertView;
            }
        };
        listView.setAdapter(adapter);
    }

    /**
     * 데이터 저장하기(History API)
     */
    public void onClickSaveData(View arg) {
        new AsyncTask<Void, Void, com.google.android.gms.common.api.Status>() {
            @Override
            protected com.google.android.gms.common.api.Status doInBackground(Void... params) {
                DataSource dataSource = new DataSource.Builder()
                        .setAppPackageName(getApplicationContext())
                        .setDataType(DataType.TYPE_WEIGHT)
                        .setName("fitproj-weight")
                        .setType(DataSource.TYPE_RAW)
                        .build();

                DataSet dataSet = DataSet.create(dataSource);

                long time = System.currentTimeMillis() - (86400000L * 4);
                long startTime = time;
                long endTime = startTime + 1000;

                DataPoint dataPoint = dataSet.createDataPoint()
//                        .setTimestamp(time, TimeUnit.MICROSECONDS);
                        .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS);
                dataPoint.getValue(Field.FIELD_WEIGHT).setFloat(65f);
                dataSet.add(dataPoint);

                com.google.android.gms.common.api.Status insertStatus
                        = Fitness.HistoryApi.insertData(client, dataSet).await(1, TimeUnit.MINUTES);

                return insertStatus;
            }

            @Override
            protected void onPostExecute(com.google.android.gms.common.api.Status status) {
                if (status.isSuccess()) {
                    log("Data insert was successful!");
                } else {
                    log("There was a problem inserting the dataset.");
                    Log.e(TAG, status.getStatusCode()+": "+status.getStatusMessage());
                }
            }
        }.execute();
    }

    public void onClickNotification(View arg) {
        Intent i = new Intent(getApplicationContext(), AlarmReceiver.class);
        sendBroadcast(i);
    }

}
