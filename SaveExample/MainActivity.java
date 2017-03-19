package com.example.saveexample;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

public class MainActivity extends Activity {
	File fileDir;
	List<String> spinnerArray;
	ArrayAdapter<String> spinnerAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		initListeners();
		initSpinner();
		
		Context context = getApplicationContext();
		fileDir = context.getFilesDir();
	}
	
	//add listeners to the buttons (can also be done through xml)
	public void initListeners(){
		findViewById(R.id.generate_btn).setOnClickListener(generateListener);
		findViewById(R.id.load_btn).setOnClickListener(loadListener);
	}
	
	private View.OnClickListener generateListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			generateAndSave();
		}
	};
	
	private View.OnClickListener loadListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			loadFile();
		}
	};
	
	//load list of filenames from shared preferences (local storage) and add them to the spinner through an adapter
	public void initSpinner(){
		spinnerArray =  new ArrayList<String>();
		
		SharedPreferences preferences = getSharedPreferences("PREFS", 0);
		String[] fileNameList = preferences.getString("fileNameList", "").split(" ");
		for(String s : fileNameList)
			spinnerArray.add(s);

		spinnerAdapter = new ArrayAdapter<String>(
		    this, android.R.layout.simple_spinner_item, spinnerArray);

		spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		Spinner sItems = (Spinner) findViewById(R.id.filename_spinner);
		sItems.setAdapter(spinnerAdapter);
	}
	
	public void generateAndSave(){
		//get filename from edittext field
		EditText filenameEditText = (EditText) findViewById(R.id.filename_edittext);
		String filename = filenameEditText.getText().toString();
		
		if(filename.equals(""))
			return;
		
		//create random bytes
		byte[] randomBytes = new byte[20];
		new Random().nextBytes(randomBytes);
		
		//show bytes in output textfield
		EditText output = (EditText) findViewById(R.id.output_edittext);
		output.setText(byteArrayToString(randomBytes));
		
		try {
			//create new file and write bytes to it in string form (can also be done in bytes)
			File file = new File(fileDir, filename);
			file.createNewFile();
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			writer.write(byteArrayToString(randomBytes));
			writer.close();
			
			//add filename to list in shared preferences
			SharedPreferences preferences = getSharedPreferences("PREFS", 0);
			String fileNameList = preferences.getString("fileNameList", "");
			
			if(fileNameList.equals("")){
				fileNameList = filename;
			}
			else{
				fileNameList += " " + filename;
			}
			
			SharedPreferences.Editor editor = preferences.edit();
			
			editor.putString("fileNameList", fileNameList);
			
			editor.commit();
			
			//reload spinner
			initSpinner();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void loadFile(){
		//get selected item from spinner
		Spinner spinner = (Spinner) findViewById(R.id.filename_spinner);
		String selectedFile = spinner.getSelectedItem().toString();
		
		if(selectedFile.equals(""))
			return;
		
		try {
			//get file and read
			File file = new File(fileDir, selectedFile);
			BufferedReader br = new BufferedReader(new FileReader(file));
		    String line;
		    String out = "";
		    
		    //(only one line at the moment, so kind of unnecessary to do it like this)
		    while ((line = br.readLine()) != null) {
		        out += line;
		    }
		    
		    br.close();
		    
		    //set read file as output text
		    EditText output = (EditText) findViewById(R.id.output_edittext);
			output.setText(out);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String byteArrayToString(byte[] in){
		String out = "";
		
		for(byte b : in){
			out += b + " ";
		}
		
		return out;
	}
}
