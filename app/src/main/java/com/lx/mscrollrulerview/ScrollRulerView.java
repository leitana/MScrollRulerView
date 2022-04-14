package com.lx.mscrollrulerview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Scroller;

import androidx.annotation.Nullable;

import java.util.List;

/**
 * @author linxiao
 * @title：ScrollRulerView
 * @projectName CustomView
 * @description: <Description>
 * @data Created in 2021/01/30
 */
public class ScrollRulerView extends View {

    private static final int DEFAULT_LINE_COLOR = Color.WHITE; //线的颜色
    private static final int DEFAULT_TEXT_COLOR = Color.WHITE; // 下标文字颜色
    private static final int DEFAULT_VALUE_TEXT = 0xFFFF8C00;
    private static final int DEFAULT_BACKGROUND = 0x00000000; //默认没颜色
    private static final String DEFAULT_UNIT = "x"; //单位
    private static final int DEFAULT_START_NUM = 0;
    private static final int DEFAULT_END_NUM = 10;

    private Scroller scroller;
    private ViewConfiguration viewConfiguration;
    private VelocityTracker velocityTracker;

    private float minWidth; //View的最小宽度
    private float minHeight; //View的最小高度
    private float contentWidth; //实际宽度

    private Paint mLinePaint; // 画刻度的paint
    private Paint mScaleTextPaint; //刻度文本的paint
    private Paint mValueTextPaint;
    private Paint backgroundPaint;//刻度尺背景

    private Rect indexTextRect = new Rect();//下标的rect
    private Rect currentValueTextRect = new Rect();

    private float mSmallLineHeight; //短刻度dp
    private float mLongLineHeight; //长刻度dp
    private float mLineWidth; //每根刻度的宽度
    private float mLineGap; //刻度间间距

    private int mLineColor;
    private int mScaleTextColor;
    private int mScaleTextSize;
    private int mValueTextColor;
    private int mValueTextSize;
    private String unit = "x";

    private List<Integer> index;
    private int startNum; // 起点值
    private int endNum; // 终点值

    private OnValueChangedListener listener;

    public ScrollRulerView(Context context) {
        this(context, null);
    }

    public ScrollRulerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScrollRulerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        scroller = new Scroller(context);
        viewConfiguration = new ViewConfiguration();

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ScrollRulerView);
        mLineColor = ta.getColor(R.styleable.ScrollRulerView_line_color, DEFAULT_LINE_COLOR);
        mSmallLineHeight = ta.getDimension(R.styleable.ScrollRulerView_small_line_height, dp2px(5));
        mLongLineHeight = ta.getDimension(R.styleable.ScrollRulerView_long_line_height, dp2px(10));
        mLineGap = ta.getDimension(R.styleable.ScrollRulerView_line_gap, dp2px(10));
        mLineWidth = ta.getDimension(R.styleable.ScrollRulerView_line_width, dp2px(1));
        mScaleTextColor = ta.getColor(R.styleable.ScrollRulerView_scale_text_color, DEFAULT_TEXT_COLOR);
        mScaleTextSize = ta.getDimensionPixelSize(R.styleable.ScrollRulerView_scale_text_size, dp2px(8));
        mValueTextColor = ta.getColor(R.styleable.ScrollRulerView_value_text_color, DEFAULT_VALUE_TEXT);
        mValueTextSize = ta.getDimensionPixelSize(R.styleable.ScrollRulerView_value_text_size, dp2px(12));
        unit = ta.getString(R.styleable.ScrollRulerView_unit);
        unit = unit == null ? DEFAULT_UNIT : unit;
        startNum = ta.getInteger(R.styleable.ScrollRulerView_start_num, DEFAULT_START_NUM);
        endNum = ta.getInteger(R.styleable.ScrollRulerView_end_num, DEFAULT_END_NUM);
        ta.recycle();

        initPaint();
        initSize();
    }

    private void initPaint(){
        mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mScaleTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mValueTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mLinePaint.setColor(mLineColor);
        mLinePaint.setStrokeWidth(mLineWidth);

        mScaleTextPaint.setColor(mScaleTextColor);
        mScaleTextPaint.setTextSize(mScaleTextSize);
        mScaleTextPaint.setTextAlign(Paint.Align.CENTER);

        mValueTextPaint.setColor(mValueTextColor);
        mValueTextPaint.setTextSize(mValueTextSize);
        mValueTextPaint.setTextAlign(Paint.Align.CENTER);

        backgroundPaint.setColor(DEFAULT_BACKGROUND);
    }

    private void initSize() {
        minHeight = dp2px(100);
        minWidth = dp2px(200);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthMode != MeasureSpec.EXACTLY) {
            widthSize = (int) minWidth;
        }
        if (heightMode != MeasureSpec.EXACTLY) {
            heightSize = (int) minHeight;
        }

        setMeasuredDimension(widthSize, heightSize);
        //setMeasuredDimension结束后，即可计算出内容实际宽度

        //多加了一个MeasuredWidth是因为最左端和最右端将各空出半个MeasuredWidth
        contentWidth = (endNum - startNum) * 10 * mLineGap + getMeasuredWidth();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawScaleplate(canvas);
        drawCurrentText(canvas);
    }

    //画刻度尺
    private void drawScaleplate(Canvas canvas) {
        float startX, startY, endX, endY;
        float mBaseLineY = getMeasuredHeight() / 2;

        startX = endX = getMeasuredWidth() / 2; //从View的一半宽开始画
        int lineCount = Math.abs(endNum - startNum) * 10;  //刻度的数量

        mScaleTextPaint.getTextBounds("0", 0, "0".length(), indexTextRect);
        Paint.FontMetrics metrics = mScaleTextPaint.getFontMetrics();
        float textCenterY;

        for (int i = 0; i <= lineCount; i++) {
            if (i % 10 == 0) { //整数
                startY = mBaseLineY - mLongLineHeight;
                endY = mBaseLineY + mLongLineHeight;

                String value = startNum + i / 10 + "";
                mScaleTextPaint.getTextBounds(value, 0, value.length(), indexTextRect);
                textCenterY = endY + dp2px(10) + indexTextRect.height() / 2 - (metrics.ascent + metrics.descent) / 2;
                canvas.drawText(value, startX, textCenterY, mScaleTextPaint);
            } else {
                startY = mBaseLineY - mSmallLineHeight;
                endY = mBaseLineY + mSmallLineHeight;
            }
            canvas.drawLine(startX, startY, endX, endY, mLinePaint);

            startX = endX = startX + mLineGap;
        }

        //画中间的指针
        mLinePaint.setColor(mValueTextColor);
        mLinePaint.setStrokeWidth(mLinePaint.getStrokeWidth() * 2.5f); //2.5倍粗
        startY = mBaseLineY - mLongLineHeight - dp2px(12);
        endY = mBaseLineY + mLongLineHeight + dp2px(12);
        startX = endX = getMeasuredWidth() / 2 + getScrollX();
        canvas.drawLine(startX, startY, endX, endY, mLinePaint);

        mLinePaint.setColor(mLineColor);
        mLinePaint.setStrokeWidth(mLinePaint.getStrokeWidth() / 2.5f);
    }

    private void drawCurrentText(Canvas canvas) {
        float centerX = getScrollX() + getMeasuredWidth() / 2;
        float centerY = 0.15f * getMeasuredHeight() - (mValueTextPaint.getFontMetrics().ascent + mValueTextPaint.getFontMetrics().descent) / 2;

        String currentVlaue = getCurrentValue() + unit;
        mValueTextPaint.getTextBounds(currentVlaue, 0, currentVlaue.length(), currentValueTextRect);

        canvas.drawText(currentVlaue, centerX, centerY, mValueTextPaint);
    }

    private float lastX = 0;
    private float x = 0;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!scroller.isFinished()) {
                    scroller.abortAnimation();
                }
                lastX = x = event.getX();
                return true;
            case MotionEvent.ACTION_MOVE:
                x = event.getX();
                int deltaX = (int) (lastX - x);
                if (getScrollX() + deltaX < 0) {
                    scrollTo(0,0);
                    return true;
                } else if (getScrollX() + deltaX > contentWidth - getMeasuredWidth()) {
                    scrollTo((int) (contentWidth - getMeasuredWidth()), 0);
                    return true;
                }
                scrollBy(deltaX, 0);
                lastX = x;
                break;
            case MotionEvent.ACTION_UP:
                x = event.getX();
                velocityTracker.computeCurrentVelocity(1000);//计算1s内滑过多少像素
                int xVelocity = (int) velocityTracker.getXVelocity();
                if (Math.abs(xVelocity) > viewConfiguration.getScaledMinimumFlingVelocity()) { //滑动速度可被判定为抛动
                    scroller.fling(getScrollX(), 0, -xVelocity, 0, 0, (int) (contentWidth - getMeasuredWidth()), 0, 0);
                    invalidate();
                }
                break;

        }
        return super.onTouchEvent(event);
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.getCurrX(), scroller.getCurrY());
            invalidate();
        }
    }

    private float lastValue;//上次选中的Value

    public float getCurrentValue(){
        float value;
        int gapCount = (int) (getScrollX() / mLineGap); //已经划过的间隔数量
        value = startNum * 10  + gapCount;

        if (value != lastValue && listener != null) {
            listener.onValueChanged(value / 10);
        }
        lastValue = value;
        return value / 10;
    }

    /**
     * 设置当前值
     * @param currentValue
     */
    public void setCurrentValue(float currentValue) {
        float value = currentValue;
        int gapCount;
        value = Math.max(startNum,value);
        value = Math.min(endNum,value);
        gapCount = (int) ((value - startNum) * 10);

        final float scrollX = gapCount * mLineGap;
        post(new Runnable() {
            @Override
            public void run() {
                scrollTo((int) scrollX,0);
            }
        });
    }



    public void setOnValueChangedListener(OnValueChangedListener onValueChangedListener) {
        this.listener = onValueChangedListener;
    }

    /**
     * 当前刻度变化回调
     */
    public interface OnValueChangedListener{
        void onValueChanged(float value);
    }

    private int dp2px(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private int sp2px(float sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics());
    }

}
