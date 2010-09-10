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
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * Draws buttons and handles their clicks.  Buttons are given IDs 
 * from 0 to (number of buttons - 1) to identify them with.  This
 * is used particularly by Arranger to determine when a sound needs 
 * trashing.
 * 
 * @author alex shaw
 *
 */
public class Dashboard extends Drawable {

	private int buttonHeight = 50;
	public static final int trashId = 0, recordId = 1;
	protected int height; // Needed for locating buttons that have been drawn
	private Arranger.RefreshHandler refreshHandler;
	
	protected boolean isRecording = false;
	public Paint buttonPaint;
	public Bitmap[] buttonImages = new Bitmap[2]; // The currently-visible buttons
	private Bitmap waitButton, recButton; // buttonImages 
	
	public Dashboard(Context c, RefreshHandler refreshHandler) {	

		buttonPaint = new Paint(); // used only on bitmaps, doesn't do very much
		buttonPaint.setAntiAlias(false);
		
		buttonImages[trashId]  = BitmapFactory.decodeResource(c.getResources(), R.drawable.bin);
		buttonImages[recordId] = BitmapFactory.decodeResource(c.getResources(), R.drawable.rec);
		recButton = buttonImages[recordId];
		waitButton = BitmapFactory.decodeResource(c.getResources(), R.drawable.wait);

		buttonHeight     = buttonImages[0].getHeight();
		this.refreshHandler = refreshHandler;
		
	}

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
			isRecording = true;
			buttonImages[recordId] = waitButton;
			levels = sound.intamps;
			refreshHandler.trigger();
		}
		
		@Override
		public void recordEnd() {
			isRecording = false;
			buttonImages[recordId] = recButton;
			refreshHandler.trigger();
		}
	};
	
	@Override
	public void draw(Canvas canvas) {
		height = canvas.getHeight();
		if (isRecording) {
			int progressMiddle = 2* height / 5;
			SoundView.drawWave(canvas, recordMonitor.levels, progressMiddle, 100);
		}
		for (int i=0; i<buttonImages.length; ++i)
			canvas.drawBitmap(buttonImages[i],null, locateButton(i),buttonPaint);
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
	
	@Override
	public int getOpacity() { return 0; }
	@Override
	public void setAlpha(int alpha) {}
	@Override
	public void setColorFilter(ColorFilter cf) {}

	public void stopRecording() { 
		isRecording= false; 
		buttonImages[recordId] = recButton;
	}
}
