package com.teuskim.fitproj;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.teuskim.fitproj.common.CompatUtil;
import com.teuskim.fitproj.common.FitPreference;
import com.teuskim.fitproj.common.RefreshListener;
import com.teuskim.fitproj.view.FitViewPager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


/**
 * 메인 뷰페이저 액티비티
 * 대부분의 화면이 이 액티비티를 컨테이너로 한 프레그먼트로 표현된다.
 */
public class MainActivity extends FragmentActivity {

    private static final String TAG = "WalkHolic";
    private static final int POSITION_DASHBOARD = 0;
    private static final int POSITION_GOALS = 1;
    private static final int POSITION_DATA = 2;
    private static final String AUTH_PENDING = "auth_state_pending";
    private static final int REQUEST_OAUTH_REQUEST_CODE = 1;
    private static final int REQUEST_FITNESS_PERMISSIONS = 2;

    private View tabDashboard;
    private View tabSelectedBarContainer, tabSelectedBar;
    private FitViewPager pager;
    private MainPagerAdapter adapter;

    private FitPreference pref;
    private boolean authInProgress = false;
    private Handler handler = new Handler();

    private List<RefreshListener> refreshList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        pref = FitPreference.getInstance(getApplicationContext());
        if (savedInstanceState != null) {
            authInProgress = savedInstanceState.getBoolean(AUTH_PENDING);
        }
        initViews();
        initAlarm();
        initFitness();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // 뷰의 크기가 모두 결정되고 나서 최초 한번 tabSelectedBar의 width를 설정한다.
            if (tabSelectedBar.getWidth() == 0) {
                ViewGroup.LayoutParams lp = tabSelectedBar.getLayoutParams();
                lp.width = tabDashboard.getWidth();
                tabSelectedBar.setLayoutParams(lp);
            }
        }
    }

    public void addRefreshListener(RefreshListener rl) {
        refreshList.add(rl);
    }

    public void removeRefreshListener(RefreshListener rl) {
        refreshList.remove(rl);
    }

    public void refreshAll() {
        for (RefreshListener rl : refreshList) {
            rl.refresh();
        }
    }

    private void initViews() {
        View.OnClickListener clickListener = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.tab_dashboard:
                        pager.setCurrentItem(0, true);
                        break;
                    case R.id.tab_goals:
                        pager.setCurrentItem(1, true);
                        break;
                    case R.id.tab_data:
                        pager.setCurrentItem(2, true);
                        break;
                }
            }
        };
        tabDashboard = findViewById(R.id.tab_dashboard);
        tabDashboard.setOnClickListener(clickListener);

        View tabGoals = findViewById(R.id.tab_goals);
        tabGoals.setOnClickListener(clickListener);

        View tabData = findViewById(R.id.tab_data);
        tabData.setOnClickListener(clickListener);

        tabSelectedBarContainer = findViewById(R.id.tab_selected_bar_container);
        tabSelectedBar = findViewById(R.id.tab_selected_bar);

        pager = findViewById(R.id.pager);
        adapter = new MainPagerAdapter(getSupportFragmentManager());
        pager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                setTabSelectedBarPosition(position, positionOffset);
            }

            @Override public void onPageSelected(int position) {}

            @Override public void onPageScrollStateChanged(int state) {}
        });
    }

    private void setTabSelectedBarPosition(int position, float positionOffset) {
        int paddingLeft = (int)(tabSelectedBar.getWidth() * (position + positionOffset));
        tabSelectedBarContainer.setPadding(paddingLeft, 0, 0, 0);
    }

    public void openFragment(BaseFragment fr) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.add(R.id.main_activity, fr, "fragments");
        ft.commit();

        final int statusBarColor = fr.getStatusBarColor();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                CompatUtil.setStatusBarColor(getWindow(), statusBarColor);
            }
        }, 200);
    }

    public void closeFragment(BaseFragment fr) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.remove(fr);
        ft.commit();

        resetStatusBarColor();
    }

    public void resetStatusBarColor() {
        CompatUtil.setStatusBarColor(getWindow(), getResources().getColor(R.color.bg_notibar));
    }

    @Override
    public void onBackPressed() {
        Fragment fr = getSupportFragmentManager().findFragmentByTag("fragments");
        if (fr != null) {
            ((BaseFragment)fr).finish();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(AUTH_PENDING, authInProgress);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_OAUTH_REQUEST_CODE) {
                checkAndRequestPermissions();
            }
        }
    }

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FITNESS_PERMISSIONS);
        } else {
            onPostInitFitness();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_FITNESS_PERMISSIONS) {
            onPostInitFitness();
        }
    }

    private void initFitness() {
        FitnessOptions fitnessOptions =
                FitnessOptions.builder()
                        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE)
                        .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE)
                        .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_WRITE)
                        .addDataType(DataType.AGGREGATE_DISTANCE_DELTA, FitnessOptions.ACCESS_WRITE)
                        .addDataType(DataType.TYPE_WEIGHT, FitnessOptions.ACCESS_WRITE)
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

    private void onPostInitFitness() {
        if (pager.getAdapter() != adapter) {
            pager.setAdapter(adapter);
        }
        adapter.notifyDataSetChanged();
        subscribeOrCancel();
    }

    public void subscribeOrCancel() {
        if (pref.isRecordingOn()) {
            subscribe();
        } else {
            cancelSubscription();
        }
    }

    private void subscribe() {
        DataType[] dataTypes = new DataType[]{ DataType.TYPE_STEP_COUNT_DELTA, DataType.TYPE_DISTANCE_DELTA };
        for (int i=0; i<dataTypes.length; i++) {
            Fitness.getRecordingClient(this, GoogleSignIn.getLastSignedInAccount(this))
                    .subscribe(dataTypes[i])
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.i(TAG, "Successfully subscribed!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "There was a problem subscribing.", e);
                        }
                    });
        }
    }

    private void cancelSubscription() {
        DataType[] dataTypes = new DataType[]{ DataType.TYPE_STEP_COUNT_DELTA, DataType.TYPE_DISTANCE_DELTA };
        for (int i=0; i<dataTypes.length; i++) {
            Fitness.getRecordingClient(this, GoogleSignIn.getLastSignedInAccount(this))
                    .unsubscribe(dataTypes[i])
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.i(TAG, "Successfully unsubscribed!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Subscription not removed
                            Log.e(TAG, "Failed to unsubscribe.");
                        }
                    });
        }
    }

    public void initAlarm() {
        AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        Intent i = new Intent(getApplicationContext(), AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(getApplicationContext(), 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
        if (pref.isAlarmOn()) {
            Calendar cal = Calendar.getInstance();
            cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 17, 0);
            long stime = cal.getTimeInMillis();
            if (stime < System.currentTimeMillis()) {
                stime += 86400000L;
            }
            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, stime, AlarmManager.INTERVAL_DAY, pi);
        } else {
            alarmManager.cancel(pi);
        }
    }

    public void setViewPagerOnChildMoveListener(final FitViewPager.OnChildMoveListener l) {
        pager.setOnChildMoveListener(new FitViewPager.OnChildMoveListener() {
            @Override
            public boolean onMove(float diffX) {
                return (pager.getCurrentItem() == POSITION_DATA && l.onMove(diffX));
            }
        });
    }


    class MainPagerAdapter extends FragmentPagerAdapter {

        MainPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case POSITION_DASHBOARD:default:
                    return MainDashboardFragment.newInstance();
                case POSITION_GOALS:
                    return MainGoalsFragment.newInstance();
                case POSITION_DATA:
                    return MainDataFragment.newInstance();
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    }
}
