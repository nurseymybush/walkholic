package com.teuskim.fitproj.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * 원형 그래프 뷰
 */
public class CircleGraphView extends View {

    private Paint paintInnerCircle;
    private Paint paintBG;
    private RectF layoutArea;
    private List<Graph> listGraph;
    private float innerCircleRate;
    private float graphWidthRate;
    private float spacingRate;
    private int graphBgAlpha;

    private Handler invalidateHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            invalidate();
        }
    };

    public CircleGraphView(Context context) {
        super(context);
        init();
    }

    public CircleGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircleGraphView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paintInnerCircle = new Paint();
        paintBG = new Paint();
        listGraph = new ArrayList<>();
        layoutArea = new RectF();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = resolveSize(0, widthMeasureSpec);
        int h = resolveSize(0, heightMeasureSpec);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        layoutArea.set(0, 0, getWidth(), getHeight());
        float cx = layoutArea.centerX();
        float cy = layoutArea.centerY();
        float maxRadius = Math.min(cx, cy);
        float layoutSize = Math.min(getWidth(), getHeight());
        float insetX = (getWidth()-layoutSize)/2;
        float insetY = (getHeight()-layoutSize)/2;
        layoutArea.inset(insetX, insetY);

        // 작은원 반지름
        float innerCircleRadius = maxRadius * innerCircleRate;
        // 그래프 두께
        float graphWidth = maxRadius * graphWidthRate;
        // 간격 크기
        float spacingSize = maxRadius * spacingRate;

        boolean needAnimate = false;

        if (listGraph.size() > 0) {
            // 가장큰원의 inset
            float inset = maxRadius - (innerCircleRadius + ((graphWidth + spacingSize) * listGraph.size()));
            layoutArea.inset(inset, inset);
            float r = maxRadius - inset - graphWidth/2;

            // 가장 큰 그래프부터 그리기
            for (int i=listGraph.size()-1; i>=0; i--) {
                // 그래프 사이 간격을 위해 원그리고 크기 줄이기
                canvas.drawCircle(cx, cy, r+(graphWidth/2), paintBG);
                layoutArea.inset(spacingSize, spacingSize);
                r -= spacingSize;

                Graph g = listGraph.get(i);

                // 그래프 끝부분 둥근 효과를 위해 작은원 그리기
                // 시작지점
                canvas.drawCircle(cx, cy-r, graphWidth/2, g.getStartPaint());

                // 끝지점
                canvas.drawCircle(g.getEndX(cx, r), g.getEndY(cy, r), graphWidth/2, g.getEndPaint());

                // 그래프 그리기
                Paint pt = g.getGraphPaint(cx, cy);
                pt.setAlpha(graphBgAlpha);
                canvas.drawCircle(cx, cy, r+(graphWidth/2), pt);
                pt.setAlpha(255);
                canvas.drawArc(layoutArea, g.getStartAngle(), g.getAngle(), true, pt);

                // 원크기 줄이기
                layoutArea.inset(graphWidth, graphWidth);
                r -= (graphWidth);
            }

            // 애니메이션 관련
            for (Graph g : listGraph) {
                if (g.needAnimateMore()) {
                    needAnimate = true;
                    g.incrementAnimVal();
                    break;
                }
            }
        }

        // 중앙에 마스크 덮기
        canvas.drawCircle(cx, cy, innerCircleRadius, paintInnerCircle);

        // 애니메이션
        if (needAnimate) {
            invalidateHandler.sendEmptyMessageDelayed(0, 5);
        }
    }

    /**
     * 그래프 추가
     * @param colors 그래프 색상들. 2개이상일 경우 그라데이션 효과
     * @param maxVal 그래프의 최대값
     * @return pos 그래프의 포지션
     */
    public int addGraph(int[] colors, int maxVal) {
        listGraph.add(new Graph(colors, maxVal));
        return listGraph.size()-1;
    }

    /**
     * 그래프 초기화
     */
    public void resetGraph() {
        listGraph.clear();
    }

    /**
     * 그래프 값 셋팅
     * @param pos 그래프의 포지션
     * @param val 그래프 값
     */
    public void setValue(int pos, float val, boolean animate) {
        Graph graph = listGraph.get(pos);
        if (graph != null) {
            graph.setValue((int)val, animate);
        }
        invalidate();
    }

    /**
     * 그래프 그리는데 필요한 값들
     * @param innerCircleRate 안쪽 원형이미지의 크기비율
     * @param graphWidthRate 그래프의 두께크기비율
     * @param spacingRate 그래프들 사이의 간격
     */
    public void setFactors(float innerCircleRate, float graphWidthRate, float spacingRate, int bgColor, int innerCircleColor, int graphBgAlpha) {
        this.innerCircleRate = innerCircleRate;
        this.graphWidthRate = graphWidthRate;
        this.spacingRate = spacingRate;
        paintBG.setColor(bgColor);
        paintInnerCircle.setColor(innerCircleColor);
        this.graphBgAlpha = graphBgAlpha;
    }


    class Graph {
        private int[] colors;  // 크기 1이상의 배열이어야 한다.
        private Shader shader;
        private Paint graphPaint;
        private Paint startPaint;
        private Paint endPaint;
        private int maxVal;
        private int animVal;
        private int currVal;
        private float startAngle;

        public Graph(int[] colors, int maxVal) {
            this.colors = colors;
            this.maxVal = maxVal;
            startAngle = -90;
            graphPaint = new Paint();
            graphPaint.setColor(colors[0]);
            startPaint = new Paint();
            startPaint.setColor(colors[0]);
            endPaint = new Paint();
            endPaint.setColor(colors[0]);
        }

        public void setValue(int val, boolean animate) {
            if (val > maxVal) {
                val = maxVal;
            }
            if (animate) {
                animVal = currVal;
            } else {
                animVal = val;
            }
            currVal = val;
        }

        public float getEndX(float cx, float r) {
            return (float)(cx + (r * Math.sin(Math.toRadians(getAngle()))));
        }

        public float getEndY(float cy, float r) {
            return (float)(cy - (r * Math.cos(Math.toRadians(getAngle()))));
        }

        public Paint getStartPaint() {
            if (colors.length == 1) {
                return startPaint;
            }
            startPaint.setColor(getColor(0));
            return startPaint;
        }

        public Paint getEndPaint() {
            if (colors.length == 1) {
                return endPaint;
            }
            endPaint.setColor(getColor(getAngle()));
            return endPaint;
        }

        private int getColor(float diffAngle) {
            if (colors.length == 1) {
                return colors[0];
            }
            float rangeAngle = 360 / (colors.length-1);
            float angle = startAngle + diffAngle;
            if (angle < 0) {
                angle += 360;
            }
            int pos = (int)(angle / rangeAngle);
            if (pos == colors.length-1) {
                return colors[pos];
            }
            int sColor = colors[pos];
            int sBlue = sColor & 0x000000ff;
            int sGreen = sColor & 0x0000ff00;
            int sRed = sColor & 0x00ff0000;

            int eColor = colors[pos+1];
            int eBlue = eColor & 0x000000ff;
            int eGreen = eColor & 0x0000ff00;
            int eRed = eColor & 0x00ff0000;

            float ratio1 = (angle-(rangeAngle*pos)) / rangeAngle;
            float ratio2 = 1 - ratio1;

            int blue = (int)(sBlue*ratio2 + eBlue*ratio1);
            int green = (int)(sGreen*ratio2 + eGreen*ratio1);
            int red = (int)(sRed*ratio2 + eRed*ratio1);

            return 0xff000000 | (blue & 0x000000ff) | (green & 0x0000ff00) | (red & 0x00ff0000);
        }

        public float getStartAngle() {
            return startAngle;
        }

        public float getAngle() {
            return (animVal * 360f / maxVal);
        }

        public Paint getGraphPaint(float cx, float cy) {
            if (shader == null && colors.length > 1) {
                shader = new SweepGradient(cx, cy, colors, null);
                graphPaint.setShader(shader);
            }
            return graphPaint;
        }

        public boolean needAnimateMore() {
            return (animVal < currVal);
        }

        public void incrementAnimVal() {
            animVal++;
        }
    }
}
