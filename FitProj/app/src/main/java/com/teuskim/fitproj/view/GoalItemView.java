package com.teuskim.fitproj.view;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.teuskim.fitproj.R;
import com.teuskim.fitproj.common.Goal;

/**
 * 목표뷰
 */
public class GoalItemView extends LinearLayout {

    private ImageView iconView;
    private TextView text1View, text2View;
    private CircleGraphView graphView;
    private TextView textPercentView;
    private View clickArea;

    public GoalItemView(Context context) {
        super(context);
        init(context);
    }

    public GoalItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public GoalItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.goal_item_view, this);
        iconView = (ImageView)findViewById(R.id.icon);
        text1View = (TextView)findViewById(R.id.text1);
        text2View = (TextView)findViewById(R.id.text2);
        graphView = (CircleGraphView)findViewById(R.id.graph);
        textPercentView = (TextView)findViewById(R.id.text_percent);
        clickArea = findViewById(R.id.click_area);
    }

    @Override
    public void setOnClickListener(final OnClickListener l) {
        clickArea.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                l.onClick(GoalItemView.this);
            }
        });
    }

    public void setIcon(Goal g) {
        switch (g.getType()) {
            case WALK: iconView.setBackgroundResource(R.drawable.walk_anim_icon); break;
            case RUN: iconView.setBackgroundResource(R.drawable.run_anim_icon); break;
            case CYCLE: iconView.setBackgroundResource(R.drawable.cycle_anim_icon); break;
        }
    }

    private Handler handler;
    public void startIconAnimate(int delay) {
        if (handler == null) {
            handler = new Handler();
        }
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                AnimationDrawable ad = (AnimationDrawable)iconView.getBackground();
                ad.start();
            }
        }, delay);
    }

    public void setText(String text1, String text2) {
        text1View.setText(text1);
        text2View.setText(text2);
    }

    public void showAndSetGraph(int bgColor, int graphColor, float curr, float dest) {
        graphView.setVisibility(View.VISIBLE);
        graphView.resetGraph();
        graphView.setFactors(0.7f, 0.15f, 0.02f, bgColor, bgColor, 77);
        int maxVal = 100;
        int pos = graphView.addGraph(new int[]{graphColor}, maxVal);
        float val = maxVal*curr/dest;
        graphView.setValue(pos, val, false);
        textPercentView.setText((int)val+"%");
    }

    public void hideGraph() {
        graphView.setVisibility(View.GONE);
    }

}
