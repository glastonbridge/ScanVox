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

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import net.sf.supercollider.android.OscMessage;

import org.isophonics.scanvox.Arrangement.Sound;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Bitmap;
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
	private static final int CURSOR_COLOUR = 0x22444444;
	
	/** Paint objects for styling various parts of the Arranger */
	public Paint 
		backgroundPaint, 
		rowDivisionPaint, 
		timeDivisionPaint,
		cursorPaint,
		soundPaint;
	
	/** 
	 * Dimensions of the grid, for translating between pixel and sound values 
	 */
	public class GridDimensions {
		public int y;
		public int x = 16;
		public int verticalDivision = 4;
	}
	protected GridDimensions gridDimensions = new GridDimensions();
	private int height = 320; // will be updated later
	private Dashboard dashboard;
	SoundManager soundManager;
	
	protected Arrangement arrangement=new Arrangement(0);

	protected int cursorPos=0;
	
	public Arranger(Context c, AttributeSet s) {
		super(c,s);
		init();
	}
	
	public Arranger(Context c) {
		super(c);
		init();
	}
	
	public void setDashboard(Dashboard d) {
		dashboard = d;
		dashboard.init(this, refreshHandler, ScanVox.myMappedSynths);
	}
	
	@Override
	public void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		Thread.currentThread().setPriority(Thread.MIN_PRIORITY);  // Hopefully this will slow down the GUI a bit and give recording a chance
		gridDimensions.x = getWidth()/arrangement.length;
		height = getHeight();
		Enumeration<SoundView> soundViewEnumeration = soundViews.elements();
		while (soundViewEnumeration.hasMoreElements()) {
			soundViewEnumeration.nextElement().needsRefresh = true;
		}
	}
	
	private void init() {
		
		backgroundPaint = new Paint();
		timeDivisionPaint = new Paint();
		rowDivisionPaint = new Paint();
		cursorPaint = new Paint();
		cursorPaint.setColor(CURSOR_COLOUR);
		soundPaint = new Paint(); // used only on bitmaps, doesn't do very much
		soundPaint.setAntiAlias(false);
		
		Bitmap soundFrontLeft   = BitmapFactory.decodeResource(getResources(), R.drawable.chunk_front_left);
		gridDimensions.y        = soundFrontLeft.getHeight();
	}
	
	/**
	 * Set the arrangement to edit
	 * 
	 * @param a my arrangement
	 */
	public void setArrangement(Arrangement a) {
		arrangement = a;
		gridDimensions.x = getWidth()/arrangement.length;
	}
	
	/**
	 * Set the sound manager to notify
	 * 
	 */
	public void setSoundManager(SoundManager s) {
		soundManager = s;
	}
	
	private boolean registeredListener = false;
	/**
	 * Listen out for updates to the cursor position
	 * 
	 * @param messageMan
	 */
	public synchronized void listenToMessages(SCMessageManager messageMan) {
		if (!registeredListener) {
			registeredListener = true;
			messageMan.register(new SCMessageManager.OscListener() {
				@Override
				public void receive(OscMessage msgFromServer) {
					if(((Integer)msgFromServer.get(2)).intValue()==SoundManager.CLOCK_TRIGGER_UID) {
						cursorPos = (int)(((Float)msgFromServer.get(3))*16);
						refreshHandler.trigger();
					}
				}
				@Override
				public boolean equals(Object o) {
					return false;
				}
			},"/tr");
		}
	}
	
	SoundView draggingSoundView = null;
	private int	soundBeingMovedX, soundBeingMovedY;
	/** The distance between the top left corner and where you're actually touching */
	private int grabbedSoundHandleX, grabbedSoundHandleY; 
	
	private Arrangement.Row grabbedSoundOldHome;
	
	/**
	 * Refresh the user graphics
	 */
	@Override
	protected void onDraw(Canvas c) {
		if(soundManager.recording) return; // best chance for good quality
		height = c.getHeight();
		gridDimensions.x = c.getWidth()/arrangement.length;
		c.drawRect(0,0,c.getWidth(),height,backgroundPaint);
		drawGrid(c);
		int rowNum=0;
		for (Arrangement.Row row : arrangement.rows) drawRow(c,row,rowNum++); // crow!
		if (draggingSoundView != null) {
			placeSoundView(draggingSoundView, soundBeingMovedX, soundBeingMovedY);
			draggingSoundView.draw(c);
		}
		// cursor
		c.drawRect(
				cursorPos*gridDimensions.x,0,
				(cursorPos+1)*gridDimensions.x,
				height, cursorPaint);
	}
	
	/**
	 * Puts a SoundView at the specified x and y coordinates
	 * 
	 * @param soundView
	 * @param x
	 * @param y
	 */
	private void placeSoundView(SoundView soundView,int x, int y) {

		soundView.layout(
				x,
				y,
				x + soundView.getMeasuredWidth(),
				y + soundView.getMeasuredHeight());		
	}

	/**
	 * Set up the size of the 
	 */
	
	/**
	 * Draw a visual mesh for the user sounds to fit in
	 * @param c The canvas to draw to
	 */
	private void drawGrid (Canvas c) {
		int beyondLastRow = (int)gridDimensions.y*arrangement.rows.size() + 1;
		int width = c.getWidth();
		int height = c.getHeight();
		int blockDivision = (int) gridDimensions.x * gridDimensions.verticalDivision;
		for ( int i = (int)gridDimensions.y ; i < beyondLastRow ; i += (int) gridDimensions.y) 
			c.drawLine(0, i, width, i, rowDivisionPaint);
		if (blockDivision >0) for ( int i = blockDivision ; i < width ; i += blockDivision)
			c.drawLine(i, 0, i, height, timeDivisionPaint);
	}

	private Hashtable<Sound,SoundView>soundViews = new Hashtable<Sound,SoundView>();
	private void drawRow (Canvas c, Arrangement.Row r, int rowNum) {
		for (Sound s : r) {
			Thread.yield();
			int leftIndex  = (int)s.getStartTime()*gridDimensions.x;
			int topIndex   = rowNum*gridDimensions.y;
			SoundView thisSoundView = null;
			if (soundViews.containsKey(s)) {
				thisSoundView = ((SoundView)soundViews.get(s));
			} else {
				thisSoundView = new SoundView(getContext(), s, gridDimensions);
				soundViews.put(s, thisSoundView);
			}
			if (thisSoundView.needsRefresh) { 
				// measuring is done here, as sounds are added in an ad-hoc way
				thisSoundView.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
				thisSoundView.render(gridDimensions);
				thisSoundView.needsRefresh = false;
			}
			placeSoundView(thisSoundView,leftIndex,topIndex);
			thisSoundView.draw(c);
		}
	}
	
    private RefreshHandler refreshHandler = new RefreshHandler();

    class RefreshHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            Arranger.this.invalidate();
        }

        public void trigger() {
            sendEmptyMessage(0);
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
			if ( draggingSoundView != null ) {
				// Sanity check, we should never have an action down before a previous action up
				Log.e(TAG,"Arranger was asked to pick up a sound while it was already holding one.");
				Toast.makeText(getContext(), "I've got confused, and I might have mucked up your tune.", Toast.LENGTH_SHORT).show();
				draggingSoundView = null;
			} else {
				draggingSoundView = grabSoundAt(event.getX(),event.getY());
				if (draggingSoundView != null) return true;
			}
		} else if (event.getAction()==MotionEvent.ACTION_MOVE && dashboard.draggingPaint == null) {
			soundBeingMovedX = (int) (event.getX() - grabbedSoundHandleX);
			soundBeingMovedY = (int) (event.getY() - grabbedSoundHandleY);
			invalidate();
			return true;
		} else if (event.getAction()==MotionEvent.ACTION_UP) {
			if (dashboard.draggingPaint!=null) {
				applyPaintToCurrentSound();
			}
			if ( draggingSoundView != null ) {
				if ( Dashboard.trashId == dashboard.identifyButton((int)event.getX(), (int)event.getY()) ) {
					soundManager.removeSound(draggingSoundView.sound.id);
				} else if (!addSoundAt(event.getX() - grabbedSoundHandleX, event.getY() - grabbedSoundHandleY, draggingSoundView.sound)
				 && !grabbedSoundOldHome.add(draggingSoundView.sound))
					Log.e(TAG,"Could not replace a sound where it used to belong in an arrangement.");
				soundViews.remove(draggingSoundView.sound);
				draggingSoundView = null;
				invalidate();
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Add the currently-selected paint to the currently-selected sound
	 */
	private void applyPaintToCurrentSound() {
		MappedSynth newSynth = null;
		for (MappedSynth i : ScanVox.myMappedSynths) {
			if (i.getLabel().equals(dashboard.draggingPaint.getText().toString())) {
				newSynth = i;
				break;
			}
		}
		if (newSynth!=null) {
			try {
				PlayingSound mySound = draggingSoundView.sound.id;
				mySound.synth = newSynth;
				soundManager.removeSynth(mySound);
				soundManager.addSynth(mySound);
			} catch (IOException io) {
				io.printStackTrace();
			} 
		}
		dashboard.draggingPaint = null;
	}

	/**
	 * Grab a sound, based on its X and Y coordinates.  Remove it from
	 * the arrangement.
	 * 
	 * @param x
	 * @param y
	 * @return The SoundView related to that sound, or null if none
	 * were found
	 */
	private SoundView grabSoundAt(float x, float y) {
		int rowNum = (int)(y / gridDimensions.y);
		if (arrangement.rows.size()>rowNum) {
			Arrangement.Row row = arrangement.rows.get(rowNum);
			Sound soundBeingMoved = row.grabSoundAt(
					x/gridDimensions.x);
			if (soundBeingMoved != null) {
				draggingSoundView = soundViews.get(soundBeingMoved);
				grabbedSoundOldHome = row;
				grabbedSoundHandleX = (int) (x - draggingSoundView.getLeft());
				grabbedSoundHandleY = (int) (y - draggingSoundView.getTop());

				return draggingSoundView;
			}
		}
		return null;
	}

	/**
	 * Notifies all relevant objects to start their sound recording activity
	 */
	public void startRecording() {
		dashboard.showSynthPalette(true);
		dashboard.onSynthPaletteSelected(new SynthPalette.Handler() {
			
			@Override
			public void selected(int choice) {
				soundManager.recordNew(bufferDuration, mySac,ScanVox.myMappedSynths[choice],dashboard.recordMonitor);
			}
		});
		
	}

	private class UpdateArrangerCallback implements SoundManager.SoundAddedCallback {
		private Arranger parent;
		public UpdateArrangerCallback(Arranger parent) {this.parent = parent;}
		public void whenSoundAdded(PlayingSound thisSynth) {

			for (int rowNum = 0; rowNum<arrangement.rows.size(); ++rowNum) {
				// @HACK: what if we want to squeeze in more sounds?
				Arrangement.Row row = arrangement.rows.get(rowNum);
				if (row.isEmpty()) {
					int arrangedSize = (thisSynth.length*arrangement.bpm*arrangement.ticksPerBeat)/(60*1000);
					row.add (arrangement.new Sound (thisSynth, 16*thisSynth.phase, arrangedSize));
					break;
				}
			}
			if (parent != null) parent.refreshHandler.trigger();
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
		int rowNum = (int) (y / gridDimensions.y);
		if (arrangement.rows.size() <= rowNum || rowNum <0) return false;
		Arrangement.Row row = arrangement.rows.get(rowNum);
		Sound updatedSound = arrangement.new Sound (s.id,x/gridDimensions.x,s.getLength());
		boolean couldAddSound = row.add (updatedSound);
		if (couldAddSound) {
			soundManager.setSoundStart(updatedSound.id,x/(gridDimensions.x*arrangement.length));
		}
		return couldAddSound;
	}
	
}
