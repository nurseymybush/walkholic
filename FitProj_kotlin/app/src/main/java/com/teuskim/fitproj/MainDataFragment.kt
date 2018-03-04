package com.teuskim.fitproj

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.data.DataSet
import com.google.android.gms.fitness.data.DataSource
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import com.teuskim.fitproj.common.FitPreference
import com.teuskim.fitproj.common.FitUtil
import com.teuskim.fitproj.view.FitViewPager
import com.teuskim.fitproj.view.HorizontalListView
import com.teuskim.fitproj.view.WeightItemView
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * 메인탭의 데이터 프레그먼트
 * 체중 등 사용자 정보 입력
 */
class MainDataFragment : BaseFragment() {
    private var lastWeightView: EditText? = null
    private var lastWeightUnitView: TextView? = null
    private var btnModifyWrapper: View? = null
    private var lastDateView: TextView? = null
    private var weightListView: HorizontalListView? = null
    private var enterWeightMsgView: View? = null

    private var adapter: WeightAdapter? = null
    private var lastWeight: Weight? = null
    private var pref: FitPreference? = null
    private var isClickedWeight: Boolean = false
    private var isConvertUnitOn: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pref = FitPreference.getInstance(activity)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        isClickedWeight = false
        isConvertUnitOn = pref!!.isConvertUnitOn
        val v = inflater.inflate(R.layout.main_data_fragment, container, false)
        initViews(v)
        loadData()
        (activity as MainActivity).setViewPagerOnChildMoveListener(object: FitViewPager.OnChildMoveListener {
            override fun onMove(diffX: Float): Boolean {
                val x = weightListView!!.currentX
                return !(x == 0 && diffX > 0)
            }
        })
        return v
    }

    private fun initViews(v: View) {
        lastWeightView = v.findViewById(R.id.last_weight)
        lastWeightView!!.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                if (!isClickedWeight) {
                    return
                }
                val wstr = s.toString().trim { it <= ' ' }
                try {
                    val w = java.lang.Float.parseFloat(wstr)
                    if ((lastWeight == null || w > 0 && w != lastWeight!!.weight) && btnModifyWrapper!!.visibility != View.VISIBLE) {
                        val anim = AnimationUtils.loadAnimation(activity, R.anim.right_in)
                        btnModifyWrapper!!.startAnimation(anim)
                        btnModifyWrapper!!.visibility = View.VISIBLE
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "weight view text changed error", e)
                }

            }
        })
        lastWeightView!!.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                isClickedWeight = true
            }
        }

        lastWeightUnitView = v.findViewById(R.id.last_weight_unit)

        val l = View.OnClickListener { v ->
            when (v.id) {
                R.id.btn_modify -> {
                    val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm?.hideSoftInputFromWindow(v.windowToken, 0)
                    saveWeight()
                }
            }
        }
        btnModifyWrapper = v.findViewById(R.id.btn_modify_wrapper)
        v.findViewById<View>(R.id.btn_modify).setOnClickListener(l)
        lastDateView = v.findViewById(R.id.last_date)
        weightListView = v.findViewById(R.id.weight_list)
        enterWeightMsgView = v.findViewById(R.id.enter_weight_img)
        adapter = WeightAdapter()
        weightListView!!.adapter = adapter

        val switchRecordingView = v.findViewById<Switch>(R.id.switch_recording)
        switchRecordingView.isChecked = pref!!.isRecordingOn
        switchRecordingView.setOnCheckedChangeListener { buttonView, isChecked ->
            pref!!.setRecordingOn(isChecked)
            (activity as MainActivity).subscribeOrCancel()
        }

        val switchAlertView = v.findViewById<Switch>(R.id.switch_alert)
        switchAlertView.isChecked = pref!!.isAlarmOn
        switchAlertView.setOnCheckedChangeListener { buttonView, isChecked ->
            pref!!.setAlarmOn(isChecked)
            (activity as MainActivity).initAlarm()
        }

        val switchConvertUnitView = v.findViewById<Switch>(R.id.switch_convert_unit)
        switchConvertUnitView.isChecked = pref!!.isConvertUnitOn
        switchConvertUnitView.setOnCheckedChangeListener { buttonView, isChecked ->
            pref!!.setConvertUnitOn(isChecked)
            isConvertUnitOn = isChecked
            loadData()
        }
    }

    private fun loadData() {
        if (isConvertUnitOn) {
            lastWeightUnitView!!.text = "lbs"
        } else {
            lastWeightUnitView!!.text = "kg"
        }

        // 요청 만들기
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val endTime = cal.timeInMillis + 86400000L
        val startTime = endTime - 86400000L * 365
        val req = DataReadRequest.Builder()
                .aggregate(DataType.TYPE_WEIGHT, DataType.AGGREGATE_WEIGHT_SUMMARY)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build()

        // 데이터 가져오기
        Fitness.getHistoryClient(context, GoogleSignIn.getLastSignedInAccount(context))
                .readData(req)
                .addOnSuccessListener { dataReadResponse ->
                    val weightList = ArrayList<Weight>()
                    @SuppressLint("SimpleDateFormat") val dateFormat = SimpleDateFormat("MM.dd")
                    for (bk in dataReadResponse.buckets) {
                        val list = bk.dataSets
                        for (ds in list) {
                            for (dp in ds.dataPoints) {
                                if (dp.dataType.fields.contains(Field.FIELD_AVERAGE)) {
                                    val date = dp.getStartTime(TimeUnit.MILLISECONDS)
                                    val dateText = dateFormat.format(date)
                                    val weight = getConvertedWeight(dp.getValue(Field.FIELD_AVERAGE).asFloat())
                                    weightList.add(Weight(weight, date, dateText))
                                }
                            }
                        }
                    }
                    val reverseWeightList = ArrayList<Weight>()
                    for (i in weightList.indices.reversed()) {
                        reverseWeightList.add(weightList[i])
                    }
                    if (reverseWeightList.size > 0) {
                        val cal = Calendar.getInstance()
                        lastWeight = reverseWeightList[0]
                        lastWeightView!!.setText(String.format("%.1f", lastWeight!!.weight))
                        cal.timeInMillis = lastWeight!!.date
                        lastDateView!!.text = DateFormat.getDateInstance(DateFormat.FULL).format(cal.time)
                        enterWeightMsgView!!.visibility = View.GONE

                        adapter!!.setList(reverseWeightList)
                        adapter!!.notifyDataSetChanged()
                    } else {  // 몸무게 데이터가 없을때
                        lastWeightView!!.setText("0.0")
                        lastDateView!!.setText(R.string.enter_weight)
                        enterWeightMsgView!!.visibility = View.VISIBLE
                    }
                }
                .addOnFailureListener {
                    // There was a problem reading the data.
                }
    }

    private fun getConvertedWeight(weightKg: Float): Float {
        return if (isConvertUnitOn) {
            (weightKg * 2.20462262).toFloat()  // convert from kg to lbs
        } else weightKg
    }

    private fun saveWeight() {
        var w = 0f
        try {
            w = java.lang.Float.parseFloat(lastWeightView!!.text.toString().trim { it <= ' ' })
        } catch (e: Exception) {
            Log.e(TAG, "save weight error", e)
        }

        if (w <= 0) {
            showToast(R.string.wrong_number)
            return
        }

        if (isConvertUnitOn) {
            w = (w * 0.45359237).toFloat()  // convert from lbs to kg
        }

        val dataSource = DataSource.Builder()
                .setAppPackageName(activity)
                .setDataType(DataType.TYPE_WEIGHT)
                .setName("fitproj-weight")
                .setType(DataSource.TYPE_RAW)
                .build()

        val dataSet = DataSet.create(dataSource)

        val cal = Calendar.getInstance()
        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
        val startTime = cal.timeInMillis
        val endTime = startTime + 86400000L - 3600000L // 0시에서 23시까지

        val dataPoint = dataSet.createDataPoint()
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
        dataPoint.getValue(Field.FIELD_WEIGHT).setFloat(w)
        dataSet.add(dataPoint)

        Fitness.getHistoryClient(context, GoogleSignIn.getLastSignedInAccount(context))
                .insertData(dataSet)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        showToast(R.string.save_success)
                        btnModifyWrapper!!.visibility = View.GONE
                        loadData()
                    } else {
                        // There was a problem inserting the dataset.
                        showToast(R.string.common_error)
                        Log.e(TAG, "save weight error", task.exception)
                    }
                }
    }


    internal inner class Weight(var weight: Float, var date: Long, var dateText: String)

    internal inner class WeightAdapter : BaseAdapter() {

        private var list: List<Weight> = ArrayList()
        private var maxWeight: Float = 0.toFloat()
        private var minWeight: Float = 0.toFloat()
        private var itemWidth: Int = 0

        fun setList(list: List<Weight>) {
            this.list = list
            maxWeight = 0f
            minWeight = 1000f
            for (w in list) {
                if (w.weight > maxWeight) {
                    maxWeight = w.weight
                }
                if (w.weight < minWeight) {
                    minWeight = w.weight
                }
            }
            if (list.size < 6) {
                itemWidth = FitUtil.getScreenWidth(activity) / list.size
            } else {
                itemWidth = FitUtil.getScreenWidth(activity) / 6
            }
        }

        override fun getCount(): Int {
            return list.size
        }

        override fun getItem(position: Int): Weight {
            return list[position]
        }

        override fun getItemId(position: Int): Long {
            return 0
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val v: WeightItemView
            if (convertView == null) {
                v = WeightItemView(activity)
                val lp = ViewGroup.LayoutParams(itemWidth, ViewGroup.LayoutParams.MATCH_PARENT)
                v.layoutParams = lp
                v.setRange(maxWeight, minWeight)
            } else {
                v = (convertView as WeightItemView?)!!
            }

            val w = getItem(position)
            v.setDateText(w.dateText)
            v.setWeight(w.weight, isConvertUnitOn)
            if (position > 0) {
                val prev = getItem(position - 1)
                v.setPrevWeight(prev.weight)
            } else {
                v.setPrevWeight(0f)
            }
            if (position < count - 1) {
                val next = getItem(position + 1)
                v.setNextWeight(next.weight)
            } else {
                v.setNextWeight(0f)
            }

            return v
        }
    }

    companion object {

        private val TAG = "WalkHolic"

        fun newInstance(): MainDataFragment {
            return MainDataFragment()
        }
    }

}
