package com.teuskim.fitproj.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.teuskim.fitproj.R;

/**
 * 목표상세보기 하단의 원형상태뷰
 */
public class CircleStateView extends View {

    private Bitmap maskBm;
    private Paint filledPaint;
    private float filledRate;

    public CircleStateView(Context context) {
        super(context);
        init();
    }

    public CircleStateView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircleStateView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        maskBm = BitmapFactory.decodeResource(getResources(), R.drawable.mask_circle);
        filledPaint = new Paint();
    }

    public void setColor(int color) {
        filledPaint.setColor(color);
    }

    public void setFilledRate(float filledRate) {
        this.filledRate = filledRate;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 배경색
        if (isEnabled()) {
            canvas.drawColor(0xff314257);

            // 채우는색
            float filledTop = getHeight() * (1 - filledRate);
            canvas.drawRect(0, filledTop, getWidth(), getHeight(), filledPaint);
        } else {
            canvas.drawColor(0xff64758a);
        }

        // 원형 마스크
        canvas.save();
        canvas.scale(getWidth() / (float) maskBm.getWidth(), getHeight() / (float) maskBm.getHeight());
        canvas.drawBitmap(maskBm, 0, 0, null);
        canvas.restore();
    }
}
