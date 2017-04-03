package org.fingerblox.fingerblox;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;


public class ImageDisplayActivity extends AppCompatActivity {
    public static final String TAG = "ImageDisplayActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle("Zebra");
        setContentView(R.layout.activity_image_display);
        ImageView mImageView = (ImageView) findViewById(R.id.image_view);

        mImageView.setImageBitmap(ImageSingleton.image);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finish();
    }

}
