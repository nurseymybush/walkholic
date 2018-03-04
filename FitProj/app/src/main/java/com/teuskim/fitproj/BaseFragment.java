package com.teuskim.fitproj;

import android.support.v4.app.Fragment;
import android.widget.Toast;

/**
 * 모든 프레그먼트의 기반이 되는 프레그먼트
 */
public class BaseFragment extends Fragment {

    @Override
    public void onStart() {
        super.onStart();
        getView().setClickable(true);
    }

    protected void openFragment(BaseFragment fr) {
        try {
            ((MainActivity)getActivity()).openFragment(fr);
        } catch (Exception e) {}
    }

    protected void finish() {
        try {
            ((MainActivity)getActivity()).closeFragment(this);
        } catch (Exception e) {}
    }

    protected void showToast(int resId) {
        Toast.makeText(getActivity(), resId, Toast.LENGTH_SHORT).show();
    }

    protected int getStatusBarColor() {
        return getResources().getColor(R.color.bg_notibar);
    }

}
