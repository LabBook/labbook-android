package uk.ac.ic.bss.labbook.media;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.TimeZone;

import uk.ac.ic.bss.labbook.NotesList;
import uk.ac.ic.bss.labbook.models.Notebook;
import uk.ac.ic.bss.labbook.models.Notebooks;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.apps.analytics.easytracking.TrackedActivity;


public class Camcorder extends TrackedActivity {

	     private uk.ac.ic.bss.labbook.media.CamcorderView camcorderView; 
//	     private boolean recording = false; 
	     private static final String KEY_VIDEO = "VIDEO_ID";
	     private ImageButton startButton; //, stopButton;
	     private TextView rectv;
	     private boolean start = false;
	     private Notebook notebook;
	     
	     /** Called when the activity is first created. */ 
	     @Override 
	     public void onCreate(Bundle savedInstanceState) { 
	          super.onCreate(savedInstanceState); 
	          requestWindowFeature(Window.FEATURE_NO_TITLE); 
	          setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); 
	          setContentView(uk.ac.ic.bss.labbook.R.layout.camcorder_preview); 
	          
	          //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
	          camcorderView = (uk.ac.ic.bss.labbook.media.CamcorderView) findViewById(uk.ac.ic.bss.labbook.R.id.camcorder_preview); 
 	     		    	     	 
	          startButton = (ImageButton) findViewById(uk.ac.ic.bss.labbook.R.id.startbut);
	          //startButton.setText("Start");
	          startButton.setImageResource(uk.ac.ic.bss.labbook.R.drawable.record24);
	          //stopButton = (Button) findViewById(uk.ac.imperial.epi_collect2.R.id.stopbut);
	          //stopButton.setEnabled(false);
	          
	          rectv = (TextView)findViewById(uk.ac.ic.bss.labbook.R.id.rectext);
	          
	  	     
	         //videodir = Environment.getExternalStorageDirectory()+"/LabBook/Videos"; //this.getResources().getString(this.getResources().getIdentifier(this.getPackageName()+":string/project", null, null));
	          
	         notebook = Notebooks.get(new File(getIntent().getExtras().getString(NotesList.KEY_FOLDER)));
	         
	         
	         
	          startButton.setOnClickListener(new View.OnClickListener() {

	              public void onClick(View arg0) {
	            	  //startRecording();
	            	  setRecording();
	              }
	             
	          });
	          
	        /*  stopButton.setOnClickListener(new View.OnClickListener() {

	              public void onClick(View arg0) {
	            	  rectv.setText("");
	            	  camcorderView.stopRecording();
	            	  endRecording();
	              }
	             
	          }); */
	     } 
	     
	    /* private void startRecording(){
	    		    	 
	    	 recording = true; 
             camcorderView.startRecording(); 
             startButton.setEnabled(false);
             stopButton.setEnabled(true);
             rectv.setText("RECORDING");
             rectv.setTextColor(Color.RED);
	     } */
	     
	     private void setRecording(){
	    	 
	    	 if(start == false){
//	    		 recording = true; 
	    		 camcorderView.startRecording(); 
	    		 //startButton.setEnabled(false);
	    		 //stopButton.setEnabled(true);
	    		 rectv.setText("RECORDING");
	    		 rectv.setTextColor(Color.RED);
	    		 start = true;
	    		 //startButton.setText("Stop");
	    		 startButton.setImageResource(uk.ac.ic.bss.labbook.R.drawable.stop24);
	    	 }
	    	 else{
	    		rectv.setText("");
           	  	camcorderView.stopRecording();
           	  	//endRecording();
           	  	
  	          Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
  	          File video = new File(notebook.videos, String.format("vid_%s.mp4", cal.getTimeInMillis()));
           	  	
           	  	copyFile(video);
	    	 
           	  	Bundle extras = getIntent().getExtras();
           	  	extras.putString(KEY_VIDEO, video.getPath());
           	  	this.getIntent().putExtras(extras);
           	  	setResult(RESULT_OK, this.getIntent());
           	  	finish();  
	    	 	}
	     }
	     
	    /* private void endRecording(){
	    	 
	    	copyFile();
	    	 
	    	Bundle extras = getIntent().getExtras();
	    	extras.putString(KEY_VIDEO, videoid);
	    	this.getIntent().putExtras(extras);
	    	setResult(RESULT_OK, this.getIntent());
	    	finish();  
	     } */
	     
	     
       /*  @Override 
         public boolean onKeyDown(int keyCode, KeyEvent event) 
         { 
             if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) 
             { 
           	  if (recording) { 
           		  	camcorderView.stopRecording();
                    finish(); 
                } else { 
                    recording = true; 
                    camcorderView.startRecording(); 
                } 
                 return true; 
             } 
             return super.onKeyDown(keyCode, event); 
         }	  */   
         
         public void showAlert(String result){
        		new AlertDialog.Builder(this)
        	    .setTitle("Error")
        	    .setMessage(result)
        	    .setNegativeButton("OK", new DialogInterface.OnClickListener() {

        	         public void onClick(DialogInterface dialog, int whichButton) {
        	        	endCamcorder();
        	         }
        	    }).show();	
        	}
         
         private void endCamcorder(){
        	setResult(RESULT_OK, this.getIntent());
	 	    finish();  
         }
         
         private void copyFile(File video){ //String srFile, String dtFile){
        	 File tempfile = new File(Environment.getExternalStorageDirectory()+"/LabBook/temp.mp4");   
        	 try{

        	      InputStream in = new FileInputStream(tempfile);

        	      //For Overwrite the file.
        	      OutputStream out = new FileOutputStream(video);

        	      byte[] buf = new byte[1024];
        	      int len;
        	      while ((len = in.read(buf)) > 0){
        	        out.write(buf, 0, len);
        	      }
        	      in.close();
        	      out.close();
        	    }
        	    catch(FileNotFoundException ex){
        	    	Log.i("Camcorder", ex.toString());
        	    }
        	    catch(IOException e){
        	    	Log.i("Camcorder", e.toString());
        	    }
        	    
        	    try{
        	   		tempfile.delete();
        	   	}
        	   	catch (Exception e){
        	   		
        	   	}
        	  }
}
