package org.fingerblox.fingerblox;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.github.clans.fab.FloatingActionButton;

import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;


public class ImageDisplayActivity extends AppCompatActivity {
    public static final String TAG = "ImageDisplayActivity";
    public File fileDir;
    public final String kpFileSuffix = "_keypoints.json";
    public final String descFileSuffix = "_descriptors.json";

    /*
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    */


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle("Zebra");
        setContentView(R.layout.activity_image_display);
        ImageView mImageView = (ImageView) findViewById(R.id.image_view);

        mImageView.setImageBitmap(ImageSingleton.image);

        Context context = getApplicationContext();
        fileDir = context.getFilesDir();

        FloatingActionButton saveFeaturesButton = (FloatingActionButton) findViewById(R.id.btn_save_feat);
        assert saveFeaturesButton != null;
        saveFeaturesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSaveDialog();
            }
        });

        FloatingActionButton matchFeaturesButton = (FloatingActionButton) findViewById(R.id.btn_match_feat);
        assert matchFeaturesButton != null;
        matchFeaturesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findMatch();
            }
        });
    }

    public void openSaveDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this, R.style.DialogTheme);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.save_dialog, null);
        dialogBuilder.setView(dialogView);

        final EditText fileNameEditText = (EditText) dialogView.findViewById(R.id.filename_edit_text);

        dialogBuilder.setMessage("Enter a name");
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
        boolean saveSuccess = true;

        MatOfKeyPoint keypoints = ImageProcessing.getKeypoints();
        Mat descriptors = ImageProcessing.getDescriptors();

        String keypointsJSON = keypointsToJSON(keypoints);
        String descriptorsJSON = matToJSON(descriptors);

        try{
            FileWriter fw;

            File keypointsFile = new File(fileDir, fileName+kpFileSuffix);
            fw = new FileWriter(keypointsFile);
            fw.write(keypointsJSON);
            fw.flush();
            fw.close();

            File descriptorsFile = new File(fileDir, fileName+descFileSuffix);
            fw = new FileWriter(descriptorsFile);
            fw.write(descriptorsJSON);
            fw.flush();
            fw.close();
        } catch (Exception e){
            saveSuccess = false;
            e.printStackTrace();
        }
        if(saveSuccess) {
            makeToast("Save success!");

            SharedPreferences preferences = getSharedPreferences("PREFS", 0);
            String fileNameList = preferences.getString("fileNameList", "");

            if (fileNameList.equals("")) {
                fileNameList = fileName;
            } else {
                fileNameList += " " + fileName;
            }

            SharedPreferences.Editor editor = preferences.edit();

            editor.putString("fileNameList", fileNameList);

            editor.apply();
        }
        else{
            makeToast("Save failed!");
        }
    }

    public void findMatch() {
        SharedPreferences preferences = getSharedPreferences("PREFS", 0);
        String[] fileNameList = preferences.getString("fileNameList", "").split(" ");

        double maxRatio = 0;
        String bestFileName = null;
        for (String fileName : fileNameList) {
            double ratio = matchFeaturesFile(fileName);
            if (ratio > maxRatio) {
                maxRatio = ratio;
                bestFileName = fileName;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Match found");
        builder.setMessage(String.format("%s: %s%%", bestFileName, (int) (maxRatio * 100)));
        builder.setPositiveButton("OK", null);
        AlertDialog dialog = builder.show();

        // Must call show() prior to fetching text view
        TextView messageView = (TextView)dialog.findViewById(android.R.id.message);
        messageView.setGravity(Gravity.CENTER);
    }

    private double matchFeaturesFile(String fileName){
        String[] loadedFeatures = loadFiles(fileName);

        Log.d(TAG, "Features loaded from file");
        Log.d(TAG, "Keypoints: "+loadedFeatures[0]);
        Log.d(TAG, "Descriptors: "+loadedFeatures[1]);

        MatOfKeyPoint keypointsToMatch = jsonToKeypoints(loadedFeatures[0]);
        Mat descriptorsToMatch = jsonToMat(loadedFeatures[1]);

        return ImageProcessing.matchFeatures(keypointsToMatch, descriptorsToMatch);
    }

    private String[] loadFiles(String fileName){
        String[] res = new String[2];

        try {
            File kpFile = new File(fileDir, fileName + kpFileSuffix);
            FileInputStream is = new FileInputStream(kpFile);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String kpJson = new String(buffer);
            res[0] = kpJson;
        } catch(Exception e){
            e.printStackTrace();
        }

        try {
            File descFile = new File(fileDir, fileName + descFileSuffix);
            FileInputStream is = new FileInputStream(descFile);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String descJson = new String(buffer);
            res[1] = descJson;
        } catch(Exception e){
            e.printStackTrace();
        }

        return res;
    }

    public String keypointsToJSON(MatOfKeyPoint kps){
        Gson gson = new Gson();

        JsonArray jsonArr = new JsonArray();

        KeyPoint[] kpsArray = kps.toArray();
        for(KeyPoint kp : kpsArray){
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

            kp.pt = new Point(
                    obj.get("x").getAsDouble(),
                    obj.get("y").getAsDouble()
            );
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

        return gson.toJson(obj);
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

    public void makeToast(String toShow){
        Toast toast = Toast.makeText(getApplicationContext(), toShow, Toast.LENGTH_SHORT);
        toast.show();
    }
}
