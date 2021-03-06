package ij.plugin;
import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.measure.Calibration;

/** This plugin animates stacks. */
public class Animator implements PlugIn {

	private static double animationRate = Prefs.getDouble(Prefs.FPS, 7.0);
	private static int firstFrame=0, lastFrame=0;
	private ImagePlus imp;
	private StackWindow swin;
	private int slice;
	private int nSlices;	
	/** Set 'arg' to "set" to display a dialog that allows the user to specify the
		animation speed. Set it to "start" to start animating the current stack.
		Set it to "stop" to stop animation. Set it to "next" or "previous"
		to stop any animation and display the next or previous frame. 
	*/
	public void run(String arg) {
		imp = WindowManager.getCurrentImage();
		if (imp==null)
			{IJ.noImage(); return;}
    	nSlices = imp.getStackSize();
		if (nSlices<2)
			{IJ.error("Stack required."); return;}
		ImageWindow win = imp.getWindow();
		if (win==null || !(win instanceof StackWindow))
			return;
		swin = (StackWindow)win;
		ImageStack stack = imp.getStack();
		slice = imp.getCurrentSlice();
		IJ.register(Animator.class);
		
		if (arg.equals("options")) {
			doOptions();
			return;
		}
			
		if (arg.equals("start")) {
			startAnimation();
			return;
		}

		if (swin.running2) // "stop", "next" and "previous" all stop animation
			stopAnimation();

		if (arg.equals("stop")) {
			return;
		}
			
		if (arg.equals("next")) {
			nextSlice();
			return;
		}
		
		if (arg.equals("previous")) {
			previousSlice();
			return;
		}
		
		if (arg.equals("set")) {
			setSlice();
			return;
		}
	}

	void stopAnimation() {
		swin.running2 = false;
		IJ.wait(500+(int)(1000.0/animationRate));
		imp.unlock(); 
	}

	void startAnimation() {
		int first=firstFrame, last=lastFrame;
		if (first<1 || first>nSlices || last<1 || last>nSlices)
			{first=1; last=nSlices;}
		if (swin.running2)
			{stopAnimation(); return;}
		imp.unlock(); // so users can adjust brightness/contrast/threshold
		swin.running2 = true;
		long time, nextTime=System.currentTimeMillis();
		Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
		int sliceIncrement = 1;
		Calibration cal = imp.getCalibration();
		if (cal.fps!=0.0)
			animationRate = cal.fps;
		while (swin.running2) {
			time = System.currentTimeMillis();
			if (time<nextTime)
				IJ.wait((int)(nextTime-time));
			else
				Thread.yield();
			nextTime += (long)(1000.0/animationRate);
			slice += sliceIncrement;
			if (slice<first) {
				slice = first+1;
				sliceIncrement = 1;
			}
			if (slice>last) {
				if (cal.loop) {
					slice = last-1;
					sliceIncrement = -1;
				} else {
					slice = first;
					sliceIncrement = 1;
				}
			}
			swin.showSlice(slice);
		}
	}

	void doOptions() {
		if (firstFrame<1 || firstFrame>nSlices || lastFrame<1 || lastFrame>nSlices) {
			firstFrame = 1;
			lastFrame = nSlices;
		}
		boolean start = !swin.running2;
		Calibration cal = imp.getCalibration();
		if (cal.fps!=0.0)
			animationRate = cal.fps;
		else if (cal.frameInterval!=0.0 && cal.getTimeUnit().equals("sec"))
			animationRate = 1.0/cal.frameInterval;
		int decimalPlaces = (int)animationRate==animationRate?0:1;
		GenericDialog gd = new GenericDialog("Animation Options");
		gd.addNumericField("Speed (0.1-100 fps):", animationRate, decimalPlaces);
		gd.addNumericField("First Frame:", firstFrame, 0);
		gd.addNumericField("Last Frame:", lastFrame, 0);
		gd.addCheckbox("Loop Back and Forth", cal.loop);
		gd.addCheckbox("Start Animation", start);
		gd.showDialog();
		if (gd.wasCanceled()) {
			if (firstFrame==1 && lastFrame==nSlices)
				{firstFrame=0; lastFrame=0;}
			return;
		}
		double speed = gd.getNextNumber();
		firstFrame = (int)gd.getNextNumber();
		lastFrame = (int)gd.getNextNumber();
		if (firstFrame==1 && lastFrame==nSlices)
			{firstFrame=0; lastFrame=0;}
		cal.loop = gd.getNextBoolean();
		start = gd.getNextBoolean();
		if (speed>100.0) speed = 100.0;
		if (speed<0.1) speed = 0.1;
		animationRate = speed;
		if (animationRate!=0.0)
			cal.fps = animationRate;
		if (start && !swin.running2)
			startAnimation();
	}
	
	void nextSlice() {
		if (!imp.lock())
			return;
		if (IJ.altKeyDown())
			slice += 10;
		else
			slice++;
		if (slice>nSlices)
			slice = nSlices;
		swin.showSlice(slice);
		imp.updateStatusbarValue();
		imp.unlock();
	}
	
	void previousSlice() {
		if (!imp.lock())
			return;
		if (IJ.altKeyDown())
			slice -= 10;
		else
			slice--;
		if (slice<1)
			slice = 1;
		swin.showSlice(slice);
		imp.updateStatusbarValue();
		imp.unlock();
	}

	void setSlice() {
        GenericDialog gd = new GenericDialog("Set Slice");
        gd.addNumericField("Slice Number (1-"+nSlices+"):", slice, 0);
        gd.showDialog();
        if (!gd.wasCanceled())
        	imp.setSlice((int)gd.getNextNumber());
	}

	/** Returns the current animation speed in frames per second. */
	public static double getFrameRate() {
		return animationRate;
	}

}

