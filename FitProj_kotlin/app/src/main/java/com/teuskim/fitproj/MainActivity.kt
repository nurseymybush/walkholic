package com.teuskim.fitproj

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.*
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.util.Log
import android.view.View
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.teuskim.fitproj.common.CompatUtil
import com.teuskim.fitproj.common.FitPreference
import com.teuskim.fitproj.common.RefreshListener
import com.teuskim.fitproj.view.FitViewPager
import java.util.*


/**
 * 메인 뷰페이저 액티비티
 * 대부분의 화면이 이 액티비티를 컨테이너로 한 프레그먼트로 표현된다.
 */
class MainActivity : FragmentActivity() {

    private var tabDashboard: View? = null
    private var tabSelectedBarContainer: View? = null
    private var tabSelectedBar: View? = null
    private var pager: FitViewPager? = null
    private var adapter: MainPagerAdapter? = null

    private var pref: FitPreference? = null
    private var authInProgress = false
    private val handler = Handler()

    private val refreshList = ArrayList<RefreshListener>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        pref = FitPreference.getInstance(applicationContext)
        if (savedInstanceState != null) {
            authInProgress = savedInstanceState.getBoolean(AUTH_PENDING)
        }
        initViews()
        initAlarm()
        initFitness()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // 뷰의 크기가 모두 결정되고 나서 최초 한번 tabSelectedBar의 width를 설정한다.
            if (tabSelectedBar!!.width == 0) {
                val lp = tabSelectedBar!!.layoutParams
                lp.width = tabDashboard!!.width
                tabSelectedBar!!.layoutParams = lp
            }
        }
    }

    fun addRefreshListener(rl: RefreshListener) {
        refreshList.add(rl)
    }

    fun removeRefreshListener(rl: RefreshListener) {
        refreshList.remove(rl)
    }

    fun refreshAll() {
        for (rl in refreshList) {
            rl.refresh()
        }
    }

    private fun initViews() {
        val clickListener = View.OnClickListener { v ->
            when (v.id) {
                R.id.tab_dashboard -> pager!!.setCurrentItem(0, true)
                R.id.tab_goals -> pager!!.setCurrentItem(1, true)
                R.id.tab_data -> pager!!.setCurrentItem(2, true)
            }
        }
        tabDashboard = findViewById(R.id.tab_dashboard)
        tabDashboard!!.setOnClickListener(clickListener)

        val tabGoals = findViewById<View>(R.id.tab_goals)
        tabGoals.setOnClickListener(clickListener)

        val tabData = findViewById<View>(R.id.tab_data)
        tabData.setOnClickListener(clickListener)

        tabSelectedBarContainer = findViewById(R.id.tab_selected_bar_container)
        tabSelectedBar = findViewById(R.id.tab_selected_bar)

        pager = findViewById(R.id.pager)
        adapter = MainPagerAdapter(supportFragmentManager)
        pager!!.setOnPageChangeListener(object : ViewPager.OnPageChangeListener {

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                setTabSelectedBarPosition(position, positionOffset)
            }

            override fun onPageSelected(position: Int) {}

            override fun onPageScrollStateChanged(state: Int) {}
        })
    }

    private fun setTabSelectedBarPosition(position: Int, positionOffset: Float) {
        val paddingLeft = (tabSelectedBar!!.width * (position + positionOffset)).toInt()
        tabSelectedBarContainer!!.setPadding(paddingLeft, 0, 0, 0)
    }

    fun openFragment(fr: BaseFragment) {
        val fm = supportFragmentManager
        val ft = fm.beginTransaction()
        ft.add(R.id.main_activity, fr, "fragments")
        ft.commit()

        val statusBarColor = fr.statusBarColor
        handler.postDelayed({ CompatUtil.setStatusBarColor(window, statusBarColor) }, 200)
    }

    fun closeFragment(fr: BaseFragment) {
        val ft = supportFragmentManager.beginTransaction()
        ft.remove(fr)
        ft.commit()

        resetStatusBarColor()
    }

    fun resetStatusBarColor() {
        CompatUtil.setStatusBarColor(window, resources.getColor(R.color.bg_notibar))
    }

    override fun onBackPressed() {
        val fr = supportFragmentManager.findFragmentByTag("fragments")
        if (fr != null) {
            (fr as BaseFragment).finish()
        } else {
            super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(AUTH_PENDING, authInProgress)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_OAUTH_REQUEST_CODE) {
                checkAndRequestPermissions()
            }
        }
    }

    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_FITNESS_PERMISSIONS)
        } else {
            onPostInitFitness()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_FITNESS_PERMISSIONS) {
            onPostInitFitness()
        }
    }

    private fun initFitness() {
        val fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE)
                .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE)
                .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_WRITE)
                .addDataType(DataType.AGGREGATE_DISTANCE_DELTA, FitnessOptions.ACCESS_WRITE)
                .addDataType(DataType.TYPE_WEIGHT, FitnessOptions.ACCESS_WRITE)
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

    private fun onPostInitFitness() {
        if (pager!!.adapter !== adapter) {
            pager!!.adapter = adapter
        }
        adapter!!.notifyDataSetChanged()
        subscribeOrCancel()
    }

    fun subscribeOrCancel() {
        if (pref!!.isRecordingOn) {
            subscribe()
        } else {
            cancelSubscription()
        }
    }

    private fun subscribe() {
        val dataTypes = arrayOf(DataType.TYPE_STEP_COUNT_DELTA, DataType.TYPE_DISTANCE_DELTA)
        for (i in dataTypes.indices) {
            Fitness.getRecordingClient(this, GoogleSignIn.getLastSignedInAccount(this))
                    .subscribe(dataTypes[i])
                    .addOnSuccessListener { Log.i(TAG, "Successfully subscribed!") }
                    .addOnFailureListener { e -> Log.e(TAG, "There was a problem subscribing.", e) }
        }
    }

    private fun cancelSubscription() {
        val dataTypes = arrayOf(DataType.TYPE_STEP_COUNT_DELTA, DataType.TYPE_DISTANCE_DELTA)
        for (i in dataTypes.indices) {
            Fitness.getRecordingClient(this, GoogleSignIn.getLastSignedInAccount(this))
                    .unsubscribe(dataTypes[i])
                    .addOnSuccessListener { Log.i(TAG, "Successfully unsubscribed!") }
                    .addOnFailureListener {
                        // Subscription not removed
                        Log.e(TAG, "Failed to unsubscribe.")
                    }
        }
    }

    fun initAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager ?: return
        val i = Intent(applicationContext, AlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(applicationContext, 0, i, PendingIntent.FLAG_CANCEL_CURRENT)
        if (pref!!.isAlarmOn) {
            val cal = Calendar.getInstance()
            cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 17, 0)
            var stime = cal.timeInMillis
            if (stime < System.currentTimeMillis()) {
                stime += 86400000L
            }
            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, stime, AlarmManager.INTERVAL_DAY, pi)
        } else {
            alarmManager.cancel(pi)
        }
    }

    fun setViewPagerOnChildMoveListener(l: FitViewPager.OnChildMoveListener) {
        pager!!.setOnChildMoveListener(object: FitViewPager.OnChildMoveListener {
            override fun onMove(diffX: Float): Boolean {
                return pager!!.currentItem == POSITION_DATA && l.onMove(diffX)
            }
        })
    }


    internal inner class MainPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            when (position) {
                POSITION_DASHBOARD -> return MainDashboardFragment.newInstance()
                POSITION_GOALS -> return MainGoalsFragment.newInstance()
                POSITION_DATA -> return MainDataFragment.newInstance()
                else -> return MainDashboardFragment.newInstance()
            }
        }

        override fun getCount(): Int {
            return 3
        }
    }

    companion object {

        private val TAG = "WalkHolic"
        private val POSITION_DASHBOARD = 0
        private val POSITION_GOALS = 1
        private val POSITION_DATA = 2
        private val AUTH_PENDING = "auth_state_pending"
        private val REQUEST_OAUTH_REQUEST_CODE = 1
        private val REQUEST_FITNESS_PERMISSIONS = 2
    }
}
