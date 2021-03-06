package ij.plugin.filter;
import ij.*;
import ij.gui.*;
import ij.process.*;
import java.awt.*;

/** This plugin implements ImageJ's Process/Math submenu. */
public class ImageMath implements PlugInFilter {
	
	private String arg;
	private ImagePlus imp;
	private boolean canceled;
	
	private static boolean first;
	private static double addValue = 25;
	private static double mulValue = 1.25;
	private static double minValue = 0;
	private static double maxValue = 255;
	private static final String defaultAndValue = "11110000";
	private static String andValue = defaultAndValue;
	private static final double defaultGammaValue = 0.5;
	private static double gammaValue = defaultGammaValue;

	public int setup(String arg, ImagePlus imp) {
		this.arg = arg;
		this.imp = imp;
		first = true;
		IJ.register(ImageMath.class);
		return IJ.setupDialog(imp, DOES_ALL+SUPPORTS_MASKING);
	}

	public void run(ImageProcessor ip) {
	
		double value;
		
	 	if (canceled)
	 		return;
	 	
	 	if (arg.equals("add")) {
	 		if (first) addValue = getValue("Add", "Value: ", addValue, 0);
	 		if (canceled) return;
			ip.add(addValue);
			return;
		}

	 	if (arg.equals("sub")) {
	 		if (first) addValue = getValue("Subtract", "Value: ", addValue, 0);
	 		if (canceled) return;
			ip.add(-addValue);
			return;
		}

	 	if (arg.equals("mul")) {
	 		if (first) mulValue = getValue("Multiply", "Value: ", mulValue, 2);
	 		if (canceled) return;
			ip.multiply(mulValue);
			return;
		}

	 	if (arg.equals("div")) {
	 		if (first) mulValue = getValue("Divide", "Value: ", mulValue, 2);
	 		if (canceled) return;
			if (mulValue!=0.0) ip.multiply(1.0/mulValue);
			return;
		}

	 	if (arg.equals("and")) {
	 		if (first) andValue = getBinaryValue("AND", "Value (binary): ", andValue);
	 		if (canceled) return;
	 		try {
				ip.and(Integer.parseInt(andValue,2));
			} catch (NumberFormatException e) {
				andValue = defaultAndValue;
				IJ.error("Binary number required");
			}
			return;
		}

	 	if (arg.equals("or")) {
	 		if (first) andValue = getBinaryValue("OR", "Value (binary): ", andValue);
	 		if (canceled) return;
	 		try {
				ip.or(Integer.parseInt(andValue,2));
			} catch (NumberFormatException e) {
				andValue = defaultAndValue;
				IJ.error("Binary number required");
			}
			return;
		}
			
	 	if (arg.equals("xor")) {
	 		if (first) andValue = getBinaryValue("XOR", "Value (binary): ", andValue);
	 		if (canceled) return;
	 		try {
				ip.xor(Integer.parseInt(andValue,2));
			} catch (NumberFormatException e) {
				andValue = defaultAndValue;
				IJ.error("Binary number required");
			}
			return;
		}
		
	 	if (arg.equals("min")) {
	 		if (first) minValue = getValue("Min", "Value: ", minValue, 0);
	 		if (canceled) return;
	 		ip.min(minValue);
			if (!(ip instanceof ByteProcessor))
				ip.resetMinAndMax();
			return;
		}

	 	if (arg.equals("max")) {
	 		if (first) maxValue = getValue("Max", "Value: ", maxValue, 0);
	 		if (canceled) return;
	 		ip.max(maxValue);
			if (!(ip instanceof ByteProcessor))
				ip.resetMinAndMax();
			return;
		}

	 	if (arg.equals("gamma")) {
	 		if (first) gammaValue = getValue("Gamma", "Value (0.1-5.0): ", gammaValue, 2);
	 		if (canceled) return;
	 		if (gammaValue<0.1 || gammaValue>5.0) {
	 			IJ.error("Gamma must be between 0.1 and 5.0");
	 			gammaValue = defaultGammaValue;
	 			return;
	 		}
			ip.gamma(gammaValue);
			return;
		}

	 	if (arg.equals("log")) {
			ip.log();
			return;
		}
		
	 	if (arg.equals("sqr")) {
			ip.sqr();
			return;
		}

	 	if (arg.equals("sqrt")) {
			ip.sqrt();
			return;
		}

	 	if (arg.equals("reciprocal")) {
			if (!(ip instanceof FloatProcessor)) {
				IJ.error("32-bit float image required");
				canceled = true;
				return;
			}
			float[] pixels = (float[])ip.getPixels();
			for (int i=0; i<ip.getWidth()*ip.getHeight(); i++) {
				if (pixels[i]==0f)
					pixels[i] = Float.NaN;
				else
					pixels[i] = 1f/pixels[i];
			}
			if (!(ip instanceof ByteProcessor))
				ip.resetMinAndMax();
			return;
		}
		
	}
	
	double getValue (String title, String prompt, double defaultValue, int digits) {
			GenericDialog gd = new GenericDialog(title);
			gd.addNumericField(prompt, defaultValue, digits);
			gd.showDialog();
			if (first) imp.startTiming();
			first = false;
			canceled = gd.wasCanceled();
			if (canceled)
				return defaultValue;
			return gd.getNextNumber();
	}

	String getBinaryValue (String title, String prompt, String defaultValue) {
			GenericDialog gd = new GenericDialog(title);
			gd.addStringField(prompt, defaultValue);
			gd.showDialog();
			if (first) imp.startTiming();
			first = false;
			canceled = gd.wasCanceled();
			if (canceled)
				return defaultValue;
			return gd.getNextString();
	}
}
