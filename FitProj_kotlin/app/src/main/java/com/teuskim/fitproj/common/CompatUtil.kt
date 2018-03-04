package com.teuskim.fitproj.common

import android.os.Build
import android.view.View
import android.view.Window

/**
 * 하위호환 필요한 메소드들 모음
 */
object CompatUtil {

    fun setElevation(v: View, elevation: Float) {
        if (Build.VERSION.SDK_INT >= 21) {
            v.elevation = elevation
        }
    }

    fun setStatusBarColor(window: Window, color: Int) {
        if (Build.VERSION.SDK_INT >= 21) {
            window.statusBarColor = color
        }
    }
}
