package com.teuskim.fitproj;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
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
import com.teuskim.fitproj.view.CircleGraphView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Test 전용 화면
 * 2015년 2월에 오픈한 후 잊고 지내다가 앱이 실행되지 않는 것을 인지하고,
 * 기존 API가 하위호환을 하지 않는다는 것을 확인한 후,
 * 업데이트 하면서 TestActivity를 대체하면서 새로 만들었음.
 */
public class Test2Activity extends Activity {

    public static final String TAG = "WalkHolic";
    private static final int REQUEST_OAUTH_REQUEST_CODE = 1;
    private static final int REQUEST_FITNESS_PERMISSIONS = 2;

    private TextView consoleLog;
    private View consoleLogWrapper;
    private Handler handler = new Handler();
    private boolean isSubscribed = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_activity);
        consoleLogWrapper = findViewById(R.id.console_log_wrapper);
        consoleLog = findViewById(R.id.console_log);

        FitnessOptions fitnessOptions =
                FitnessOptions.builder()
                        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE)
                        .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE)
                        .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_WRITE)
                        .addDataType(DataType.AGGREGATE_DISTANCE_DELTA, FitnessOptions.ACCESS_WRITE)
                        .addDataType(DataType.TYPE_WEIGHT)
                        .build();
        if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                this,
                REQUEST_OAUTH_REQUEST_CODE,
                GoogleSignIn.getLastSignedInAccount(this),
                fitnessOptions);
        } else {
            checkAndRequestPermissions();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_OAUTH_REQUEST_CODE) {
                log("on activity result! request oauth request code!");
                checkAndRequestPermissions();
            }
        }
    }

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FITNESS_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_FITNESS_PERMISSIONS) {
            log("on request permissions result for request fitness permissions!!");
        }
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

    private void log(String s) {
        log(s, null);
    }
    private void log(final String s, final Throwable e) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                consoleLog.append(s+"\n");
                if (e != null) {
                    Log.e(TAG, s, e);
                } else {
                    Log.e(TAG, s);
                }
            }
        });
    }

    /**
     * 메인화면으로 이동
     */
    public void onClickMoveToMain(View arg) {
        Intent i = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(i);
    }

    /**
     * 데이터 불러오기
     */
    public void onClickLoadData(View arg) {
        loadTodaySteps();
    }

    private void loadDailySteps() {
        Calendar cal = Calendar.getInstance();
        cal.set(2018, Calendar.JANUARY, 20);
        long startTime = cal.getTimeInMillis();
        cal.add(Calendar.WEEK_OF_YEAR, 1);
        long endTime = cal.getTimeInMillis();

        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();

        loadHistoryData(readRequest);
    }

    private void loadTodaySteps() {
        Calendar cal = Calendar.getInstance();

//        long endTime = System.currentTimeMillis();
//        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
//        long startTime = cal.getTimeInMillis();

        cal.set(2018, Calendar.FEBRUARY, 7, 0, 0, 0);
        long startTime = cal.getTimeInMillis();
        cal.set(2018, Calendar.FEBRUARY, 8, 0, 0, 0);
        long endTime = cal.getTimeInMillis();

        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
//                .aggregate(DataType.TYPE_SPEED, DataType.AGGREGATE_SPEED_SUMMARY)
//                .aggregate(DataType.TYPE_ACTIVITY_SEGMENT, DataType.AGGREGATE_ACTIVITY_SUMMARY)
                .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
//                .aggregate(DataType.TYPE_LOCATION_SAMPLE, DataType.AGGREGATE_LOCATION_BOUNDING_BOX)
                .bucketByTime(1, TimeUnit.MINUTES)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();

        loadHistoryData(readRequest);
    }

    private void loadAggregateWeights() {
        long endTime = System.currentTimeMillis();
        long startTime = endTime - (86400000L * 10);

        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_WEIGHT, DataType.AGGREGATE_WEIGHT_SUMMARY)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();

        loadHistoryData(readRequest);
    }

    private void loadHistoryData(DataReadRequest readRequest) {
        Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .readData(readRequest)
                .addOnSuccessListener(
                        new OnSuccessListener<DataReadResponse>() {
                            @Override
                            public void onSuccess(DataReadResponse dataReadResponse) {
                                log("read data!!!");
                                dumpDataReadResult(dataReadResponse);
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                log("There was a problem reading the data.", e);
                            }
                        });
    }

    private void dumpDataReadResult(DataReadResponse result) {
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

    private void simulateData(DataReadResponse result) {
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

    private void simulateData2(DataReadResponse result) {
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

    private void loadWeights() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        long endTime = cal.getTimeInMillis() + 86400000L;
        long startTime = endTime - (86400000L * 30);

        DataReadRequest readRequest = new DataReadRequest.Builder()
                .read(DataType.TYPE_WEIGHT)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();

        Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .readData(readRequest)
                .addOnSuccessListener(
                        new OnSuccessListener<DataReadResponse>() {
                            @Override
                            public void onSuccess(DataReadResponse dataReadResponse) {
                                log("read data!!!");
                                SimpleDateFormat dateFormat = new SimpleDateFormat("MM.dd");

                                for (Bucket bk : dataReadResponse.getBuckets()) {
                                    List<DataSet> list = bk.getDataSets();
                                    for (DataSet ds : list) {
                                        for (DataPoint dp : ds.getDataPoints()) {
                                            if (dp.getDataType().getFields().contains(Field.FIELD_AVERAGE)) {
                                                long date = dp.getStartTime(TimeUnit.MILLISECONDS);
                                                String dateText = dateFormat.format(date);
                                                float weight = dp.getValue(Field.FIELD_AVERAGE).asFloat();
                                                log("1 dateText:"+dateText+" , weight:"+weight);
                                            } else if (dp.getDataType().getFields().contains(Field.FIELD_WEIGHT)) {
                                                long date = dp.getStartTime(TimeUnit.MILLISECONDS);
                                                String dateText = dateFormat.format(date);
                                                float weight = dp.getValue(Field.FIELD_WEIGHT).asFloat();
                                                log("2 dateText:"+dateText+" , weight:"+weight);
                                            }
                                        }
                                    }
                                }

                                for (DataSet ds : dataReadResponse.getDataSets()) {
                                    for (DataPoint dp : ds.getDataPoints()) {
                                        log("222: "+dp.getDataType().getFields());
                                        if (dp.getDataType().getFields().contains(Field.FIELD_WEIGHT)) {
                                            long date = dp.getStartTime(TimeUnit.MILLISECONDS);
                                            String dateText = dateFormat.format(date);
                                            float weight = dp.getValue(Field.FIELD_WEIGHT).asFloat();
                                            log("3 dateText:"+dateText+" , weight:"+weight);
                                        }
                                    }
                                }
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                log("There was a problem reading the data.", e);
                            }
                        });
    }

    /**
     * 데이터 저장하기(History API)
     */
    public void onClickSaveData(View arg) {
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
//                .setTimestamp(time, TimeUnit.MICROSECONDS);
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS);
        dataPoint.getValue(Field.FIELD_WEIGHT).setFloat(65f);
        dataSet.add(dataPoint);

        Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .insertData(dataSet)
                .addOnCompleteListener(
                        new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    // At this point, the data has been inserted and can be read.
                                    log("Data insert was successful!");
                                } else {
                                    log("There was a problem inserting the dataset.", task.getException());
                                }
                            }
                        });
    }

    /**
     * 데이터 기록 시작 및 종료
     */
    public void onClickRecordData(View arg) {
        if (isSubscribed) {
            unsubscribe();
        } else {
            subscribe();
        }
    }

    public void subscribe() {
        Fitness.getRecordingClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .subscribe(DataType.TYPE_ACTIVITY_SAMPLES)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        log("Successfully subscribed!");
                        isSubscribed = true;
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        log("There was a problem subscribing.");
                    }
                });
    }

    private void unsubscribe() {
        final String dataTypeStr = DataType.TYPE_ACTIVITY_SAMPLES.toString();
        log("Unsubscribing from data type: " + dataTypeStr);

        Fitness.getRecordingClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .unsubscribe(DataType.TYPE_ACTIVITY_SAMPLES)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        log("Successfully unsubscribed for data type: " + dataTypeStr);
                        isSubscribed = false;
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Subscription not removed
                        log("Failed to unsubscribe for data type: " + dataTypeStr);
                    }
                });
    }

    /**
     * 실시간 데이터 확인
     */
    public void onClickSensors(View arg) {
        // 필요하면 추가하자.
    }

    public void onClickNotification(View arg) {
        Intent i = new Intent(getApplicationContext(), AlarmReceiver.class);
        sendBroadcast(i);
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
}
