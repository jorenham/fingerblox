package org.fingerblox.fingerblox;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class CameraOverlayView extends View {
    public final static float PADDING = 0.25f;

    private Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint innerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RectF overlayRect = new RectF();

    public CameraOverlayView(Context context) {
        super(context);
        initPaints();
    }

    public CameraOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaints();
    }

    public CameraOverlayView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initPaints();
    }

    @Override
    public void onDraw(Canvas canvas) {
        overlayRect.set(getWidth() * PADDING,
                        getHeight() * PADDING,
                        getWidth() * (1.0f - PADDING),
                        getHeight() * (1.0f - PADDING));

        canvas.drawOval(overlayRect, innerPaint);
        canvas.drawOval(overlayRect, borderPaint);
    }

    private void initPaints() {
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(6);
        borderPaint.setShadowLayer(12f, 0, 0, Color.GREEN);

        innerPaint.setARGB(0, 0, 0, 0);
        innerPaint.setStyle(Paint.Style.FILL);

        setLayerType(LAYER_TYPE_SOFTWARE, borderPaint);
    }
}
