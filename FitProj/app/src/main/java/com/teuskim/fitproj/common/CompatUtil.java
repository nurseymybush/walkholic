package com.teuskim.fitproj.common;

import android.os.Build;
import android.view.View;
import android.view.Window;

/**
 * 하위호환 필요한 메소드들 모음
 */
public class CompatUtil {

    public static void setElevation(View v, float elevation) {
        if (Build.VERSION.SDK_INT >= 21) {
            v.setElevation(elevation);
        }
    }

    public static void setStatusBarColor(Window window, int color) {
        if (Build.VERSION.SDK_INT >= 21) {
            window.setStatusBarColor(color);
        }
    }
}
