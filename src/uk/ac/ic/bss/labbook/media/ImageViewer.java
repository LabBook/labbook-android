package uk.ac.ic.bss.labbook.media;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.google.android.apps.analytics.easytracking.TrackedActivity;

public class ImageViewer extends TrackedActivity {

	     private ImageView imageView; 
	     private ImageButton endButton; // playButton, 
	     private Bitmap bmp;
	     
	     /** Called when the activity is first created. */ 
	     @Override 
	     public void onCreate(Bundle savedInstanceState) { 
	          super.onCreate(savedInstanceState); 
	          requestWindowFeature(Window.FEATURE_NO_TITLE); 
	          //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); 
	          setContentView(uk.ac.ic.bss.labbook.R.layout.image_view); 
	          	         		         
	          imageView = (ImageView)findViewById(uk.ac.ic.bss.labbook.R.id.imageView); 
	         
	          endButton = (ImageButton) findViewById(uk.ac.ic.bss.labbook.R.id.endbut);
	  	     
	         Bundle extras = getIntent().getExtras();
	         if (extras != null && extras.get("IMAGE_ID") != null){
	        	// Log.i("IMAGE FILE", imagedir+"/"+extras.getString("IMAGE_ID"));
	        	 bmp = BitmapFactory.decodeFile(extras.get("IMAGE_ID").toString());
	        	 imageView.setImageBitmap(bmp); //setImageURI(Uri.fromFile((File) extras.get("IMAGE_ID")));
	         }
	         else{
	        	 showAlert("Problem with image!");
	         }
	         
	         endButton.setBackgroundColor(Color.DKGRAY);
	         endButton.setImageResource(uk.ac.ic.bss.labbook.R.drawable.undo);
	                  
	         endButton.setOnClickListener(new View.OnClickListener() {

	              public void onClick(View arg0) {
	            	  //startRecording();
	            	  endImage();
	              }
	             
	          });
	         
	     } 
	     
	     public boolean onKeyDown(int keyCode, KeyEvent event)
		    {
		    	if(keyCode == KeyEvent.KEYCODE_BACK){ // event.KEYCODE_BACK
		    		endImage();
		    		
		    	}

		        return true;
		    } 
	     
	     private void endImage(){

	    	imageView.setImageBitmap(null);
	    	bmp.recycle();
       	  	Bundle extras = getIntent().getExtras();
       	  	this.getIntent().putExtras(extras);
       	  	setResult(RESULT_OK, this.getIntent());
       	  	finish();  
	     }
	     
	     public void showAlert(String result){
     		new AlertDialog.Builder(this)
     	    .setTitle("Error")
     	    .setMessage(result)
     	    .setNegativeButton("OK", new DialogInterface.OnClickListener() {

     	         public void onClick(DialogInterface dialog, int whichButton) {
     	        	endImage();
     	         }
     	    }).show();	
     	}
	     
	     
	     
	     
}
