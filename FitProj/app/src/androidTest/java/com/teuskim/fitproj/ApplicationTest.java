package com.teuskim.fitproj;

import android.app.Application;
import android.test.ApplicationTestCase;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    public ApplicationTest() {
        super(Application.class);
    }

    public void testJavaExample1() {
        Calendar cal = Calendar.getInstance();
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        Log.e("TEST", "today:"+dayOfWeek);

        cal.set(2015, 1, 1);
        cal = Calendar.getInstance();
        dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        Log.e("TEST", "other day:"+dayOfWeek);

        cal.set(2015, 1, 2);
        cal = Calendar.getInstance();
        dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        Log.e("TEST", "other day:"+dayOfWeek);
    }

    public void testHasStep() {
        List<Long> stimes = new ArrayList<>();
        List<Long> etimes = new ArrayList<>();

        stimes.add(1l); etimes.add(2l);
        stimes.add(2l); etimes.add(3l);
        stimes.add(6l); etimes.add(7l);

        assertTrue(hasStep(1l, 3l, stimes, etimes));
        assertFalse(hasStep(4l, 5l, stimes, etimes));
    }

    private boolean hasStep(long smin, long emin, List<Long> startStepTimes, List<Long> endStepTimes) {
        // smin 직전에 endtime이 있고(즉, starttime이 없고), [smin,emin] 구간에 starttime이 없으면, 해당 구간에 step이 없는 것이다.
        long stime = 0;
        long stime2 = 0;
        for (long t : startStepTimes) {
            if (t > smin) {
                if (t > emin) {
                    break;
                } else {
                    stime2 = t;
                }
            } else {
                stime = t;
            }
        }
        long etime = 0;
        for (long t : endStepTimes) {
            if (t > smin) {
                break;
            } else {
                etime = t;
            }
        }
        if (stime < etime && stime2 == 0) {
            return false;
        }
        return true;
    }
}