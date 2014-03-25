package uk.ac.ic.bss.labbook.media;

import java.io.IOException;

import uk.ac.ic.bss.labbook.LabBookActivity;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.widget.ImageButton;
import android.widget.TextView;

	public class AudioRecorder {

		  MediaRecorder recorder;// = new MediaRecorder(); 
		  MediaPlayer player; // = new MediaPlayer();
		  public boolean recording = false, playing = false;
		  ImageButton playButton, recordButton, stopButton;
		  String oldtext;
		  TextView tview;
		  LabBookActivity calling_enote;
		  
		  public AudioRecorder() {			  
			  
		  }
		  
		  /**
		   * Starts a new recording.
		   */
		  
		  public void record(String path, TextView tv, ImageButton rbutton, ImageButton pbutton, ImageButton sbutton) throws IOException {
			  
			  if(playing){ //player.isPlaying()){
				  return;
			  }
			  
			  recordButton = rbutton;
			  playButton = pbutton;
			  stopButton = sbutton;
			  
			  recorder = new MediaRecorder();
			  recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			  recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
			  recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
			  			  
			  recorder.setOutputFile(path);
			  recorder.prepare();
			  recorder.start();
			  recording = true;
			  playButton.setEnabled(false);
			  recordButton.setEnabled(false);
			  stopButton.setEnabled(true);
			  tv.setTextColor(Color.RED);
			  tv.setText("RECORDING");
		  }

		  public void play(LabBookActivity en, String path, TextView tv, ImageButton pbutton, ImageButton rbutton, ImageButton sbutton) throws IOException {

			  calling_enote = en;
			  tview = tv;
			  playButton = pbutton;
			  recordButton = rbutton;
			  stopButton = sbutton;
			  oldtext = tv.getText().toString();
			  
			  if(recording){
				  return;
			//	  stop();
			//	  recording = false;
			  }
			  
			  player = new MediaPlayer();
			  
			  player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
	  		    	//@Override
					public void onCompletion(MediaPlayer mp) {
						player.reset();
						player.release();
						playing = false;
						playButton.setEnabled(true);
						recordButton.setEnabled(true);
						stopButton.setEnabled(false);
						tview.setTextColor(Color.WHITE);
						tview.setText(oldtext);
						// Ensures previous/next buttons work when playback stops
						calling_enote.audioactive = false;
					}
	  		           
	  		    }); 
			  
			  //player = new MediaPlayer();
			  player.reset();
			  player.setDataSource(path);
			  player.prepare();
			  
			  player.start();
			  playing = true;
			  playButton.setEnabled(false);
			  recordButton.setEnabled(false);
			  stopButton.setEnabled(true);
			  tv.setTextColor(Color.BLUE);
			  tv.setText("PLAYING");
			  /*recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			  recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
			  recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
			  recorder.setOutputFile(path);
			  recorder.prepare();
			  recorder.start();*/
			  }
		  
		  /**
		   * Stops a recording that has been previously started.
		   */
		  public void stop(TextView tv) throws IOException {
			  if(recording){
				  recorder.stop();
				  recorder.reset();
				  recorder.release();
				  recording = false;
				  playButton.setEnabled(true);
				  recordButton.setEnabled(true);
				  stopButton.setEnabled(false);
				  tv.setTextColor(Color.WHITE);
				  tv.setText("Audio Available");
			  }
			  else if(playing){
				  player.stop();
				  player.reset();
				  player.release();
				  playing = false;
				  playButton.setEnabled(true);
				  recordButton.setEnabled(true);
				  stopButton.setEnabled(false);
				  tv.setTextColor(Color.WHITE);
				  tv.setText(oldtext);
			  }
		  }
		  
		  //public void stop() throws IOException {
			//  player.stop();
			//  player.release();
		  //}
		  

		}



