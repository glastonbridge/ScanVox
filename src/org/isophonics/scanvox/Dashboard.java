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

import org.isophonics.scanvox.Arranger.RefreshHandler;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Debug;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

/**
 * Draws buttons and handles their clicks.  Buttons are given IDs 
 * from 0 to (number of buttons - 1) to identify them with.  This
 * is used particularly by Arranger to determine when a sound needs 
 * trashing.
 * 
 * @TODO: would this class be better as a View?  We're making more
 * use of the measure/layout model now
 * 
 * @author alex shaw
 *
 */
public class Dashboard extends View {

	public Dashboard(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	private int buttonHeight = 50;
	public static final int trashId = 0, recordId = 1, synthId = 2;
	private static final String TAG = "Dashboard";
	private static final float SCALE_SMALL = 0.7f;
	protected int height; // Needed for locating buttons that have been drawn
	private Arranger.RefreshHandler refreshHandler;
	private ListView synthPalette;
	private MappedSynth[] synthList;
	protected boolean isRecording = false;
	public Paint buttonPaint;
	public Bitmap[] buttonImages; // The currently-visible buttons
	//private Bitmap waitButton, recButton; // buttonImages 
	public Bitmap[] buttonImagesSmall = new Bitmap[3];
	public Bitmap[] buttonImagesLarge = new Bitmap[3];
	
	// @TODO: this is just a stop-gap while i transfer dashboard-related stuff here
	private Arranger parent;
	
	public void init(Arranger parent, RefreshHandler refreshHandler, MappedSynth[] synthList) {	

		this.parent = parent;
		buttonPaint = new Paint(); // used only on bitmaps, doesn't do very much
		buttonPaint.setAntiAlias(false);
		
		buttonImages = buttonImagesLarge;
		
		buttonImages[trashId]  = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.bin);
		buttonImages[recordId] = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.rec);
		buttonImages[synthId]  = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.wave);
		//recButton = buttonImages[recordId];
		//waitButton = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.wait);

		Matrix shrink = new Matrix();
		shrink.postScale(SCALE_SMALL, SCALE_SMALL);
		
		for (int i = 0; i< buttonImages.length; ++i)
			buttonImagesSmall[i] = Bitmap.createBitmap(
				buttonImages[i], 0, 0, 
				buttonImages[i].getWidth(), buttonImages[i].getHeight(),
				shrink, true);
		
		buttonHeight     = buttonImages[0].getHeight();
		this.refreshHandler = refreshHandler;
		
		this.synthList = synthList;
	}
	
	/**
	 * Construct a listview for showing synths in a palette
	 */
	public void makeSynthList(ListView synthPalette) {
		this.synthPalette = synthPalette;
		synthPalette.setBackgroundColor(0xCCFFFFFF);
		synthPalette.setAdapter(new SynthPalette(
				getContext(),
				android.R.layout.simple_list_item_1,
				synthList));
		synthPalette.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				showSynthPalette(false);
				if (toDoAfterSynthPalette != null) {
					toDoAfterSynthPalette.selected(arg2);
					toDoAfterSynthPalette = null;
				} else {
					draggingPaint = (TextView) arg1;
					Toast.makeText(getContext(), "Tap a sound to change its synth", Toast.LENGTH_SHORT).show();
				}
			}
		});
		synthPalette.setClickable(true);
	}
	
	TextView draggingPaint;
	
	/**
	 * lay out the synth palette
	 */
	public void onLayout(boolean changed, int l,int t,int r,int b) {
		super.onLayout(changed, l, t, r, b);
		buttonImages = ( r-l < 400 )?buttonImagesSmall:buttonImagesLarge;
	}
	
	/**
	 * Toggle synth list visibility
	 */

	/**
	 * Update the graphics during a recording session
	 */
	protected RecordMonitor recordMonitor = new RecordMonitor();

	private class RecordMonitor implements SoundManager.RecordListener {
		int[] levels;
		
		@Override
		public void recordUpdate() {
			refreshHandler.trigger();
		}
		
		@Override
		public void recordStart(PlayingSound sound) {
			//Debug.startMethodTracing("recording");
			isRecording = true;
			//buttonImages[recordId] = waitButton;
			levels = sound.intamps;
			refreshHandler.trigger();
		}
		
		@Override
		public void recordEnd() {
			isRecording = false;
			//buttonImages[recordId] = recButton;
			refreshHandler.trigger();
			//Debug.startMethodTracing("playing");
			//Debug.stopMethodTracing();
		}
	};
	
	@Override
	public void onDraw(Canvas canvas) {
		height = canvas.getHeight();
		if (isRecording) {
			int progressMiddle = 2* height / 5;
			SoundView.drawWave(canvas, recordMonitor.levels, progressMiddle, 100);
			return;
		}
		for (int i=0; i<buttonImages.length; ++i) {
			Thread.yield();
			canvas.drawBitmap(buttonImages[i],null, locateButton(i),buttonPaint);
		}
		if (getSynthPaletteVisibility()) {
			Log.d(TAG,String.format("palette %d,%d,%d,%d",
					synthPalette.getLeft(),
					synthPalette.getTop(),
					synthPalette.getWidth(),
					synthPalette.getHeight()));
			canvas.clipRect(
					synthPalette.getLeft(), 
					synthPalette.getTop(), 
					synthPalette.getRight(),
					synthPalette.getBottom());
			canvas.translate(
					synthPalette.getLeft(), 
					synthPalette.getTop());
			synthPalette.draw(canvas);
		}
	}
		
	/**
	 * Locate a button on the button bar at the bottom of the screen.
	 * 
	 * Note that we assume all buttons are the same height.
	 */
	private Rect locateButton(int id) {
		Rect result = new Rect();
		int buttonLeft = 0;
		for (int i = 0; i<id; ++i) buttonLeft += buttonImages[i].getWidth();
		int buttonRight = buttonLeft + buttonImages[id].getWidth();
		result.bottom = height-buttonHeight;
		result.top = height - 2*buttonHeight;
		result.left = buttonLeft;
		result.right = buttonRight;
		return result;
	}
	/**
	 * Determine a button ID from a coordinate.  Loosely speaking the inverse
	 * of locateButton
	 * 
	 * @return an ID, or -1 if none found
	 */
	public int identifyButton(int x, int y) {
		if (y<height-buttonHeight && y > height - 2*buttonHeight) {
			int left = 0;
			int right = 0;
			int id = 0;
			for (Bitmap button : buttonImages) {
				if (x<left) return -1;
				right += button.getWidth();
				if (x<right) return id;
				++id;
				left = right;
			}
		}
		return -1;
	}

	public void stopRecording() { 
		isRecording= false; 
		//buttonImages[recordId] = recButton;
	}
	
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			int buttonId = identifyButton((int)event.getX(), (int)event.getY());
			if (buttonId != -1) {
				if (buttonId == Dashboard.recordId) {
					if(parent.soundManager!=null) {
						if (parent.soundManager.recording) {
							parent.soundManager.stopRecording();
							stopRecording();
						} else {
							parent.startRecording();
							return true;
						}
					}
				} else if (buttonId == Dashboard.trashId) {
					Toast.makeText(getContext(),"Drag a sound onto the trashcan to delete it.",Toast.LENGTH_SHORT).show();
					return true;
				} else if (buttonId == Dashboard.synthId) {
					showSynthPalette(! getSynthPaletteVisibility());
					return true;
				}
			}
		} else if (event.getAction()==MotionEvent.ACTION_UP ) {
			if (parent.draggingSoundView != null) {
				int buttonId = identifyButton((int)event.getX(), (int)event.getY());
				if (buttonId == Dashboard.trashId) {
					parent.soundManager.removeSound(parent.draggingSoundView.sound.id);
					parent.draggingSoundView = null;
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Switch the synth palette visibility
	 * 
	 * @param synthPaletteOpen true to show, false to hide
	 */
	public void showSynthPalette(boolean synthPaletteOpen) {
		synthPalette.setVisibility(
				synthPaletteOpen?View.VISIBLE:View.INVISIBLE
		);
		synthPalette.setClickable(synthPaletteOpen);
	}

	private boolean getSynthPaletteVisibility() {
		return synthPalette.getVisibility() == View.VISIBLE;
	}

	// TODO: this is nasty, refactor
	private SynthPalette.Handler toDoAfterSynthPalette;
	public void onSynthPaletteSelected(SynthPalette.Handler handler) {
		toDoAfterSynthPalette = handler;
	}
}
