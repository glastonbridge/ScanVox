package org.isophonics.scanvox;

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

public class ScanVox extends Activity {
	public static enum UserActivity {
		WELCOME, RECORDING, ARRANGING
	}
	
	SCAudio superCollider = new SCAudio();
	public static final int numberOfRows = 10;
	private static final String TAG = "ScanVox";
	
	protected Arrangement arrangement = new Arrangement(numberOfRows);
	
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
			break;
		case ARRANGING:
			break;
		}
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // test data
        Row r = arrangement.rows.get(0);
        r.add(new Sound(0,14));
        // /test data
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
}