package com.teuskim.fitproj

import android.app.Application

import com.teuskim.fitproj.common.FitDao

/**
 * 앱의 어플리케이션 클래스
 */
class FitApplication : Application() {

    override fun onTerminate() {
        FitDao.getInstance(applicationContext)!!.close()
        super.onTerminate()
    }
}
