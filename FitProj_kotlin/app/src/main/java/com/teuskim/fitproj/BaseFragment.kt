package com.teuskim.fitproj

import android.support.v4.app.Fragment
import android.widget.Toast

/**
 * 모든 프레그먼트의 기반이 되는 프레그먼트
 */
open class BaseFragment : Fragment() {

    open var statusBarColor: Int = 0
        get() = resources.getColor(R.color.bg_notibar)

    override fun onStart() {
        super.onStart()
        view!!.isClickable = true
    }

    protected fun openFragment(fr: BaseFragment) {
        try {
            (activity as MainActivity).openFragment(fr)
        } catch (e: Exception) {
        }

    }

    open fun finish() {
        try {
            (activity as MainActivity).closeFragment(this)
        } catch (e: Exception) {
        }

    }

    protected fun showToast(resId: Int) {
        Toast.makeText(activity, resId, Toast.LENGTH_SHORT).show()
    }

}
