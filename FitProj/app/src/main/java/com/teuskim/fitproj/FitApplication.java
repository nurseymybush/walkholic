package com.teuskim.fitproj;

import android.app.Application;

import com.teuskim.fitproj.common.FitDao;

/**
 * 앱의 어플리케이션 클래스
 */
public class FitApplication extends Application {

    @Override
    public void onTerminate() {
        FitDao.getInstance(getApplicationContext()).close();
        super.onTerminate();
    }
}
