package org.isophonics.scanvox;

import org.isophonics.scanvox.R;
import org.isophonics.scanvox.Arrangement.Row;
import org.isophonics.scanvox.Arrangement.Sound;

import android.app.Activity;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

public class ScanVox extends Activity {
	public static enum UserActivity {
		WELCOME, RECORDING, ARRANGING
	}
	
	protected Arrangement arrangement = new Arrangement(10);
	
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
			Arranger arranger = new Arranger((SurfaceView) findViewById(R.id.Tracker));
			arranger.setBackgroundColour(getResources().getColor(android.R.color.background_light));
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
        setUserActivity(UserActivity.WELCOME);
        // test data
        Row r = arrangement.rows.get(0);
        r.add(new Sound(0,14));
        // /test data
    }
}