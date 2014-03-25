package uk.ac.ic.bss.labbook.calculator;

/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//package com.android.calculator2;


import uk.ac.ic.bss.labbook.R;
import android.content.Intent;
import android.os.Bundle;
import android.util.Config;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.apps.analytics.easytracking.TrackedActivity;

public class Calculator extends TrackedActivity {
    EventListener mListener = new EventListener();
    private CalculatorDisplay mDisplay;
    private Persist mPersist;
    private History mHistory;
    private Logic mLogic;
    private PanelSwitcher mPanelSwitcher;
    private Button backbutton;

    private static final int CMD_CLEAR_HISTORY  = 1;
    private static final int CMD_BASIC_PANEL    = 2;
    private static final int CMD_ADVANCED_PANEL = 3;

    private static final int HVGA_WIDTH_PIXELS  = 320;

    static final int BASIC_PANEL    = 0;
    static final int ADVANCED_PANEL = 1;

    private static final String LOG_TAG = "Calculator";
    private static final boolean DEBUG  = false;
    private static final boolean LOG_ENABLED = DEBUG ? Config.LOGD : Config.LOGV;
    private static final String STATE_CURRENT_VIEW = "state-current-view";

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

       // setContentView(R.layout.main);
        setContentView(uk.ac.ic.bss.labbook.R.layout.calc);

        mPersist = new Persist(this);
        mHistory = mPersist.history;

        mDisplay = (CalculatorDisplay) findViewById(R.id.display);

        mLogic = new Logic(this, mHistory, mDisplay, (Button) findViewById(uk.ac.ic.bss.labbook.R.id.equal));
        HistoryAdapter historyAdapter = new HistoryAdapter(this, mHistory, mLogic);
        mHistory.setObserver(historyAdapter);
        
        mPanelSwitcher = (PanelSwitcher) findViewById(uk.ac.ic.bss.labbook.R.id.panelswitch);
               mPanelSwitcher.setCurrentIndex(state==null ? 0 : state.getInt(STATE_CURRENT_VIEW, 0));

        mListener.setHandler(mLogic, mPanelSwitcher);

        mDisplay.setOnKeyListener(mListener);

        View view;
        if ((view = findViewById(uk.ac.ic.bss.labbook.R.id.del)) != null) {
//            view.setOnClickListener(mListener);
            view.setOnLongClickListener(mListener);
        }
        
        backbutton = (Button) findViewById(uk.ac.ic.bss.labbook.R.id.back);
        backbutton.setOnClickListener(new View.OnClickListener() {
 			public void onClick(View view) {
 				finish();
 			}
     	});
        
        //Bundle extras = getIntent().getExtras();
        /*
        if ((view = findViewById(uk.ac.ic.bss.labbook.R.id.clear)) != null) {
            view.setOnClickListener(mListener);
        }
        */
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuItem item;
        
        item = menu.add(0, CMD_CLEAR_HISTORY, 0, uk.ac.ic.bss.labbook.R.string.clear_history);
        item.setIcon(uk.ac.ic.bss.labbook.R.drawable.clear_history);
        
        item = menu.add(0, CMD_ADVANCED_PANEL, 0, uk.ac.ic.bss.labbook.R.string.advanced);
        item.setIcon(uk.ac.ic.bss.labbook.R.drawable.advanced);
        
        item = menu.add(0, CMD_BASIC_PANEL, 0, uk.ac.ic.bss.labbook.R.string.basic);
        item.setIcon(uk.ac.ic.bss.labbook.R.drawable.simple);

        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(CMD_BASIC_PANEL).setVisible(mPanelSwitcher != null && 
                          mPanelSwitcher.getCurrentIndex() == ADVANCED_PANEL);
        
        menu.findItem(CMD_ADVANCED_PANEL).setVisible(mPanelSwitcher != null && 
                          mPanelSwitcher.getCurrentIndex() == BASIC_PANEL);
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case CMD_CLEAR_HISTORY:
            mHistory.clear();
            break;

        case CMD_BASIC_PANEL:
            if (mPanelSwitcher != null && 
                mPanelSwitcher.getCurrentIndex() == ADVANCED_PANEL) {
                mPanelSwitcher.moveRight();
            }
            break;

        case CMD_ADVANCED_PANEL:
            if (mPanelSwitcher != null && 
                mPanelSwitcher.getCurrentIndex() == BASIC_PANEL) {
                mPanelSwitcher.moveLeft();
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putInt(STATE_CURRENT_VIEW, mPanelSwitcher.getCurrentIndex());
    }

    @Override
    public void onPause() {
        super.onPause();
        mLogic.updateHistory();
        mPersist.save();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        if (keyCode == KeyEvent.KEYCODE_BACK 
            && mPanelSwitcher.getCurrentIndex() == ADVANCED_PANEL) {
            mPanelSwitcher.moveRight();
            return true;
        } else {
            return super.onKeyDown(keyCode, keyEvent);
        }
    }

    static void log(String message) {
        if (LOG_ENABLED) {
            Log.v(LOG_TAG, message);
        }
    }

    /**
     * The font sizes in the layout files are specified for a HVGA display.
     * Adjust the font sizes accordingly if we are running on a different
     * display.
     */
    public void adjustFontSize(TextView view) {
        float fontPixelSize = view.getTextSize();
        Display display = getWindowManager().getDefaultDisplay();
        int h = Math.min(display.getWidth(), display.getHeight());
        float ratio = (float)h/HVGA_WIDTH_PIXELS;
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontPixelSize*ratio);
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
		data.putExtra("returnKey1", mDisplay.getText().toString());
		data.putExtra("returnKey2", "You could be better then you are. ");
		setResult(RESULT_OK, data);
		super.finish();
    	//finish();  
    }
}