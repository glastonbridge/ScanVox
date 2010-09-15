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

import org.isophonics.scanvox.Arrangement.Sound;
import org.isophonics.scanvox.Arranger.GridDimensions;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Bitmap.Config;
import android.graphics.Paint.Style;
import android.util.Log;
import android.view.View;

/**
 * A visual representation of a sound in a sequence
 * @author alex
 *
 */
public class SoundView extends View {
	protected Sound sound;
	protected Bitmap internalRepresentation;
	private GridDimensions gridDimensions;
	private Paint backgroundPaint;

	private static final int LEVEL_QUIET = 30;
	private static final int LEVEL_MID   = 70;
	private static final String TAG = "SoundView";
	
	public SoundView(Context context, Sound s,GridDimensions gd) {
		super(context);
		sound = s;
		gridDimensions = gd;
		backgroundPaint = new Paint();
		backgroundPaint.setAntiAlias(true);
		backgroundPaint.setStyle(Style.FILL);
		backgroundPaint.setColor(0xFFAAAA44); // Yellowy
		render();
	}
	private static class StaticDataStore {
		public Bitmap soundFrontLeft;
		public Bitmap soundFrontMiddle;
		public Bitmap soundFrontRight;
		public int bevelWidth=0;
		public Paint soundPaint = new Paint();
		Paint quietPaint = new Paint();
		Paint midPaint = new Paint();
		Paint loudPaint = new Paint();

		public StaticDataStore(Resources res) {
			soundFrontLeft   = BitmapFactory.decodeResource(res, R.drawable.chunk_front_left);
			soundFrontMiddle = BitmapFactory.decodeResource(res, R.drawable.chunk_front_mid);
			soundFrontRight  = BitmapFactory.decodeResource(res, R.drawable.chunk_front_right);
			bevelWidth       = soundFrontLeft.getWidth();
			quietPaint.setColor(0xFF008800);
			midPaint.setColor(0xFF888800);
			loudPaint.setColor(0xFF880000);			
			
			quietPaint.setAntiAlias(false);
			midPaint.setAntiAlias(false);
			loudPaint.setAntiAlias(false);
		}
	}
	private static StaticDataStore graphics = null;
	
	/**
	 * Do this once per scanvox process, provides standard images for drawing sounds
	 */
	public static void initDataStore(Resources res) {
		if (graphics == null) graphics = new StaticDataStore(res);
	}

	/**
	 * Draws a sound's "waveform" onto a canvas, around a given axis
	 * 
	 * @param c
	 * @param levels the levels recorded for each sample
	 * @param axis the position of the y axis
	 * @param the maximum size of the waveform 
	 */
	public static void drawWave(
			Canvas canvas, 
			int[] levels, 
			int axis ,
			int height) {
		// NB. will fail if initDataStore has not been called
		int height2 = height/2;
		float barWidth = canvas.getWidth() / levels.length;
		int progressLeft =0,progressRight = 0 ;
		Paint recordPaint;
		int barIterator = 0;
		for (int level : levels) {
			Thread.yield(); // if we're in recording mode, be gentle with processor time
			progressLeft = (int)(barWidth*barIterator);
			progressRight = (int)(barWidth*(barIterator+1));
			if      (level < LEVEL_QUIET ) recordPaint = graphics.quietPaint;
			else if (level < LEVEL_MID   ) recordPaint = graphics.midPaint;
			else                           recordPaint = graphics.loudPaint;
			int scaledLevel = height2*level/100;
			if (scaledLevel == 0) scaledLevel = 1;
			canvas.drawRect(
					progressLeft,
					axis- scaledLevel, 
					progressRight, 
					axis+ scaledLevel, 
					recordPaint);
			++barIterator;
		}		
	}

	/**
	 * This should usually be fancier (see docs), but as we're only called from Arranger it's all that's required
	 */
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension((int)(gridDimensions.x*sound.getLength()),(int)gridDimensions.y);
	}
		
	/**
	 * Render the sound to a bitmap in advance, so there's only one
	 * performance hit
	 */
	private void render() {
		if (graphics == null) initDataStore(getResources());
		int height = gridDimensions.y;
		int width  = (int)(sound.getLength()*gridDimensions.x);
		internalRepresentation = Bitmap.createBitmap(
				width, 
				height, 
				Config.ARGB_8888);
		
		Canvas c = new Canvas(internalRepresentation);
		RectF outer = new RectF();
		outer.top = 0;
		outer.left = 0;
		outer.bottom = height;
		outer.right = width;
		Rect inner = new Rect();
		inner.top = 0;
		inner.bottom = height;
		inner.left = graphics.bevelWidth;
		inner.right = width - graphics.bevelWidth;

		float roundness = graphics.bevelWidth;
		c.drawRoundRect(outer, roundness, roundness, backgroundPaint);
		drawWave(c, sound.id.intamps, height/2, height);
		c.drawBitmap(graphics.soundFrontLeft  , 0         , 0   , graphics.soundPaint);
		c.drawBitmap(graphics.soundFrontMiddle, null      , inner, graphics.soundPaint);
		c.drawBitmap(graphics.soundFrontRight , inner.right, 0   , graphics.soundPaint);		
	}
	
	public int getButtonHeight() {
		if (graphics == null) initDataStore(getResources());
		return graphics.soundFrontLeft.getHeight();
	}
	
	/**
	 * This is where the real drawing goes on
	 */
	@Override
	public void onDraw(Canvas c) {
		c.drawBitmap(internalRepresentation,getLeft(),getTop(),graphics.soundPaint);
	}
}
