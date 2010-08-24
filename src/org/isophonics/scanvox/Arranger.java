package org.isophonics.scanvox;

import org.isophonics.scanvox.Arrangement.Sound;

import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

/**
 * Shows the arrangement on-screen, lets the user move the bars around,
 * and updates during playback.  This is the visual side, Arrangement is
 * the data.
 * 
 * @author alex shaw
 */
public class Arranger implements SurfaceHolder.Callback, View.OnTouchListener {
	private static final String TAG = "Arranger";
	public Paint backgroundPaint;
	public int rowHeight;
	// The mythical audio tick refers to whatever we use to sync
	// up with the arrangement (poss some subdivision of beat?)
	public float pixelsPerAudioTick = 16; // if this goes < 14 interesting things may have to happen with the bevel graphics
	public float bevelWidth; // the width of the nice rounded edges on sounds
	private SurfaceHolder holder;
	private SurfaceView view;
	protected Arrangement arrangement=new Arrangement(0);
	public Arranger(SurfaceView s) {
		holder = s.getHolder();
		holder.addCallback(this);
		view = s;
		backgroundPaint = new Paint();
		s.setOnTouchListener(this);
		soundBackLeft   = BitmapFactory.decodeResource(s.getResources(), R.drawable.chunk_back_left);
		soundBackMiddle = BitmapFactory.decodeResource(s.getResources(), R.drawable.chunk_back_mid);
		soundBackRight  = BitmapFactory.decodeResource(s.getResources(), R.drawable.chunk_back_right);
		soundFrontLeft   = BitmapFactory.decodeResource(s.getResources(), R.drawable.chunk_front_left);
		soundFrontMiddle = BitmapFactory.decodeResource(s.getResources(), R.drawable.chunk_front_mid);
		soundFrontRight  = BitmapFactory.decodeResource(s.getResources(), R.drawable.chunk_front_right);
		bevelWidth = soundBackLeft.getWidth();
		rowHeight  = soundBackLeft.getHeight();
	}
	
	/**
	 * Set the background colour for the arranger
	 * 
	 * @param colourCode
	 */
	public void setBackgroundColour(int colourCode) {
		backgroundPaint.setColor(colourCode);
	}
	
	/**
	 * Set the arrangement to edit
	 * 
	 * @param a
	 */
	public void setArrangement(Arrangement a) {
		arrangement = a;
		pixelsPerAudioTick = view.getWidth()/arrangement.length;
		Log.d(TAG,"Setting pixels per audio tick to "+pixelsPerAudioTick);
	}
	
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {

		pixelsPerAudioTick = view.getWidth()/arrangement.length;
		drawSurface();
	}

	public void surfaceCreated(SurfaceHolder holder) {
		drawSurface();
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
	}

	private Sound soundBeingMoved = null;
	private float soundBeingMovedX, soundBeingMovedY;
	/** The distance between the top left corner and where you're actually touching */
	private float soundBeingMovedHandleX, soundBeingMovedHandleY; 
	
	private Arrangement.Row soundBeingMovedOldHome;
	
	/**
	 * Refresh the graphics on the surface
	 */
	private void drawSurface() {
		Canvas c = null;
		try {
			c = holder.lockCanvas();
			if (c != null) {
				c.drawRect(0,0,c.getWidth(),c.getHeight(),backgroundPaint);
				int rowNum=0;
				for (Arrangement.Row row : arrangement.rows) drawRow(c,row,rowNum++); // crow!
				if (soundBeingMoved != null) 
					drawSound(
							c,
							soundBeingMoved,
							soundBeingMovedX,
							soundBeingMovedY);
			}
		} finally {
			if (c != null) {
				holder.unlockCanvasAndPost(c);
			}
		}
	}
	private void drawRow (Canvas c, Arrangement.Row r, int rowNum) {
		for (Sound s : r) drawSound(c,s, s.getStartTime()*pixelsPerAudioTick, rowNum*rowHeight);
	}

	private Bitmap soundBackLeft;
	private Bitmap soundBackMiddle;
	private Bitmap soundBackRight;
	private Bitmap soundFrontLeft;
	private Bitmap soundFrontMiddle;
	private Bitmap soundFrontRight;
	private void drawSound (Canvas c, Sound s,float x, float y) {
		Paint soundPaint = new Paint();
		soundPaint.setColor (Color.BLUE);
		soundPaint.setStyle (Style.FILL);
		
		Rect dest = new Rect ();
		dest.top = (int) y;
		dest.bottom = (int) (y + rowHeight);
		dest.left = (int) (x+bevelWidth);
		dest.right = (int) (x + s.getLength()*pixelsPerAudioTick - bevelWidth);
		c.drawBitmap(soundBackLeft, x, y, soundPaint);
		c.drawBitmap(soundBackMiddle, null, dest, soundPaint);
		c.drawBitmap(soundBackRight, x + s.getLength()*pixelsPerAudioTick - bevelWidth,y,soundPaint);
		c.drawBitmap(soundFrontLeft, x, y, soundPaint);
		c.drawBitmap(soundFrontMiddle, null, dest, soundPaint);
		c.drawBitmap(soundFrontRight, x + s.getLength()*pixelsPerAudioTick - bevelWidth,y,soundPaint);
	}

	/**
	 * The cases we want to consider are:
	 * 
	 * 1) A freshly-touched sound which we want to drag
	 * 2) A sound in the process of being dragged
	 * 3) The release of a sound back into a row
	 * 
	 */
	public boolean onTouch(View v, MotionEvent event) {
		if (event.getAction()==MotionEvent.ACTION_DOWN) {
			if ( soundBeingMoved != null ) {
				// Sanity check, we should never have an action down before a previous action up
				Log.e(TAG,"Arranger was asked to pick up a sound while it was already holding one.");
				Toast.makeText(view.getContext(), "I've got confused, and I might have mucked up your tune.", Toast.LENGTH_SHORT);
				soundBeingMoved = null;
			}
			int rowNum = (int) event.getY() / rowHeight;
			if (arrangement.rows.size()>rowNum) {
				Arrangement.Row row = arrangement.rows.get(rowNum); 
				soundBeingMoved = row.grabSoundAt(
						(int) (event.getX()/pixelsPerAudioTick));
				if(soundBeingMoved!=null) {
					soundBeingMovedOldHome = row;
					soundBeingMovedHandleX = event.getX() - soundBeingMoved.getStartTime()*pixelsPerAudioTick;
					soundBeingMovedHandleY = event.getY() % rowHeight;
				}
			}
			return true;
		} else if (event.getAction()==MotionEvent.ACTION_MOVE) {
			soundBeingMovedX = event.getX() - soundBeingMovedHandleX;
			soundBeingMovedY = event.getY() - soundBeingMovedHandleY;
			drawSurface();
			return true;
		} else if (event.getAction()==MotionEvent.ACTION_UP && soundBeingMoved != null) {
			if (!addSoundAt(event.getX() - soundBeingMovedHandleX, event.getY() - soundBeingMovedHandleY, soundBeingMoved)
			 && !soundBeingMovedOldHome.add(soundBeingMoved))
				Log.e(TAG,"Could not replace a sound where it used to belong in an arrangement.");
			soundBeingMoved = null;
			drawSurface();
			return true;
		}
		return false;
	}
	
	/** 
	 * Push a sound (back) into the world of rows
	 * @param x
	 * @param y
	 * @return true on success
	 */
	private boolean addSoundAt(float x, float y, Sound s) {
		Log.d(TAG,"Adding sound at "+x+","+y);
		int rowNum = (int) (y / rowHeight);
		if (arrangement.rows.size() <= rowNum) return false;
		Arrangement.Row row = arrangement.rows.get(rowNum);
		return row.add (new Sound ((int)(x/pixelsPerAudioTick),s.getLength()));
	}
	
}
