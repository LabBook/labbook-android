package uk.ac.ic.bss.labbook;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

public class CustomScrollView extends ScrollView {

	private DrawOverlay drawOverlay;
	//	private GestureDetector gestureDetector;

	public CustomScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);

		//		gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
		//			@Override
		//			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		//				System.out.println("@@@onFling");
		//				return super.onFling(e1, e2, velocityX, velocityY);
		//			}
		//			@Override
		//			public boolean onDown(MotionEvent e) {
		//				System.out.println("@@@onDown");
		//				return super.onDown(e);
		//			}
		//		});
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		if (!drawOverlay.candraw || event.getPointerCount() > 1) {
			//gestureDetector.onTouchEvent(event);
			return onTouchEvent(event);
		} else if (event.getAction() == MotionEvent.ACTION_DOWN) {
			onTouchEvent(event);
		}
		return false;
	}

	public void setDoverlay(DrawOverlay doverlay) {
		this.drawOverlay = doverlay;
	}

}
