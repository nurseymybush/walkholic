package com.teuskim.fitproj.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.teuskim.fitproj.common.FitUtil;

/**
 * 몸무게 그래프 아이템뷰
 */
public class WeightItemView extends View {

    private static final int COLOR_WHITE = 0xffffffff;
    private static final int COLOR_CYAN = 0xff57e2ff;
    private static final int COLOR_LINE_DARK = 0xff51647c;

    private Paint paint;
    private float dp1, dp4, dp8, dp12, dp30;
    private String dateText;
    private String weightText;
    private float maxWeight, minWeight;
    private float weightRate, prevWeightRate, nextWeightRate;
    private boolean hasPrev, hasNext;

    public WeightItemView(Context context) {
        super(context);
        init();
    }

    public WeightItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WeightItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        dp1 = FitUtil.convertDpToPx(1, getResources());
        dp4 = FitUtil.convertDpToPx(4, getResources());
        dp8 = FitUtil.convertDpToPx(8, getResources());
        dp12 = FitUtil.convertDpToPx(12, getResources());
        dp30 = FitUtil.convertDpToPx(30, getResources());
    }

    public void setDateText(String dateText) {
        this.dateText = dateText;
    }

    public void setRange(float maxWeight, float minWeight) {
        this.maxWeight = maxWeight;
        this.minWeight = minWeight;
    }

    public void setWeight(float weight, boolean isConvertedUnit) {
        if (isConvertedUnit) {
            weightText = String.format("%.1f lbs", weight);
        } else {
            weightText = String.format("%.1f kg", weight);
        }
        weightRate = getWeightRate(weight);
    }

    public void setPrevWeight(float prevWeight) {
        if (prevWeight > 0) {
            prevWeightRate = getWeightRate(prevWeight);
            hasPrev = true;
        } else {
            hasPrev = false;
        }
    }

    public void setNextWeight(float nextWeight) {
        if (nextWeight > 0) {
            nextWeightRate = getWeightRate(nextWeight);
            hasNext = true;
        } else {
            hasNext = false;
        }
    }

    private float getWeightRate(float weight) {
        if (maxWeight == minWeight) {
            return 0.5f;
        } else {
            return (maxWeight-weight) / (maxWeight-minWeight);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float h = getHeight();
        float currX = getWidth() / 2;
        float tmp = dp30+dp12+dp8+dp8;
        float currY = (h-tmp) * weightRate + dp8;

        // 이전몸무게에서 이어지는 선그리기
        paint.setColor(COLOR_CYAN);
        paint.setStrokeWidth(dp4);
        if (hasPrev) {
            float prevX = currX - getWidth();
            float prevY = (h-tmp) * prevWeightRate + dp8;
            canvas.drawLine(currX, currY, prevX, prevY, paint);
        }
        // 다음몸무게로 이어지는 선그리기
        if (hasNext) {
            float nextX = currX + getWidth();
            float nextY = (h-tmp) * nextWeightRate + dp8;
            canvas.drawLine(currX, currY, nextX, nextY, paint);
        }

        // 아래로 내려가는 기준선 그리기
        paint.setColor(COLOR_LINE_DARK);
        paint.setStrokeWidth(dp1);
        canvas.drawLine(currX, currY, currX, h-dp12-dp12-dp4, paint);

        // 몸무게 해당 높이에 원그리기
        paint.setColor(COLOR_WHITE);
        canvas.drawCircle(currX, currY, dp8, paint);
        paint.setColor(COLOR_CYAN);
        canvas.drawCircle(currX, currY, dp4, paint);

        // 날짜텍스트 출력
        paint.setColor(COLOR_WHITE);
        paint.setTextSize(dp12);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(dateText, getWidth()/2, h-dp12-dp4, paint);

        // 몸무게 출력
        paint.setColor(COLOR_CYAN);
        paint.setTextSize(dp12);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(weightText, getWidth()/2, h-dp4, paint);
    }
}
