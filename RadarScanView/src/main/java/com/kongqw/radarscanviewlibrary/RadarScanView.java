package com.kongqw.radarscanviewlibrary;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.SweepGradient;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * Created by kongqingwei on 2017/3/9.
 * 雷达扫描图
 */
public class RadarScanView extends View {

    private static final String TAG = "RadarScanView";
    // 默认大小
    private static final int DEFAULT_SIZE = 200;
    // 刷新界面频率 黄金16毫秒
    private static final long REFRESH_RATE = 16;
    // 圆周360°
    private static final float ANGLE_360 = 360.0F;
    // 雷达扫描一圈的时间 默认1秒转一圈
    private int mRadarScanTime = 1000;
    // 雷达背景线的条数 默认3条
    private int mDefaultRadarBackgroundLinesNumber = 3;
    // 雷达背景线的宽度 默认2
    private static final float DEFAULT_RADAR_BACKGROUND_LINES_WIDTH = 2.0F;

    private static final int DEFAULT_RADAR_BACKGROUND_LINES_COLOR = Color.GRAY;

    private static final int DEFAULT_RADAR_BACKGROUND_COLOR = Color.argb(0x00, 0x00, 0x00, 0x00);

    private static final int DEFAULT_RADAR_SCAN_ALPHA = 0x99;
    // 雷达扫描的起始颜色
    private int mShaderStartColor = Color.parseColor("#00000000");
    // 雷达扫描的结束颜色
    private int mShaderEndColor = Color.parseColor("#FF888888");

    private Paint mRadarBackgroundLinesPaint;

    private Point mCenterPoint;
    private Matrix mMatrix;
    private Handler handler = new Handler();
    private Runnable run = new Runnable() {
        @Override
        public void run() {
            mMatrix.postRotate(ANGLE_360 / mRadarScanTime * REFRESH_RATE, mCenterPoint.x, mCenterPoint.y);
            postInvalidate();
            handler.postDelayed(run, REFRESH_RATE);
        }
    };
    private Paint mRadarScanPaint;
    private Paint mRadarBackgroundPaint;
    private int mViewRadius;
    private float mScanRadius;

    public RadarScanView(Context context) {
        super(context);
        initView();
    }

    public RadarScanView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView();
        initAttribute(context, attrs);
    }

    public RadarScanView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
        initAttribute(context, attrs);
    }

    /**
     * 初始化
     */
    private void initView() {
        // 中心点
        mCenterPoint = new Point();
        // 扫描旋转
        mMatrix = new Matrix();
        // 背景的画笔
        mRadarBackgroundPaint = new Paint();
        mRadarBackgroundPaint.setAntiAlias(true);
        mRadarBackgroundPaint.setColor(DEFAULT_RADAR_BACKGROUND_COLOR);
        // mRadarBackgroundPaint.setAlpha(0x00);
        // 背景线的画笔
        mRadarBackgroundLinesPaint = new Paint();
        mRadarBackgroundLinesPaint.setAntiAlias(true);
        mRadarBackgroundLinesPaint.setStyle(Paint.Style.STROKE);
        mRadarBackgroundLinesPaint.setColor(DEFAULT_RADAR_BACKGROUND_LINES_COLOR);
        mRadarBackgroundLinesPaint.setStrokeWidth(DEFAULT_RADAR_BACKGROUND_LINES_WIDTH);
        // 雷达扫描的画笔
        mRadarScanPaint = new Paint();
        mRadarScanPaint.setAntiAlias(true);
        mRadarScanPaint.setAlpha(DEFAULT_RADAR_SCAN_ALPHA);
    }

    /**
     * 获取属性
     *
     * @param context context
     * @param attrs   attrs
     */
    private void initAttribute(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RadarScanView);

        // 设置雷达扫描一圈时间
        setRadarScanTime(typedArray.getInteger(
                R.styleable.RadarScanView_radarScanTime,
                mRadarScanTime
        ));

        // 设置雷达背景圆圈数
        setRadarBackgroundLinesNumber(typedArray.getInteger(
                R.styleable.RadarScanView_radarBackgroundLinesNumber,
                mDefaultRadarBackgroundLinesNumber
        ));

        // 设置雷达圆圈宽度
        setRadarBackgroundLinesWidth(typedArray.getFloat(
                R.styleable.RadarScanView_radarBackgroundLinesWidth,
                DEFAULT_RADAR_BACKGROUND_LINES_WIDTH
        ));

        // 设置雷达圆圈颜色
        setRadarBackgroundLinesColor(typedArray.getColor(
                R.styleable.RadarScanView_radarBackgroundLinesColor,
                DEFAULT_RADAR_BACKGROUND_LINES_COLOR
        ));

        // 设置雷达背景颜色
        setRadarBackgroundColor(typedArray.getColor(
                R.styleable.RadarScanView_radarBackgroundColor,
                DEFAULT_RADAR_BACKGROUND_COLOR
        ));

        // 设置雷达扫描颜色
        setRadarScanColor(typedArray.getColor(
                R.styleable.RadarScanView_radarScanColor,
                mShaderEndColor
        ));

        // 设置雷达扫描透明度
        setRadarScanAlpha(typedArray.getInteger(
                R.styleable.RadarScanView_radarScanAlpha,
                DEFAULT_RADAR_SCAN_ALPHA
        ));

        typedArray.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(getMeasuredSize(widthMeasureSpec), getMeasuredSize(heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            // 控件宽度
            int measuredWidth = getMeasuredWidth();
            // 控件高度
            int measuredHeight = getMeasuredHeight();
            // Log.i(TAG, "onLayout: -------------------");
            // Log.i(TAG, "onLayout: measuredWidth = " + measuredWidth + "   measuredHeight = " + measuredHeight);
            // Log.i(TAG, "onLayout: -------------------");
            // 控件半径
            mViewRadius = Math.min(measuredWidth, measuredHeight) / 2;
            // 扫描半径
            mScanRadius = (mViewRadius - mRadarBackgroundLinesPaint.getStrokeWidth() / 2);
            // 中心点
            mCenterPoint.set(measuredWidth / 2, measuredHeight / 2);

            startScan();
        }
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (VISIBLE == visibility) {
            startScan();
        } else {
            stopScan();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        stopScan();
        super.onDetachedFromWindow();
    }

    /**
     * 获取测量后的大小
     *
     * @param measureSpec measureSpec
     * @return measuredSize
     */
    private int getMeasuredSize(int measureSpec) {
        switch (MeasureSpec.getMode(measureSpec)) {
            case MeasureSpec.EXACTLY: // 精确值模式（指定值/match_parent）
                return MeasureSpec.getSize(measureSpec);
            case MeasureSpec.AT_MOST: // 最大值模式（wrap_content）
                return Math.min(DEFAULT_SIZE, MeasureSpec.getSize(measureSpec));
            case MeasureSpec.UNSPECIFIED: // 不指定大小的测量模式
                return DEFAULT_SIZE;
            default:
                return DEFAULT_SIZE;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 画背景
        canvas.drawCircle(mCenterPoint.x, mCenterPoint.y, mViewRadius, mRadarBackgroundPaint);
        // 划线
        for (int i = 1; i <= mDefaultRadarBackgroundLinesNumber; i++) {
            int radius = (int) (mScanRadius / mDefaultRadarBackgroundLinesNumber * i);
            // 画线
            canvas.drawCircle(mCenterPoint.x, mCenterPoint.y, radius, mRadarBackgroundLinesPaint);
        }
        // 画扇形
        drawFan(canvas, mViewRadius);
    }

    /**
     * 画扇形
     *
     * @param canvas     canvas
     * @param scanRadius scanRadius
     */
    private void drawFan(Canvas canvas, int scanRadius) {
        mRadarScanPaint.setShader(new SweepGradient(
                mCenterPoint.x, // 中心X坐标
                mCenterPoint.y, // 中心Y坐标
                mShaderStartColor, // Shader始端颜色
                mShaderEndColor // Shader末端颜色
        ));
        canvas.concat(mMatrix);
        canvas.drawCircle(mCenterPoint.x, mCenterPoint.y, scanRadius, mRadarScanPaint);
    }

    /**
     * 设置雷达背景圆圈数
     *
     * @param number 数量
     * @return RadarScanView
     */
    public RadarScanView setRadarBackgroundLinesNumber(int number) {
        if (0 < mDefaultRadarBackgroundLinesNumber) {
            this.mDefaultRadarBackgroundLinesNumber = number;
        }
        return this;
    }

    /**
     * 设置雷达背景圆圈宽度
     *
     * @param width 宽度
     * @return RadarScanView
     */
    public RadarScanView setRadarBackgroundLinesWidth(float width) {
        mRadarBackgroundLinesPaint.setStrokeWidth(width);
        // 扫描半径
        mScanRadius = mViewRadius - width / 2;
        return this;
    }

    /**
     * 设置雷达背景圆圈颜色
     *
     * @param color 色值
     * @return RadarScanView
     */
    public RadarScanView setRadarBackgroundLinesColor(int color) {
        mRadarBackgroundLinesPaint.setColor(color);
        return this;
    }

    /**
     * 设置雷达背景色
     *
     * @param color 色值
     * @return RadarScanView
     */
    public RadarScanView setRadarBackgroundColor(int color) {
        mRadarBackgroundPaint.setColor(color);
        return this;
    }

    /**
     * 设置雷达扫描颜色
     *
     * @param endColor 末端色值
     * @return RadarScanView
     */
    public RadarScanView setRadarScanColor(int endColor) {
        setRadarScanColors(0x00000000, endColor);
        return this;
    }

    /**
     * 设置雷达扫描颜色
     *
     * @param startColor 始端色值
     * @param endColor   末端色值
     * @return RadarScanView
     */
    public RadarScanView setRadarScanColors(int startColor, int endColor) {
        mShaderStartColor = startColor;
        mShaderEndColor = endColor;
        return this;
    }

//    public RadarScanView setRadarScanColors(int colors[], float positions[]) {
//        shader = new SweepGradient(
//                mCenterPoint.x, // 中心X坐标
//                mCenterPoint.y, // 中心Y坐标
//                colors, // 颜色
//                positions // 位置
//        );
//        mRadarScanPaint.setShader(shader);
//        return this;
//    }

    /**
     * 设置雷达扫描透明度
     *
     * @param alpha 透明度
     * @return RadarScanView
     */
    public RadarScanView setRadarScanAlpha(int alpha) {
        mRadarScanPaint.setAlpha(alpha);
        return this;
    }


    /**
     * 设置雷达扫描一圈的时间
     *
     * @param milliseconds 时间毫秒值
     */
    public RadarScanView setRadarScanTime(int milliseconds) {
        if (0 < milliseconds) {
            this.mRadarScanTime = milliseconds;
        }
        return this;
    }

    /**
     * 开始扫描
     */
    public void startScan() {
        stopScan();
        Log.i(TAG, "startScan: ");
        handler.post(run);
    }

    /**
     * 结束扫描
     */
    public void stopScan() {
        Log.i(TAG, "stopScan: ");
        handler.removeCallbacks(run);
    }
}
