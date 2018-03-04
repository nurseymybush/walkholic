package com.teuskim.fitproj

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Vibrator

import com.teuskim.fitproj.common.DistanceSet
import com.teuskim.fitproj.common.FitDao
import com.teuskim.fitproj.common.FitPreference
import com.teuskim.fitproj.common.FitUtil
import com.teuskim.fitproj.common.Goal

import java.util.Calendar

/**
 * 알람 설정이 켜져있는경우 매일 오후 5시에 알림이 온다.
 */
class AlarmReceiver : BroadcastReceiver() {

    private var context: Context? = null

    override fun onReceive(pcontext: Context, intent: Intent) {
        this.context = pcontext

        // 오늘 달성해야하는 목표들 가져오기
        val cal = Calendar.getInstance()
        val todayPosition = cal.get(Calendar.DAY_OF_WEEK) - 1
        val glist = FitDao.getInstance(pcontext)!!.getGoalList(todayPosition)

        // 오늘 목표가 없는 경우
        if (glist.size == 0) {
            return
        }

        // 목표량
        var walkGoal = 0f
        var runGoal = 0f
        var cycleGoal = 0f
        val isConvertUnitOn = FitPreference.getInstance(pcontext).isConvertUnitOn
        for (g in glist) {
            when (g.type) {
                Goal.Type.WALK -> walkGoal = g.getAmount(isConvertUnitOn)
                Goal.Type.RUN -> runGoal = g.getAmount(isConvertUnitOn)
                Goal.Type.CYCLE -> cycleGoal = g.getAmount(isConvertUnitOn)
            }
        }

        // 데이터 가져오기
        val endTime = cal.timeInMillis
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        cal.set(year, month, day, 0, 0, 0)
        val startTime = cal.timeInMillis

        val walkGoal2 = walkGoal
        val runGoal2 = runGoal
        val cycleGoal2 = cycleGoal

        FitUtil.getDistanceSet(pcontext, startTime, endTime, object: FitUtil.RecvDistanceSet {
            override fun onRecvDistanceSet(distanceSet: DistanceSet) {
                // 목표량을 채우지 못한 목표를 확인하고, 메시지를 만든다.
                if (distanceSet.getWalk(isConvertUnitOn) < walkGoal2
                        || distanceSet.getRun(isConvertUnitOn) < runGoal2
                        || distanceSet.getCycle(isConvertUnitOn) < cycleGoal2) {

                    var walk = false
                    var run = false
                    var cycle = false
                    val contentTitle = context!!.getString(R.string.noti_msg)
                    val sb = StringBuilder()
                    if (distanceSet.getWalk(isConvertUnitOn) < walkGoal2) {
                        sb.append(context!!.getString(R.string.noti_walk))
                        walk = true
                    }
                    if (distanceSet.getRun(isConvertUnitOn) < runGoal2) {
                        sb.append(context!!.getString(R.string.noti_run))
                        run = true
                    }
                    if (distanceSet.getCycle(isConvertUnitOn) < cycleGoal2) {
                        sb.append(context!!.getString(R.string.noti_cycle))
                        cycle = true
                    }
                    val contentText = sb.toString().substring(1)

                    // 진동
                    val v = context!!.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    v.vibrate(500)

                    // 노티바에 노티한다.
                    val i = Intent(context, MainActivity::class.java)
                    i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    val pi = PendingIntent.getActivity(context, 0, i, 0)
                    val icon: Int
                    if (cycle) {
                        icon = R.drawable.ic_status_c
                    } else if (run) {
                        icon = R.drawable.ic_status_r
                    } else {
                        icon = R.drawable.ic_status_w
                    }

                    val n = Notification.Builder(context)
                            .setContentTitle(contentTitle)
                            .setContentText(contentText)
                            .setSmallIcon(icon)
                            .setContentIntent(pi)
                            .setAutoCancel(true)
                            .notification
                    val nm = context!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(0, n)
                }
            }
        })
    }
}
