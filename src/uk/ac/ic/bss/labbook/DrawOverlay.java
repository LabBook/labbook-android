package uk.ac.ic.bss.labbook;

import java.io.FileOutputStream;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class DrawOverlay extends View {

//	private static final float MINP = 0.25F;
//	private static final float MAXP = 0.75F;
	public Bitmap mBitmap = null; //, oldBitmap = null;
	private Canvas mCanvas;
	private Path mPath;
	private Paint mBitmapPaint;
	private Paint mPaint;
	public boolean candraw = false, havebmp = false, erase = false, showalert = false;
	private float mX;
	private float mY;
//	private static final float TOUCH_TOLERANCE = 4F;
	// private EditText etext;
	// private CustomScrollView myscroll;
	private int initialheight = 100000, initialwidth = 100000;
	public boolean canexpand = true, updated = false;

	//private float pensize = 4F;
	//private float erasersize = 30F;

	public DrawOverlay(Context context) {
		super(context);
		init();
	}

	private void init() {
		mPath = new Path();
		mBitmapPaint = new Paint(4);
		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setDither(true);
		mPaint.setColor(Color.RED);
		mPaint.setStyle(android.graphics.Paint.Style.STROKE);
		mPaint.setStrokeJoin(android.graphics.Paint.Join.ROUND);
		mPaint.setStrokeCap(android.graphics.Paint.Cap.ROUND);
		mPaint.setStrokeWidth(NotesList.pensize); //6F); //(12F);
	}

	public void setColor(int color) {
		mPaint.setColor(color);
	}

	public boolean canExpand() {
		return canexpand;
	}

	public void setEditText(int w, int h) { // EditText et, CustomScrollView scroll, 
		//etext = et;
		//myscroll = scroll;
		initialwidth = w;
		initialheight = h;

		mBitmap = Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888);
		mCanvas = new Canvas(mBitmap);
		havebmp = true;
	}

	public void saveBitmap(String file) {
		try {
			FileOutputStream out = new FileOutputStream(file); // book_version
			mBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
			out.close();
			updated = false;
		} catch (Exception e) {
			e.printStackTrace();
			Log.i("FILE BMP WRITE ERROR", e.toString());
		}
	}

	public void setPenSize() {
		//pensize = psize;
		if(!erase)
			mPaint.setStrokeWidth(NotesList.pensize);
	}

	public void setEraserSize() {
		//erasersize = esize;
		if(erase)
			mPaint.setStrokeWidth(NotesList.erasersize);
	}

	/* protected void setBitmap(Bitmap bmp){
       	mBitmap = null;
    	try{
    		mBitmap = bmp.copy(Bitmap.Config.ARGB_8888, true);
    		havebmp = true;
    	}
    	catch(NullPointerException npe){}
    	//mBitmap = bmp;
    } */

	protected void setBitmap(String file) {

		if(mBitmap != null)
			mBitmap.recycle();
		mBitmap = null;
		try{
			//Options options = new Options();
			//options.inSampleSize = 2;

			//try {
			mBitmap = BitmapFactory.decodeFile(file); //Stream(new FileInputStream(file), null, options);
			havebmp = true;
			//} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			//	e.printStackTrace();
			//havebmp = false;
			//} //  decodeFile(file).copy(Bitmap.Config.ARGB_4444, true);
			Bitmap oldbitmap = mBitmap;

			mBitmap = Bitmap.createBitmap(initialwidth, initialheight, android.graphics.Bitmap.Config.ARGB_8888);
			mCanvas = new Canvas(mBitmap);
			mCanvas.drawBitmap(oldbitmap, 0, 0, mPaint);	 // oldbitmap
		}
		catch(NullPointerException npe){}
	}

	protected void erase() {
		mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
		mPaint.setStrokeWidth(NotesList.erasersize); //12F);
		erase = true;
	}

	protected void setDrawing() {
		mPaint.setXfermode(null);
		mPaint.setAlpha(0xFF);
		mPaint.setStrokeWidth(NotesList.pensize); //6F); //(12F);
		erase = false;
	}

	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		Log.i("SIZE CHANGE", "OLD "+oldw+" "+oldh+" NEW "+w+ " "+h+" INITIAL "+initialheight);

		// On new page old height will be greater than the new, resized, height so need to allow
		// for that and prevent new page alert from showing
		/*if(oldh > initialheight && h > oldh){
    		// Only want to show message once
    		if(canexpand){
    			showalert = true;
    			//showAlert("End of Page", "End of page reached - a new page should be created");
    		}
    		else
    			showalert = false;
    		canexpand = false;
    		return;
    	}

    	canexpand = true; */

		super.onSizeChanged(w, h, oldw, oldh);
		//if(oldw <= 0)
		//    return; //w = 1;
		//if(oldh <= 0)
		//    return; //h = 1;
		if(w <= 0)
			w = 1;
		if(h <= 0)
			h = 1;


		Bitmap oldbitmap = mBitmap;

		//BitmapFactory.Options options = new BitmapFactory.Options(); 
		//options.inPurgeable = true;

		//mBitmap = Bitmap.createScaledBitmap(mBitmap, w, h, false);
		//mBitmap.recycle();
		mBitmap = Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888);

		mCanvas = new Canvas(mBitmap);
		//mBitmap.recycle();

		if(havebmp){
			mCanvas.drawBitmap(oldbitmap, 0, 0, mPaint);	 // oldbitmap
		}
		try{
			oldbitmap.recycle();
			oldbitmap = null;
		}
		catch(NullPointerException npe){} 

		//if(h > initialheight)
		//	showAlert("End of Page", "End of page reached - a new page should be created");
	}

	public void showAlert(String title, String result) {
		new AlertDialog.Builder(getContext())
		.setTitle(title)
		.setMessage(result)
		.setNegativeButton("OK", null).show();	
	}

	public void resetBitmap() {
		// Need to create the canvas again to resize it correctly - make it smaller
		mBitmap = Bitmap.createBitmap(initialwidth, initialheight, android.graphics.Bitmap.Config.ARGB_8888);

		mCanvas = new Canvas(mBitmap);
		//mCanvas = new Canvas(Bitmap.createBitmap(initialwidth, initialheight, android.graphics.Bitmap.Config.ARGB_8888));
		//mCanvas.drawBitmap(Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888), 0, 0, mPaint);
		//mBitmap.recycle();
		//mBitmap = null;
		canexpand = true;
		showalert = false;
		candraw = false;
	}

	//decodes image and scales it to reduce memory consumption
	/*  private Bitmap decodeFile(File f){
        try {
            //Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f),null,o);

            //The new size we want to scale to
            final int REQUIRED_SIZE=70;

            //Find the correct scale value. It should be the power of 2.
            int scale=1;
            while(o.outWidth/scale/2>=REQUIRED_SIZE && o.outHeight/scale/2>=REQUIRED_SIZE)
                scale*=2;

            //Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize=scale;
            return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
        } catch (FileNotFoundException e) {}
        return null;
    } */


	protected void onDraw(Canvas canvas) {
		try {
			canvas.drawBitmap(mBitmap, 0.0F, 0.0F, mBitmapPaint);
			canvas.drawPath(mPath, mPaint);
		} catch(NullPointerException npe) {
		}
	}

	public boolean onTouchEvent(MotionEvent event) {
		if (!candraw) {
			return false;
		}
		float x = event.getX();
		float y = event.getY();
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mPath.reset();
			mPath.moveTo(x, y);
			mX = x;
			mY = y;
			invalidate();
			break;
		case MotionEvent.ACTION_MOVE:
			float dx = Math.abs(x - mX);
			float dy = Math.abs(y - mY);
			if (dx >= 4F || dy >= 4F) {
				updated = true;
				mPath.quadTo(mX, mY, (x + mX) / 2.0F, (y + mY) / 2.0F);
				mX = x;
				mY = y;
			}
			invalidate();
			break;
		case MotionEvent.ACTION_UP:
			mPath.lineTo(mX, mY);
			mCanvas.drawPath(mPath, mPaint);
			mPath.reset();
			invalidate();
			break;
		}
		return true;
	}

}
