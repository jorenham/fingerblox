package org.fingerblox.fingerblox;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;

public class ScannerOverlayView extends View {
    private final String TAG = "ScannerOverlayView";

    private Paint paint = new Paint();

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
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(getHeight() * 0.01f);

        float deltaY = (CameraOverlayView.PADDING * 2) * getHeight();
        Log.i(TAG, String.format("Delta Y : %s", deltaY));

        TranslateAnimation mAnimation = new TranslateAnimation(0f, 0f, 0f, deltaY);
        mAnimation.setDuration(5000);
        mAnimation.setRepeatCount(-1);
        mAnimation.setRepeatMode(Animation.REVERSE);
        mAnimation.setInterpolator(new LinearInterpolator());
        setAnimation(mAnimation);
    }

    @Override
    public void onDraw(Canvas canvas) {
        float padding = CameraOverlayView.PADDING;
        canvas.drawLine(getWidth() * padding,
                getHeight() * padding,
                getWidth() * (1.0f - padding),
                getHeight() * padding,
                paint);
    }
}
