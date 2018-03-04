package com.teuskim.fitproj.common

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

/**
 * 데이터 저장 클래스 (DB를 사용하지 않는 간단한 것들)
 */
class FitPreference private constructor(context: Context) {
    private val pref: SharedPreferences
    private val editor: SharedPreferences.Editor

    val isRecordingOn: Boolean
        get() = pref.getBoolean(KEY_IS_RECORDING_ON, true)

    val isAlarmOn: Boolean
        get() = pref.getBoolean(KEY_IS_ALARM_ON, true)

    val isConvertUnitOn: Boolean
        get() = pref.getBoolean(KEY_IS_CONVERT_UNIT_ON, false)

    init {
        pref = PreferenceManager.getDefaultSharedPreferences(context)
        editor = pref.edit()
    }

    fun setRecordingOn(isRecordingOn: Boolean): Boolean {
        editor.putBoolean(KEY_IS_RECORDING_ON, isRecordingOn)
        return editor.commit()
    }

    fun setAlarmOn(isAlarmOn: Boolean): Boolean {
        editor.putBoolean(KEY_IS_ALARM_ON, isAlarmOn)
        return editor.commit()
    }

    fun setConvertUnitOn(isConvertUnitOn: Boolean): Boolean {
        editor.putBoolean(KEY_IS_CONVERT_UNIT_ON, isConvertUnitOn)
        return editor.commit()
    }

    companion object {

        private val KEY_IS_RECORDING_ON = "recording_on"
        private val KEY_IS_ALARM_ON = "alarm_on"
        private val KEY_IS_CONVERT_UNIT_ON = "convert_unit_on"

        private var instance: FitPreference? = null

        @Synchronized
        fun getInstance(context: Context): FitPreference {

            if (instance != null) {
                return instance as FitPreference
            }

            instance = FitPreference(context)
            return instance as FitPreference
        }
    }
}
