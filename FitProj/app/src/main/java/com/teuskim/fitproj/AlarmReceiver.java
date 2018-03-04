package com.teuskim.fitproj;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;

import com.teuskim.fitproj.common.DistanceSet;
import com.teuskim.fitproj.common.FitDao;
import com.teuskim.fitproj.common.FitPreference;
import com.teuskim.fitproj.common.FitUtil;
import com.teuskim.fitproj.common.Goal;

import java.util.Calendar;
import java.util.List;

/**
 * 알람 설정이 켜져있는경우 매일 오후 5시에 알림이 온다.
 */
public class AlarmReceiver extends BroadcastReceiver {

    private Context context;

    @Override
    public void onReceive(Context pcontext, Intent intent) {
        this.context = pcontext;

        // 오늘 달성해야하는 목표들 가져오기
        Calendar cal = Calendar.getInstance();
        int todayPosition = cal.get(Calendar.DAY_OF_WEEK)-1;
        List<Goal> glist = FitDao.getInstance(context).getGoalList(todayPosition);

        // 오늘 목표가 없는 경우
        if (glist.size() == 0) {
            return;
        }

        // 목표량
        float walkGoal = 0;
        float runGoal = 0;
        float cycleGoal = 0;
        final boolean isConvertUnitOn = FitPreference.getInstance(context).isConvertUnitOn();
        for (Goal g : glist) {
            switch (g.getType()) {
                case WALK: walkGoal=g.getAmount(isConvertUnitOn); break;
                case RUN: runGoal=g.getAmount(isConvertUnitOn); break;
                case CYCLE: cycleGoal=g.getAmount(isConvertUnitOn); break;
            }
        }

        // 데이터 가져오기
        long endTime = cal.getTimeInMillis();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        cal.set(year, month, day, 0, 0, 0);
        long startTime = cal.getTimeInMillis();

        final float walkGoal2 = walkGoal;
        final float runGoal2 = runGoal;
        final float cycleGoal2 = cycleGoal;

        FitUtil.getDistanceSet(context, startTime, endTime, new FitUtil.RecvDistanceSet() {
            @Override
            public void onRecvDistanceSet(DistanceSet distanceSet) {
                // 목표량을 채우지 못한 목표를 확인하고, 메시지를 만든다.
                if (distanceSet.getWalk(isConvertUnitOn) < walkGoal2
                        || distanceSet.getRun(isConvertUnitOn) < runGoal2
                        || distanceSet.getCycle(isConvertUnitOn) < cycleGoal2) {

                    boolean walk = false;
                    boolean run = false;
                    boolean cycle = false;
                    String contentTitle = context.getString(R.string.noti_msg);
                    StringBuilder sb = new StringBuilder();
                    if (distanceSet.getWalk(isConvertUnitOn) < walkGoal2) {
                        sb.append(context.getString(R.string.noti_walk));
                        walk = true;
                    }
                    if (distanceSet.getRun(isConvertUnitOn) < runGoal2) {
                        sb.append(context.getString(R.string.noti_run));
                        run = true;
                    }
                    if (distanceSet.getCycle(isConvertUnitOn) < cycleGoal2) {
                        sb.append(context.getString(R.string.noti_cycle));
                        cycle = true;
                    }
                    String contentText = sb.toString().substring(1);

                    // 진동
                    Vibrator v = (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
                    v.vibrate(500);

                    // 노티바에 노티한다.
                    Intent i = new Intent(context, MainActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    PendingIntent pi = PendingIntent.getActivity(context, 0, i, 0);
                    int icon;
                    if (cycle) {
                        icon = R.drawable.ic_status_c;
                    } else if (run) {
                        icon = R.drawable.ic_status_r;
                    } else {
                        icon = R.drawable.ic_status_w;
                    }

                    Notification n = new Notification.Builder(context)
                            .setContentTitle(contentTitle)
                            .setContentText(contentText)
                            .setSmallIcon(icon)
                            .setContentIntent(pi)
                            .setAutoCancel(true)
                            .getNotification();
                    NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
                    nm.notify(0, n);
                }
            }
        });
    }
}
