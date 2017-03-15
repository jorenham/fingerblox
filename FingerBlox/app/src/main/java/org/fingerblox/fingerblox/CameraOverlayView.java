package org.fingerblox.fingerblox;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class CameraOverlayView extends View {
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

    public static RectDimensions getDimensions(int width, int height) {
        int padding = (int) (0.25 * width);
        int bottom = 4 * padding;
        if (bottom > height)
            bottom = height;
        return new RectDimensions(padding, padding, width - padding, bottom);
    }

    @Override
    public void onDraw(Canvas canvas) {
        borderPaint.setColor(Color.MAGENTA);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(10);

        innerPaint.setARGB(0, 0, 0, 0);
        innerPaint.setStyle(Paint.Style.FILL);

        RectDimensions d = getDimensions(getWidth(), getHeight());

        overlayRect.set(d.left, d.top, d.right, d.bottom);

        canvas.drawOval(overlayRect, innerPaint);
        canvas.drawOval(overlayRect, borderPaint);
    }
}


class RectDimensions {
    public int left;
    public int top;
    public int right;
    public int bottom;

    public RectDimensions(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.bottom = bottom;
        this.right = right;
    }
}