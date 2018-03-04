package com.teuskim.fitproj

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.graphics.Color
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.Scope
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessStatusCodes
import com.google.android.gms.fitness.data.DataSet
import com.google.android.gms.fitness.data.DataSource
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.*
import com.google.android.gms.fitness.result.DataReadResult
import com.teuskim.fitproj.common.Goal
import com.teuskim.fitproj.view.CircleGraphView
import com.teuskim.fitproj.view.CircleStateView
import com.teuskim.fitproj.view.HorizontalListView
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 2015년 2월 오픈 당시의 Test 전용 화면
 * 잊고 지내다가 앱이 실행되지 않는 것을 인지하고 기존 API가 하위호환을 하지 않는다는 것을 확인한 후,
 * 2018년 2월에 업데이트 하면서 Test2Activity를 만들었는데, 기존 코드를 남기고자 삭제하지 않음.
 */
class TestActivity : Activity() {

    private var consoleLog: TextView? = null
    private var consoleLogWrapper: View? = null
    private val handler = Handler()

    private var client: GoogleApiClient? = null
    private var authInProgress = false
    private var odpListener: OnDataPointListener? = null

    /**
     * 일간 데이터 (걸음수, 거리)
     * 2014년 12월 1일부터 7일까지
     */
    private val reqDaily: DataReadRequest
        get() {
            val cal = Calendar.getInstance()
            cal.set(2014, Calendar.DECEMBER, 1)
            val startTime = cal.timeInMillis
            cal.add(Calendar.WEEK_OF_YEAR, 1)
            val endTime = cal.timeInMillis

            return DataReadRequest.Builder()
                    .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                    .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
                    .bucketByTime(1, TimeUnit.DAYS)
                    .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                    .build()
        }

    /**
     * 오늘 운동량
     */
    private//        long endTime = System.currentTimeMillis();
            //        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
            //        long startTime = cal.getTimeInMillis();
            //                .aggregate(DataType.TYPE_SPEED, DataType.AGGREGATE_SPEED_SUMMARY)
            //                .aggregate(DataType.TYPE_ACTIVITY_SEGMENT, DataType.AGGREGATE_ACTIVITY_SUMMARY)
            //                .aggregate(DataType.TYPE_LOCATION_SAMPLE, DataType.AGGREGATE_LOCATION_BOUNDING_BOX)
    val reqTodayData: DataReadRequest
        get() {
            val cal = Calendar.getInstance()

            cal.set(2015, Calendar.FEBRUARY, 11, 0, 0, 0)
            val startTime = cal.timeInMillis
            cal.set(2015, Calendar.FEBRUARY, 12, 0, 0, 0)
            val endTime = cal.timeInMillis

            return DataReadRequest.Builder()
                    .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                    .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
                    .bucketByTime(1, TimeUnit.MINUTES)
                    .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                    .build()
        }

    private val reqWeightData: DataReadRequest
        get() {
            val endTime = System.currentTimeMillis()
            val startTime = endTime - 86400000L * 10

            return DataReadRequest.Builder()
                    .aggregate(DataType.TYPE_WEIGHT, DataType.AGGREGATE_WEIGHT_SUMMARY)
                    .bucketByTime(1, TimeUnit.DAYS)
                    .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                    .build()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.test_activity)
        consoleLogWrapper = findViewById(R.id.console_log_wrapper)
        consoleLog = findViewById<View>(R.id.console_log) as TextView

        if (savedInstanceState != null) {
            authInProgress = savedInstanceState.getBoolean(AUTH_PENDING)
        }
        buildFitnessClient()
        initHorizontalListView()
    }

    private fun log(s: String, e: Throwable? = null) {
        handler.post {
            consoleLog!!.append(s + "\n")
            if (e != null) {
                Log.e("FitProj", s, e)
            } else {
                Log.e("FitProj", s)
            }
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

    override fun onStart() {
        super.onStart()
        log("Connecting...")
        client!!.connect()
    }

    override fun onStop() {
        super.onStop()
        if (client!!.isConnected) {
            client!!.disconnect()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == REQUEST_OAUTH) {
            authInProgress = false
            if (resultCode == Activity.RESULT_OK) {
                if (!client!!.isConnecting && !client!!.isConnected) {
                    client!!.connect()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(AUTH_PENDING, authInProgress)
    }

    /**
     * 메인화면으로 이동
     */
    fun onClickMoveToMain(arg: View) {
        val i = Intent(applicationContext, MainActivity::class.java)
        startActivity(i)
    }

    private fun buildFitnessClient() {
        client = GoogleApiClient.Builder(this)
                //                .addApi(Fitness.API)
                .addScope(Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                .addScope(Scope(Scopes.FITNESS_BODY_READ_WRITE))
                .addConnectionCallbacks(object : GoogleApiClient.ConnectionCallbacks {
                    override fun onConnected(bundle: Bundle?) {
                        log("Connected!")
                        dumpSubscriptionsList()
                    }

                    override fun onConnectionSuspended(i: Int) {
                        if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                            log("Connection lost.  Cause: Network Lost.")
                        } else if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
                            log("Connection lost.  Reason: Service Disconnected")
                        }
                    }
                })
                .addOnConnectionFailedListener(GoogleApiClient.OnConnectionFailedListener { result ->
                    log("Connection failed. Cause: " + result.toString())
                    if (!result.hasResolution()) {
                        GooglePlayServicesUtil.getErrorDialog(result.errorCode, this@TestActivity, 0).show()
                        return@OnConnectionFailedListener
                    }
                    if (!authInProgress) {
                        try {
                            log("Attempting to resolve failed connection")
                            authInProgress = true
                            result.startResolutionForResult(this@TestActivity, REQUEST_OAUTH)
                        } catch (e: IntentSender.SendIntentException) {
                            log("Exception while starting resolution activity", e)
                        }

                    }
                })
                .build()
    }

    /**
     * 데이터 불러오기
     */
    fun onClickLoadData(arg: View) {
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

        object : AsyncTask<Void, Void, Void>() {
            override fun doInBackground(vararg params: Void): Void? {
                //                Calendar cal = Calendar.getInstance();
                //                cal.set(2015, Calendar.FEBRUARY, 11, 0, 0, 0);
                //                long startTime = cal.getTimeInMillis();
                //                cal.set(2015, Calendar.FEBRUARY, 12, 0, 0, 0);
                //                long endTime = cal.getTimeInMillis();
                //                DistanceSet resultSet = FitUtil.getDistanceSet(client, startTime, endTime);
                //                Log.e(TAG, "walk: "+resultSet.getWalk());

                // 요청 만들기
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val endTime = cal.timeInMillis + 86400000L
                val startTime = endTime - 86400000L * 30

                //                DataReadRequest req = new DataReadRequest.Builder()
                //                        .aggregate(DataType.TYPE_WEIGHT, DataType.AGGREGATE_WEIGHT_SUMMARY)
                //                        .bucketByTime(1, TimeUnit.DAYS)
                //                        .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                //                        .build();

                val req = DataReadRequest.Builder()
                        .read(DataType.TYPE_WEIGHT)
                        .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                        .build()

                // 데이터 가져오기
                val result = Fitness.HistoryApi.readData(client, req).await(1, TimeUnit.MINUTES)
                if (result.status.isSuccess == false) {
                    Log.e(TAG, "error: " + result.status.statusMessage!!)
                    return null
                }
                val dateFormat = SimpleDateFormat("MM.dd")

                for (bk in result.buckets) {
                    val list = bk.dataSets
                    for (ds in list) {
                        for (dp in ds.dataPoints) {
                            if (dp.dataType.fields.contains(Field.FIELD_AVERAGE)) {
                                val date = dp.getStartTime(TimeUnit.MILLISECONDS)
                                val dateText = dateFormat.format(date)
                                val weight = dp.getValue(Field.FIELD_AVERAGE).asFloat()
                                Log.e(TAG, "1 dateText:$dateText , weight:$weight")
                            } else if (dp.dataType.fields.contains(Field.FIELD_WEIGHT)) {
                                val date = dp.getStartTime(TimeUnit.MILLISECONDS)
                                val dateText = dateFormat.format(date)
                                val weight = dp.getValue(Field.FIELD_WEIGHT).asFloat()
                                Log.e(TAG, "2 dateText:$dateText , weight:$weight")
                            }
                        }
                    }
                }

                for (ds in result.dataSets) {
                    Log.e(TAG, "111")
                    for (dp in ds.dataPoints) {
                        Log.e(TAG, "222: " + dp.dataType.fields)
                        if (dp.dataType.fields.contains(Field.FIELD_WEIGHT)) {
                            val date = dp.getStartTime(TimeUnit.MILLISECONDS)
                            val dateText = dateFormat.format(date)
                            val weight = dp.getValue(Field.FIELD_WEIGHT).asFloat()
                            Log.e(TAG, "3 dateText:$dateText , weight:$weight")
                        }
                    }
                }

                return null
            }
        }.execute()
    }

    private fun dumpDataReadResult(result: DataReadResult) {
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

    private fun simulateData(result: DataReadResult) {
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

    private fun simulateData2(result: DataReadResult) {
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

    private fun readSession() {
        Log.e(TAG, "readSession() called!")
        //        long DAY_IN_MS = TimeUnit.DAYS.toMillis(7);
        //        Date now = new Date();
        //        // Set a range of the day, using a start time of 7 days before this moment.
        //        long endTime = now.getTime();
        //        long startTime = endTime - DAY_IN_MS;

        val cal = Calendar.getInstance()
        cal.set(2014, Calendar.DECEMBER, 3, 9, 8, 35)
        val startTime = cal.timeInMillis
        cal.set(2014, Calendar.DECEMBER, 3, 9, 12, 45)
        val endTime = cal.timeInMillis

        val request = SessionReadRequest.Builder()
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                //                .read(dataTypeJumpHeight)
                //                .read(dataTypeHustle)
                //                .read(dataTypeQuickness)
                .read(DataType.TYPE_HEART_RATE_BPM)
                .read(DataType.TYPE_DISTANCE_DELTA)
                .read(DataType.TYPE_SPEED)
                .read(DataType.TYPE_STEP_COUNT_DELTA)
                .readSessionsFromAllApps()
                .build()

        val pendingResult = Fitness.SessionsApi.readSession(client, request)


        // 3. Check the result
        pendingResult.setResultCallback { sessionReadResult ->
            // Get a list of sessions that match the criteria
            Log.e(TAG, "Sessions found: " + sessionReadResult.sessions.size)

            val mSessions = sessionReadResult.sessions
            //                        mDemoApplication.setSessions(mSessions);
            for (session in mSessions) {
                val sessionName = session.name
                Log.d(TAG, "Session: " + sessionName)

                // Get the currentSessionDataSets for the time interval of this session
                val currentSessionDataSets = sessionReadResult.getDataSet(session)
                //                            mSessionDataSets.put(session.getIdentifier(), currentSessionDataSets);
            }
            //                        mDemoApplication.setSessionDataSets(mSessionDataSets);
            Log.d(TAG, "The number of sessions is: " + mSessions.size)
            //                        mSessionAdapter.setSessions(mSessions);
            //                        mSessionAdapter.notifyDataSetChanged();
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

    /**
     * 데이터 기록 시작 및 종료
     */
    fun onClickRecordData(arg: View) {
        subscribe()
    }

    private fun dumpSubscriptionsList() {
        Fitness.RecordingApi.listSubscriptions(client, DataType.TYPE_STEP_COUNT_DELTA).setResultCallback { listSubscriptionsResult ->
            for (sc in listSubscriptionsResult.subscriptions) {
                val dt = sc.dataType
                log("Active subscription for data type: " + dt.name)
            }
        }
    }

    private fun subscribe() {
        Fitness.RecordingApi.subscribe(client, DataType.TYPE_STEP_COUNT_DELTA).setResultCallback { status ->
            if (status.isSuccess) {
                if (status.statusCode == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
                    log("Existing subscription for activity detected. Unsubscribe..")
                    unsubscribe()
                } else {
                    log("Successfully subscribed!")
                }
            } else {
                log("There was a problem subscribing.")
            }
        }
    }

    private fun unsubscribe() {
        val dataTypeStr = DataType.TYPE_STEP_COUNT_DELTA.toString()
        log("Unsubscribing from data type: " + dataTypeStr)

        Fitness.RecordingApi.unsubscribe(client, DataType.TYPE_STEP_COUNT_DELTA).setResultCallback { status ->
            if (status.isSuccess) {
                log("Successfully unsubscribed for data type: " + dataTypeStr)
            } else {
                log("Failed to unsubscribe for data type: " + dataTypeStr)
            }
        }
    }

    /**
     * 실시간 데이터 확인
     */
    fun onClickSensors(arg: View) {
        findFitnessDataSources()
    }

    private fun findFitnessDataSources() {
        val dataType = DataType.TYPE_STEP_COUNT_CUMULATIVE
        Fitness.SensorsApi.findDataSources(client, DataSourcesRequest.Builder()
                .setDataTypes(dataType)
                .setDataSourceTypes(DataSource.TYPE_RAW)
                .build())
                .setResultCallback { dataSourcesResult ->
                    log("findDataSources result: " + dataSourcesResult.status.toString())
                    for (dataSource in dataSourcesResult.dataSources) {
                        log("Data source found: " + dataSource.toString())
                        log("Data Source type: " + dataSource.dataType.name)

                        if (dataSource.dataType == dataType) {
                            if (odpListener == null) {
                                log("Data source registering..")
                                registerFitnessDataListener(dataSource, dataType)
                            } else {
                                log("Data source unregistering..")
                                unregisterFitnessDataListener()
                            }
                        }
                    }
                }
    }

    private fun registerFitnessDataListener(dataSource: DataSource, dataType: DataType) {
        odpListener = OnDataPointListener { dataPoint ->
            for (field in dataPoint.dataType.fields) {
                val `val` = dataPoint.getValue(field)
                log("Detected DataPoint field: " + field.name)
                log("Detected DataPoint value: " + `val`)
            }
        }

        Fitness.SensorsApi.add(
                client,
                SensorRequest.Builder()
                        .setDataSource(dataSource)
                        .setDataType(dataType) // Can't be omitted.
                        .setSamplingRate(5, TimeUnit.SECONDS)
                        .build(),
                odpListener)
                .setResultCallback { status ->
                    if (status.isSuccess) {
                        log("odpListener registered!")
                    } else {
                        log("odpListener not registered.")
                    }
                }
    }

    private fun unregisterFitnessDataListener() {
        if (odpListener == null) {
            return
        }

        Fitness.SensorsApi.remove(
                client,
                odpListener)
                .setResultCallback { status ->
                    if (status.isSuccess) {
                        log("odpListener was removed!")
                    } else {
                        log("odpListener was not removed.")
                    }
                }
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

    /**
     * 횡스크롤 리스트뷰 테스트
     */
    private fun initHorizontalListView() {
        val listView = findViewById<View>(R.id.hlist) as HorizontalListView
        val adapter = object : BaseAdapter() {
            override fun getCount(): Int {
                return 10
            }

            override fun getItem(position: Int): Any? {
                return null
            }

            override fun getItemId(position: Int): Long {
                return 0
            }

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                var convertView = convertView
                if (convertView == null) {
                    convertView = LayoutInflater.from(applicationContext).inflate(R.layout.history_list_item, parent, false)
                }
                val csv = convertView!!.findViewById<View>(R.id.circle_state) as CircleStateView
                csv.setColor(Goal.getColor(Goal.Type.WALK))
                csv.setFilledRate(0.5f)
                (convertView.findViewById<View>(R.id.date) as TextView).text = "1." + (position + 1)
                return convertView
            }
        }
        listView.adapter = adapter
    }

    /**
     * 데이터 저장하기(History API)
     */
    fun onClickSaveData(arg: View) {
        object : AsyncTask<Void, Void, com.google.android.gms.common.api.Status>() {
            override fun doInBackground(vararg params: Void): com.google.android.gms.common.api.Status {
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
                        //                        .setTimestamp(time, TimeUnit.MICROSECONDS);
                        .setTimeInterval(time, endTime, TimeUnit.MILLISECONDS)
                dataPoint.getValue(Field.FIELD_WEIGHT).setFloat(65f)
                dataSet.add(dataPoint)

                return Fitness.HistoryApi.insertData(client, dataSet).await(1, TimeUnit.MINUTES)
            }

            override fun onPostExecute(status: com.google.android.gms.common.api.Status) {
                if (status.isSuccess) {
                    log("Data insert was successful!")
                } else {
                    log("There was a problem inserting the dataset.")
                    Log.e(TAG, status.statusCode.toString() + ": " + status.statusMessage)
                }
            }
        }.execute()
    }

    fun onClickNotification(arg: View) {
        val i = Intent(applicationContext, AlarmReceiver::class.java)
        sendBroadcast(i)
    }

    companion object {

        private val TAG = "WalkHolic"
        private val AUTH_PENDING = "auth_state_pending"
        private val REQUEST_OAUTH = 1
    }

}
