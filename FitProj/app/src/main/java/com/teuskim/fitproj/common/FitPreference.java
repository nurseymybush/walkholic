package com.teuskim.fitproj.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * 데이터 저장 클래스 (DB를 사용하지 않는 간단한 것들)
 */
public class FitPreference {

    private static final String KEY_IS_RECORDING_ON = "recording_on";
    private static final String KEY_IS_ALARM_ON = "alarm_on";
    private static final String KEY_IS_CONVERT_UNIT_ON = "convert_unit_on";

    private static FitPreference instance;
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    private FitPreference(Context context){
        pref = PreferenceManager.getDefaultSharedPreferences(context);
        editor = pref.edit();
    }

    public synchronized static FitPreference getInstance(Context context){

        if(instance != null){
            return instance;
        }

        instance = new FitPreference(context);
        return instance;
    }

    public boolean setRecordingOn(boolean isRecordingOn) {
        editor.putBoolean(KEY_IS_RECORDING_ON, isRecordingOn);
        return editor.commit();
    }

    public boolean isRecordingOn() {
        return pref.getBoolean(KEY_IS_RECORDING_ON, true);
    }

    public boolean setAlarmOn(boolean isAlarmOn) {
        editor.putBoolean(KEY_IS_ALARM_ON, isAlarmOn);
        return editor.commit();
    }

    public boolean isAlarmOn() {
        return pref.getBoolean(KEY_IS_ALARM_ON, true);
    }

    public boolean setConvertUnitOn(boolean isConvertUnitOn) {
        editor.putBoolean(KEY_IS_CONVERT_UNIT_ON, isConvertUnitOn);
        return editor.commit();
    }

    public boolean isConvertUnitOn() {
        return pref.getBoolean(KEY_IS_CONVERT_UNIT_ON, false);
    }
}
