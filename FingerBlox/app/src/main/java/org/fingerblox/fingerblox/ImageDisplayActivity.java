package org.fingerblox.fingerblox;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.core.Size;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;


public class ImageDisplayActivity extends AppCompatActivity {
    public static final String TAG = "ImageDisplayActivity";
    public File fileDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle("Zebra");
        setContentView(R.layout.activity_image_display);
        ImageView mImageView = (ImageView) findViewById(R.id.image_view);

        mImageView.setImageBitmap(ImageSingleton.image);

        Context context = getApplicationContext();
        fileDir = context.getFilesDir();

        Button saveFeaturesButton = (Button) findViewById(R.id.btn_save_feat);
        assert saveFeaturesButton != null;
        saveFeaturesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSaveDialog();
            }
        });
    }

    public void openSaveDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.save_dialog, null);
        dialogBuilder.setView(dialogView);

        final EditText fileNameEditText = (EditText) dialogView.findViewById(R.id.filename_edit_text);

        dialogBuilder.setMessage("Enter filename");
        dialogBuilder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                saveFeatures(fileNameEditText.getText().toString());
            }
        });
        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //do nothing
            }
        });
        AlertDialog saveDialog = dialogBuilder.create();
        saveDialog.show();
    }

    protected void saveFeatures(String fileName){
        MatOfKeyPoint keypoints = ImageProcessing.getKeypoints();
        Mat descriptors = ImageProcessing.getDescriptors();

        String keypointsJSON = keypointsToJSON(keypoints);
        String descriptorsJSON = matToJSON(descriptors);

        try{
            FileWriter fw;

            File keypointsFile = new File(fileDir, fileName+"_keypoints");
            fw = new FileWriter(keypointsFile);
            fw.write(keypointsJSON);
            fw.flush();
            fw.close();

            File descriptorsFile = new File(fileDir, fileName+"_descriptors");
            fw = new FileWriter(descriptorsFile);
            fw.write(descriptorsJSON);
            fw.flush();
            fw.close();
        } catch (Exception e){
            e.printStackTrace();
        }

        try{
            FileWriter fw = null;

            File keypointsFile = new File(fileDir, fileName+"_keypoints");
            fw = new FileWriter(keypointsFile);
            fw.write(keypointsJSON);
            fw.flush();
            fw.close();

            File descriptorsFile = new File(fileDir, fileName+"_descriptors");
            fw = new FileWriter(descriptorsFile);
            fw.write(descriptorsJSON);
            fw.flush();
            fw.close();
        } catch (Exception e){
            e.printStackTrace();
        }

        /*
        Read the file using:

        File file = new File(fileDir, fileName+"_keypoints");
        FileInputStream is = new FileInputStream(file);
        int size = is.available();
        byte[] buffer = new byte[size];
        is.read(buffer);
        is.close();
        String json = new String(buffer);

        Then convert using jsonToMat or jsonToKeypoints
        */
    }

    public String keypointsToJSON(MatOfKeyPoint kps){
        Gson gson = new Gson();

        JsonArray jsonArr = new JsonArray();

        KeyPoint[] kpsArray = kps.toArray();
        for(int i=0; i<kpsArray.length; i++){
            KeyPoint kp = kpsArray[i];

            JsonObject obj = new JsonObject();

            obj.addProperty("class_id", kp.class_id);
            obj.addProperty("x", kp.pt.x);
            obj.addProperty("y", kp.pt.y);
            obj.addProperty("size", kp.size);
            obj.addProperty("angle", kp.angle);
            obj.addProperty("octave", kp.octave);
            obj.addProperty("response", kp.response);

            jsonArr.add(obj);
        }

        return gson.toJson(jsonArr);
    }

    public static MatOfKeyPoint jsonToKeypoints(String json){
        MatOfKeyPoint result = new MatOfKeyPoint();

        JsonParser parser = new JsonParser();
        JsonArray jsonArr = parser.parse(json).getAsJsonArray();

        int size = jsonArr.size();

        KeyPoint[] kpArray = new KeyPoint[size];

        for(int i=0; i<size; i++){
            KeyPoint kp = new KeyPoint();

            JsonObject obj = (JsonObject) jsonArr.get(i);

            Point point = new Point(
                obj.get("x").getAsDouble(),
                obj.get("y").getAsDouble()
            );

            kp.pt = point;
            kp.class_id = obj.get("class_id").getAsInt();
            kp.size = obj.get("size").getAsFloat();
            kp.angle = obj.get("angle").getAsFloat();
            kp.octave = obj.get("octave").getAsInt();
            kp.response = obj.get("response").getAsFloat();

            kpArray[i] = kp;
        }

        result.fromArray(kpArray);

        return result;
    }

    public static String matToJSON(Mat mat){
        JsonObject obj = new JsonObject();

        int cols = mat.cols();
        int rows = mat.rows();
        int elemSize = (int) mat.elemSize();

        byte[] data = new byte[cols * rows * elemSize];

        mat.get(0, 0, data);

        obj.addProperty("rows", mat.rows());
        obj.addProperty("cols", mat.cols());
        obj.addProperty("type", mat.type());

        String dataString = new String(Base64.encode(data, Base64.DEFAULT));

        obj.addProperty("data", dataString);

        Gson gson = new Gson();
        String json = gson.toJson(obj);

        return json;
    }

    public static Mat jsonToMat(String json){
        JsonParser parser = new JsonParser();
        JsonObject JsonObject = parser.parse(json).getAsJsonObject();

        int rows = JsonObject.get("rows").getAsInt();
        int cols = JsonObject.get("cols").getAsInt();
        int type = JsonObject.get("type").getAsInt();

        String dataString = JsonObject.get("data").getAsString();
        byte[] data = Base64.decode(dataString.getBytes(), Base64.DEFAULT);

        Mat mat = new Mat(rows, cols, type);
        mat.put(0, 0, data);

        return mat;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finish();
    }
}
