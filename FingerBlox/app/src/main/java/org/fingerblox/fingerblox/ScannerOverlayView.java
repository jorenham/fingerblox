package org.fingerblox.fingerblox;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;

public class ScannerOverlayView extends View {
    private final String TAG = "ScannerOverlayView";

    private Paint paint = new Paint();
    private Paint paintGlow = new Paint();

    public ScannerOverlayView(Context context) {
        super(context);
    }

    public ScannerOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ScannerOverlayView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        initAnimation();
    }

    private void initAnimation() {
        paint.setStrokeWidth(getHeight() * 0.01f);
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setColor(Color.argb(248, 255, 255, 255));
        paint.setStrokeWidth(20f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);

        paintGlow.set(paint);
        paintGlow.setColor(Color.argb(235, 74, 138, 255));
        paintGlow.setStrokeWidth(30f);
        paintGlow.setMaskFilter(new BlurMaskFilter(15, BlurMaskFilter.Blur.NORMAL));

        float deltaY = (CameraOverlayView.PADDING * 2) * getHeight();
        Log.i(TAG, String.format("Delta Y : %s", deltaY));

        TranslateAnimation mAnimation = new TranslateAnimation(0f, 0f, 0f, deltaY);
        mAnimation.setDuration(3000);
        mAnimation.setRepeatCount(-1);
        mAnimation.setRepeatMode(Animation.REVERSE);
        mAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        setAnimation(mAnimation);
    }

    @Override
    public void onDraw(Canvas canvas) {
        float padding = CameraOverlayView.PADDING;
        canvas.drawLine(getWidth() * padding,
                getHeight() * padding,
                getWidth() * (1.0f - padding),
                getHeight() * padding,
                paintGlow);
        canvas.drawLine(getWidth() * padding,
                getHeight() * padding,
                getWidth() * (1.0f - padding),
                getHeight() * padding,
                paint);

    }
}
