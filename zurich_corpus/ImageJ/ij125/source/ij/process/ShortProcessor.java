package ij.process;

import java.util.*;
import java.awt.*;
import java.awt.image.*;
import ij.gui.*;

/** Objects of the class contain a 16-bit unsigned image and
	methods that operate on that image. */
public class ShortProcessor extends ImageProcessor {

	private int min, max, snapshotMin, snapshotMax;
	private short[] pixels;
	private short[] snapshotPixels;
	private byte[] pixels8;
	private byte[] LUT;
	private boolean fixedScale;

	/** Creates a new ShortProcessor using the specified pixel array and ColorModel.
		Set 'cm' to null to use the default grayscale LUT. */
	public ShortProcessor(int width, int height, short[] pixels, ColorModel cm) {
		this.width = width;
		this.height = height;
		this.pixels = pixels;
		this.cm = cm;
		setRoi(null);
		findMinAndMax();
		fgColor = max;
	}

	/** Creates a blank ShortProcessor using the default grayscale LUT that
		displays zero as black. Call invertLut() to display zero as white. */
	public ShortProcessor(int width, int height) {
		this(width, height, new short[width*height], null);
	}

	/** Obsolete. Images are always unsigned. */
	public ShortProcessor(int width, int height, short[] pixels, ColorModel cm, boolean unsigned) {
		this(width, height, pixels, cm);
	}

	/** Obsolete. Images are always unsigned. */
	public ShortProcessor(int width, int height,  boolean unsigned) {
		this(width, height);
	}

	public void findMinAndMax() {
		if (fixedScale)
			return;
		int size = width*height;
		int value;
		min = 65535;
		max = 0;
		for (int i=0; i<size; i++) {
			value = pixels[i]&0xffff;
			if (value<min)
				min = value;
			if (value>max)
				max = value;
		}
		hideProgress();
	}

	/** Create an 8-bit AWT image by scaling pixels in the range min-max to 0-255. */
	public Image createImage() {
		boolean rescale = !lutAnimation || pixels8==null;
		lutAnimation = false;
		if (pixels8==null)
			pixels8 = new byte[width*height];
		if (cm==null)
			makeDefaultColorModel();
		if (rescale) {
			int value;
			double scale = 255.0/(max-min);
			for (int i=0; i < width*height; i++) {
				value = (pixels[i]&0xffff)-min;
				if (value<0) value = 0;
				value = (int)(value*scale);
				if (value>255) value = 255;
				pixels8[i] = (byte)value;
			}
		}
	    ImageProducer p = new MemoryImageSource(width, height, cm, pixels8, 0, width);
	    return Toolkit.getDefaultToolkit().createImage(p);
	}
	
	/** Returns a new, blank ShortProcessor with the specified width and height. */
	public ImageProcessor createProcessor(int width, int height) {
		ImageProcessor ip2 = new ShortProcessor(width, height, new short[width*height], getColorModel());
		ip2.setMinAndMax(getMin(), getMax());
		return ip2;
	}

	public void snapshot() {
		snapshotWidth=width;
		snapshotHeight=height;
		snapshotMin=min;
		snapshotMax=max;
		if (snapshotPixels==null || (snapshotPixels!=null && snapshotPixels.length!=pixels.length))
			snapshotPixels = new short[width * height];
		System.arraycopy(pixels, 0, snapshotPixels, 0, width*height);
		newSnapshot = true;
	}
	
	public void reset() {
		if (snapshotPixels==null)
			return;
	    min=snapshotMin;
		max=snapshotMax;
        System.arraycopy(snapshotPixels, 0, pixels, 0, width*height);
        newSnapshot = true;
	}
	
	public void reset(int[] mask) {
		if (mask==null || snapshotPixels==null || mask.length!=roiWidth*roiHeight)
			return;
		for (int y=roiY, my=0; y<(roiY+roiHeight); y++, my++) {
			int i = y * width + roiX;
			int mi = my * roiWidth;
			for (int x=roiX; x<(roiX+roiWidth); x++) {
				if (mask[mi++]!=BLACK)
					pixels[i] = snapshotPixels[i];
				i++;
			}
		}
	}

	/* Obsolete. */
	//public boolean isUnsigned() {
	//	return true;
	//}

	/** Returns the smallest displayed pixel value. */
	public double getMin() {
		return min;
	}

	/** Returns the largest displayed pixel value. */
	public double getMax() {
		return max;
	}

	/**
	Sets the min and max variables that control how real
	pixel values are mapped to 0-255 screen values.
	@see resetMinAndMax
	@see ij.plugin.frame.ContrastAdjuster 
	*/
	public void setMinAndMax(double min, double max) {
		if (min==0.0 && max==0.0)
			{resetMinAndMax(); return;}
		if (min<0.0)
			min = 0.0;
		if (max>65535.0)
			max = 65535.0;
		this.min = (int)min;
		this.max = (int)max;
		fixedScale = true;
		resetThreshold();
	}
	
	/** Recalculates the min and max values used to scale pixel
		values to 0-255 for display. This ensures that this 
		ShortProcessor is set up to correctly display the image. */
	public void resetMinAndMax() {
		fixedScale = false;
		findMinAndMax();
		resetThreshold();
	}

	public int getPixel(int x, int y) {
		if (x>=0 && x<width && y>=0 && y<height) {
			int value = pixels[y*width + x]&0xffff;
			return value;
		} else
			return 0;
	}

	/** Uses bilinear interpolation to find the pixel value at real coordinates (x,y). */
	public double getInterpolatedPixel(double x, double y) {
		int basex = (int)x;
		int basey = (int)y;
		if (basex>=0 && (basex+1)<width && basey>=0 && (basey+1)<height)
			return getInterpolatedPixel(x, y, pixels);
		else
			return 0f;
	}

	/** Stores the specified value at (x,y). */
	public void putPixel(int x, int y, int value) {
		if (x>=0 && x<width && y>=0 && y<height) {
			pixels[y*width + x] = (short)value;
		}
	}

	/** Stores the specified real value at (x,y). */
	public void putPixelValue(int x, int y, double value) {
		if (x>=0 && x<width && y>=0 && y<height)
			pixels[y*width + x] = (short)value;
	}

	/** Draws a pixel in the current foreground color. */
	public void drawPixel(int x, int y) {
		putPixel(x, y, fgColor);
	}

	public float getPixelValue(int x, int y) {
		if (x>=0 && x<width && y>=0 && y<height) {
			if (cTable==null)
				return pixels[y*width + x]&0xffff;
			else
				return cTable[pixels[y*width + x]&0xffff];
		} else
			return 0f;
	}

	/**	Returns a reference to the short array containing this image's
		pixel data. To avoid sign extension, the pixel values must be
		accessed using a mask (e.g. int i = pixels[j]&0xffff). */
 	public Object getPixels() {
		return (Object)pixels;
	}

	/** Returns a reference to this image's snapshot (undo) array. If
		the snapshot array is null, returns a copy of the pixel data. */
	public Object getPixelsCopy() {
		if (newSnapshot)
			return snapshotPixels;
		else {
			short[] pixels2 = new short[width*height];
        	System.arraycopy(pixels, 0, pixels2, 0, width*height);
			return pixels2;
		}
	}

	public void setPixels(Object pixels) {
		this.pixels = (short[])pixels;
		snapshotPixels = null;
	}

	void getRow2(int x, int y, int[] data, int length) {
		int value;
		for (int i=0; i<length; i++)
			data[i] = pixels[y*width+x+i]&0xffff;
	}
	
	void putColumn2(int x, int y, int[] data, int length) {
		int value;
		for (int i=0; i<length; i++)
			pixels[(y+i)*width+x] = (short)data[i];
	}
	
	/** Copies the image contained in 'ip' to (xloc, yloc) using one of
		the transfer modes defined in the Blitter interface. */
	public void copyBits(ImageProcessor ip, int xloc, int yloc, int mode) {
		if (!(ip instanceof ShortProcessor))
			throw new IllegalArgumentException("16-bit image required");
		new ShortBlitter(this).copyBits(ip, xloc, yloc, mode);
	}

	/** Transforms the pixel data using a 65536 entry lookup table. */
	public void applyTable(int[] lut) {
		if (lut.length!=65536)
			throw new IllegalArgumentException("lut.length!=65536");
		int lineStart, lineEnd, v;
		for (int y=roiY; y<(roiY+roiHeight); y++) {
			lineStart = y * width + roiX;
			lineEnd = lineStart + roiWidth;
			for (int i=lineEnd; --i>=lineStart;) {
				v = lut[pixels[i]&0xffff];
				pixels[i] = (short)v;
			}
		}
		findMinAndMax();
	}

	private void process(int op, double value) {
		int v1, v2;
		double range = max-min;
		
		for (int y=roiY; y<(roiY+roiHeight); y++) {
			int i = y * width + roiX;
			for (int x=roiX; x<(roiX+roiWidth); x++) {
				v1 = pixels[i]&0xffff;
				switch(op) {
					case INVERT:
						v2 = max - (v1 - min);
						break;
					case FILL:
						v2 = fgColor;
						break;
					case ADD:
						v2 = v1 + (int)value;
						break;
					case MULT:
						v2 = (int)Math.round(v1 * value);
						break;
					case AND:
						v2 = v1 & (int)value;
						break;
					case OR:
						v2 = v1 | (int)value;
						break;
					case XOR:
						v2 = v1 ^ (int)value;
						break;
					case GAMMA:
						if (range<=0.0 || v1==min)
							v2 = v1;
						else					
							v2 = (int)(Math.exp(value*Math.log((v1-min)/range))*range+min);
						break;
					case LOG:
						if (v1<=0)
							v2 = 0;
						else 
							v2 = (int)(Math.log(v1)*(max/Math.log(max)));
						break;
					case SQR:
							v2 = v1*v1;
						break;
					case SQRT:
							v2 = (int)Math.sqrt(v1);
						break;
					case MINIMUM:
						if (v1<value)
							v2 = (int)value;
						else
							v2 = v1;
						break;
					case MAXIMUM:
						if (v1>value)
							v2 = (int)value;
						else
							v2 = v1;
						break;
					 default:
					 	v2 = v1;
				}
				if (v2 < 0)
					v2 = 0;
				if (v2 > 65535)
					v2 = 65535;
				pixels[i++] = (short)v2;
			}
			if (y%20==0)
				showProgress((double)(y-roiY)/roiHeight);
		}
		findMinAndMax();
		showProgress(1.0);
    }

	public void invert() {
		resetMinAndMax();
		process(INVERT, 0.0);
	}
	
	public void add(int value) {process(ADD, value);}
	public void add(double value) {process(ADD, value);}
	public void multiply(double value) {process(MULT, value);}
	public void and(int value) {process(AND, value);}
	public void or(int value) {process(OR, value);}
	public void xor(int value) {process(XOR, value);}
	public void gamma(double value) {process(GAMMA, value);}
	public void log() {process(LOG, 0.0);}
	public void sqr() {process(SQR, 0.0);}
	public void sqrt() {process(SQRT, 0.0);}
	public void min(double value) {process(MINIMUM, value);}
	public void max(double value) {process(MAXIMUM, value);}

	/** Fills the current rectangular ROI. */
	public void fill() {
		process(FILL, 0.0);
	}

	/** Fills pixels that are within roi and part of the mask. */
	public void fill(int[] mask) {
		for (int y=roiY, my=0; y<(roiY+roiHeight); y++, my++) {
			int i = y * width + roiX;
			int mi = my * roiWidth;
			for (int x=roiX; x<(roiX+roiWidth); x++) {
				if (mask[mi++]==BLACK)
					pixels[i] = (short)fgColor;
				i++;
			}
		}
	}

	/** 3x3 convolution contributed by Glynne Casteel. */
	public void convolve3x3(int[] kernel) {
		int p1, p2, p3,
		    p4, p5, p6,
		    p7, p8, p9;
		int k1=kernel[0], k2=kernel[1], k3=kernel[2],
		    k4=kernel[3], k5=kernel[4], k6=kernel[5],
		    k7=kernel[6], k8=kernel[7], k9=kernel[8];

		int scale = 0;
		for (int i=0; i<kernel.length; i++)
			scale += kernel[i];
		if (scale==0) scale = 1;
		int inc = roiHeight/25;
		if (inc<1) inc = 1;
		
		short[] pixels2 = (short[])getPixelsCopy();
		int offset, sum;
        int rowOffset = width;
		for (int y=yMin; y<=yMax; y++) {
			offset = xMin + y * width;
			p1 = 0;
			p2 = pixels2[offset-rowOffset-1]&0xffff;
			p3 = pixels2[offset-rowOffset]&0xffff;
			p4 = 0;
			p5 = pixels2[offset-1]&0xffff;
			p6 = pixels2[offset]&0xffff;
			p7 = 0;
			p8 = pixels2[offset+rowOffset-1]&0xffff;
			p9 = pixels2[offset+rowOffset]&0xffff;

			for (int x=xMin; x<=xMax; x++) {
				p1 = p2; p2 = p3;
				p3 = pixels2[offset-rowOffset+1]&0xffff;
				p4 = p5; p5 = p6;
				p6 = pixels2[offset+1]&0xffff;
				p7 = p8; p8 = p9;
				p9 = pixels2[offset+rowOffset+1]&0xffff;
				sum = k1*p1 + k2*p2 + k3*p3
				    + k4*p4 + k5*p5 + k6*p6
				    + k7*p7 + k8*p8 + k9*p9;
				sum /= scale;
				pixels[offset++] = (short)sum;
			}
			if (y%inc==0)
				showProgress((double)(y-roiY)/roiHeight);
		}
		hideProgress();
	}

	/** Filters using a 3x3 neighborhood. */
	public void filter(int type) {
		int p1, p2, p3, p4, p5, p6, p7, p8, p9;
		int inc = roiHeight/25;
		if (inc<1) inc = 1;
		
		short[] pixels2 = (short[])getPixelsCopy();
		int offset, sum1, sum2, sum=0;
        int rowOffset = width;
		for (int y=yMin; y<=yMax; y++) {
			offset = xMin + y * width;
			p1 = 0;
			p2 = pixels2[offset-rowOffset-1]&0xffff;
			p3 = pixels2[offset-rowOffset]&0xffff;
			p4 = 0;
			p5 = pixels2[offset-1]&0xffff;
			p6 = pixels2[offset]&0xffff;
			p7 = 0;
			p8 = pixels2[offset+rowOffset-1]&0xffff;
			p9 = pixels2[offset+rowOffset]&0xffff;

			for (int x=xMin; x<=xMax; x++) {
				p1 = p2; p2 = p3;
				p3 = pixels2[offset-rowOffset+1]&0xffff;
				p4 = p5; p5 = p6;
				p6 = pixels2[offset+1]&0xffff;
				p7 = p8; p8 = p9;
				p9 = pixels2[offset+rowOffset+1]&0xffff;

				switch (type) {
					case BLUR_MORE:
						sum = (p1+p2+p3+p4+p5+p6+p7+p8+p9)/9;
						break;
					case FIND_EDGES:
	        			sum1 = p1 + 2*p2 + p3 - p7 - 2*p8 - p9;
	        			sum2 = p1  + 2*p4 + p7 - p3 - 2*p6 - p9;
	        			sum = (int)Math.sqrt(sum1*sum1 + sum2*sum2);
	        			break;
				}
				
				pixels[offset++] = (short)sum;
			}
			if (y%inc==0)
				showProgress((double)(y-roiY)/roiHeight);
		}
		if (type==BLUR_MORE)
			hideProgress();
		else
			findMinAndMax();
	}

	/** Rotates the image or ROI 'angle' degrees clockwise.
		@see ImageProcessor#setInterpolate
	*/
	public void rotate(double angle) {
		short[] pixels2 = (short[])getPixelsCopy();
		double centerX = roiX + roiWidth/2.0;
		double centerY = roiY + roiHeight/2.0;
		int xMax = roiX + this.roiWidth - 1;
		
		double angleRadians = -angle/(180.0/Math.PI);
		double ca = Math.cos(angleRadians);
		double sa = Math.sin(angleRadians);
		double tmp1 = centerY*sa-centerX*ca;
		double tmp2 = -centerX*sa-centerY*ca;
		double tmp3, tmp4, xs, ys;
		int index, xsi, ysi;
		
		for (int y=roiY; y<(roiY + roiHeight); y++) {
			index = y*width + roiX;
			tmp3 = tmp1 - y*sa + centerX;
			tmp4 = tmp2 + y*ca + centerY;
			for (int x=roiX; x<=xMax; x++) {
				xs = x*ca + tmp3;
				ys = x*sa + tmp4;
				if ((xs>=0.0) && (xs<width) && (ys>=0.0) && (ys<height)) {
					if (interpolate)
				  		pixels[index++] = (short)(getInterpolatedPixel(xs, ys, pixels2)+0.5);
				  	else
						pixels[index++] = pixels2[width*(int)ys+(int)xs];
    			} else
					pixels[index++] = 0;
			}
			if (y%20==0)
			showProgress((double)(y-roiY)/roiHeight);
		}
		hideProgress();
	}

	/** Scales the image or selection using the specified scale factors.
		@see ImageProcessor#setInterpolate
	*/
	public void scale(double xScale, double yScale) {
		double xCenter = roiX + roiWidth/2.0;
		double yCenter = roiY + roiHeight/2.0;
		int xmin, xmax, ymin, ymax;
		
		if ((xScale>1.0) && (yScale>1.0)) {
			//expand roi
			xmin = (int)(xCenter-(xCenter-roiX)*xScale);
			if (xmin<0) xmin = 0;
			xmax = xmin + (int)(roiWidth*xScale) - 1;
			if (xmax>=width) xmax = width - 1;
			ymin = (int)(yCenter-(yCenter-roiY)*yScale);
			if (ymin<0) ymin = 0;
			ymax = ymin + (int)(roiHeight*yScale) - 1;
			if (ymax>=height) ymax = height - 1;
		} else {
			xmin = roiX;
			xmax = roiX + roiWidth - 1;
			ymin = roiY;
			ymax = roiY + roiHeight - 1;
		}
		short[] pixels2 = (short[])getPixelsCopy();
		boolean checkCoordinates = (xScale < 1.0) || (yScale < 1.0);
		int index1, index2, xsi, ysi;
		double ys, xs;
		for (int y=ymin; y<=ymax; y++) {
			ys = (y-yCenter)/yScale + yCenter;
			ysi = (int)ys;
			index1 = y*width + xmin;
			index2 = width*(int)ys;
			for (int x=xmin; x<=xmax; x++) {
				xs = (x-xCenter)/xScale + xCenter;
				xsi = (int)xs;
				if (checkCoordinates && ((xsi<xmin) || (xsi>xmax) || (ysi<ymin) || (ys>ymax)))
					pixels[index1++] = (short)min;
				else {
					if (interpolate)
						pixels[index1++] = (short)(getInterpolatedPixel(xs, ys, pixels2)+0.5);
					else
						pixels[index1++] = pixels2[index2+xsi];
				}
			}
			if (y%20==0)
			showProgress((double)(y-ymin)/height);
		}
		hideProgress();
	}

	/** Uses bilinear interpolation to find the pixel value at real coordinates (x,y). */
	private final double getInterpolatedPixel(double x, double y, short[] pixels) {
		int xbase = (int)x;
		int ybase = (int)y;
		double xFraction = x - xbase;
		double yFraction = y - ybase;
		int offset = ybase * width + xbase;
		int lowerLeft = pixels[offset]&0xffff;
		if ((xbase>=(width-1))||(ybase>=(height-1)))
			return lowerLeft;
		int lowerRight = pixels[offset + 1]&0xffff;
		int upperRight = pixels[offset + width + 1]&0xffff;
		int upperLeft = pixels[offset + width]&0xffff;
		double upperAverage = upperLeft + xFraction * (upperRight - upperLeft);
		double lowerAverage = lowerLeft + xFraction * (lowerRight - lowerLeft);
		return lowerAverage + yFraction * (upperAverage - lowerAverage);
	}

	/** Creates a new ShortProcessor containing a scaled copy of this image or selection. */
	public ImageProcessor resize(int dstWidth, int dstHeight) {
		double srcCenterX = roiX + roiWidth/2.0;
		double srcCenterY = roiY + roiHeight/2.0;
		double dstCenterX = dstWidth/2.0;
		double dstCenterY = dstHeight/2.0;
		double xScale = (double)dstWidth/roiWidth;
		double yScale = (double)dstHeight/roiHeight;
		ImageProcessor ip2 = createProcessor(dstWidth, dstHeight);
		short[] pixels2 = (short[])ip2.getPixels();
		double xs, ys=0.0;
		int index1, index2;
		for (int y=0; y<=dstHeight-1; y++) {
			index1 = width*(int)ys;
			index2 = y*dstWidth;
			ys = (y-dstCenterY)/yScale + srcCenterY;
			for (int x=0; x<=dstWidth-1; x++) {
				xs = (x-dstCenterX)/xScale + srcCenterX;
				if (interpolate)
					pixels2[index2++] = (short)(getInterpolatedPixel(xs, ys, pixels)+0.5);
				else
		  			pixels2[index2++] = pixels[index1+(int)xs];
			}
			if (y%20==0)
			showProgress((double)y/dstHeight);
		}
		hideProgress();
		return ip2;
	}

	public ImageProcessor crop() {
		ImageProcessor ip2 = createProcessor(roiWidth, roiHeight);
		short[] pixels2 = (short[])ip2.getPixels();
		for (int ys=roiY; ys<roiY+roiHeight; ys++) {
			int offset1 = (ys-roiY)*roiWidth;
			int offset2 = ys*width+roiX;
			for (int xs=0; xs<roiWidth; xs++)
				pixels2[offset1++] = pixels[offset2++];
		}
        return ip2;
	}
	
	/** Sets the foreground fill/draw color. */
	public void setColor(Color color) {
		int bestIndex = getBestIndex(color);
		if (bestIndex>0 && getMin()==0.0 && getMax()==0.0) {
			setValue(bestIndex);
			setMinAndMax(0.0,255.0);
		} else if (bestIndex==0 && getMin()>0.0 && (color.getRGB()&0xffffff)==0)
			setValue(0.0);
		else
			fgColor = (int)(getMin() + (getMax()-getMin())*(bestIndex/255.0));

	}
	
	/** Sets the default fill/draw value, where 0<=value<=65535). */
	public void setValue(double value) {
			fgColor = (int)value;
			if (fgColor<0) fgColor = 0;
			if (fgColor>65535) fgColor = 65535;
	}

	/** Returns 65536 bin histogram of the current ROI, which
		can be non-rectangular. */
	public int[] getHistogram() {
		if (mask!=null)
			return getHistogram(mask);
		int[] histogram = new int[65536];
		for (int y=roiY; y<(roiY+roiHeight); y++) {
			int i = y*width + roiX;
			for (int x=roiX; x<(roiX+roiWidth); x++)
					histogram[pixels[i++]&0xffff]++;
		}
		return histogram;
	}

	int[] getHistogram(int[] mask) {
		int[] histogram = new int[65536];
		for (int y=roiY, my=0; y<(roiY+roiHeight); y++, my++) {
			int i = y * width + roiX;
			int mi = my * roiWidth;
			for (int x=roiX; x<(roiX+roiWidth); x++) {
				if (mask[mi++]==BLACK)
					histogram[pixels[i]&0xffff]++;
				i++;
			}
		}
		return histogram;
	}

	public void setThreshold(double minThreshold, double maxThreshold, int lutUpdate) {
		if (minThreshold!=NO_THRESHOLD && max>min) {
			double minT = ((minThreshold-min)/(max-min))*255.0;
			double maxT = ((maxThreshold-min)/(max-min))*255.0;
			super.setThreshold(minT, maxT, lutUpdate);
			this.minThreshold = minThreshold;
			this.maxThreshold = maxThreshold;
		} else
			super.resetThreshold();
	}

    public void noise(double range) {}
	public void threshold(int level) {}
	public void autoThreshold() {}
	public void medianFilter() {}
	public void erode() {}
	public void dilate() {}

}

