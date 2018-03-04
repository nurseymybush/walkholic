package com.teuskim.fitproj.common;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * static 메소드들
 */
public class FitUtil {

    /**
     * DP 값을 PX 값으로 변환
     */
    private static float dpToPxRatio = -1;
    public static int convertDpToPx(float dp, Resources r) {
        if (dpToPxRatio < 0) {
            dpToPxRatio = r.getDisplayMetrics().density;
        }
        return Math.round(dp * dpToPxRatio);
    }

    /**
     * 화면의 너비 구하기
     */
    private static int screenWidth = 0;
    public static int getScreenWidth(Activity activity){
        if(screenWidth == 0){
            DisplayMetrics dm = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
            screenWidth = dm.widthPixels;
        }
        return screenWidth;
    }

    public interface RecvDistanceSet {
        void onRecvDistanceSet(DistanceSet distanceSet);
    }

    /**
     * 구글핏 history api에서 거리 데이터 셋 가져오기
     * @param startTime 오늘의 0시
     * @param endTime 다음날 0시
     */
    public static void getDistanceSet(Context context, final long startTime, long endTime, final RecvDistanceSet recvDistanceSet) {
        if (context == null) {
            return;
        }
        // 요청 만들기
        DataReadRequest req = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
                .bucketByTime(1, TimeUnit.MINUTES)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();

        Fitness.getHistoryClient(context, GoogleSignIn.getLastSignedInAccount(context))
                .readData(req)
                .addOnSuccessListener(
                        new OnSuccessListener<DataReadResponse>() {
                            @Override
                            public void onSuccess(DataReadResponse dataReadResponse) {
                                // 스텝수 범위
                                int walkStepLow = 10;
                                int walkStepHigh = 70;

                                // 거리 범위
                                float walkDistHigh = 100;
                                float runDistHigh = 400;
                                float cycleDistLow = 300;
                                float cycleDistHigh = 650;
                                float carDistLow = 800;

                                int[] stepPerMin = new int[1440];  // 분당스텝수
                                float[] distPerMin = new float[1440];  // 분당이동거리
                                boolean[] carMap = new boolean[1440];  // 각 분별 차량탑승여부
                                List<Integer> carPosList = new ArrayList<>();  // 차 탑승 확신할 수 있는 시간대
                                boolean[] checkedCar = new boolean[1440];  // 차 탑승 과련 체크된 내용

                                // 분당스텝수, 분당이동거리, 차탑승시간대 셋팅
                                for (Bucket bk : dataReadResponse.getBuckets()) {
                                    List<DataSet> list = bk.getDataSets();
                                    for (DataSet ds : list) {
                                        for (DataPoint dp : ds.getDataPoints()) {
                                            int smin = getMin(dp.getStartTime(TimeUnit.MILLISECONDS), startTime);
                                            int emin = getMin(dp.getEndTime(TimeUnit.MILLISECONDS), startTime);
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
                                        if (stepPerMin[i] < walkStepLow && !checkedCar[i]) {
                                            carMap[i] = true;
                                            checkedCar[i] = true;
                                        } else {
                                            if (carMap[i + 1] && distPerMin[i] == distPerMin[i+1]) {
                                                carMap[i] = true;
                                            }
                                            checkedCar[i] = true;
                                            break;
                                        }
                                        i--;
                                    }
                                    i = pos + 1;
                                    while (i < 1440) {
                                        if (stepPerMin[i] < walkStepLow && !checkedCar[i]) {
                                            carMap[i] = true;
                                            checkedCar[i] = true;
                                        } else {
                                            if (carMap[i - 1] && distPerMin[i] == distPerMin[i-1]) {
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
                                    } else if (!carMap[i] && dist >= cycleDistLow && dist < cycleDistHigh) {
                                        cycleDist += dist;
                                    }
                                }

                                DistanceSet resultSet = new DistanceSet();
                                resultSet.setWalk(walkDist / 1000);
                                resultSet.setRun(runDist / 1000);
                                resultSet.setCycle(cycleDist / 1000);

                                recvDistanceSet.onRecvDistanceSet(resultSet);
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // nothing
                                Log.e("walkholic", "getdistanceset error", e);
                            }
                        });
    }

    private static int getMin(long time, long todayStart) {
        int min = (int)((time-todayStart) / 60000);
        if (min < 0) {
            return 0;
        } else if (min >= 1440) {
            return 1439;
        }
        return min;
    }

    public static float convertToKm(float mi) {
        return (float)(mi * 1.609344);
    }

    public static float convertToMi(float km) {
        return (float)(km * 0.621371192);
    }

    public static float convertToKg(float lbs) {
        return (float)(lbs * 0.45359237);
    }

    public static float convertToLbs(float kg) {
        return (float)(kg * 2.20462262);
    }
}
