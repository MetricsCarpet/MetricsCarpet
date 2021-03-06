package ij.plugin;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.awt.event.*;
import ij.*;
import ij.io.*;
import ij.gui.*;
import ij.process.*;
import ij.measure.Calibration;

/** Opens a folder of images as a stack. */
public class FolderOpener implements PlugIn {

	private static boolean convertToGrayscale, convertToRGB;
	private static double scale = 100.0;
	private int n, start, increment;
	private String filter;
	private FileInfo fi;
	private String info1;

	public void run(String arg) {
		OpenDialog od = new OpenDialog("Open Image Sequence...", arg);
		String directory = od.getDirectory();
		String name = od.getFileName();
		if (name==null)
			return;

		String[] list = new File(directory).list();
		if (list==null)
			return;
		IJ.register(FolderOpener.class);
		ij.util.StringSorter.sort(list);
		if (IJ.debugMode) IJ.log("FolderOpener: "+directory+" ("+list.length+" files)");
		int width=0,height=0,bitDepth=0;
		ImageStack stack = null;
		double min = Double.MAX_VALUE;
		double max = -Double.MAX_VALUE;
		Calibration cal = null;
		boolean allSameCalibration = true;
		IJ.resetEscape();		
		try {
			for (int i=0; i<list.length; i++) {
				if (list[i].endsWith(".txt"))
					continue;
				ImagePlus imp = new Opener().openImage(directory, list[i]);
				if (imp!=null) {
					width = imp.getWidth();
					height = imp.getHeight();
					bitDepth = imp.getBitDepth();
					fi = imp.getOriginalFileInfo();
					if (!showDialog(imp, list))
						return;
					break;
				}
			}
			if (width==0) {
				IJ.error("Import Sequence", "This folder does not appear to contain any TIFF,\n"
				+ "JPEG, BMP, DICOM, GIF, FITS or PGM files.");
				return;
			}

			if (filter!=null && (filter.equals("") || filter.equals("*")))
				filter = null;
			if (filter!=null) {
				int filteredImages = 0;
  				for (int i=0; i<list.length; i++) {
 					if (list[i].indexOf(filter)>=0)
 						filteredImages++;
 					else
 						list[i] = null;
 				}
  				if (filteredImages==0) {
  					IJ.error("None of the "+list.length+" files contain\n the string '"+filter+"' in their name.");
  					return;
  				}
  				String[] list2 = new String[filteredImages];
  				int j = 0;
  				for (int i=0; i<list.length; i++) {
 					if (list[i]!=null)
 						list2[j++] = list[i];
 				}
  				list = list2;
  			}

			if (n<1)
				n = list.length;
			if (start<1 || start>list.length)
				start = 1;
			if (start+n-1>list.length)
				n = list.length-start+1;
			int count = 0;
			int counter = 0;
			for (int i=start-1; i<list.length; i++) {
				if (list[i].endsWith(".txt"))
					continue;
				if ((counter++%increment)!=0)
					continue;
				ImagePlus imp = new Opener().openImage(directory, list[i]);
				if (imp!=null && stack==null) {
					width = imp.getWidth();
					height = imp.getHeight();
					bitDepth = imp.getBitDepth();
					cal = imp.getCalibration();
					if (convertToRGB) bitDepth = 24;
					if (convertToGrayscale) bitDepth = 8;
					ColorModel cm = imp.getProcessor().getColorModel();
					if (scale<100.0)						
						stack = new ImageStack((int)(width*scale/100.0), (int)(height*scale/100.0), cm);
					else
						stack = new ImageStack(width, height, cm);
					info1 = (String)imp.getProperty("Info");
				}
				if (imp==null) {
					if (!list[i].startsWith("."))
						IJ.log(list[i] + ": unable to open");
					continue;
				}
				ImageProcessor ip = imp.getProcessor();
				int bitDepth2 = imp.getBitDepth();
				if (convertToRGB) {
					ip = ip.convertToRGB();
					bitDepth2 = 24;
				} else if(convertToGrayscale) {
					ip = ip.convertToByte(true);
					bitDepth2 = 8;
				}
				if (bitDepth2!=bitDepth) {
					if (bitDepth==8) {
						ip = ip.convertToByte(true);
						bitDepth2 = 8;
					} else if (bitDepth==24) {
						ip = ip.convertToRGB();
						bitDepth2 = 24;
					}
				}
				if (imp.getWidth()!=width || imp.getHeight()!=height)
					IJ.log(list[i] + ": wrong dimensions");
				else if (bitDepth2!=bitDepth) {
					IJ.log(list[i] + ": wrong bit depth, "+bitDepth+" expected, "+bitDepth2+" found");
				} else {
					count = stack.getSize()+1;
					IJ.showStatus(count+"/"+n);
					IJ.showProgress(count, n);
					if (scale<100.0)
						ip = ip.resize((int)(width*scale/100.0), (int)(height*scale/100.0));
					if (ip.getMin()<min) min = ip.getMin();
					if (ip.getMax()>max) max = ip.getMax();
					String label = imp.getTitle();
					if (imp.getCalibration().pixelWidth!=cal.pixelWidth)
						allSameCalibration = false;
					String info = (String)imp.getProperty("Info");
					if (info!=null)
						label += "\n" + info;
					stack.addSlice(label, ip);
				}
				if (count>=n)
					break;
				if (IJ.escapePressed())
					{IJ.beep(); break;}
				//System.gc();
			}
		} catch(OutOfMemoryError e) {
			IJ.outOfMemory("FolderOpener");
			if (stack!=null) stack.trim();
		}
		if (stack!=null && stack.getSize()>0) {
			ImagePlus imp2 = new ImagePlus("Stack", stack);
			if (imp2.getType()==ImagePlus.GRAY16 || imp2.getType()==ImagePlus.GRAY32)
				imp2.getProcessor().setMinAndMax(min, max);
			imp2.setFileInfo(fi); // saves FileInfo of the first image
			if (allSameCalibration)
				imp2.setCalibration(cal); // use calibration from first image
			if (imp2.getStackSize()==1 && info1!=null)
				imp2.setProperty("Info", info1);
			imp2.show();
		}
		IJ.showProgress(1.0);
	}
	
	boolean showDialog(ImagePlus imp, String[] list) {
		int fileCount = list.length;
		FolderOpenerDialog gd = new FolderOpenerDialog("Sequence Options", imp, list);
		gd.addNumericField("Number of Images:", fileCount, 0);
		gd.addNumericField("Starting Image:", 1, 0);
		gd.addNumericField("Increment:", 1, 0);
		//gd.addMessage("");
		gd.addStringField("File Name Contains:", "");
		gd.addNumericField("Scale Images", scale, 0, 4, "%");
		gd.addCheckbox("Convert to 8-bit Grayscale", convertToGrayscale);
		gd.addCheckbox("Convert_to_RGB", convertToRGB);
		gd.addMessage("10000 x 10000 x 1000 (100.3MB)");
		gd.showDialog();
		if (gd.wasCanceled())
			return false;
		n = (int)gd.getNextNumber();
		start = (int)gd.getNextNumber();
		increment = (int)gd.getNextNumber();
		if (increment<1)
			increment = 1;
		scale = gd.getNextNumber();
		if (scale<5.0) scale = 5.0;
		if (scale>100.0) scale = 100.0;
		filter = gd.getNextString();
		convertToGrayscale = gd.getNextBoolean();
		convertToRGB = gd.getNextBoolean();
		return true;
	}

}

class FolderOpenerDialog extends GenericDialog {
	ImagePlus imp;
	int fileCount;
 	boolean eightBits, rgb;
 	String[] list;

	public FolderOpenerDialog(String title, ImagePlus imp, String[] list) {
		super(title);
		this.imp = imp;
		this.list = list;
		this.fileCount = list.length;
	}

	protected void setup() {
 		eightBits = ((Checkbox)checkbox.elementAt(0)).getState();
 		rgb = ((Checkbox)checkbox.elementAt(1)).getState();
		setStackInfo();
	}
 	
	public void itemStateChanged(ItemEvent e) {
		Checkbox item = (Checkbox)e.getSource();
		Checkbox grayscaleCB = (Checkbox)checkbox.elementAt(0);
		Checkbox rgbCB = (Checkbox)checkbox.elementAt(1);
		if (item==grayscaleCB) {
			eightBits = item.getState();
			if (eightBits) {
			 rgbCB.setState(false);
			 rgb = false;
			}
		}
		if (item==rgbCB) {
			rgb = item.getState();
			if (rgb) {
				grayscaleCB.setState(false);
				eightBits = false;
			}
		}
 		setStackInfo();
	}
	
	public void textValueChanged(TextEvent e) {
 		setStackInfo();
	}

	void setStackInfo() {
		int width = imp.getWidth();
		int height = imp.getHeight();
		int bytesPerPixel = 1;
 		int n = getNumber(numberField.elementAt(0));
		int start = getNumber(numberField.elementAt(1));
		int inc = getNumber(numberField.elementAt(2));
		double scale = getNumber(numberField.elementAt(3));
		if (scale<5.0) scale = 5.0;
		if (scale>100.0) scale = 100.0;
		
		if (n<1)
			n = fileCount;
		if (start<1 || start>fileCount)
			start = 1;
		if (start+n-1>fileCount) {
			n = fileCount-start+1;
			//TextField tf = (TextField)numberField.elementAt(0);
			//tf.setText(""+nImages);
		}
		if (inc<1)
			inc = 1;
 		TextField tf = (TextField)stringField.elementAt(0);
 		String filter = tf.getText();
		// IJ.write(nImages+" "+startingImage);
 		if (!filter.equals("") && !filter.equals("*")) {
 			int n2 = 0;
 			for (int i=0; i<list.length; i++) {
 				if (list[i].indexOf(filter)>=0)
 					n2++;
			}
			if (n2<n) n = n2;
 		}
		switch (imp.getType()) {
			case ImagePlus.GRAY16:
				bytesPerPixel=2;break;
			case ImagePlus.COLOR_RGB:
			case ImagePlus.GRAY32:
				bytesPerPixel=4; break;
		}
		if (eightBits)
			bytesPerPixel = 1;
		if (rgb)
			bytesPerPixel = 4;
		width = (int)(width*scale/100.0);
		height = (int)(height*scale/100.0);
		int n2 = n/inc;
		if (n2<0)
			n2 = 0;
		double size = ((double)width*height*n2*bytesPerPixel)/(1024*1024);
 		((Label)theLabel).setText(width+" x "+height+" x "+n2+" ("+IJ.d2s(size,1)+"MB)");
	}

	public int getNumber(Object field) {
		TextField tf = (TextField)field;
		String theText = tf.getText();
		double value;
		Double d;
		try {d = new Double(theText);}
		catch (NumberFormatException e){
			d = null;
		}
		if (d!=null)
			return (int)d.doubleValue();
		else
			return 0;
      }

}
