package ij.plugin.filter;
import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.measure.*;
import java.awt.*;

/** This plugin implements the Edit/Scale command. */
public class Scaler implements PlugInFilter {
    private ImagePlus imp;
    private static double xscale = 0.5;
    private static double yscale = 0.5;
    private static boolean newWindow = true;
    private static boolean interpolate = true;

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		IJ.register(Scaler.class);
		if (imp!=null) {
			Roi roi = imp.getRoi();
			if (roi!=null && roi.getType()>Roi.TRACED_ROI)
				imp.killRoi(); // ignore any line selection
		}
		return DOES_ALL;
	}

	public void run(ImageProcessor ip) {
		GenericDialog gd = new GenericDialog("Scale", IJ.getInstance());
		gd.addNumericField("X Scale (0.05-25):", xscale, 2);
		gd.addNumericField("Y Scale (0.05-25):", yscale, 2);
		gd.addCheckbox("Create New Window", newWindow);
		gd.addCheckbox("Interpolate", interpolate);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		xscale = gd.getNextNumber();
		yscale = gd.getNextNumber();
		if (gd.invalidNumber()) {
			IJ.error("X or Y scale are invalid.");
			return;
		}
		if (xscale > 25.0) xscale = 25.0;
		if (xscale < 0.05) xscale = 0.05;
		if (yscale > 25.0) yscale = 25.0;
		if (yscale < 0.05) yscale = 0.05;
		newWindow = gd.getNextBoolean();
		interpolate = gd.getNextBoolean();
		ip.setInterpolate(interpolate);
		imp.startTiming();
		try {
			if (newWindow) {
				Rectangle r = ip.getRoi();
				int newWidth = (int)(xscale*r.width);
				int newHeight = (int)(yscale*r.height);
				ImagePlus imp2 = imp.createImagePlus();
				imp2.setProcessor("Untitled", ip.resize(newWidth, newHeight));
				Calibration cal = imp2.getCalibration();
		    	if (cal.scaled()) {
		    		cal.pixelWidth *= 1.0/xscale;
		    		cal.pixelHeight *= 1.0/yscale;
		    	}
				imp2.show();
				imp.trimProcessor();
				imp2.trimProcessor();
			} else {
				int flags = IJ.setupDialog(imp, 0);
				if (flags==DONE)
					return;
				if ((flags&DOES_STACKS)!=0 && imp.getStackSize()>1) {
					Undo.reset();
	    			StackProcessor sp = new StackProcessor(imp.getStack(), ip);
	    			sp.scale(xscale, yscale);
	    		} else
	    			ip.scale(xscale, yscale);
	    		imp.killRoi();
			}
		}
		catch(OutOfMemoryError o) {
			IJ.outOfMemory("Scale");
		}
	}
	
}