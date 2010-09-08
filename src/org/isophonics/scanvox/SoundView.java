package org.isophonics.scanvox;

import org.isophonics.scanvox.Arrangement.Sound;
import org.isophonics.scanvox.Arranger.GridDimensions;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Bitmap.Config;
import android.graphics.Paint.Style;
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
	private static class BitmapStore {
		public Bitmap soundFrontLeft;
		public Bitmap soundFrontMiddle;
		public Bitmap soundFrontRight;
		public int bevelWidth=0;
		public Paint soundPaint = new Paint();
	}
	private BitmapStore bitmaps = null;
	
	/**
	 * Do this once per scanvox process, provides standard images for drawing sounds
	 */
	private void initBitmapTable() {
		bitmaps = new BitmapStore();
		bitmaps.soundFrontLeft   = BitmapFactory.decodeResource(getResources(), R.drawable.chunk_front_left);
		bitmaps.soundFrontMiddle = BitmapFactory.decodeResource(getResources(), R.drawable.chunk_front_mid);
		bitmaps.soundFrontRight  = BitmapFactory.decodeResource(getResources(), R.drawable.chunk_front_right);
		bitmaps.bevelWidth       = bitmaps.soundFrontLeft.getWidth();
	}
	
	/**
	 * This should usually be fancier (see docs), but as we're only called from Arranger it's all that's required
	 */
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension((int)gridDimensions.x*sound.getLength(),(int)gridDimensions.y);
	}
		
	/**
	 * Render the sound to a bitmap in advance, so there's only one
	 * performance hit
	 */
	private void render() {
		if (bitmaps == null) initBitmapTable();
		int height = gridDimensions.y;
		int width  = sound.getLength()*gridDimensions.x; 
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
		inner.left = bitmaps.bevelWidth;
		inner.right = width - bitmaps.bevelWidth;

		float roundness = bitmaps.bevelWidth;
		c.drawRoundRect(outer, roundness, roundness, backgroundPaint);
		c.drawBitmap(bitmaps.soundFrontLeft  , 0         , 0   , bitmaps.soundPaint);
		c.drawBitmap(bitmaps.soundFrontMiddle, null      , inner, bitmaps.soundPaint);
		c.drawBitmap(bitmaps.soundFrontRight , inner.right, 0   , bitmaps.soundPaint);		
	}
	
	public int getButtonHeight() {
		if (bitmaps == null) initBitmapTable();
		return bitmaps.soundFrontLeft.getHeight();
	}
	
	/**
	 * This is where the real drawing goes on
	 */
	@Override
	public void onDraw(Canvas c) {
		c.drawBitmap(internalRepresentation,getLeft(),getTop(),bitmaps.soundPaint);
	}
}
