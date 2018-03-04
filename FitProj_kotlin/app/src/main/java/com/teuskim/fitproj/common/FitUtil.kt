package com.teuskim.fitproj.common

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.util.DisplayMetrics
import android.util.Log

import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.data.Bucket
import com.google.android.gms.fitness.data.DataPoint
import com.google.android.gms.fitness.data.DataSet
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.result.DataReadResponse
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener

import java.util.ArrayList
import java.util.concurrent.TimeUnit

/**
 * static 메소드들
 */
object FitUtil {

    /**
     * DP 값을 PX 값으로 변환
     */
    private var dpToPxRatio = -1f

    /**
     * 화면의 너비 구하기
     */
    private var screenWidth = 0

    fun convertDpToPx(dp: Float, r: Resources): Int {
        if (dpToPxRatio < 0) {
            dpToPxRatio = r.displayMetrics.density
        }
        return Math.round(dp * dpToPxRatio)
    }

    fun getScreenWidth(activity: Activity): Int {
        if (screenWidth == 0) {
            val dm = DisplayMetrics()
            activity.windowManager.defaultDisplay.getMetrics(dm)
            screenWidth = dm.widthPixels
        }
        return screenWidth
    }

    interface RecvDistanceSet {
        fun onRecvDistanceSet(distanceSet: DistanceSet)
    }

    /**
     * 구글핏 history api에서 거리 데이터 셋 가져오기
     * @param startTime 오늘의 0시
     * @param endTime 다음날 0시
     */
    fun getDistanceSet(context: Context?, startTime: Long, endTime: Long, recvDistanceSet: RecvDistanceSet) {
        if (context == null) {
            return
        }
        // 요청 만들기
        val req = DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
                .bucketByTime(1, TimeUnit.MINUTES)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build()

        Fitness.getHistoryClient(context, GoogleSignIn.getLastSignedInAccount(context))
                .readData(req)
                .addOnSuccessListener { dataReadResponse ->
                    // 스텝수 범위
                    val walkStepLow = 10
                    val walkStepHigh = 70

                    // 거리 범위
                    val walkDistHigh = 100f
                    val runDistHigh = 400f
                    val cycleDistLow = 300f
                    val cycleDistHigh = 650f
                    val carDistLow = 800f

                    val stepPerMin = IntArray(1440)  // 분당스텝수
                    val distPerMin = FloatArray(1440)  // 분당이동거리
                    val carMap = BooleanArray(1440)  // 각 분별 차량탑승여부
                    val carPosList = ArrayList<Int>()  // 차 탑승 확신할 수 있는 시간대
                    val checkedCar = BooleanArray(1440)  // 차 탑승 과련 체크된 내용

                    // 분당스텝수, 분당이동거리, 차탑승시간대 셋팅
                    for (bk in dataReadResponse.buckets) {
                        val list = bk.dataSets
                        for (ds in list) {
                            for (dp in ds.dataPoints) {
                                val smin = getMin(dp.getStartTime(TimeUnit.MILLISECONDS), startTime)
                                val emin = getMin(dp.getEndTime(TimeUnit.MILLISECONDS), startTime)
                                val diff = emin - smin + 1
                                for (i in smin..emin) {
                                    if (dp.dataType.fields.contains(Field.FIELD_STEPS)) {
                                        stepPerMin[i] = dp.getValue(Field.FIELD_STEPS).asInt() / diff
                                    } else if (dp.dataType.fields.contains(Field.FIELD_DISTANCE)) {
                                        distPerMin[i] = dp.getValue(Field.FIELD_DISTANCE).asFloat() / diff
                                        if (distPerMin[i] > carDistLow) {
                                            carPosList.add(i)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 각 분별 차량탑승여부 셋팅
                    for (pos in carPosList) {
                        carMap[pos] = true
                        checkedCar[pos] = true
                        var i = pos - 1
                        while (i >= 0) {
                            if (stepPerMin[i] < walkStepLow && !checkedCar[i]) {
                                carMap[i] = true
                                checkedCar[i] = true
                            } else {
                                if (carMap[i + 1] && distPerMin[i] == distPerMin[i + 1]) {
                                    carMap[i] = true
                                }
                                checkedCar[i] = true
                                break
                            }
                            i--
                        }
                        i = pos + 1
                        while (i < 1440) {
                            if (stepPerMin[i] < walkStepLow && !checkedCar[i]) {
                                carMap[i] = true
                                checkedCar[i] = true
                            } else {
                                if (carMap[i - 1] && distPerMin[i] == distPerMin[i - 1]) {
                                    carMap[i] = true
                                }
                                checkedCar[i] = true
                                break
                            }
                            i++
                        }
                    }

                    var walkDist = 0f
                    var runDist = 0f
                    var cycleDist = 0f

                    var step: Int
                    var dist: Float

                    for (i in 0..1439) {
                        step = stepPerMin[i]
                        dist = distPerMin[i]
                        if (step >= walkStepLow && dist < runDistHigh) {
                            if (step < walkStepHigh && dist < walkDistHigh) {
                                walkDist += dist
                            } else {
                                runDist += dist
                            }
                        } else if (!carMap[i] && dist >= cycleDistLow && dist < cycleDistHigh) {
                            cycleDist += dist
                        }
                    }

                    val resultSet = DistanceSet()
                    resultSet.setWalk(walkDist / 1000)
                    resultSet.setRun(runDist / 1000)
                    resultSet.setCycle(cycleDist / 1000)

                    recvDistanceSet.onRecvDistanceSet(resultSet)
                }
                .addOnFailureListener { e ->
                    // nothing
                    Log.e("walkholic", "getdistanceset error", e)
                }
    }

    private fun getMin(time: Long, todayStart: Long): Int {
        val min = ((time - todayStart) / 60000).toInt()
        if (min < 0) {
            return 0
        } else if (min >= 1440) {
            return 1439
        }
        return min
    }

    fun convertToKm(mi: Float): Float {
        return (mi * 1.609344).toFloat()
    }

    fun convertToMi(km: Float): Float {
        return (km * 0.621371192).toFloat()
    }

    fun convertToKg(lbs: Float): Float {
        return (lbs * 0.45359237).toFloat()
    }

    fun convertToLbs(kg: Float): Float {
        return (kg * 2.20462262).toFloat()
    }
}
