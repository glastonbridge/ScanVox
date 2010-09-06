package org.isophonics.scanvox;

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

	private int buttonHeight = 50, buttonWidth = 50;
	public static final int trashId = 0, recordId = 1;;
	protected int height,width;
	public long recordTime = -1;
	private long recordStarted = -1;
	private Arranger.RefreshHandler refreshHandler;
	protected boolean isRecording = false;
	public Paint buttonPaint;
	public Bitmap[] buttonImages = new Bitmap[2]; // The currently-visible buttons
	private Bitmap stopButton, recButton; // buttonImages 
	
	public Dashboard(Context c) {	

		buttonPaint = new Paint(); // used only on bitmaps, doesn't do very much
		buttonPaint.setAntiAlias(false);
		
		buttonImages[trashId]  = BitmapFactory.decodeResource(c.getResources(), R.drawable.bin);
		buttonImages[recordId] = BitmapFactory.decodeResource(c.getResources(), R.drawable.rec_sq);
		recButton = buttonImages[recordId];
		stopButton = BitmapFactory.decodeResource(c.getResources(), R.drawable.stop);

		buttonHeight     = buttonImages[0].getHeight();
		buttonWidth      = buttonImages[0].getWidth();		
	}

	@Override
	public void draw(Canvas canvas) {
		height = canvas.getHeight();
		if (isRecording) {
			long amountRecorded = System.currentTimeMillis()-recordStarted;
			if (amountRecorded>recordTime) stopRecording();
			Paint recordPaint = new Paint();
			recordPaint.setColor(0xFF440000);
			canvas.drawRect(0,(float) (height-2.5*buttonHeight),canvas.getWidth()*amountRecorded/recordTime, height - 2* buttonHeight, recordPaint);
			refreshHandler.sleep(50);
		}
		for (int i=0; i<buttonImages.length; ++i)
			canvas.drawBitmap(buttonImages[i],null, locateButton(i),buttonPaint);
	}
	
	/**
	 * Trigger the "recording" progress bar
	 * 
	 * @param duration
	 */
	public void startRecording(long duration, Arranger.RefreshHandler refreshHandler) {
		recordTime = duration;
		recordStarted = System.currentTimeMillis();
		isRecording = true;
		buttonImages[recordId] = stopButton;
		this.refreshHandler = refreshHandler;
		refreshHandler.sleep(50);
	}

	/**
	 * Locate a button on the button bar at the bottom of the screen
	 */
	private Rect locateButton(int id) {
		Rect result = new Rect();
		result.bottom = height-buttonHeight;
		result.top = height - 2*buttonHeight;
		result.left = id * buttonWidth;
		result.right = (id + 1) * buttonWidth;
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
			int possibleId = x / buttonWidth; 
			if (possibleId < buttonImages.length) return possibleId; // match found
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
