package org.fingerblox.fingerblox;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class CameraOverlayView extends View {
    public final static float PADDING = 0.2f;

    private Paint borderPaint = new Paint();
    private Paint innerPaint = new Paint();
    private RectF overlayRect = new RectF();

    public CameraOverlayView(Context context) {
        super(context);
    }

    public CameraOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraOverlayView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onDraw(Canvas canvas) {
        borderPaint.setColor(Color.MAGENTA);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(10);

        innerPaint.setARGB(0, 0, 0, 0);
        innerPaint.setStyle(Paint.Style.FILL);

        overlayRect.set(getWidth() * PADDING,
                        getHeight() * PADDING,
                        getWidth() * (1.0f - PADDING),
                        getHeight() * (1.0f - PADDING));

        canvas.drawOval(overlayRect, innerPaint);
        canvas.drawOval(overlayRect, borderPaint);
    }
}
