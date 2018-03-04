package com.teuskim.fitproj

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataSet
import com.google.android.gms.fitness.data.DataSource
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.result.DataReadResponse
import com.teuskim.fitproj.view.CircleGraphView
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Test 전용 화면
 * 2015년 2월에 오픈한 후 잊고 지내다가 앱이 실행되지 않는 것을 인지하고,
 * 기존 API가 하위호환을 하지 않는다는 것을 확인한 후,
 * 업데이트 하면서 TestActivity를 대체하면서 새로 만들었음.
 */
class Test2Activity : Activity() {

    private var consoleLog: TextView? = null
    private var consoleLogWrapper: View? = null
    private val handler = Handler()
    private var isSubscribed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.test_activity)
        consoleLogWrapper = findViewById(R.id.console_log_wrapper)
        consoleLog = findViewById(R.id.console_log)

        val fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE)
                .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE)
                .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_WRITE)
                .addDataType(DataType.AGGREGATE_DISTANCE_DELTA, FitnessOptions.ACCESS_WRITE)
                .addDataType(DataType.TYPE_WEIGHT)
                .build()
        if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                    this,
                    REQUEST_OAUTH_REQUEST_CODE,
                    GoogleSignIn.getLastSignedInAccount(this),
                    fitnessOptions)
        } else {
            checkAndRequestPermissions()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_OAUTH_REQUEST_CODE) {
                log("on activity result! request oauth request code!")
                checkAndRequestPermissions()
            }
        }
    }

    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_FITNESS_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_FITNESS_PERMISSIONS) {
            log("on request permissions result for request fitness permissions!!")
        }
    }

    /**
     * 콘솔창 닫기/열기
     */
    fun onClickBtnConsole(arg: View) {
        if (consoleLog!!.isShown) {
            consoleLogWrapper!!.visibility = View.GONE
            (arg as Button).text = "콘솔창 열기"
        } else {
            consoleLogWrapper!!.visibility = View.VISIBLE
            (arg as Button).text = "콘솔창 닫기"
        }
    }

    private fun log(s: String, e: Throwable? = null) {
        handler.post {
            consoleLog!!.append(s + "\n")
            if (e != null) {
                Log.e(TAG, s, e)
            } else {
                Log.e(TAG, s)
            }
        }
    }

    /**
     * 메인화면으로 이동
     */
    fun onClickMoveToMain(arg: View) {
        val i = Intent(applicationContext, MainActivity::class.java)
        startActivity(i)
    }

    /**
     * 데이터 불러오기
     */
    fun onClickLoadData(arg: View) {
        loadTodaySteps()
    }

    private fun loadDailySteps() {
        val cal = Calendar.getInstance()
        cal.set(2018, Calendar.JANUARY, 20)
        val startTime = cal.timeInMillis
        cal.add(Calendar.WEEK_OF_YEAR, 1)
        val endTime = cal.timeInMillis

        val readRequest = DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build()

        loadHistoryData(readRequest)
    }

    private fun loadTodaySteps() {
        val cal = Calendar.getInstance()

        //        long endTime = System.currentTimeMillis();
        //        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        //        long startTime = cal.getTimeInMillis();

        cal.set(2018, Calendar.FEBRUARY, 7, 0, 0, 0)
        val startTime = cal.timeInMillis
        cal.set(2018, Calendar.FEBRUARY, 8, 0, 0, 0)
        val endTime = cal.timeInMillis

        val readRequest = DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                //                .aggregate(DataType.TYPE_SPEED, DataType.AGGREGATE_SPEED_SUMMARY)
                //                .aggregate(DataType.TYPE_ACTIVITY_SEGMENT, DataType.AGGREGATE_ACTIVITY_SUMMARY)
                .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
                //                .aggregate(DataType.TYPE_LOCATION_SAMPLE, DataType.AGGREGATE_LOCATION_BOUNDING_BOX)
                .bucketByTime(1, TimeUnit.MINUTES)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build()

        loadHistoryData(readRequest)
    }

    private fun loadAggregateWeights() {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 86400000L * 10

        val readRequest = DataReadRequest.Builder()
                .aggregate(DataType.TYPE_WEIGHT, DataType.AGGREGATE_WEIGHT_SUMMARY)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build()

        loadHistoryData(readRequest)
    }

    private fun loadHistoryData(readRequest: DataReadRequest) {
        Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .readData(readRequest)
                .addOnSuccessListener { dataReadResponse ->
                    log("read data!!!")
                    dumpDataReadResult(dataReadResponse)
                }
                .addOnFailureListener { e -> log("There was a problem reading the data.", e) }
    }

    private fun dumpDataReadResult(result: DataReadResponse) {
        if (result.buckets.size > 0) {
            log("\nbuckets!")
            for (bk in result.buckets) {
                val list = bk.dataSets
                for (ds in list) {
                    dumpDataSet(ds)
                }
            }
        }
        if (result.dataSets.size > 0) {
            log("\ndata sets!")
            for (ds in result.dataSets) {
                dumpDataSet(ds)
            }
        }
    }

    private fun dumpDataSet(dataSet: DataSet) {
        log("Data returned for Data type: " + dataSet.dataType.name)
        //        log(dataSet.toString());

        val dateFormat = SimpleDateFormat("yyyy.MM.dd HH:mm:ss")

        for (dp in dataSet.dataPoints) {
            log("Data point:")
            log("\tType: " + dp.dataType.name)
            log("\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)))
            log("\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)))
            for (field in dp.dataType.fields) {
                log("\tField: " + field.name + " Value: " + dp.getValue(field))
            }
        }
    }

    private fun simulateData(result: DataReadResponse) {
        val dateFormat = SimpleDateFormat("HH:mm:ss")

        val walkLow = 50f
        val walkHigh = 120f
        val runLow = 120f
        val runHigh = 400f
        val cycleLow = 400f
        val cycleHigh = 650f
        val carLow = 800f

        val startStepTimes = ArrayList<Long>()
        val endStepTimes = ArrayList<Long>()

        // steps time list 셋팅
        for (bk in result.buckets) {
            val list = bk.dataSets
            for (ds in list) {
                for (dp in ds.dataPoints) {
                    if (dp.dataType.fields.contains(Field.FIELD_STEPS) && dp.getValue(Field.FIELD_STEPS).asInt() >= walkLow) {
                        startStepTimes.add(dp.getStartTime(TimeUnit.MINUTES))
                        endStepTimes.add(dp.getEndTime(TimeUnit.MINUTES))
                        Log.e(TAG, dp.getValue(Field.FIELD_STEPS).toString()
                                + " (" + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS))
                                + " ~ " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)) + ")")
                    }
                }
            }
        }

        var walkDistance = 0f
        var runDistance = 0f
        var cycleDistance = 0f
        var cycleDistancePart = 0f

        var duration: Int
        var smin: Long
        var emin: Long
        var `val`: Float
        var avg: Float
        var inCycle = false
        var inCar = false
        var hasStep: Boolean?

        // 데이터 산출
        for (bk in result.buckets) {
            val list = bk.dataSets
            for (ds in list) {
                for (dp in ds.dataPoints) {
                    /*
                    Log.e(TAG, dp.getValue(Field.FIELD_DISTANCE)
                                    +" ("+dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS))
                                    +" ~ "+dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS))+")");
                    */
                    if (dp.dataType.fields.contains(Field.FIELD_DISTANCE) == false) {
                        continue
                    }
                    smin = dp.getStartTime(TimeUnit.MINUTES)
                    emin = dp.getEndTime(TimeUnit.MINUTES)
                    duration = (emin - smin).toInt()
                    `val` = dp.getValue(Field.FIELD_DISTANCE).asFloat()
                    avg = `val` / duration
                    hasStep = null

                    // 차를 타고 있나?
                    if (avg >= carLow) {
                        inCar = true
                    } else if (inCar && avg < runHigh) {
                        hasStep = hasStep(smin, emin, startStepTimes, endStepTimes)
                        if (hasStep) {
                            inCar = false
                        }
                    }
                    // 차 타고 있으면 패스
                    if (inCar) {
                        continue
                    }
                    // 자전거 타고 있나?
                    if (avg >= cycleLow && avg < cycleHigh) {
                        inCycle = true
                        cycleDistancePart += `val`
                    } else if (inCycle) {
                        inCycle = false
                        if (avg < walkHigh) {
                            cycleDistance += cycleDistancePart
                            Log.e(TAG, "cycle data added! " + cycleDistancePart
                                    + " , (" + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS))
                                    + " ~ " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)) + ")")
                        } else if (avg >= cycleHigh) {
                            cycleDistancePart = 0f
                        }
                    }
                    // 자전거 타고 있으면 패스
                    if (inCycle) {
                        continue
                    }
                    // 달리고 있나?
                    if (avg >= runLow && avg < runHigh) {
                        if (hasStep == null) {
                            hasStep = hasStep(smin, emin, startStepTimes, endStepTimes)
                        }
                        if (hasStep) {
                            runDistance += `val`
                            Log.e(TAG, "run! duration: " + duration + " , val: " + `val`
                                    + " , (" + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS))
                                    + " ~ " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)) + ")")
                            continue
                        }
                    }
                    // 걷고 있나?
                    if (avg >= walkLow && avg < walkHigh) {
                        if (hasStep == null) {
                            hasStep = hasStep(smin, emin, startStepTimes, endStepTimes)
                        }
                        if (hasStep) {
                            walkDistance += `val`
                            Log.e(TAG, "walk! duration: " + duration + " , val: " + `val`
                                    + " , (" + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS))
                                    + " ~ " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)) + ")")
                        }
                    }
                }
            }
        }

        Log.e(TAG, "walk: $walkDistance , run: $runDistance , cycle: $cycleDistance")
    }

    private fun hasStep(smin: Long, emin: Long, startStepTimes: List<Long>, endStepTimes: List<Long>): Boolean {
        // smin 직전에 endtime이 있고(즉, starttime이 없고), [smin,emin] 구간에 starttime이 없으면, 해당 구간에 step이 없는 것이다.
        var stime: Long = 0
        var stime2: Long = 0
        for (t in startStepTimes) {
            if (t > smin) {
                if (t > emin) {
                    break
                } else {
                    stime2 = t
                }
            } else {
                stime = t
            }
        }
        var etime: Long = 0
        for (t in endStepTimes) {
            if (t > smin) {
                break
            } else {
                etime = t
            }
        }
        return if (stime < etime && stime2 == 0L) {
            false
        } else true
    }

    private fun simulateData2(result: DataReadResponse) {
        val dateFormat = SimpleDateFormat("HH:mm:ss")

        // 스텝수 범위
        val walkStepLow = 10
        val walkStepHigh = 70

        // 거리 범위
        val walkDistHigh = 100f
        val runDistHigh = 400f
        val cycleDistLow = 300f
        val cycleDistHigh = 650f
        val carDistLow = 800f

        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 11) // TODO: for test
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val todayStart = cal.timeInMillis

        val stepPerMin = IntArray(1440)  // 분당스텝수
        val distPerMin = FloatArray(1440)  // 분당이동거리
        val carMap = BooleanArray(1440)  // 각 분별 차량탑승여부
        val carPosList = ArrayList<Int>()  // 차 탑승 확신할 수 있는 시간대
        val checkedCar = BooleanArray(1440)  // 차 탑승 과련 체크된 내용

        // 분당스텝수, 분당이동거리, 차탑승시간대 셋팅
        for (bk in result.buckets) {
            val list = bk.dataSets
            for (ds in list) {
                for (dp in ds.dataPoints) {
                    val smin = getMin(dp.getStartTime(TimeUnit.MILLISECONDS), todayStart)
                    val emin = getMin(dp.getEndTime(TimeUnit.MILLISECONDS), todayStart)
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
                if (stepPerMin[i] < walkStepLow && checkedCar[i] == false) {
                    carMap[i] = true
                    checkedCar[i] = true
                } else {
                    if (carMap[i + 1] == true && distPerMin[i] == distPerMin[i + 1]) {
                        carMap[i] = true
                    }
                    checkedCar[i] = true
                    break
                }
                i--
            }
            i = pos + 1
            while (i < 1440) {
                if (stepPerMin[i] < walkStepLow && checkedCar[i] == false) {
                    carMap[i] = true
                    checkedCar[i] = true
                } else {
                    if (carMap[i - 1] == true && distPerMin[i] == distPerMin[i - 1]) {
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
            } else if (carMap[i] == false && dist >= cycleDistLow && dist < cycleDistHigh) {
                cycleDist += dist
            }

            if (step > 0) {
                Log.e(TAG, "step: " + getHourMinStr(i) + ", " + step)
            }
            if (dist > 0) {
                Log.e(TAG, "dist: " + getHourMinStr(i) + ", " + dist)
            }
            if (carMap[i]) {
                Log.e(TAG, "in car: " + getHourMinStr(i))
            }
        }

        log("walk: $walkDist , run: $runDist , cycle: $cycleDist")
    }

    private fun getHourMinStr(m: Int): String {
        val hh = m / 60
        val mm = m % 60
        return hh.toString() + ":" + mm
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

    private fun loadWeights() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val endTime = cal.timeInMillis + 86400000L
        val startTime = endTime - 86400000L * 30

        val readRequest = DataReadRequest.Builder()
                .read(DataType.TYPE_WEIGHT)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build()

        Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .readData(readRequest)
                .addOnSuccessListener { dataReadResponse ->
                    log("read data!!!")
                    val dateFormat = SimpleDateFormat("MM.dd")

                    for (bk in dataReadResponse.buckets) {
                        val list = bk.dataSets
                        for (ds in list) {
                            for (dp in ds.dataPoints) {
                                if (dp.dataType.fields.contains(Field.FIELD_AVERAGE)) {
                                    val date = dp.getStartTime(TimeUnit.MILLISECONDS)
                                    val dateText = dateFormat.format(date)
                                    val weight = dp.getValue(Field.FIELD_AVERAGE).asFloat()
                                    log("1 dateText:$dateText , weight:$weight")
                                } else if (dp.dataType.fields.contains(Field.FIELD_WEIGHT)) {
                                    val date = dp.getStartTime(TimeUnit.MILLISECONDS)
                                    val dateText = dateFormat.format(date)
                                    val weight = dp.getValue(Field.FIELD_WEIGHT).asFloat()
                                    log("2 dateText:$dateText , weight:$weight")
                                }
                            }
                        }
                    }

                    for (ds in dataReadResponse.dataSets) {
                        for (dp in ds.dataPoints) {
                            log("222: " + dp.dataType.fields)
                            if (dp.dataType.fields.contains(Field.FIELD_WEIGHT)) {
                                val date = dp.getStartTime(TimeUnit.MILLISECONDS)
                                val dateText = dateFormat.format(date)
                                val weight = dp.getValue(Field.FIELD_WEIGHT).asFloat()
                                log("3 dateText:$dateText , weight:$weight")
                            }
                        }
                    }
                }
                .addOnFailureListener { e -> log("There was a problem reading the data.", e) }
    }

    /**
     * 데이터 저장하기(History API)
     */
    fun onClickSaveData(arg: View) {
        val dataSource = DataSource.Builder()
                .setAppPackageName(applicationContext)
                .setDataType(DataType.TYPE_WEIGHT)
                .setName("fitproj-weight")
                .setType(DataSource.TYPE_RAW)
                .build()

        val dataSet = DataSet.create(dataSource)

        val time = System.currentTimeMillis() - 86400000L * 4
        val endTime = time + 1000

        val dataPoint = dataSet.createDataPoint()
                //                .setTimestamp(time, TimeUnit.MICROSECONDS);
                .setTimeInterval(time, endTime, TimeUnit.MILLISECONDS)
        dataPoint.getValue(Field.FIELD_WEIGHT).setFloat(65f)
        dataSet.add(dataPoint)

        Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .insertData(dataSet)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // At this point, the data has been inserted and can be read.
                        log("Data insert was successful!")
                    } else {
                        log("There was a problem inserting the dataset.", task.exception)
                    }
                }
    }

    /**
     * 데이터 기록 시작 및 종료
     */
    fun onClickRecordData(arg: View) {
        if (isSubscribed) {
            unsubscribe()
        } else {
            subscribe()
        }
    }

    fun subscribe() {
        Fitness.getRecordingClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .subscribe(DataType.TYPE_ACTIVITY_SAMPLES)
                .addOnSuccessListener {
                    log("Successfully subscribed!")
                    isSubscribed = true
                }
                .addOnFailureListener { log("There was a problem subscribing.") }
    }

    private fun unsubscribe() {
        val dataTypeStr = DataType.TYPE_ACTIVITY_SAMPLES.toString()
        log("Unsubscribing from data type: " + dataTypeStr)

        Fitness.getRecordingClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .unsubscribe(DataType.TYPE_ACTIVITY_SAMPLES)
                .addOnSuccessListener {
                    log("Successfully unsubscribed for data type: " + dataTypeStr)
                    isSubscribed = false
                }
                .addOnFailureListener {
                    // Subscription not removed
                    log("Failed to unsubscribe for data type: " + dataTypeStr)
                }
    }

    /**
     * 실시간 데이터 확인
     */
    fun onClickSensors(arg: View) {
        // 필요하면 추가하자.
    }

    fun onClickNotification(arg: View) {
        val i = Intent(applicationContext, AlarmReceiver::class.java)
        sendBroadcast(i)
    }

    /**
     * 원형 그래프뷰 클릭했을때
     */
    fun onClickCircleGraphView(arg: View) {
        val v = arg as CircleGraphView
        v.setFactors(0.5f, 0.1f, 0.02f, -0x111112, -0x111112, 20)
        var pos = v.addGraph(intArrayOf(Color.RED, Color.YELLOW, Color.RED), 100)
        v.setValue(pos, 200f, true)
        pos = v.addGraph(intArrayOf(Color.BLUE, Color.GREEN, Color.RED, Color.BLUE), 100)
        v.setValue(pos, 30f, true)

        log("onClickCircleGraphView!")
    }

    companion object {

        val TAG = "WalkHolic"
        private val REQUEST_OAUTH_REQUEST_CODE = 1
        private val REQUEST_FITNESS_PERMISSIONS = 2
    }
}
