package org.isophonics.scanvox;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.Toast;
import net.sf.supercollider.android.OscMessage;
import net.sf.supercollider.android.SCAudio;
import net.sf.supercollider.android.ScService;

public class ScanVox extends Activity {
	public static final String dllDirStr = "/data/data/org.isophonics.scanvox/lib"; // TODO: not very extensible, hard coded, generally sucks
	public static enum UserActivity {
		WELCOME, RECORDING, ARRANGING
	}
	
	public static final String scanvoxTreeDirectory ="scanvox/treeData/";
	
	public static final String[] mySynthDefs = {
		"clockodile.scsyndef",
		"_scanvox_playcontrols1.scsyndef",
		"_scanvox_playcontrols2.scsyndef",
		"_scanvox_playcontrols3.scsyndef",
		"_scanvox_playcontrols4.scsyndef",
		"_scanvox_playcontrols5.scsyndef",
		"_scanvox_playcontrols6.scsyndef",
		"_scanvox_playcontrols7.scsyndef",
		"_scanvox_playcontrols8.scsyndef",
		"_scanvox_playcontrols9.scsyndef",
		"_scanvox_rec.scsyndef",
		"_maptsyn_ay1.scsyndef"
	};
	
	public static final String[] myTreeFiles = {
		"mixedvoicedata_MappedSynthAY1_tcbuf_d5m12p99.0.aiff",
		"mixedvoicedata_MappedSynthAY1_tcbuf_d5m12p99.trevmap1.aiff"
	};
	
	SCAudio superCollider = new SCAudio(dllDirStr);
	
	public static final int numberOfRows = 10;
	private static final String TAG = "ScanVox";

	public static final long GRAPHIC_REFRESH_PERIOD = 150;
	
	protected Arrangement arrangement = new Arrangement(numberOfRows);
	protected SoundManager soundManager = null; 
	
	/**
	 * Change the application state to allow the user to do something
	 * else.  This will update the visible content view as appropriate
	 * and notify any dependent objects.
	 * 
	 * @param s the new user activity
	 */
	public void setUserActivity(UserActivity s) {
		switch(s) {
		case WELCOME:
	        setContentView(R.layout.welcome);
	        ImageButton rec = (ImageButton) findViewById(R.id.Record);
	        rec.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					setUserActivity(UserActivity.RECORDING);
				}
	        });
			break;
		case RECORDING:
			setContentView(R.layout.arranger);
			Arranger arranger = (Arranger) findViewById(R.id.arranger);
			arranger.backgroundPaint.setColor(getResources().getColor(android.R.color.background_light));
			arranger.rowDivisionPaint.setColor(getResources().getColor(android.R.color.background_dark));
			arranger.timeDivisionPaint.setColor(getResources().getColor(android.R.color.primary_text_light));
			arranger.setArrangement(arrangement);
			if (soundManager != null ) arranger.setSoundManager(soundManager);
			else (Toast.makeText(this, "Something is wrong, there is no sound manager", Toast.LENGTH_LONG)).show();
			break;
		case ARRANGING:
			break;
		}
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);
    	try {

    		File dataDir = new File(ScService.dataDirStr);
    		dataDir.mkdirs(); 
    		for (String synthdef : mySynthDefs )
    			pipeFile(synthdef, ScService.dataDirStr);
    		File sndDir = new File("/sdcard/",scanvoxTreeDirectory);
//    		File sndDir = new File(Environment.getExternalStorageDirectory(),scanvoxTreeDirectory);
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

        // /test data
        //superCollider.openUDP(57110);
        superCollider.start();
        soundManager = new SoundManager(superCollider);
        setUserActivity(UserActivity.RECORDING);
    }
    
    @Override
    public void onPause() {
    	super.onPause();
		superCollider.sendMessage (OscMessage.quitMessage());

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
    
    /**
     * Create files from assets, removing readonly state.
     * 
     * @param assetName
     * @param targetDir
     * @throws IOException
     */
	protected void pipeFile(String assetName, String targetDir) throws IOException {
		InputStream is;
		is = getAssets().open("m"+assetName); // Android assets don't get copied out if they begin with _
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
}