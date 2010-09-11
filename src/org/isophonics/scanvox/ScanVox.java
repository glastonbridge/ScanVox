/*
  This file is part of ScanVox
  (c) 2010 Queen Mary University of London

    ScanVox is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    ScanVox is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with ScanVox.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.isophonics.scanvox;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
//import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import net.sf.supercollider.android.SCAudio;
import net.sf.supercollider.android.ScService;

public class ScanVox extends Activity {
	public static final String dllDirStr = "/data/data/org.isophonics.scanvox/lib"; // TODO: not very extensible, hard coded, generally sucks
	private static final int DEFAULT_BPM = 120;
	private SCMessageManager messageManager;
	public static enum UserActivity {
		WELCOME, RECORDING, ARRANGING, FATAL_ERROR
	}
	
	private PowerManager.WakeLock wakeLock; // For holding the screen on
	
	public static final String scanvoxTreeDirectory ="scanvox/treeData/";
	
	public static final String[] mySynthDefs = {
		"_scanvox_rec.scsyndef",
		"clockodile.scsyndef",
		"_scanvox_supervisor.scsyndef",
		"_scanvox_playcontrols1.scsyndef",
		"_scanvox_playcontrols2.scsyndef",
		"_scanvox_playcontrols3.scsyndef",
		"_scanvox_playcontrols4.scsyndef",
		"_scanvox_playcontrols5.scsyndef",
		"_scanvox_playcontrols6.scsyndef",
		"_scanvox_playcontrols7.scsyndef",
		"_scanvox_playcontrols8.scsyndef",
		"_scanvox_playcontrols9.scsyndef",
		"_scanvox_ampmatch.scsyndef",
		"_maptsyn_ay1.scsyndef"
	};
	
	public static final String[] myTreeFiles = {
		"mixedvoicedata_MappedSynthAY1_tcbuf_d5m12p99.0.aiff",
		"mixedvoicedata_MappedSynthAY1_tcbuf_d5m12p99.trevmap1.aiff"
	};
	
	public static final MappedSynth[] myMappedSynths = {
		new MappedSynth1AY1(),
		new MappedSynth1Gendy1(),
		new MappedSynth1GrainAmen1(),
		new MappedSynth1Moogy1(),
		new MappedSynth1SuperSimple()
	};
	
	SCAudio superCollider;
	protected String errorMessage = null;  // to throw a fatal error, populate this and call setUserActivity(FATAL_ERROR)
	public static final int numberOfRows = 10;
	private static final String TAG = "ScanVox";

	public static final long GRAPHIC_REFRESH_PERIOD = 150;
	
	protected Arrangement arrangement = new Arrangement(numberOfRows);

	protected SoundManager soundManager = null; 
	
	private class ActivityChooser extends Handler {
        @Override
        public void handleMessage(Message msg) {
        	setUserActivity((UserActivity)msg.obj);
        }
        private void setUserActivity(UserActivity s) {
        	Arranger arranger;
            switch(s) {
    		case WELCOME:
    	        setContentView(R.layout.welcome);
    	        
    	        // Load local html content for welcome page
	        	WebView webView = (WebView) findViewById(R.id.webcontent);
	        	webView.loadUrl("file:///android_asset/welcome/welcome.html");
    	        
    	        ImageButton rec = (ImageButton) findViewById(R.id.Record);
    	        if (scanVoxIsInitialised) {
	    	        rec.setOnClickListener(new OnClickListener() {
	    				public void onClick(View v) {
	    					setUserActivity(UserActivity.RECORDING);
	    				}
	    	        });
	    	        rec.setImageResource(R.drawable.rec);
    	        }
    			break;
    		case RECORDING:
    			setUserActivity(UserActivity.ARRANGING);
    			arranger = (Arranger) findViewById(R.id.arranger);
    			arranger.startRecording();
    			break;
    		case ARRANGING:
    			setContentView(R.layout.arranger);
    			arranger = (Arranger) findViewById(R.id.arranger);
    			Dashboard d = (Dashboard) findViewById(R.id.dashboard);
    			arranger.setDashboard(d);
    			ListView p = (ListView) findViewById(R.id.synthpalette);
    			d.makeSynthList(p);
    			arranger.backgroundPaint.setColor(getResources().getColor(android.R.color.background_light));
    			arranger.rowDivisionPaint.setColor(getResources().getColor(android.R.color.background_dark));
    			arranger.timeDivisionPaint.setColor(getResources().getColor(android.R.color.primary_text_light));
    			arranger.setArrangement(arrangement);
    			arranger.listenToMessages(messageManager);
    			if (soundManager != null ) arranger.setSoundManager(soundManager);
    			else (Toast.makeText(ScanVox.this, "Something is wrong, there is no sound manager", Toast.LENGTH_LONG)).show();
    			break;
    		case FATAL_ERROR:
				setContentView(R.layout.fatal);
				TextView content = (TextView) ScanVox.this.findViewById(R.id.content);
				if (errorMessage != null ) content.setText(errorMessage);
				Button quit = (Button) ScanVox.this.findViewById(R.id.Quit);
				quit.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						System.exit(0);
					}
				});
				break;
    		}
        }
    };

    ActivityChooser activityChooser;
	public boolean scanVoxIsInitialised = false;
    
	/**
	 * Change the application state to allow the user to do something
	 * else.  This will update the visible content view as appropriate
	 * and notify any dependent objects.
	 * 
	 * @param s the new user activity
	 */
	public void setUserActivity(UserActivity s) {
		Message toSend = new Message();
		toSend.obj = s;
		activityChooser.sendMessage(toSend);
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);


        activityChooser = new ActivityChooser();
        setUserActivity(UserActivity.WELCOME);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "ScanVox");
        wakeLock.setReferenceCounted(false);
        wakeLock.acquire();
        
        arrangement.bpm = DEFAULT_BPM;
		(new LaunchSCWhenFilesAreReady()).start();
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	if (superCollider != null) {
	    	superCollider.closeUDP();
			superCollider.sendQuit();

			while (!superCollider.isEnded()) {
				try {
					Thread.sleep(50L);
				} catch (InterruptedException err) {
					Log.e(TAG,"An interruption happened while ScanVox was waiting for SuperCollider to exit.");
					err.printStackTrace();
					break;
				}
			}

    	}
		if (wakeLock!=null) wakeLock.release();
		this.finish();
    }
    
    /**
     * Create files from assets, removing readonly state.
     * 
     * @param assetName
     * @param targetDir
     * @throws IOException
     */
	protected void pipeFile(String assetName, String targetDir) throws IOException {
		InputStream is;
		AssetManager am = getAssets();
		if (assetName.endsWith(".scsyndef"))
			is = am.open("m"+assetName); // Android assets don't get copied out if they begin with _ so they get prefixed with m
		else
			is = am.open(assetName);
		File target = new File(targetDir,assetName);
		OutputStream os = new FileOutputStream(target);
		byte[] buf = new byte[1024];
		int bytesRead = 0;
		while (-1 != (bytesRead = is.read(buf))) {
			os.write(buf,0,bytesRead);
		}
		is.close();
		os.close();
	}
	
	private class LaunchSCWhenFilesAreReady extends Thread {
		public static final int MAX_TRIES = 500;
		public static final long TIME_TWIXT_TRIES = 10;
		public void run () {
	        SoundView.initDataStore(getResources());
	    	try {

	    		File dataDir = new File(ScService.dataDirStr);
	    		dataDir.mkdirs(); 
	    		for (String synthdef : mySynthDefs )
	    			pipeFile(synthdef, ScService.dataDirStr);
	    		File sndDir = new File("/sdcard/",scanvoxTreeDirectory);
//	    		File sndDir = new File(Environment.getExternalStorageDirectory(),scanvoxTreeDirectory);
	    		sndDir.mkdirs();
	    		for (String tree : myTreeFiles)
	    			pipeFile(tree,sndDir.getAbsolutePath());
			} catch (IOException e) {
				Log.e(TAG,"Couldn't copy required files to the external storage device.");
				e.printStackTrace();
			}
			
			try {
				for (String ass : getAssets().list("")) Log.d("TAG",String.format("Asset: '%s'",ass));
			} catch (IOException e) {
				Log.e(TAG,"Couldn't even LIST assets :(");
				e.printStackTrace();
			}

			boolean hasAllSynths = false, hasAllTrees = false;
			int remainingTries = MAX_TRIES;
    		File sndDir = new File("/sdcard/",scanvoxTreeDirectory);
    		File dataDir = new File(ScService.dataDirStr);

			while ((!hasAllTrees || !hasAllSynths) && remainingTries-- > 0) {
				hasAllSynths = true;
				for (String synth : mySynthDefs) {
					if (!(new File(dataDir,synth).exists())) {
						hasAllSynths = false;
						break;
					}
				}
				hasAllTrees = true;
				for (String tree : myTreeFiles) {
					if (!(new File(sndDir,tree).exists())) {
						hasAllTrees = false;
						break;
					}
				}
				if (!hasAllTrees || !hasAllSynths) {
					try {
						sleep(TIME_TWIXT_TRIES);
					} catch (InterruptedException e) { }
				}
			}
			if (!hasAllTrees || !hasAllSynths) {
				errorMessage = "ScanVox could not copy all of its data to this device's storage.  If you have removed your SD card, please re-insert it and try again.";
				setUserActivity(UserActivity.FATAL_ERROR);
			} else {
				superCollider = new SCAudio(dllDirStr);
				superCollider.openUDP(57110); // can remove this when stable - using UDP for dev testing
				superCollider.start();
			    messageManager = new SCMessageManager();
			    messageManager.startListening(superCollider);
				soundManager = new SoundManager(superCollider,messageManager);
				soundManager.setBPM(DEFAULT_BPM);
				scanVoxIsInitialised  = true;
				// trigger redraw of the Record button
		        setUserActivity(UserActivity.WELCOME);
			}
		}
	}
}