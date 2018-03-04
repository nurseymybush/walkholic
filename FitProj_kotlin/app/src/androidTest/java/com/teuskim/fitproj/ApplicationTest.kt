package com.teuskim.fitproj

import android.app.Application
import android.test.ApplicationTestCase
import android.util.Log

import java.util.ArrayList
import java.util.Calendar

/**
 * [Testing Fundamentals](http://d.android.com/tools/testing/testing_android.html)
 */
class ApplicationTest : ApplicationTestCase<Application>(Application::class.java) {

    fun testJavaExample1() {
        var cal = Calendar.getInstance()
        var dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        Log.e("TEST", "today:" + dayOfWeek)

        cal.set(2015, 1, 1)
        cal = Calendar.getInstance()
        dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        Log.e("TEST", "other day:" + dayOfWeek)

        cal.set(2015, 1, 2)
        cal = Calendar.getInstance()
        dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        Log.e("TEST", "other day:" + dayOfWeek)
    }

    fun testHasStep() {
        val stimes = ArrayList<Long>()
        val etimes = ArrayList<Long>()

        stimes.add(1L)
        etimes.add(2L)
        stimes.add(2L)
        etimes.add(3L)
        stimes.add(6L)
        etimes.add(7L)

        Assert.assertTrue(hasStep(1L, 3L, stimes, etimes))
        Assert.assertFalse(hasStep(4L, 5L, stimes, etimes))
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
}