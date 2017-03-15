package org.fingerblox.fingerblox;

import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import org.opencv.core.Mat;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class ImageDisplayActivity extends AppCompatActivity {
    private ImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_image_display);
        mImageView = (ImageView) findViewById(R.id.image_view);

        Bundle bundle = getIntent().getExtras();
        Bitmap bmp = (Bitmap) bundle.get("CAPTURED_IMAGE");
        mImageView.setImageBitmap(bmp);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finish();
    }
}
