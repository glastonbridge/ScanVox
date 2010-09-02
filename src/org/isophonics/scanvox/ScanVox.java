package org.isophonics.scanvox;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.isophonics.scanvox.Arrangement.Row;
import org.isophonics.scanvox.Arrangement.Sound;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import net.sf.supercollider.android.OscMessage;
import net.sf.supercollider.android.SCAudio;
import net.sf.supercollider.android.ScService;

public class ScanVox extends Activity {
	public static final String dllDirStr = "/data/data/org.isophonics.scanvox/lib"; // TODO: not very extensible, hard coded, generally sucks
	public static enum UserActivity {
		WELCOME, RECORDING, ARRANGING
	}
	
	SCAudio superCollider = new SCAudio(dllDirStr);
	
	public static final int numberOfRows = 10;
	private static final String TAG = "ScanVox";
	
	protected Arrangement arrangement = new Arrangement(numberOfRows);
	protected SoundManager soundManager = new SoundManager(superCollider); 
	
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
			arranger.setSoundManager(soundManager);
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
    		pipeFile("clockodile.scsyndef",ScService.dataDirStr);
    		pipeFile("recordbuffer.scsyndef",ScService.dataDirStr);
			pipeFile("playbuffer.scsyndef",ScService.dataDirStr);
		} catch (IOException e) {
			Log.e(TAG,"Couldn't copy synths to the synthdef directory.");
			e.printStackTrace();
		}

        // /test data
        superCollider.openUDP(57110);
        superCollider.start();
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
		InputStream is = getAssets().open(assetName);
		OutputStream os = new FileOutputStream(targetDir+"/"+assetName);
		byte[] buf = new byte[1024];
		int bytesRead = 0;
		while (-1 != (bytesRead = is.read(buf))) {
			os.write(buf,0,bytesRead);
		}
		is.close();
		os.close();
	}
}