package uk.ac.ic.bss.labbook.stocksolution;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;

import kankan.wheel.widget.WheelView;
import kankan.wheel.widget.adapters.ArrayWheelAdapter;
import uk.ac.ic.bss.labbook.R;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.apps.analytics.easytracking.TrackedActivity;

public class StockSolutionActivity extends TrackedActivity implements OnItemClickListener, OnItemSelectedListener {
    
	//private WheelView = new WheelView();
	private Button calcButton;
	private Button closeButton;
	private EditText weight;
	private EditText molarity;
	private EditText volume;
	private TextView answer;
	private WheelView volWheel;
	private WheelView molWheel;
	private Double volumeMultiplier[];
	private Double molarityMultiplier[];
	private String volumes[];
	private ArrayList molNameList;
	private ArrayList molWeightList;
	private ArrayAdapter<String> molAdapter;
	private AutoCompleteTextView acTextView;
	private String compoundSelected;
	private String ans;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stock_solution_layout);
        
        molNameList = new ArrayList<String>();
        molWeightList = new ArrayList<Double>();
        
        compoundSelected="";
        ans="";
        
        molWheel = (WheelView) findViewById(R.id.molarity);
        volWheel = (WheelView) findViewById(R.id.volume_wheel);
        weight= (EditText) findViewById(R.id.molweightentry);
    	molarity= (EditText) findViewById(R.id.concentry);
    	molarity.setText("1");
    	volume= (EditText) findViewById(R.id.volumeentry);
    	volume.setText("1");
    	answer = (TextView) findViewById(R.id.answer_text);
    	molarityMultiplier = new Double[] {1.0, 0.001, 0.000001, 0.000000001, 0.000000000001};
        final String cities[] = new String[] {"molar [M]", "millimolar [mM]", "micromolar [�M]", "nanomolar [nM]", "picomolar [pM]"};
        ArrayWheelAdapter<String> adapter = new ArrayWheelAdapter<String>(this, cities);
            adapter.setTextSize(18);
            molWheel.setViewAdapter(adapter);
            Log.i("READ ERRROR","cities[0].length: "+cities.length);
            molWheel.setCurrentItem(0);
            molWheel.setVisibleItems(3);
            
        volumeMultiplier = new Double[] {1.0, 0.001, 0.000001};    
         volumes = new String[] {"litre(s)", "millilitre(s)", "microlitre(s)"};
        ArrayWheelAdapter<String> volAdapter = new ArrayWheelAdapter<String>(this, volumes);
        volAdapter.setTextSize(18);
        volWheel.setViewAdapter(volAdapter);
            Log.i("READ ERRROR","cities[0].length: "+volumes.length);
            volWheel.setCurrentItem(0);
            volWheel.setVisibleItems(3);
            
            calcButton = (Button)this.findViewById(R.id.ok_button);
            calcButton.setOnClickListener(new View.OnClickListener() {
    			public void onClick(View view) {
    				calcMix();
    			}
        	});
            InputStream inputStream = null;
            try  
            { 
            	inputStream = getAssets().open("molWeightTable.txt");
            
                if (inputStream != null) {
                	InputStreamReader streamReader = new InputStreamReader(inputStream);
                	  BufferedReader bufferedReader = new BufferedReader(streamReader);

                	  String l;
                	  String[] splitter = new String[2];
                	  while ( (l = bufferedReader.readLine())!=null) {
                	    // split the string into name and molweight
                		  
                		  splitter = l.split("\t");
                		  
                		  molNameList.add(splitter[0]);
                		  molWeightList.add(splitter[1]);
                	  }
                	  inputStream.close();
                }
                
            }  
            catch (IOException e)  
            {  
            	Log.i("test", "File: not found!");
            }
            
            molAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_dropdown_item_1line,molNameList);
            acTextView = (AutoCompleteTextView)findViewById(R.id.molWeightSelector);
            acTextView.setThreshold(1);
            acTextView.setAdapter(molAdapter); 
            acTextView.setOnItemSelectedListener(this);
            acTextView.setOnItemClickListener(this);
            
            closeButton = (Button)this.findViewById(R.id.close_button);
            closeButton.setOnClickListener(new View.OnClickListener() {
    			public void onClick(View view) {
    				finish();
    			}
        	});

    		    
    }
    
    private void calcMix()
    {
    	Log.i(NOTIFICATION_SERVICE, "vol wheel:"+volWheel.getCurrentItem() );
    	Double volMult = volumeMultiplier[volWheel.getCurrentItem()];
    	Double molMult = molarityMultiplier[molWheel.getCurrentItem()];
    	System.out.println("weight: "+weight.getText().toString());
    	System.out.println("molarity: "+molarity.getText().toString());
    	System.out.println("volume: "+volume.getText().toString());
    	Log.d("test_click",weight.getText().toString());
    	if(weight.getText().toString().equals("or enter molecular weight")||(weight.getText().toString()==null)||weight.getText().toString().equals(""))
    	{
    		//users needs to add a mol weight
    		weight.setText("");
    		weight.setHighlightColor(65536);
    		weight.requestFocus();
    		Toast toast = Toast.makeText(this, "Please enter a molecular weight", Toast.LENGTH_SHORT);
    		toast.show();
    	}
    	else
    	{	
	    	Number grams=  Float.parseFloat(weight.getText().toString())*Float.parseFloat(molarity.getText().toString())*Float.parseFloat(volume.getText().toString())*volMult*molMult;
	    	DecimalFormat df = new DecimalFormat("0.0000");
	    	String compoundName="compound";
	    	if(!compoundSelected.equals(""))
	    	{
	    		compoundName = acTextView.getText().toString();
	    	}
	    	ans="Weigh out "+df.format(grams)+" grams of "+compoundName+" and fill up to "+volume.getText().toString() +" "+volumes[volWheel.getCurrentItem()]+" of water.";
	    	answer.setText(ans);
    	}	
    }
    
    /**
     * Implements OnItemClickListener
     */
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.i("test_click", "onItemClick() position " + position);
        //EditText localText = (EditText) view;
        Log.i("test_click", "onItemClick() view text " + molAdapter.getItem(position).toString());
        compoundSelected=molAdapter.getItem(position).toString();
        setWeightText(molAdapter.getItem(position).toString());
        //mItemClickCalled = true;
        //mItemClickPosition = position;
    }
    
    private void setWeightText(String name)
    {
    	int counter = 0;
    	 for (Object a : molNameList) {
             if(a.toString().compareTo(name)==0)
             {
            	 weight.setText(molWeightList.get(counter).toString());
            	 break;
             }
             else
             {
            	 counter++;
             }
         }
    }

    /** 
     * Implements OnItemSelectedListener
     */
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Log.i("test_selected", "onItemSelected() position " + position);
       // mItemSelectedCalled = true;
        //mItemSelectedPosition = position;
    }

    /** 
     * Implements OnItemSelectedListener
     */
    public void onNothingSelected(AdapterView<?> parent) {
        Log.i("test", "onNothingSelected()");
        //mNothingSelectedCalled = true;
    }
    
    @Override
    public void finish() {
    	// Prepare data intent 
    	//Bundle extras = getIntent().getExtras();
   	  	//extras.putString("testName", "hello");
   	  	//this.getIntent().putExtras(extras);
   	  	//setResult(RESULT_OK, this.getIntent());
    	Intent data = new Intent();
		// Return some hard-coded values
		data.putExtra("returnKey1", ans);
		data.putExtra("returnKey2", "You could be better then you are. ");
		setResult(RESULT_OK, data);
		super.finish();
    	//finish();  
    }
}