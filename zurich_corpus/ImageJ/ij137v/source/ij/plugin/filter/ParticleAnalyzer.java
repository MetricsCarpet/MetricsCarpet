package ij.plugin.filter;
import java.awt.*;
import java.awt.image.IndexColorModel;
import java.util.Properties;
import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.measure.*;
import ij.text.*;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.Recorder;
import ij.plugin.frame.RoiManager;
import ij.util.Tools;

/** Implements ImageJ's Analyze Particles command.
	<p>
	<pre>
	for each line do
	    for each pixel in this line do
	        if the pixel value is "inside" the threshold range then
	            trace the edge to mark the object
	            do the measurement
		        fill the object with a color outside the threshold range
		    else
		        continue the scan
	</pre>
*/
public class ParticleAnalyzer implements PlugInFilter, Measurements {

	/** Display results in the ImageJ console. */
	public static final int SHOW_RESULTS = 1;
	
	/** Obsolete */
	public static final int SHOW_SUMMARY = 2;
	
	/** Display image containing outlines of measured paticles. */
	public static final int SHOW_OUTLINES = 4;
	
	/** Do not measure particles touching edge of image. */
	public static final int EXCLUDE_EDGE_PARTICLES = 8;
	
	/** Display a progress bar. */
	public static final int SHOW_PROGRESS = 32;
	
	/** Clear ImageJ console before starting. */
	public static final int CLEAR_WORKSHEET = 64;
	
	/** Record starting coordinates so outline can be recreated later using doWand(x,y). */
	public static final int RECORD_STARTS = 128;

	/** Display a summary. */
	public static final int DISPLAY_SUMMARY = 256;

	/** Do not display particle outline image. */
	public static final int SHOW_NONE = 512;

	/** Flood fill to ignore interior holes. */
	public static final int INCLUDE_HOLES = 1024;
	
	/** Add particles to ROI Manager. */
	public static final int ADD_TO_MANAGER = 2048;


	static final String OPTIONS = "ap.options";
	
	static final int BYTE=0, SHORT=1, FLOAT=2, RGB=3;
	static final double DEFAULT_MIN_SIZE = 0.0;
	static final double DEFAULT_MAX_SIZE = Double.POSITIVE_INFINITY;
	
	private static double staticMinSize = 0.0;
	private static double staticMaxSize = DEFAULT_MAX_SIZE;
	private static int staticOptions = Prefs.getInt(OPTIONS,CLEAR_WORKSHEET);
	private static String[] showStrings = {"Nothing","Outlines","Masks","Ellipses"};
	private static double minCircularity=0.0, maxCircularity=1.0;
		
	protected static final int NOTHING=0,OUTLINES=1,MASKS=2,ELLIPSES=3;
	protected static int showChoice;
	protected ImagePlus imp;
	protected ResultsTable rt;
	protected Analyzer analyzer;
	protected int slice;
	protected boolean processStack;
	protected boolean showResults,excludeEdgeParticles,showSizeDistribution,
		resetCounter,showProgress, recordStarts, displaySummary, floodFill, addToManager;
		
	private double level1, level2;
	private double minSize;
	private double maxSize;
	private int options;
	private int measurements;
	private Calibration calibration;
	private String arg;
	private double fillColor;
	private boolean thresholdingLUT;
	private ImageProcessor drawIP;
	private int width,height;
	private boolean canceled;
	private ImageStack outlines;
	private IndexColorModel customLut;
	private int particleCount;
	private int totalCount;
	private TextWindow tw;
	private Wand wand;
	private int imageType, imageType2;
	private boolean roiNeedsImage;
	private int minX, maxX, minY, maxY;
	private ImagePlus redirectImp;
	private ImageProcessor redirectIP;
	private PolygonFiller pf;
    private Roi saveRoi;
    private int beginningCount;
	private Rectangle r;
	private ImageProcessor mask;
	private double totalArea;
	private FloodFiller ff;
	private Polygon polygon;
	private RoiManager roiManager;


	
	/** Construct a ParticleAnalyzer.
		@param options	a flag word created by Oring SHOW_RESULTS, EXCLUDE_EDGE_PARTICLES, etc.
		@param measurements	a flag word created by ORing constants defined in the Measurements interface
		@param rt		a ResultsTable where the measurements will be stored
		@param minSize	the smallest particle size in pixels
		@param maxSize	the largest particle size in pixels
	*/
	public ParticleAnalyzer(int options, int measurements, ResultsTable rt, double minSize, double maxSize) {
		this.options = options;
		this.measurements = measurements;
		this.rt = rt;
		if (this.rt==null)
			this.rt = new ResultsTable();
		this.minSize = minSize;
		this.maxSize = maxSize;
		slice = 1;
	}
	
	/** Default constructor */
	public ParticleAnalyzer() {
		slice = 1;
	}
	
	public int setup(String arg, ImagePlus imp) {
		this.arg = arg;
		this.imp = imp;
		IJ.register(ParticleAnalyzer.class);
		if (imp==null)
			{IJ.noImage();return DONE;}
		if (!showDialog())
			return DONE;
		int baseFlags = DOES_8G+DOES_16+DOES_32+NO_CHANGES+NO_UNDO;
		int flags = IJ.setupDialog(imp, baseFlags);
		processStack = (flags&DOES_STACKS)!=0;
		slice = 0;
        saveRoi = imp.getRoi();
        if (saveRoi!=null && saveRoi.getType()!=Roi.RECTANGLE && saveRoi.isArea())
        	polygon = saveRoi.getPolygon();
		imp.startTiming();
		return flags;
	}

	public void run(ImageProcessor ip) {
		if (canceled)
			return;
		slice++;
		if (imp.getStackSize()>1 && processStack)
			imp.setSlice(slice);
		if (!analyze(imp, ip))
            canceled = true;
		if (slice==imp.getStackSize()) {
			imp.updateAndDraw();
            if (saveRoi!=null) imp.setRoi(saveRoi);
        }
	}
	
	/** Displays a modal options dialog. */
	public boolean showDialog() {
		Calibration cal = imp!=null?imp.getCalibration():(new Calibration());
		double unitSquared = cal.pixelWidth*cal.pixelHeight;
		if (Macro.getOptions()!=null) {
			boolean oldMacro = updateMacroOptions();
			if (oldMacro) unitSquared = 1.0;
		}
		GenericDialog gd = new GenericDialog("Analyze Particles");
		minSize = staticMinSize;
		maxSize = staticMaxSize;
		if (maxSize==999999) maxSize = DEFAULT_MAX_SIZE;
		options = staticOptions;
		String units = cal.getUnit()+"^2";
		int places = 0;
		double cmin = minSize*unitSquared;
		if ((int)cmin!=cmin) places = 2;
		double cmax = maxSize*unitSquared;
		if ((int)cmax!=cmax && cmax!=DEFAULT_MAX_SIZE) places = 2;
		String minStr = IJ.d2s(cmin,places);
		if (minStr.indexOf("-")!=-1) {
			for (int i=places; i<=6; i++) {
				minStr = IJ.d2s(cmin, i);
				if (minStr.indexOf("-")==-1) break;
			}
		}
		String maxStr = IJ.d2s(cmax, places);
		if (maxStr.indexOf("-")!=-1) {
			for (int i=places; i<=6; i++) {
				maxStr = IJ.d2s(cmax, i);
				if (maxStr.indexOf("-")==-1) break;
			}
		}
		gd.addStringField("Size ("+units+"):", minStr+"-"+maxStr, 12);
		gd.addStringField("Circularity:", IJ.d2s(minCircularity)+"-"+IJ.d2s(maxCircularity), 12);
		gd.addChoice("Show:", showStrings, showStrings[showChoice]);
		
		String[] labels = new String[7];
		boolean[] states = new boolean[7];
		labels[0]="Display Results"; states[0] = (options&SHOW_RESULTS)!=0;
		labels[1]="Exclude on Edges"; states[1]=(options&EXCLUDE_EDGE_PARTICLES)!=0;
		labels[2]="Clear Results"; states[2]=(options&CLEAR_WORKSHEET)!=0;
		labels[3]="Include Holes"; states[3]=(options&INCLUDE_HOLES)!=0;
		labels[4]="Summarize"; states[4]=(options&DISPLAY_SUMMARY)!=0;
		labels[5]="Record Starts"; states[5]=(options&RECORD_STARTS)!=0;
		labels[6]="Add to Manager"; states[6]=(options&ADD_TO_MANAGER)!=0;
		gd.addCheckboxGroup(4, 2, labels, states);

		gd.showDialog();
		if (gd.wasCanceled())
			return false;
			
		String[] minAndMax = Tools.split(gd.getNextString(), " -");
		double mins = Tools.parseDouble(minAndMax[0]);
		double maxs = minAndMax.length==2?Tools.parseDouble(minAndMax[1]):Double.NaN;
		minSize = Double.isNaN(mins)?DEFAULT_MIN_SIZE:mins/unitSquared;
		maxSize = Double.isNaN(maxs)?DEFAULT_MAX_SIZE:maxs/unitSquared;
		if (minSize<DEFAULT_MIN_SIZE) minSize = DEFAULT_MIN_SIZE;
		if (maxSize<minSize) maxSize = DEFAULT_MAX_SIZE;
		
		minAndMax = Tools.split(gd.getNextString(), " -");
		double minc = Tools.parseDouble(minAndMax[0]);
		double maxc = minAndMax.length==2?Tools.parseDouble(minAndMax[1]):Double.NaN;
		minCircularity = Double.isNaN(minc)?0.0:minc;
		maxCircularity = Double.isNaN(maxc)?1.0:maxc;
		if (minCircularity<0.0 || minCircularity>1.0) minCircularity = 0.0;
		if (maxCircularity<minCircularity || maxCircularity>1.0) maxCircularity = 1.0;
		if (minCircularity==1.0 && maxCircularity==1.0) minCircularity = 0.0;
		
		if (gd.invalidNumber()) {
			IJ.error("Bins invalid.");
			canceled = true;
			return false;
		}
		staticMinSize = minSize;
		staticMaxSize = maxSize;
		showChoice = gd.getNextChoiceIndex();
		if (gd.getNextBoolean())
			options |= SHOW_RESULTS; else options &= ~SHOW_RESULTS;
		if (gd.getNextBoolean())
			options |= EXCLUDE_EDGE_PARTICLES; else options &= ~EXCLUDE_EDGE_PARTICLES;
		if (gd.getNextBoolean())
			options |= CLEAR_WORKSHEET; else options &= ~CLEAR_WORKSHEET;
		if (gd.getNextBoolean())
			options |= INCLUDE_HOLES; else options &= ~INCLUDE_HOLES;
		if (gd.getNextBoolean())
			options |= DISPLAY_SUMMARY; else options &= ~DISPLAY_SUMMARY;
		if (gd.getNextBoolean())
			options |= RECORD_STARTS; else options &= ~RECORD_STARTS;
		if (gd.getNextBoolean())
			options |= ADD_TO_MANAGER; else options &= ~ADD_TO_MANAGER;
		staticOptions = options;
		options |= SHOW_PROGRESS;
		if ((options&DISPLAY_SUMMARY)!=0)
			Analyzer.setMeasurements(Analyzer.getMeasurements()|AREA);
		return true;
	}
	
	boolean updateMacroOptions() {
		String options = Macro.getOptions();
		int index = options.indexOf("maximum=");
		if (index==-1) return false;
		index +=8;
		int len = options.length();
		while (index<len-1 && options.charAt(index)!=' ')
			index++;
		if (index==len-1) return false;
		int min = (int)Tools.parseDouble(Macro.getValue(options, "minimum", "1"));
		int max = (int)Tools.parseDouble(Macro.getValue(options, "maximum", "999999"));
		options = "size="+min+"-"+max+options.substring(index, len);
		Macro.setOptions(options);
		return true;
	}

	/** Performs particle analysis on the specified image. Returns
		false if there is an error. */
	public boolean analyze(ImagePlus imp) {
		return analyze(imp, imp.getProcessor());
	}

	/** Performs particle analysis on the specified ImagePlus and
		ImageProcessor. Returns false if there is an error. */
	public boolean analyze(ImagePlus imp, ImageProcessor ip) {
		showResults = (options&SHOW_RESULTS)!=0;
		excludeEdgeParticles = (options&EXCLUDE_EDGE_PARTICLES)!=0;
		resetCounter = (options&CLEAR_WORKSHEET)!=0;
		showProgress = (options&SHOW_PROGRESS)!=0;
		floodFill = (options&INCLUDE_HOLES)==0;
		recordStarts = (options&RECORD_STARTS)!=0;
		addToManager = (options&ADD_TO_MANAGER)!=0;
		displaySummary = (options&DISPLAY_SUMMARY)!=0;
		if ((options&SHOW_OUTLINES)!=0)
			showChoice = OUTLINES;
		if ((options&SHOW_NONE)!=0)
			showChoice = NOTHING;
		ip.snapshot();
		ip.setProgressBar(null);
		if (Analyzer.isRedirectImage()) {
			redirectImp = Analyzer.getRedirectImage(imp);
			if (redirectImp==null) return false;
			int depth = redirectImp.getStackSize();
			if (depth>1 && depth==imp.getStackSize()) {
				ImageStack redirectStack = redirectImp.getStack();
				redirectIP = redirectStack.getProcessor(imp.getCurrentSlice());
			} else
				redirectIP = redirectImp.getProcessor();
		}
		if (!setThresholdLevels(imp, ip))
            return false;
		width = ip.getWidth();
		height = ip.getHeight();
		if (showChoice!=NOTHING) {
			if (slice==1)
				outlines = new ImageStack(width, height);
			drawIP = new ByteProcessor(width, height);
			if (showChoice==MASKS)
				drawIP.invertLut();
			else if (showChoice==OUTLINES) {
				if (customLut==null)
					makeCustomLut();
				drawIP.setColorModel(customLut);
				drawIP.setFont(new Font("SansSerif", Font.PLAIN, 9));

			}
			outlines.addSlice(null, drawIP);
			drawIP.setColor(Color.white);
			drawIP.fill();
			drawIP.setColor(Color.black);
		}
		calibration = redirectImp!=null?redirectImp.getCalibration():imp.getCalibration();
		
		if (rt==null) {
			rt = Analyzer.getResultsTable();
			analyzer = new Analyzer(imp);
		} else
			analyzer = new Analyzer(imp, measurements, rt);
		if (resetCounter && slice==1) {
			if (!Analyzer.resetCounter())
                return false;
		}
		beginningCount = Analyzer.getCounter();

		byte[] pixels = null;
		if (ip instanceof ByteProcessor)
			pixels = (byte[])ip.getPixels();
		if (r==null) {
			r = ip.getRoi();
			mask = ip.getMask();
			if (displaySummary) {
				if (mask!=null)
					totalArea = ImageStatistics.getStatistics(ip, AREA, calibration).area;
				else
					totalArea = r.width*calibration.pixelWidth*r.height*calibration.pixelHeight;
			}
		}
		minX=r.x; maxX=r.x+r.width; minY=r.y; maxY=r.y+r.height;
		if (r.width<width || r.height<height || mask!=null) {
			if (!eraseOutsideRoi(ip, r, mask)) return false;
		}
		int offset;
		double value;
		int inc = Math.max(r.height/25, 1);
		int mi = 0;
		ImageWindow win = imp.getWindow();
		if (win!=null)
			win.running = true;
		if (measurements==0)
			measurements = Analyzer.getMeasurements();
		if (showChoice==ELLIPSES)
			measurements |= ELLIPSE;
		measurements &= ~LIMIT;  // ignore "Limit to Threshold"
		roiNeedsImage = (measurements&PERIMETER)!=0 || (measurements&CIRCULARITY)!=0 || (measurements&FERET)!=0;
		particleCount = 0;
		wand = new Wand(ip);
		pf = new PolygonFiller();
		if (floodFill) {
            ImageProcessor ipf = ip.duplicate();
            ipf.setValue(fillColor);
            ff = new FloodFiller(ipf);
        }

		for (int y=r.y; y<(r.y+r.height); y++) {
			offset = y*width;
			for (int x=r.x; x<(r.x+r.width); x++) {
				if (pixels!=null)
					value = pixels[offset+x]&255;
				else if (imageType==SHORT)
					value = ip.getPixel(x, y);
				else
					value = ip.getPixelValue(x, y);
				if (value>=level1 && value<=level2)
					analyzeParticle(x,y,imp,ip);
			}
			if (showProgress && ((y%inc)==0))
				IJ.showProgress((double)(y-r.y)/r.height);
			if (win!=null)
				canceled = !win.running;
			if (canceled) {
				Macro.abort();
				break;
			}
		}
		if (showProgress)
			IJ.showProgress(1.0);
		imp.killRoi();
		ip.resetRoi();
		ip.reset();
		if (displaySummary && processStack && IJ.getInstance()!=null)
			updateSliceSummary();
		if (addToManager && roiManager!=null) {
			ImageCanvas ic = imp.getCanvas();
			if (ic!=null) ic.setShowAllROIs(true);
		}
		totalCount += particleCount;
		if (!canceled)
			showResults();
		return true;
	}
	
	void updateSliceSummary() {
		float[] areas = rt.getColumn(ResultsTable.AREA);
		String label = imp.getStack().getShortSliceLabel(slice);
		label = label!=null&&!label.equals("")?label:""+slice;
		String aLine;
		if (areas!=null) {
			double sum = 0.0;
			int start = areas.length-particleCount;
			if (start<0)
				return;
			for (int i=start; i<areas.length; i++)
				sum += areas[i];
			int places = Analyzer.getPrecision();
			Calibration cal = imp.getCalibration();
			String total = "\t"+IJ.d2s(sum,places);
			String average = "\t"+IJ.d2s(sum/particleCount,places);
			String fraction = "\t"+IJ.d2s(sum*100.0/totalArea,1);
			aLine = label+"\t"+particleCount+total+average+fraction;
		} else
			aLine = label+"\t"+particleCount;
		if (tw==null) {
			String title = "Summary of "+imp.getTitle();
			String headings = "Slice\tCount\tTotal Area\tAverage Size\tArea Fraction";
			tw = new TextWindow(title, headings, aLine, 180, 360);
		} else
			tw.append(aLine);
	}

	boolean eraseOutsideRoi(ImageProcessor ip, Rectangle r, ImageProcessor mask) {
		int width = ip.getWidth();
		int height = ip.getHeight();
		ip.setRoi(r);
 		if (excludeEdgeParticles && polygon!=null) {
			ImageStatistics stats = ImageStatistics.getStatistics(ip, MIN_MAX, null);
			if (fillColor>=stats.min && fillColor<=stats.max) {
				double replaceColor = level1-1.0;
				if (replaceColor<0.0 || replaceColor==fillColor) {
					replaceColor = level2+1.0;
					int maxColor = imageType==BYTE?255:65535;
					if (replaceColor>maxColor || replaceColor==fillColor) {
						IJ.error("Particle Analyzer", "Unable to remove edge particles");
						return false;
					}
				}
				for (int y=minY; y<maxY; y++) {
					for (int x=minX; x<maxX; x++) {
						int v  = ip.getPixel(x, y);
						if (v==fillColor) ip.putPixel(x, y, (int)replaceColor);
					}
				}
			}
 		}
		ip.setValue(fillColor);		
		if (mask!=null) {
			mask = mask.duplicate();
			mask.invert();
			ip.fill(mask);
 		} 		
 		ip.setRoi(0, 0, r.x, height);
 		ip.fill();
 		ip.setRoi(r.x, 0, r.width, r.y);
 		ip.fill();
 		ip.setRoi(r.x, r.y+r.height, r.width, height-(r.y+r.height));
 		ip.fill();
 		ip.setRoi(r.x+r.width, 0, width-(r.x+r.width), height);
 		ip.fill();
 		ip.resetRoi();
		//IJ.log("erase: "+fillColor+"  "+level1+"  "+level2+"  "+excludeEdgeParticles);
		//(new ImagePlus("ip2", ip.duplicate())).show();
		return true;
	}

	boolean setThresholdLevels(ImagePlus imp, ImageProcessor ip) {
		double t1 = ip.getMinThreshold();
		double t2 = ip.getMaxThreshold();
		boolean invertedLut = imp.isInvertedLut();
		boolean byteImage = ip instanceof ByteProcessor;
		if (ip instanceof ShortProcessor)
			imageType = SHORT;
		else if (ip instanceof FloatProcessor)
			imageType = FLOAT;
		else
			imageType = BYTE;
		if (t1==ip.NO_THRESHOLD) {
			ImageStatistics stats = imp.getStatistics();
			if (imageType!=BYTE || (stats.histogram[0]+stats.histogram[255]!=stats.pixelCount)) {
				IJ.error("Particle Analyzer",
					"A thresholded image or 8-bit binary image is\n"
					+"required. Threshold levels can be set using\n"
					+"the Image->Adjust->Threshold tool.");
				canceled = true;
				return false;
			}
			if (invertedLut) {
				level1 = 255;
				level2 = 255;
				fillColor = 64;
			} else {
				level1 = 0;
				level2 = 0;
				fillColor = 192;
			}
		} else {
			level1 = t1;
			level2 = t2;
			if (imageType==BYTE) {
				if (level1>0)
					fillColor = 0;
				else if (level2<255)
					fillColor = 255;
			} else if (imageType==SHORT) {
				if (level1>0)
					fillColor = 0;
				else if (level2<65535)
					fillColor = 65535;
			} else if (imageType==FLOAT)
					fillColor = -Float.MAX_VALUE;
			else
				return false;
		}
		imageType2 = imageType;
		if (redirectIP!=null) {
			if (redirectIP instanceof ShortProcessor)
				imageType2 = SHORT;
			else if (redirectIP instanceof FloatProcessor)
				imageType2 = FLOAT;
			else if (redirectIP instanceof ColorProcessor)
				imageType2 = RGB;
			else
				imageType2 = BYTE;
		}
		return true;
	}
	
	int counter = 0;
	
	void analyzeParticle(int x, int y, ImagePlus imp, ImageProcessor ip) {
		//Wand wand = new Wand(ip);
		ImageProcessor ip2 = redirectIP!=null?redirectIP:ip;
		wand.autoOutline(x,y, level1, level2);
		if (wand.npoints==0)
			{IJ.log("wand error: "+x+" "+y); return;}
		Roi roi = new PolygonRoi(wand.xpoints, wand.ypoints, wand.npoints, Roi.TRACED_ROI);
		Rectangle r = roi.getBounds();
		if (r.width>1 && r.height>1) {
			PolygonRoi proi = (PolygonRoi)roi;
			pf.setPolygon(proi.getXCoordinates(), proi.getYCoordinates(), proi.getNCoordinates());
			ip2.setMask(pf.getMask(r.width, r.height));
			if (floodFill) ff.particleAnalyzerFill(x,y, level1, level2, ip2.getMask(), r);
		}
		ip2.setRoi(r);
		ip.setValue(fillColor);
		ImageStatistics stats = getStatistics(ip2, measurements, calibration);
		boolean include = true;
		if (excludeEdgeParticles) {
			if (r.x==minX||r.y==minY||r.x+r.width==maxX||r.y+r.height==maxY)
				include = false;
			if (polygon!=null) {
				Rectangle bounds = roi.getBounds();
				int x1=bounds.x+wand.xpoints[wand.npoints-1];
				int y1=bounds.y+wand.ypoints[wand.npoints-1];
				int x2, y2;
				for (int i=0; i<wand.npoints; i++) {
					x2=bounds.x+wand.xpoints[i];
					y2=bounds.y+wand.ypoints[i];
					if (!polygon.contains(x2, y2))
						{include = false; break;}
					if ((x1==x2 && ip.getPixel(x1,y1-1)==fillColor) || (y1==y2 && ip.getPixel(x1-1,y1)==fillColor))
						{include = false; break;}
					x1=x2; y1=y2;
				}
			}
		}
		ImageProcessor mask = ip2.getMask();
		if (minCircularity>0.0 || maxCircularity<1.0) {
			double perimeter = roi.getLength();
			double circularity = perimeter==0.0?0.0:4.0*Math.PI*(stats.pixelCount/(perimeter*perimeter));
			if (circularity>1.0) circularity = 1.0;
			//IJ.log(circularity+"  "+perimeter+"  "+stats.area);
			if (circularity<minCircularity || circularity>maxCircularity) include = false;
		}
		if (stats.pixelCount>=minSize && stats.pixelCount<=maxSize && include) {
			particleCount++;
			if (roiNeedsImage)
				roi.setImage(imp);
			saveResults(stats, roi);
			if (showChoice!=NOTHING)
				drawParticle(drawIP, roi, stats, mask);
		}
		if (redirectIP!=null)
			ip.setRoi(r);
		ip.fill(mask);
	}

    ImageStatistics getStatistics(ImageProcessor ip, int mOptions, Calibration cal) {
    	switch (imageType2) {
        	case BYTE:
            	return new ByteStatistics(ip, mOptions, cal);
        	case SHORT:
            	return new ShortStatistics(ip, mOptions, cal);
            case FLOAT:
            	return new FloatStatistics(ip, mOptions, cal);
            case RGB:
            	return new ColorStatistics(ip, mOptions, cal);
			default:
				return null;
		}
    }

	/** Saves statistics for one particle in a results table. This is
		a method subclasses may want to override. */
	protected void saveResults(ImageStatistics stats, Roi roi) {
		analyzer.saveResults(stats, roi);
		if (recordStarts) {
			int coordinates = ((PolygonRoi)roi).getNCoordinates();
			Rectangle r = roi.getBounds();
			int x = r.x+((PolygonRoi)roi).getXCoordinates()[coordinates-1];
			int y = r.y+((PolygonRoi)roi).getYCoordinates()[coordinates-1];
			rt.addValue("XStart", x);
			rt.addValue("YStart", y);
		}
		if (addToManager) {
			if (roiManager==null) {
				Frame frame = WindowManager.getFrame("ROI Manager");
				if (frame==null)
					IJ.run("ROI Manager...");
				frame = WindowManager.getFrame("ROI Manager");
				if (frame==null || !(frame instanceof RoiManager))
					{addToManager=false; return;}
				roiManager = (RoiManager)frame;
				if (resetCounter)
					roiManager.runCommand("reset");
			}
			roiManager.add(imp, roi, Analyzer.getCounter());
		}
		if (showResults)
			analyzer.displayResults();
	}
	
	/** Draws a selected particle in a separate image.  This is
		another method subclasses may want to override. */
	protected void drawParticle(ImageProcessor drawIP, Roi roi,
	ImageStatistics stats, ImageProcessor mask) {
		switch (showChoice) {
			case MASKS: drawFilledParticle(drawIP, roi, mask); break;
			case OUTLINES: drawOutline(drawIP, roi, rt.getCounter()); break;
			case ELLIPSES: drawEllipse(drawIP, stats, rt.getCounter()); break;
			default:
		}
	}

	void drawFilledParticle(ImageProcessor ip, Roi roi, ImageProcessor mask) {
		//IJ.write(roi.getBounds()+" "+mask.length);
		ip.setRoi(roi.getBounds());
		ip.fill(mask);
	}

	void drawOutline(ImageProcessor ip, Roi roi, int count) {
		Rectangle r = roi.getBounds();
		int nPoints = ((PolygonRoi)roi).getNCoordinates();
		int[] xp = ((PolygonRoi)roi).getXCoordinates();
		int[] yp = ((PolygonRoi)roi).getYCoordinates();
		int x=r.x, y=r.y;
		ip.setValue(0.0);
		ip.moveTo(x+xp[0], y+yp[0]);
		for (int i=1; i<nPoints; i++)
			ip.lineTo(x+xp[i], y+yp[i]);
		ip.lineTo(x+xp[0], y+yp[0]);
		String s = IJ.d2s(count,0);
		ip.moveTo(r.x+r.width/2-ip.getStringWidth(s)/2, r.y+r.height/2+4);
		ip.setValue(1.0);
		ip.drawString(s);
	}

	void drawEllipse(ImageProcessor ip, ImageStatistics stats, int count) {
		stats.drawEllipse(ip);
	}

	void showResults() {
		int count = rt.getCounter();
		if (count==0)
			return;
		boolean lastSlice = !processStack||slice==imp.getStackSize();
		if (displaySummary && lastSlice && rt==Analyzer.getResultsTable() && imp!=null) {
			showSummary();
		}
		if (outlines!=null && lastSlice) {
			String title = imp!=null?imp.getTitle():"Outlines";
			String prefix = showChoice==MASKS?"Mask of ":"Drawing of ";
			new ImagePlus(prefix+title, outlines).show();
		}
		if (showResults && !processStack) {
			Analyzer.firstParticle = beginningCount;
			Analyzer.lastParticle = Analyzer.getCounter()-1;
		} else
			Analyzer.firstParticle = Analyzer.lastParticle = 0;
	}
	
	void showSummary() {
		String s = "";
		s += "Threshold: ";
		if ((int)level1==level1 && (int)level2==level2)
			s += (int)level1+"-"+(int)level2+"\n";
		else
			s += IJ.d2s(level1,2)+"-"+IJ.d2s(level2,2)+"\n";
		s += "Count: " + totalCount+"\n";
		float[] areas = rt.getColumn(ResultsTable.AREA);
		String aLine;
		if (areas!=null) {
			double sum = 0.0;
			int start = areas.length-totalCount;
			if (start<0)
				return;
			for (int i=start; i<areas.length; i++)
				sum += areas[i];
			int places = Analyzer.getPrecision();
			Calibration cal = imp.getCalibration();
			String unit = cal.getUnit();
			String total = IJ.d2s(sum,places);
			s += "Total Area: "+total+" "+unit+"^2\n";
			String average = IJ.d2s(sum/totalCount,places);
			s += "Average Size: "+IJ.d2s(sum/totalCount,places)+" "+unit+"^2\n";
			if (processStack) totalArea *= imp.getStackSize();
			String fraction = IJ.d2s(sum*100.0/totalArea, Math.max(places,2));
			s += "Area Fraction: "+fraction+"%";
			aLine = " "+"\t"+totalCount+"\t"+total+"\t"+average+"\t"+fraction;
		} else
			aLine = " "+"\t"+totalCount;			
		if (tw!=null) {
			tw.append("");
			tw.append(aLine);
		} else
			new TextWindow("Summary of "+imp.getTitle(), s, 300, 200);
	}

	int getColumnID(String name) {
		int id = rt.getFreeColumn(name);
		if (id==ResultsTable.COLUMN_IN_USE)
			id = rt.getColumnIndex(name);
		return id;
	}

	void makeCustomLut() {
		IndexColorModel cm = (IndexColorModel)LookUpTable.createGrayscaleColorModel(false);
		byte[] reds = new byte[256];
		byte[] greens = new byte[256];
		byte[] blues = new byte[256];
		cm.getReds(reds);
		cm.getGreens(greens);
		cm.getBlues(blues);
		reds[1] =(byte) 255;
		greens[1] = (byte)0;
		blues[1] = (byte)0;
		customLut = new IndexColorModel(8, 256, reds, greens, blues);
	}

	/** Called once when ImageJ quits. */
	public static void savePreferences(Properties prefs) {
		prefs.put(OPTIONS, Integer.toString(staticOptions));
	}

}
