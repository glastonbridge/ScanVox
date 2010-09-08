package org.isophonics.scanvox;

import org.isophonics.scanvox.Arrangement.Sound;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

/**
 * Shows the arrangement on-screen, lets the user move the bars around,
 * and updates during playback.
 * 
 * Key responsibilities of this class:
 *  - Visualise an Arrangement
 *  - When any interaction occurs, notify and update:
 *     -- Dashboard
 *     -- SoundManager
 *     -- Arrangement
 * @TODO: Split this class out into individual responsibilities
 * 
 * @author Alex Shaw
 */
public class Arranger extends View {
	private static final String TAG = "Arranger";
	
	private static final int bufferDuration = 2000; // ms
	
	/** Paint objects for styling various parts of the Arranger */
	public Paint 
		backgroundPaint, 
		rowDivisionPaint, 
		timeDivisionPaint,
		soundPaint;
	
	/** Dimensions of the arrangement display */
	public int rowHeight;
	// The mythical audio tick refers to whatever we use to sync
	// up with the arrangement (poss some subdivision of beat?)
	public float pixelsPerAudioTick = 16; // if this goes < 14 interesting things may have to happen with the bevel graphics
	public float bevelWidth; // the width of the nice rounded edges on sounds
	public int ticksPerDivision = 4;
	private int height = 320; // will be updated later
	private Dashboard dashboard;
	private SoundManager soundManager;
	
	protected Arrangement arrangement=new Arrangement(0);
	
	public Arranger(Context c, AttributeSet s) {
		super(c,s);
		init();
	}
	
	public Arranger(Context c) {
		super(c);
		init();
	}
	
	private void init() {
		dashboard = new Dashboard(getContext());
		
		backgroundPaint = new Paint();
		timeDivisionPaint = new Paint();
		rowDivisionPaint = new Paint();
		soundPaint = new Paint(); // used only on bitmaps, doesn't do very much
		soundPaint.setAntiAlias(false);
		
		soundBackLeft    = BitmapFactory.decodeResource(getResources(), R.drawable.chunk_back_left);
		soundBackMiddle  = BitmapFactory.decodeResource(getResources(), R.drawable.chunk_back_mid);
		soundBackRight   = BitmapFactory.decodeResource(getResources(), R.drawable.chunk_back_right);
		soundFrontLeft   = BitmapFactory.decodeResource(getResources(), R.drawable.chunk_front_left);
		soundFrontMiddle = BitmapFactory.decodeResource(getResources(), R.drawable.chunk_front_mid);
		soundFrontRight  = BitmapFactory.decodeResource(getResources(), R.drawable.chunk_front_right);
		
		bevelWidth       = soundBackLeft.getWidth();
		rowHeight        = soundBackLeft.getHeight();
		pixelsPerAudioTick = getWidth()/arrangement.length;
		height = getHeight();
		
		invalidate();
	}
	
	/**
	 * Set the arrangement to edit
	 * 
	 * @param a my arrangement
	 */
	public void setArrangement(Arrangement a) {
		arrangement = a;
		pixelsPerAudioTick = getWidth()/arrangement.length;
		Log.d(TAG,"Setting pixels per audio tick to "+pixelsPerAudioTick);
	}
	
	/**
	 * Set the sound manager to notify
	 * 
	 */
	public void setSoundManager(SoundManager s) {
		soundManager = s;
	}
	
	private Sound soundBeingMoved = null;
	private float soundBeingMovedX, soundBeingMovedY;
	/** The distance between the top left corner and where you're actually touching */
	private float soundBeingMovedHandleX, soundBeingMovedHandleY; 
	
	private Arrangement.Row soundBeingMovedOldHome;
	
	/**
	 * Refresh the user graphics
	 */
	@Override
	protected void onDraw(Canvas c) {

		pixelsPerAudioTick = c.getWidth()/arrangement.length;
		c.drawRect(0,0,c.getWidth(),height,backgroundPaint);
		drawGrid(c);
		int rowNum=0;
		for (Arrangement.Row row : arrangement.rows) drawRow(c,row,rowNum++); // crow!
		if (soundBeingMoved != null) 
			drawSound(
					c,
					soundBeingMoved,
					soundBeingMovedX,
					soundBeingMovedY);
		dashboard.draw(c);
	}
	
	/**
	 * Set up the size of the 
	 */
	
	/**
	 * Draw a visual mesh for the user sounds to fit in
	 * @param c The canvas to draw to
	 */
	private void drawGrid (Canvas c) {
		int beyondLastRow = rowHeight*arrangement.rows.size() + 1;
		int width = c.getWidth();
		int height = c.getHeight();
		int blockDivision = (int) pixelsPerAudioTick * ticksPerDivision;
		for ( int i = rowHeight ; i < beyondLastRow ; i += rowHeight) 
			c.drawLine(0, i, width, i, rowDivisionPaint);
		if (blockDivision >0) for ( int i = blockDivision ; i < width ; i += blockDivision)
			c.drawLine(i, 0, i, height, timeDivisionPaint);
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

    private RefreshHandler mRedrawHandler = new RefreshHandler();

    class RefreshHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            Arranger.this.invalidate();
        }

        public void sleep(long delayMillis) {
                this.removeMessages(0);
            sendMessageDelayed(obtainMessage(0), delayMillis);
        }
    };

	
    private long lastUpdateTime = 0;
    
	/**
	 * The cases we want to consider are:
	 * 
	 * 1) A freshly-touched sound which we want to drag
	 * 2) A sound in the process of being dragged
	 * 3) The release of a sound back into a row
	 * 
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// Force frame limiting
		long now = System.currentTimeMillis();
		if (now<lastUpdateTime+ScanVox.GRAPHIC_REFRESH_PERIOD
				&& event.getAction() == MotionEvent.ACTION_MOVE) 
			return true;
		lastUpdateTime = now;
		
		if (event.getAction()==MotionEvent.ACTION_DOWN) {
			if ( soundBeingMoved != null ) {
				// Sanity check, we should never have an action down before a previous action up
				Log.e(TAG,"Arranger was asked to pick up a sound while it was already holding one.");
				Toast.makeText(getContext(), "I've got confused, and I might have mucked up your tune.", Toast.LENGTH_SHORT).show();
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
					invalidate();
					return true;
				}
			} 
			// DASHBOARD FUNCTIONS
			int buttonId = dashboard.identifyButton((int)event.getX(), (int)event.getY());
			if (buttonId != -1) {
				if (buttonId == Dashboard.recordId) {
					if(soundManager!=null) {
						if (soundManager.recording) {
							soundManager.stopRecording();
							dashboard.stopRecording();
						} else {
							startRecording();
						}
					}
				} else if (buttonId == Dashboard.trashId) {
					Toast.makeText(getContext(),"Drag a sound onto the trashcan to delete it.",Toast.LENGTH_SHORT).show();
				}
			}
		} else if (event.getAction()==MotionEvent.ACTION_MOVE) {
			soundBeingMovedX = event.getX() - soundBeingMovedHandleX;
			soundBeingMovedY = event.getY() - soundBeingMovedHandleY;
			invalidate();
			return true;
		} else if (event.getAction()==MotionEvent.ACTION_UP && soundBeingMoved != null) {
			int buttonId = dashboard.identifyButton((int)event.getX(), (int)event.getY());
			if (buttonId == Dashboard.trashId) {
				soundManager.removeSound(soundBeingMoved.id);
				soundBeingMoved = null;
				invalidate();
				return true;
			}
		
			if (!addSoundAt(event.getX() - soundBeingMovedHandleX, event.getY() - soundBeingMovedHandleY, soundBeingMoved)
			 && !soundBeingMovedOldHome.add(soundBeingMoved))
				Log.e(TAG,"Could not replace a sound where it used to belong in an arrangement.");
			soundBeingMoved = null;
			invalidate();
			return true;
		}
		return false;
	}
	
	/**
	 * Notifies all relevant objects to start their sound recording activity
	 */
	public void startRecording() {
		soundManager.recordNew(bufferDuration, mySac,new MappedSynth1AY1());
		dashboard.startRecording(bufferDuration,mRedrawHandler);
	}

	private class UpdateArrangerCallback implements SoundManager.SoundAddedCallback {
		private Arranger parent;
		public UpdateArrangerCallback(Arranger parent) {this.parent = parent;}
		public void whenSoundAdded(PlayingSound thisSynth) {
			
			for (int rowNum = 0; rowNum<arrangement.rows.size(); ++rowNum) {
				// @HACK: what if we want to squeeze in more sounds?
				Arrangement.Row row = arrangement.rows.get(rowNum);
				if (row.isEmpty()) {
					row.add (new Sound (thisSynth, 0, 1));
					break;
				}
				
			}
			if (parent != null) parent.mRedrawHandler.sleep(1);
		}
		
	}
	
	protected SoundManager.SoundAddedCallback mySac = new UpdateArrangerCallback( this ) ;
	
	/** 
	 * Push a sound (back) into the world of rows
	 * @param x
	 * @param y
	 * @return true on success
	 */
	private boolean addSoundAt (float x, float y, Sound s) {
		Log.d(TAG,"Adding sound at "+x+","+y);
		int rowNum = (int) (y / rowHeight);
		if (arrangement.rows.size() <= rowNum) return false;
		Arrangement.Row row = arrangement.rows.get(rowNum);
		Sound updatedSound = new Sound (s.id,(int)(x/pixelsPerAudioTick),s.getLength());
		boolean couldAddSound = row.add (updatedSound);
		if (couldAddSound) {
			soundManager.setSoundStart(updatedSound.id,(int)(x/pixelsPerAudioTick));
		}
		return couldAddSound;
	}
	
}
