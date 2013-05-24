package kamkowarrier.collab.resistorreader;

import java.util.ArrayList;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

public class ResistorView extends View {
	
	// Range class for storing valid touch event ranges
	private class Range {
		float start;
		float end;
		
		public Range(float start, float end) {
			this.start = start;
			this.end = end;
		}
		
		public boolean isBetween(float val) {
			if ((val > start) && (val < end)) {
				return true;
			}
			return false;
		}
		
		public float middle() {
			return ((end - start) / 2) + start;
		}
	}
	
	// Initializing band class instances
	Context ctx = getContext();
	ColorBand ColB = new ColorBand(ctx);
	ColorBand.ValBand valB = ColB.new ValBand(ctx);
	ColorBand.TolBand tolB = ColB.new TolBand(ctx);
	ColorBand.MultBand mulB = ColB.new MultBand(ctx);
	ColorBand.TempBand temB = ColB.new TempBand(ctx);
	
	// Data variables
	int[][] bandScheme = {};
	ColorBand[] bandTypeArray = {};
	int activeBandNum;
	int[] bandColors = {};
	ArrayList<Range> eventRangeList = new ArrayList<Range>();
	
	// Variables and methods to set ColorSelectionAdapter and Calculator instances
	ColorSelectionAdapter selectAdap;
	Calculator calc;
	public void setSelector(ColorSelectionAdapter s) { this.selectAdap = s; }
	public void setCalc(Calculator c) { this.calc = c; }
	
	// Drawing variables
	final Resources res = getResources();
	Bitmap resistorOutline;
	Bitmap resistorMask;
	Bitmap drawBitmap;
	int canvasWidth;
	int canvasHeight;
	Paint paint;
	Canvas drawCanvas;
	float scale;
	
	// Arrow variables
	float viewTop = 0;
	float arrowHeight = 0;
	ImageView arrow;
	public void setArrow(ImageView v) { this.arrow = v; }
	public void setArrowVars(float top, float height) { 
		this.viewTop = top; 
		this.arrowHeight = height;
	}
	
	// Init method (includes a touch listener)
	public ResistorView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		this.setBandMode(4);
		this.bandColors = new int[] { bandScheme[0][3], bandScheme[1][5], bandScheme[2][6], bandScheme[3][0] };
		this.changeBand(0);
		
		this.resistorOutline = BitmapFactory.decodeResource(res, R.drawable.resistor_outline);
		this.resistorMask = BitmapFactory.decodeResource(res, R.drawable.resistor_mask);
		
		this.canvasWidth = resistorMask.getWidth();
		this.canvasHeight = resistorMask.getHeight();
		
		this.drawBitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888);
		this.paint = new Paint();
		paint.setFilterBitmap(false);
		paint.setAntiAlias(true);
		
		this.drawCanvas = new Canvas(drawBitmap);
		
		this.setOnTouchListener(new OnTouchListener () {
			public boolean onTouch(View v, MotionEvent e) {
				switch(e.getAction())
				{
				case MotionEvent.ACTION_DOWN:
					float pos = e.getY() * scale; // Adjust to new scale of View
					int i = 0;
					while (i < eventRangeList.size()) {
							if (eventRangeList.get(i).isBetween(pos)) {
								changeBand(i); // Updates "pointer" to resistor band on touch
								updateArrow();
								selectAdap.setActives(i);
								return true;
							}
							i++;
					}
				}
				return false;
			}
		});
	}
	
	// Method that sets the band mode for the ResistorView (4 bands, 5 bands, or 6 bands)
	public void setBandMode(int mode) {
		switch(mode) {
		case 4:
			bandScheme = new int[][] { valB.colors, valB.colors, mulB.colors, tolB.colors };
			bandTypeArray = new ColorBand[] { valB, valB, mulB, tolB };
			break;
		case 5:
			bandScheme = new int[][] { valB.colors, valB.colors, valB.colors, mulB.colors, tolB.colors };
			bandTypeArray = new ColorBand[] { valB, valB, valB, mulB, tolB };
			break;
		case 6:
			bandScheme = new int[][] { valB.colors, valB.colors, valB.colors, mulB.colors, tolB.colors, temB.colors };
			bandTypeArray = new ColorBand[] { valB, valB, valB, mulB, tolB, temB };
			break;
		}
		if (this.getHeight() != 0) {
			arrow.setVisibility(View.INVISIBLE);
			invalidate();
		}
	}
	
	private void drawBands() {
		
		int startY = -360;
		int len = bandScheme.length;
		int i = 0;
		eventRangeList.clear();
		Range current;
		
		while (i < len) {
			paint.setColor(bandColors[i]);
			current = new Range((canvasHeight / 2) + (startY + (i * ((600 / len) + (40 - (10 * (len - 4)))))),
					(canvasHeight / 2) + ((startY + (600 / len)) + (i * ((600 / len) + (40 - (10 * (len - 4)))))));
		    drawCanvas.drawRect(0, current.start, canvasWidth, current.end, paint);
		    eventRangeList.add(current);
		    i++;
		}
	}
	
	private Bitmap drawBitmap() {
		
		drawBands();
		drawCanvas.drawBitmap(resistorOutline, 0, 0, paint);
		paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
		
		drawCanvas.drawBitmap(resistorMask, 0, 0, paint);
		paint.setXfermode(null);
		
		scale = (float) canvasHeight / this.getHeight();
		Bitmap scaled = Bitmap.createScaledBitmap(drawBitmap, this.getWidth(), this.getHeight(), true);
		
		if (arrow.getVisibility() == View.INVISIBLE) {
			updateArrow();
		}
		
		return scaled;
	} 
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		Bitmap finalized = drawBitmap();
		canvas.drawBitmap(finalized, 0, 0, paint);
	}
	
	public void changeBand(int i) {
		this.activeBandNum = i;
	}
	
	public void updateArrow() {
		arrow.setVisibility(VISIBLE);
		
		System.out.println(viewTop);
		System.out.println(arrowHeight);
		
		arrow.setY((eventRangeList.get(activeBandNum).middle() / scale) - (viewTop + (arrowHeight / 2)));
	}
	
	public void updateActiveBand(int color){
		bandColors[activeBandNum] = color;
		calculate();
		invalidate();
	}
	
	public void updateWithoutCalc(int color) {
		bandColors[activeBandNum] = color;
		invalidate();
		//selectAdap.setActives(activeBandNum);
	}
	
	public void calculate() {
		calc.calculate(bandColors, bandTypeArray);
	}

}