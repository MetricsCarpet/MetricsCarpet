package ij.macro;
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.plugin.frame.*;
import ij.text.*;
import ij.io.*;
import ij.util.Tools;
import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.io.*;
import java.awt.event.KeyEvent;
import java.lang.reflect.*;
import java.net.URL;
import java.awt.datatransfer.*;

/** This class implements the built-in macro functions. */
public class Functions implements MacroConstants, Measurements {
	Interpreter interp;
	Program pgm;
    boolean updateNeeded;
    boolean autoUpdate = true;
    ImagePlus defaultImp;
    ImageProcessor defaultIP;
    int imageType;
    boolean colorSet, fontSet;
    Color defaultColor;
    double defaultValue = Double.NaN;
    Plot plot;
    static int plotID;
    int justification = ImageProcessor.LEFT_JUSTIFY;
    Font font;
    GenericDialog gd;
    PrintWriter writer;
    boolean altKeyDown, shiftKeyDown;
    boolean antialiasedText;
    StringBuffer buffer;
    
    boolean saveSettingsCalled;
	boolean usePointerCursor, hideProcessStackDialog;
	float divideByZeroValue;
    int jpegQuality;
    int lineWidth;
    boolean doScaling;
    boolean weightedColor;
    double[] weights;
	boolean interpolateScaledImages, open100Percent, blackCanvas;
	boolean useJFileChooser,debugMode;
	Color foregroundColor, backgroundColor, roiColor;
	boolean pointAutoMeasure, requireControlKey, useInvertingLut;
	boolean doubleBuffer, disablePopup;
	int measurements;
	int decimalPlaces;
	boolean blackBackground;

	Functions(Interpreter interp, Program pgm) {
		this.interp = interp;
		this.pgm = pgm;
	}
 
	void doFunction(int type) {
		switch (type) {
			case RUN: doRun(); break;
			case SELECT: IJ.selectWindow(getStringArg()); resetImage(); break;
			case WAIT: IJ.wait((int)getArg()); break;
			case BEEP: interp.getParens(); IJ.beep(); break;
			case RESET_MIN_MAX: interp.getParens(); IJ.resetMinAndMax(); resetImage(); break;
			case RESET_THRESHOLD: interp.getParens(); IJ.resetThreshold(); resetImage(); break;
			case PRINT: print(); break;
			case WRITE: IJ.write(getStringArg()); break;
			case DO_WAND: IJ.doWand((int)getFirstArg(), (int)getLastArg()); resetImage(); break;
			case SET_MIN_MAX: IJ.setMinAndMax(getFirstArg(), getLastArg()); resetImage(); break;
			case SET_THRESHOLD: setThreshold(); break;
			case SET_TOOL: IJ.setTool((int)getArg()); break;
			case SET_FOREGROUND: setForegroundColor(); break;
			case SET_BACKGROUND: setBackgroundColor(); break;
			case SET_COLOR: setColor(); break;
			case MAKE_LINE: makeLine(); break;
			case MAKE_OVAL: makeOval(); break;
			case MAKE_RECTANGLE: makeRectangle(); break;
			case DUMP: interp.dump(); break;
			case LINE_TO: lineTo(); break;
			case MOVE_TO: moveTo(); break;
			case DRAW_LINE: drawLine(); break;
			case REQUIRES: requires(); break;
			case AUTO_UPDATE: autoUpdate = getBooleanArg(); break;
			case UPDATE_DISPLAY: interp.getParens(); updateDisplay(); break;
			case DRAW_STRING: drawString(); break;
			case SET_PASTE_MODE: IJ.setPasteMode(getStringArg()); break;
			case DO_COMMAND: doCommand(); break;
			case SHOW_STATUS: IJ.showStatus(getStringArg()); interp.statusUpdated=true; break;
			case SHOW_PROGRESS: showProgress(); break;
			case SHOW_MESSAGE: showMessage(false); break;
			case SHOW_MESSAGE_WITH_CANCEL: showMessage(true); break;
			case SET_PIXEL: case PUT_PIXEL: setPixel(); break;
			case SNAPSHOT: case RESET: case FILL: doIPMethod(type); break;
			case SET_LINE_WIDTH: getProcessor().setLineWidth((int)getArg()); break;
			case CHANGE_VALUES: changeValues(); break;
			case SELECT_IMAGE: selectImage(); break;
			case EXIT: exit(); break;
			case SET_LOCATION: setLocation(); break;
			case GET_CURSOR_LOC: getCursorLoc(); break;
			case GET_LINE: getLine(); break;
			case GET_VOXEL_SIZE: getVoxelSize(); break;
			case GET_HISTOGRAM: getHistogram(); break;
			case GET_BOUNDING_RECT: case GET_BOUNDS: getBounds(); break;
			case GET_LUT: getLut(); break;
			case SET_LUT: setLut(); break;
			case GET_COORDINATES: getCoordinates(); break;
			case MAKE_SELECTION: makeSelection(); break;
			case SET_RESULT: setResult(); break;
			case UPDATE_RESULTS: updateResults(); break;
			case SET_BATCH_MODE: setBatchMode(); break;
			case PLOT: doPlot(); break;
			case SET_JUSTIFICATION: setJustification(); break;
			case SET_Z_COORDINATE: setZCoordinate(); break;
			case GET_THRESHOLD: getThreshold(); break;
			case GET_PIXEL_SIZE: getPixelSize(); break;
			case SETUP_UNDO: interp.getParens(); Undo.setup(Undo.TRANSFORM, getImage()); break;
			case SAVE_SETTINGS: saveSettings(); break;
			case RESTORE_SETTINGS: restoreSettings(); break;
			case SET_KEY_DOWN: setKeyDown(); break;
			case OPEN: open(); break;
			case SET_FONT: setFont(); break;
			case GET_MIN_AND_MAX: getMinAndMax(); break;
			case CLOSE: close(); break;
			case SET_SLICE: setSlice(); break;
			case NEW_IMAGE: newImage(); break;
			case SAVE: IJ.save(getStringArg()); break;
			case SAVE_AS: saveAs(); break;
			case SET_AUTO_THRESHOLD: setAutoThreshold(); break;
			case RENAME: resetImage(); getImage().setTitle(getStringArg()); break;
			case GET_STATISTICS: getStatistics(true); break;
			case GET_RAW_STATISTICS: getStatistics(false); break;
			case FLOOD_FILL: floodFill(); break;
			case RESTORE_PREVIOUS_TOOL: restorePreviousTool(); break;
			case SET_VOXEL_SIZE: setVoxelSize(); break;
			case GET_LOCATION_AND_SIZE: getLocationAndSize(); break;
			case GET_DATE_AND_TIME: getDateAndTime(); break;
			case SET_METADATA: setMetadata(); break;
			case CALCULATOR: imageCalculator(); break;
			case SET_RGB_WEIGHTS: setRGBWeights(); break;
			case MAKE_POLYGON: makePolygon(); break;
			case SET_SELECTION_NAME: setSelectionName(); break;
			case DRAW_RECT: case FILL_RECT: case DRAW_OVAL: case FILL_OVAL: drawOrFill(type); break;
			case SET_OPTION: setOption(); break;
			case SHOW_TEXT: showText(); break;
			case SET_SELECTION_LOC: setSelectionLocation(); break;
			case GET_DIMENSIONS: getDimensions(); break;
		}
	}
	
	final double getFunctionValue(int type) {
		double value = 0.0;
		switch (type) {
			case GET_PIXEL: value = getPixel(); break;
			case ABS: case COS: case EXP: case FLOOR: case LOG: case ROUND: 
			case SIN: case SQRT: case TAN: case ATAN: case ASIN: case ACOS:
				value = math(type);
				break;
			case MAX_OF: case MIN_OF: case POW: case ATAN2: value=math2(type); break;
			case GET_TIME: interp.getParens(); value=System.currentTimeMillis(); break;
			case GET_WIDTH: interp.getParens(); value=getImage().getWidth(); break;
			case GET_HEIGHT: interp.getParens(); value=getImage().getHeight(); break;
			case RANDOM: value=random(); break;
			case GET_COUNT: case NRESULTS: value=getResultsCount(); break;
			case GET_RESULT: value=getResult(); break;
			case GET_NUMBER: value=getNumber(); break;
			case NIMAGES: value=getImageCount(); break;
			case NSLICES: value=getStackSize(); break;
			case LENGTH_OF: value=lengthOf(); break;
			case GET_ID: interp.getParens(); resetImage(); value=getImage().getID(); break;
			case BIT_DEPTH: interp.getParens(); value = getImage().getBitDepth(); break;
			case SELECTION_TYPE: value=getSelectionType(); break;
			case IS_OPEN: value=isOpen(); break;
			case IS_ACTIVE: value=isActive(); break;
			case INDEX_OF: value=indexOf(); break;
			case LAST_INDEX_OF: value=getFirstString().lastIndexOf(getLastString()); break;
			case CHAR_CODE_AT: value=getFirstString().charAt((int)getLastArg()); break;
			case GET_BOOLEAN: value=getBoolean(); break;
			case STARTS_WITH: case ENDS_WITH: value = startsWithEndsWith(type); break;
			case IS_NAN: value = Double.isNaN(getArg())?1:0; break;
			case GET_ZOOM: value = getZoom(); break;
			case PARSE_FLOAT: value = parseDouble(getStringArg()); break;
			case PARSE_INT: value = parseInt(); break;
			case IS_KEY_DOWN: value=isKeyDown(); break;
			case GET_SLICE_NUMBER: interp.getParens(); value=getImage().getCurrentSlice(); break;
			case SCREEN_WIDTH: case SCREEN_HEIGHT: value = getScreenDimension(type); break;
			case CALIBRATE: value = getImage().getCalibration().getCValue(getArg()); break;
			case ROI_MANAGER: value = roiManager(); break;
			case TOOL_ID: interp.getParens(); value = Toolbar.getToolId(); break;
			case IS: value = is(); break;
			default:
				interp.error("Numeric function expected");
		}
		return value;
	}

	String getStringFunction(int type) {
		String str;
		switch (type) {
			case D2S: str = d2s(); break;
			case TO_HEX: str = toString(16); break;
			case TO_BINARY: str = toString(2); break;
			case GET_TITLE: interp.getParens(); resetImage(); str=getImage().getTitle(); break;
			case GET_STRING: str = getStringDialog(); break;
			case SUBSTRING: str = substring(); break;
			case FROM_CHAR_CODE: str = fromCharCode(); break;
			case GET_INFO: str = getInfo(); break;			
			case GET_IMAGE_INFO: interp.getParens(); str = getImageInfo(); break;			
			case GET_DIRECTORY: str = getDirectory(); break;
			case GET_ARGUMENT: interp.getParens(); str=interp.argument!=null?interp.argument:""; break;
			case TO_LOWER_CASE: str = getStringArg().toLowerCase(Locale.US); break;
			case TO_UPPER_CASE: str = getStringArg().toUpperCase(Locale.US); break;
			case RUN_MACRO: str = runMacro(false); break;
			case EVAL: str = runMacro(true); break;
			case TO_STRING: str = getStringArg(); break;
			case REPLACE: str = replace(); break;
			case DIALOG: str = doDialog(); break;
			case GET_METADATA: str = getMetadata(); break;
			case FILE: str = doFile(); break;
			case SELECTION_NAME: str = selectionName(); break;
			case GET_VERSION: interp.getParens();  str = IJ.getVersion(); break;
			case GET_RESULT_LABEL: str = getResultLabel(); break;
			case CALL: str = call(); break;
			case STRING: str = doString(); break;
			default:
				str="";
				interp.error("String function expected");
		}
		return str;
	}

	Variable[] getArrayFunction(int type) {
		Variable[] array;
		switch (type) {
			case GET_PROFILE: array=getProfile(); break;
			case NEW_ARRAY: array = newArray(); break;
			case SPLIT: array = split(); break;
			case GET_FILE_LIST: array = getFileList(); break;
			case GET_FONT_LIST: array = getFontList(); break;
			case NEW_MENU: array = newMenu(); break;
			case GET_LIST: array = getList(); break;
			default:
				array = null;
				interp.error("Array function expected");
		}
		return array;
	}

	final double math(int type) {
		double arg = getArg();
		switch (type) {
			case ABS: return Math.abs(arg);
			case COS: return Math.cos(arg);
			case EXP: return Math.exp(arg);
			case FLOOR: return Math.floor(arg);
			case LOG: return Math.log(arg);
			case ROUND: return Math.round(arg);
			case SIN: return Math.sin(arg);
			case SQRT: return Math.sqrt(arg);
			case TAN: return Math.tan(arg);
			case ATAN: return Math.atan(arg);
			case ASIN: return Math.asin(arg);
			case ACOS: return Math.acos(arg);
			default: return 0.0;
		}
	}

	final double math2(int type) {
		double a1 = getFirstArg();
		double a2 = getLastArg();
		switch (type) {
			case MIN_OF: return Math.min(a1, a2);
			case MAX_OF: return Math.max(a1, a2);
			case POW: return Math.pow(a1, a2);
			case ATAN2: return Math.atan2(a1, a2);
			default: return 0.0;
		}
	}

	final String getString() {
		String str = interp.getStringTerm();
		while (true) {
			interp.getToken();
			if (interp.token=='+')
				str += interp.getStringTerm();
			else {
				interp.putTokenBack();
				break;
			}
		};
		return str;
	}

	final boolean isStringFunction() {
		Symbol symbol = pgm.table[interp.tokenAddress];
		return symbol.type==D2S;
	}

	final double getArg() {
		interp.getLeftParen();
		double arg = interp.getExpression();
		interp.getRightParen();
		return arg;
	}

	final double getFirstArg() {
		interp.getLeftParen();
		return interp.getExpression();
	}

	final double getNextArg() {
		interp.getComma();
		return interp.getExpression();
	}

	final double getLastArg() {
		interp.getComma();
		double arg = interp.getExpression();
		interp.getRightParen();
		return arg;
	}

	String getStringArg() {
		interp.getLeftParen();
		String arg = getString();
		interp.getRightParen();
		return arg;
	}

	final String getFirstString() {
		interp.getLeftParen();
		return getString();
	}

	final String getNextString() {
		interp.getComma();
		return getString();
	}

	final String getLastString() {
		interp.getComma();
		String arg = getString();
		interp.getRightParen();
		return arg;
	}

	boolean getBooleanArg() {
		interp.getLeftParen();
		double arg = interp.getBooleanExpression();
		interp.checkBoolean(arg);
		interp.getRightParen();
		return arg==0?false:true;
	}

	final Variable getFirstVariable() {
		interp.getLeftParen();
		return getVariable();
	}

	final Variable getNextVariable() {
		interp.getComma();
		return getVariable();
	}

	final Variable getLastVariable() {
		interp.getComma();
		Variable v = getVariable();
		interp.getRightParen();
		return v;
	}

	final Variable getVariable() {
		interp.getToken();
		if (interp.token!=WORD)
			interp.error("Variable expected");
		Variable v = interp.lookupLocalVariable(interp.tokenAddress);
		if (v==null)
				v = interp.push(interp.tokenAddress, 0.0, null, interp);
		Variable[] array = v.getArray();
		if (array!=null) {
			int index = interp.getIndex();
			checkIndex(index, 0, array.length-1);
			v = array[index]; 
		}
		return v;
	}

	final Variable getFirstArrayVariable() {
		interp.getLeftParen();
		return getArrayVariable();
	}

	final Variable getNextArrayVariable() {
		interp.getComma();
		return getArrayVariable();
	}

	final Variable getLastArrayVariable() {
		interp.getComma();
		Variable v = getArrayVariable();
		interp.getRightParen();
		return v;
	}

	final Variable getArrayVariable() {
		interp.getToken();
		if (interp.token!=WORD)
			interp.error("Variable expected");
		Variable v = interp.lookupLocalVariable(interp.tokenAddress);
		if (v==null)
				v = interp.push(interp.tokenAddress, 0.0, null, interp);
		return v;
	}

	final double[] getFirstArray() {
		interp.getLeftParen();
		return getNumericArray();
	}

	final double[] getNextArray() {
		interp.getComma();
		return getNumericArray();
	}

	final double[] getLastArray() {
		interp.getComma();
		double[] a = getNumericArray();
		interp.getRightParen();
		return a;
	}

	double[] getNumericArray() {
		Variable[] a1 = getArray();
		double[] a2 = new double[a1.length];
		for (int i=0; i<a1.length; i++)
			a2[i] = a1[i].getValue();
		return a2;
	}

	String[] getStringArray() {
		Variable[] a1 = getArray();
		String[] a2 = new String[a1.length];
		for (int i=0; i<a1.length; i++) {
			String s = a1[i].getString();
			if (s==null) s = "" + a1[i].getValue();
			a2[i] = s;
		}
		return a2;
	}

	Variable[] getArray() {
		interp.getToken();
		boolean newArray = interp.token==ARRAY_FUNCTION && pgm.table[interp.tokenAddress].type==NEW_ARRAY;
		if (!(interp.token==WORD||newArray))
			interp.error("Array expected");
		Variable[] a;
		if (newArray)
			a = getArrayFunction(NEW_ARRAY);
		else {
			Variable v = interp.lookupVariable();
			a= v.getArray();
		}
		if (a==null)
			interp.error("Array expected");
		return a;
	}
		
	Color getColor() {
		String color = getString();
		color = color.toLowerCase(Locale.US);
		if (color.equals("black"))
			return Color.black;
		else if (color.equals("white"))
			return Color.white;
		else if (color.equals("red"))
			return Color.red;
		else if (color.equals("green"))
			return Color.green;
		else if (color.equals("blue"))
			return Color.blue;
		else if (color.equals("cyan"))
			return Color.cyan;
		else if (color.equals("darkgray"))
			return Color.darkGray;
		else if (color.equals("gray"))
			return Color.gray;
		else if (color.equals("lightgray"))
			return Color.lightGray;
		else if (color.equals("magenta"))
			return Color.magenta;
		else if (color.equals("orange"))
			return Color.orange;
		else if (color.equals("yellow"))
			return Color.yellow;
		else if (color.equals("pink"))
			return Color.pink;
		else
			interp.error("'red', 'green', etc. expected");
		return null;
	}

	void checkIndex(int index, int lower, int upper) {
		if (index<lower || index>upper)
			interp.error("Index ("+index+") is outside of the "+lower+"-"+upper+" range");
	}

	void doRun() {
		interp.getLeftParen();
		String arg1 = getString();
		interp.getToken();
		if (!(interp.token==')' || interp.token==','))
			interp.error("',' or ')'  expected");
		String arg2 = null;
		if (interp.token==',') {
			arg2 = getString();
			interp.getRightParen();
		}
		if (arg2!=null)
			IJ.run(arg1, arg2);
		else
			IJ.run(arg1);
		resetImage();
		IJ.setKeyUp(IJ.ALL_KEYS);
		shiftKeyDown = altKeyDown = false;
	}

	void setForegroundColor() {
		IJ.setForegroundColor((int)getFirstArg(), (int)getNextArg(), (int)getLastArg());
		resetImage(); 
		defaultColor = null;
		defaultValue = Double.NaN;
	}

	void setBackgroundColor() {
		IJ.setBackgroundColor((int)getFirstArg(), (int)getNextArg(), (int)getLastArg());
		resetImage(); 
	}

	void setColor() {
		colorSet = true;
        interp.getLeftParen();
		if (isStringArg()) {
			defaultColor = getColor();
			getProcessor().setColor(defaultColor);
			defaultValue = Double.NaN;
        	interp.getRightParen();
        	return;
		}
		double arg1 = (int)interp.getExpression();
		if (interp.nextToken()==')')
			{interp.getRightParen(); setColor(arg1); return;}
		int red=(int)arg1, green=(int)getNextArg(), blue=(int)getLastArg();
	    if (red<0) red=0; if (green<0) green=0; if (blue<0) blue=0; 
	    if (red>255) red=255; if (green>255) green=255; if (blue>255) blue=255;  
		defaultColor = new Color(red, green, blue);
		getProcessor().setColor(defaultColor);
		defaultValue = Double.NaN;
	}
	
	void setColor(double value) {
		ImageProcessor ip = getProcessor();
		ImagePlus imp = getImage();
		switch (imp.getBitDepth()) {
			case 8:
				if (value<0 || value>255)
					interp.error("Argument out of 8-bit range (0-255)");
				ip.setValue(value);
				break;
			case 16:
				if (imp.getLocalCalibration().isSigned16Bit())
					value += 32768;
				if (value<0 || value>65535)
					interp.error("Argument out of 16-bit range (0-65535)");
				ip.setValue(value);
				break;
			default:
				ip.setValue(value);
				break;
		}
		defaultValue = value;
		defaultColor = null;
	}

	void makeLine() {
		int x1 = (int)Math.round(getFirstArg());
		int y1 = (int)Math.round(getNextArg());
		int x2 = (int)Math.round(getNextArg());
		interp.getComma();
		int y2 = (int)Math.round(interp.getExpression());
		interp.getToken();
		if (interp.token==')')
			IJ.makeLine(x1, y1, x2, y2);
		else {
			int max = 200;
			int[] x = new int[max];
			int[] y = new int[max];
			x[0]=x1; y[0]=y1; x[1]=x2; y[1]=y2;
			int n = 2;
			while (interp.token==',' && n<max) {
				x[n] = (int)Math.round(interp.getExpression());
				if (n==2 && interp.nextToken()==')') {
					interp.getRightParen();
					Line.setWidth((int)x[n]);
					IJ.makeLine(x1, y1, x2, y2);
					return;
				}
				interp.getComma();
				y[n] = (int)Math.round(interp.getExpression());
				interp.getToken();
				n++;
			}
			if (n==max && interp.token!=')')
				interp.error("More than "+max+" points");
			getImage().setRoi(new PolygonRoi(x, y, n, Roi.POLYLINE));
		}
		resetImage(); 
	}

	void makeOval() {
		Roi previousRoi = getImage().getRoi();
		if (shiftKeyDown||altKeyDown) getImage().saveRoi();
		IJ.makeOval((int)getFirstArg(), (int)getNextArg(), (int)getNextArg(), (int)getLastArg());
		Roi roi = getImage().getRoi();
		if (previousRoi!=null && roi!=null)
			updateRoi(roi);
		resetImage();
	}
	
	void makeRectangle() {
		Roi previousRoi = getImage().getRoi();
		if (shiftKeyDown||altKeyDown) getImage().saveRoi();
		IJ.makeRectangle((int)getFirstArg(), (int)getNextArg(), (int)getNextArg(), (int)getLastArg());
		Roi roi = getImage().getRoi();
		if (previousRoi!=null && roi!=null)
			updateRoi(roi);
		resetImage();
	}
	
	ImagePlus getImage() {
		if (defaultImp==null)
			defaultImp = IJ.getImage();
		if (defaultImp==null)
			{interp.error("No image"); return null;}	
		if (defaultImp.getWindow()==null && IJ.getInstance()!=null && !interp.isBatchMode() && WindowManager.getTempCurrentImage()==null)
			throw new RuntimeException(Macro.MACRO_CANCELED);
		return defaultImp;
	}
	
	void resetImage() {
		defaultImp = null;
		defaultIP = null;
		colorSet = fontSet = false;
	}

	ImageProcessor getProcessor() {
		if (defaultIP==null) {
			defaultImp = getImage();
			defaultIP = defaultImp.getProcessor();
		}
		return defaultIP;
	}

	int getType() {
		if (defaultImp==null)
			defaultImp = IJ.getImage();
		imageType = defaultImp.getType();
		return imageType;
	}

	double getPixel() {
		interp.getLeftParen();
		int a1 = (int)interp.getExpression();
		interp.getComma();
		int a2 = (int)interp.getExpression();
		interp.getRightParen();
		double value = 0.0;
		ImageProcessor ip = getProcessor();
		if (getType()==ImagePlus.GRAY32)
			value = ip.getPixelValue(a1, a2);
		else
			value = ip.getPixel(a1, a2);
		return value;
	}
	
	void setZCoordinate() {
		int z = (int)getArg();
		ImagePlus imp = getImage();
		ImageStack stack = imp.getStack();
		int size = stack.getSize();
		if (z<0 || z>=size)
			interp.error("Z coordinate ("+z+") is out of 0-"+(size-1)+ " range");
		this.defaultIP = stack.getProcessor(z+1);		
	}
	
	void setPixel() {
		interp.getLeftParen();
		int a1 = (int)interp.getExpression();
		interp.getComma();
		int a2 = (int)interp.getExpression();
		interp.getComma();
		double a3 = interp.getExpression();
		interp.getRightParen();
		if (getType()==ImagePlus.GRAY32)
			getProcessor().putPixelValue(a1, a2, a3);
		else
			getProcessor().putPixel(a1, a2, (int)a3);
		updateNeeded = true;
	}

	void moveTo() {
		interp.getLeftParen();
		int a1 = (int)(interp.getExpression()+0.5);
		interp.getComma();
		int a2 = (int)(interp.getExpression()+0.5);
		interp.getRightParen();
		getProcessor().moveTo(a1, a2);
	}
	
	void lineTo() {
		interp.getLeftParen();
		int a1 = (int)(interp.getExpression()+0.5);
		interp.getComma();
		int a2 = (int)(interp.getExpression()+0.5);
		interp.getRightParen();
		ImageProcessor ip = getProcessor();
		if (!colorSet) setForegroundColor(ip);
		ip.lineTo(a1, a2);
		updateAndDraw(defaultImp);
	}

	void drawLine() {
		interp.getLeftParen();
		int x1 = (int)(interp.getExpression()+0.5);
		interp.getComma();
		int y1 = (int)(interp.getExpression()+0.5);
		interp.getComma();
		int x2 = (int)(interp.getExpression()+0.5);
		interp.getComma();
		int y2 = (int)(interp.getExpression()+0.5);
		interp.getRightParen();
		ImageProcessor ip = getProcessor();
		if (!colorSet) setForegroundColor(ip);
		ip.drawLine(x1, y1, x2, y2);
		updateAndDraw(defaultImp);
	}
	
	void setForegroundColor(ImageProcessor ip) {
		if (defaultColor!=null)
			ip.setColor(defaultColor);
		else if (!Double.isNaN(defaultValue))
			ip.setValue(defaultValue);
		else
			ip.setColor(Toolbar.getForegroundColor());
		colorSet = true;
	}

	void doIPMethod(int type) {
        interp.getParens(); 
		ImageProcessor ip = getProcessor();
		switch (type) {
			case SNAPSHOT: ip.snapshot(); break;
			case RESET:
				ip.reset();
				updateNeeded = true;
				break;
			case FILL: 
				ImagePlus imp = getImage();
				Roi roi = imp.getRoi();
				if (!colorSet) setForegroundColor(ip);
				if (roi==null) {
					ip.resetRoi();
					ip.fill();
				} else {
					ip.setRoi(roi);
					ip.fill(ip.getMask());
				}
				updateAndDraw(imp);
				break;
		}
	}

	void updateAndDraw(ImagePlus imp) {
		if (autoUpdate)
			imp.updateChannelAndDraw();
		else
			updateNeeded = true;
	}
	
	void updateDisplay() {
		if (updateNeeded && WindowManager.getImageCount()>0) {
			ImagePlus imp = getImage();
			imp.updateAndDraw();
			updateNeeded = false;
		}
	}

	void drawString() {
		interp.getLeftParen();
		String str = getString();
		interp.getComma();
		int x = (int)(interp.getExpression()+0.5);
		interp.getComma();
		int y = (int)(interp.getExpression()+0.5);
		interp.getRightParen();
		ImageProcessor ip = getProcessor();
		if (!colorSet)
			setForegroundColor(ip);
		setFont(ip);
		ip.setJustification(justification);
		ip.setAntialiasedText(antialiasedText);
		ip.drawString(str, x, y);
		updateAndDraw(defaultImp);
	}
	
	void setFont(ImageProcessor ip) {
		if (font!=null && !fontSet)
			ip.setFont(font);
		fontSet = true;
	}

	void setJustification() {
		String str = getStringArg().toLowerCase(Locale.US);
		int just = ImageProcessor.LEFT_JUSTIFY;
		if (str.equals("center"))
			just = ImageProcessor.CENTER_JUSTIFY;
		else if (str.equals("right"))
			just = ImageProcessor.RIGHT_JUSTIFY;
		justification = just;
	}

	void changeValues() {
		double darg1 = getFirstArg();
		double darg2 = getNextArg();
		double darg3 = getLastArg();
		ImagePlus imp = getImage();
		ImageProcessor ip = getProcessor();
		Roi roi = imp.getRoi();
		ImageProcessor mask = null;
		if (roi==null || !roi.isArea()) {
			ip.resetRoi();
			roi = null;
		} else {
			ip.setRoi(roi);
			mask = ip.getMask();
			if (mask!=null) ip.snapshot();
		}
		int xmin=0, ymin=0, xmax=imp.getWidth(), ymax=imp.getHeight();
		if (roi!=null) {
			Rectangle r = roi.getBounds();
			xmin=r.x; ymin=r.y; xmax=r.x+r.width; ymax=r.y+r.height;
		}
		boolean isFloat = getType()==ImagePlus.GRAY32;
		if (imp.getBitDepth()==24) {
			darg1 = (int)darg1&0xffffff;
			darg2 = (int)darg2&0xffffff;
		}
		double v;
		for (int y=ymin; y<ymax; y++) {
			for (int x=xmin; x<xmax; x++) {
				v = isFloat?ip.getPixelValue(x,y):ip.getPixel(x,y)&0xffffff;
				if (v>=darg1 && v<=darg2) {
					if (isFloat)
						ip.putPixelValue(x, y, darg3);
					else
						ip.putPixel(x, y, (int)darg3);
				}
			}
		}
		if (mask!=null) ip.reset(mask);
		if (imp.getType()==ImagePlus.GRAY16 || imp.getType()==ImagePlus.GRAY32)
			ip.resetMinAndMax();
		imp.updateAndDraw();
		updateNeeded = false;
	}

	void requires() {
		if (IJ.versionLessThan(getStringArg()))
			interp.done = true;
	}

	Random ran;	
	double random() {
		interp.getParens();
 		if (ran==null)
			ran = new Random();
		return ran.nextDouble();
	}
	
	//void setSeed() {
	//	long seed = (long)getArg();
	//	if (ran==null)
	//		ran = new Random(seed);
	//	else
	//		ran.setSeed(seed);
	//}

	double getResult() {
		interp.getLeftParen();
		String column = getString();
		int row = -1;
		if (interp.nextNonEolToken()==',') {
			interp.getComma();
			row = (int)interp.getExpression();
		}
		interp.getRightParen();
		ResultsTable rt = Analyzer.getResultsTable();
		int counter = rt.getCounter();
		if (counter==0)
			interp.error("\"Results\" table empty");
		if (row==-1) row = counter-1;
		if (row<0 || row>=counter)
			interp.error("Row ("+row+") out of range");
		int col = rt.getColumnIndex(column);
		if (!rt.columnExists(col))
			return Double.NaN;
		else
   			return rt.getValueAsDouble(col, row);
	}

	String getResultLabel() {
		int row = (int)getArg();
		ResultsTable rt = Analyzer.getResultsTable();
		int counter = rt.getCounter();
		if (counter==0)
			interp.error("\"Results\" table empty");
		if (row<0 || row>=counter)
			interp.error("Row ("+row+") out of range");
		String label = rt.getLabel(row);
   		return label!=null?label:"";
	}

	void setResult() {
		interp.getLeftParen();
		String column = getString();
		interp.getComma();
		int row = (int)interp.getExpression();
		interp.getComma();
		double value = 0.0;
		String label = null;
		if (column.equals("Label"))
			label = getString();
		else
			value = interp.getExpression();		
		interp.getRightParen();
		ResultsTable rt = Analyzer.getResultsTable();
		if (row<0 || row>rt.getCounter())
			interp.error("Row ("+row+") out of range");
		if (row==rt.getCounter())
			rt.incrementCounter();
		try {
			if (label!=null)
				rt.setLabel(label, row);
			else
				rt.setValue(column, row, value);
		} catch (Exception e) {
			interp.error(""+e.getMessage());
		}
	}
	
	void updateResults() {
		interp.getParens();
		ResultsTable rt = Analyzer.getResultsTable();
		rt.show("Results");
	}

	double getNumber() {
		String prompt = getFirstString();
		double defaultValue = getLastArg();
		String title = interp.macroName!=null?interp.macroName:"";
		if (title.endsWith(" Options"))
			title = title.substring(0, title.length()-8);
		GenericDialog gd = new GenericDialog(title);
		int decimalPlaces = (int)defaultValue==defaultValue?0:2;
		gd.addNumericField(prompt, defaultValue, decimalPlaces);
		gd.showDialog();
		if (gd.wasCanceled()) {
			interp.done = true;
			return defaultValue;
		}
		double v = gd.getNextNumber();
		if (gd.invalidNumber())
			return defaultValue;
		else
			return v;
	}

	double getBoolean() {
		String prompt = getStringArg();
		String title = interp.macroName!=null?interp.macroName:"";
		if (title.endsWith(" Options"))
			title = title.substring(0, title.length()-8);
		YesNoCancelDialog d = new YesNoCancelDialog(IJ.getInstance(), title, prompt);
		if (d.cancelPressed()) {
			interp.done = true;
			return 0.0;
		} else if (d.yesPressed())
			return 1.0;
		else
			return 0.0;
	}

	double getBoolean2() {
		String prompt = getFirstString();
		interp.getComma();
		double defaultValue = interp.getBooleanExpression();
		interp.checkBoolean(defaultValue);
		interp.getRightParen();
		String title = interp.macroName!=null?interp.macroName:"";
		if (title.endsWith(" Options"))
			title = title.substring(0, title.length()-8);
		GenericDialog gd = new GenericDialog(title);
		gd.addCheckbox(prompt, defaultValue==1.0?true:false);
		gd.showDialog();
		if (gd.wasCanceled()) {
			interp.done = true;
			return 0.0;
		}
		return gd.getNextBoolean()?1.0:0.0;
	}
	
	String getStringDialog() {
		interp.getLeftParen();
		String prompt = getString();
		interp.getComma();
		String defaultStr = getString();
		interp.getRightParen();
		
		String title = interp.macroName!=null?interp.macroName:"";
		if (title.endsWith(" Options"))
			title = title.substring(0, title.length()-8);
		GenericDialog gd = new GenericDialog(title);
		gd.addStringField(prompt, defaultStr, 20);
		gd.showDialog();
		String str = "";
		if (gd.wasCanceled())
			interp.done = true;
		else
			str = gd.getNextString();
   		return str;
	}

	String d2s() {
		return IJ.d2s(getFirstArg(), (int)getLastArg());
	}

	String toString(int base) {
		int arg = (int)getArg();
		if (base==2)
			return Integer.toBinaryString(arg);
		else
			return Integer.toHexString(arg);
	}
	
	double getStackSize() {
		interp.getParens();
		return getImage().getStackSize();
	}
	
	double getImageCount() {
		interp.getParens();
		return WindowManager.getImageCount();
	}
	
	double getResultsCount() {
		interp.getParens();
		return Analyzer.getResultsTable().getCounter();
	}

	void getCoordinates() {
		Variable xCoordinates = getFirstArrayVariable();
		Variable yCoordinates = getLastArrayVariable();
		resetImage();
		ImagePlus imp = getImage();
		Roi roi = imp.getRoi();
		if (roi==null)
			interp.error("Selection required");
		Polygon p = roi.getPolygon();
    	Variable[] xa = new Variable[p.npoints];
    	for (int i=0; i<p.npoints; i++)
    		xa[i] = new Variable(p.xpoints[i]);
		xCoordinates.setArray(xa);
    	Variable[] ya = new Variable[p.npoints];
    	for (int i=0; i<p.npoints; i++)
    		ya[i] = new Variable(p.ypoints[i]);
		yCoordinates.setArray(ya);
	}
	
	Variable[] getProfile() {
		interp.getParens();
		ImagePlus imp = getImage();
		ProfilePlot pp = new ProfilePlot(imp, IJ.altKeyDown());
		double[] array = pp.getProfile();
		if (array==null)
			{interp.done=true; return null;}
		else
			return new Variable(array).getArray();
	}

	Variable[] newArray() {
		interp.getLeftParen();
		int next = interp.nextNonEolToken();
		if (next==STRING_CONSTANT || interp.nextNextNonEolToken()==','
		|| next=='-' || next==PI)
			return initNewArray();
		int size = (int)interp.getExpression();
		interp.getRightParen();
    	Variable[] array = new Variable[size];
    	for (int i=0; i<size; i++)
    		array[i] = new Variable();
    	return array;
	}
	
	Variable[] split() {
		String s1 = getFirstString();
		String s2 = null;
		if (interp.nextToken()==')')
			interp.getRightParen();
		else
			s2 = getLastString();
		if (s1==null) return null;
		String[] strings = (s2==null||s2.equals(""))?Tools.split(s1):Tools.split(s1, s2);
    	Variable[] array = new Variable[strings.length];
    	for (int i=0; i<strings.length; i++)
    		array[i] = new Variable(0, 0.0, strings[i]);
    	return array;
	}

	Variable[] getFileList() {
		String dir = getStringArg();
		File f = new File(dir);
		if (!f.exists() || !f.isDirectory())
			return new Variable[0];
		String[] list = f.list();
		if (list==null)
			return new Variable[0];
		if (System.getProperty("os.name").indexOf("Linux")!=-1)
			ij.util.StringSorter.sort(list);
    	File f2;
    	int hidden = 0;
    	for (int i=0; i<list.length; i++) {
    		if (list[i].startsWith(".")) {
    			list[i] = null;
    			hidden++;
    		} else {
    			f2 = new File(dir, list[i]);
    			if (f2.isDirectory())
    				list[i] = list[i] + "/";
    		}
    	}
    	int n = list.length-hidden;
		if (n<=0)
			return new Variable[0];
    	if (hidden>0) {
			String[] list2 = new String[n];
			int j = 0;
			for (int i=0; i<list.length; i++) {
				if (list[i]!=null)
					list2[j++] = list[i];
			}
			list = list2;
		}
    	Variable[] array = new Variable[n];
    	for (int i=0; i<n; i++)
    		array[i] = new Variable(0, 0.0, list[i]);
    	return array;
	}
	
	Variable[] initNewArray() {
		Vector vector = new Vector();
		int size = 0;
		do {
		    Variable v = new Variable();
			if (interp.nextNonEolToken()==STRING_CONSTANT)
				v.setString(getString());
			else
				v.setValue(interp.getExpression());
			vector.addElement(v);
			size++;
			interp.getToken();				
		} while (interp.token==',');
		if (interp.token!=')')
			interp.error("';' expected");
    	Variable[] array = new Variable[size];
		vector.copyInto((Variable[])array);
    	return array;
	}

	String fromCharCode() {
		char[] chars = new char[100];
		int count = 0;
		interp.getLeftParen();
		while(interp.nextToken()!=')') {
			int value = (int)interp.getExpression();
			if (value<0 || value>65535)
				interp.error("Value (" + value + ") out of 0-65535 range");
			chars[count++] = (char)value;
			if (interp.nextToken()==',')
				interp.getToken();
		}
		interp.getRightParen();		
    	return new String(chars, 0, count);
	}

	String getInfo() {
		if (interp.nextNextNonEolToken()==STRING_CONSTANT
		|| (interp.nextNonEolToken()=='('&&interp.nextNextNonEolToken()!=')'))
			return getInfo(getStringArg());
		else {
			interp.getParens();
			return getWindowContents();
		}
	}
	
	String getInfo(String key) {
			if (key.equals("image.subtitle")) {
				ImagePlus imp = getImage();
				ImageWindow win = imp.getWindow();
				return win!=null?win.createSubtitle():"";
			} else if (key.equals("slice.label")) {
				ImagePlus imp = getImage();
				if (imp.getStackSize()==1) return "";
				String label = imp.getStack().getShortSliceLabel(imp.getCurrentSlice());
				return label!=null?label:"";
			} else if (key.equals("window.contents")) {
				return getWindowContents();
			} else {
				String value = "";
				try {value = System.getProperty(key);}
				catch (Exception e) {};
				return value!=null?value:"";
			}
	}
	
	String getWindowContents() {
		Frame frame = WindowManager.getFrontWindow();
		if (frame!=null && frame instanceof TextWindow) {
			TextPanel tp = ((TextWindow)frame).getTextPanel();
			return tp.getText();			
		} else if (frame!=null && frame instanceof Editor) {
			return ((Editor)frame).getText();			
		} else
			return getImageInfo();
	}
	
	String getImageInfo() {		
		ImagePlus imp = getImage();
		Info infoPlugin = new Info();
		return infoPlugin.getImageInfo(imp, getProcessor());
	}

	public String getDirectory() {
		String dir = IJ.getDirectory(getStringArg());
		if (dir==null) dir = "";
		return dir;
	}

	double getSelectionType() {
		interp.getParens();
		double type = -1;
		ImagePlus imp = getImage();
		Roi roi = imp.getRoi();
		if (roi!=null)
			type = roi.getType();
		return type;
	}

	void showMessage(boolean withCancel) {
		String message;
		interp.getLeftParen();
		String title = getString();
		if (interp.nextToken()==',') {
			interp.getComma();
			message = getString();
		} else {
			message = title;
			title = "";
		}
		interp.getRightParen();
		if (withCancel)
			IJ.showMessageWithCancel(title, message);
		else
			IJ.showMessage(title, message);
	}
	
	double lengthOf() {
		int length = 0;
		interp.getLeftParen();
		switch (interp.nextToken()) {
			case STRING_CONSTANT:
			case STRING_FUNCTION:
			case USER_FUNCTION:
				length = getString().length();
				break; 
			case WORD:
				if (pgm.code[interp.pc+2]=='[') {
					length = getString().length();
					break;
				}
				interp.getToken();
				Variable v = interp.lookupVariable();
				if (v==null) return 0.0;
				String s = v.getString();
				if (s!=null)
					length = s.length();
				else {
					Variable[] array = v.getArray();
					if (array!=null)
						length = array.length;
					else
						interp.error("String or array expected");
				}					
				break;
			default:
				interp.error("String or array expected");
		}
		interp.getRightParen();
		return length;
	}
	
	void getCursorLoc() {
		Variable x = getFirstVariable();
		Variable y = getNextVariable();
		Variable z = getNextVariable();
		Variable flags = getLastVariable();
		ImagePlus imp = getImage();
		ImageCanvas ic = imp.getCanvas();
		if (ic==null) return;
		Point p = ic.getCursorLoc();
		x.setValue(p.x);
		y.setValue(p.y);
		z.setValue(imp.getCurrentSlice()-1);
		flags.setValue(ic.getModifiers());
	}
	
	void getLine() {
		Variable vx1 = getFirstVariable();
		Variable vy1 = getNextVariable();
		Variable vx2 = getNextVariable();
		Variable vy2 = getNextVariable();
		Variable lineWidth = getLastVariable();
		resetImage();
		ImagePlus imp = getImage();
		double x1=-1, y1=-1, x2=-1, y2=-1;
		Roi roi = imp.getRoi();
		if (roi!=null && roi.getType()==Roi.LINE) {
			Line line = (Line)roi;
			x1=line.x1d; y1=line.y1d; x2=line.x2d; y2=line.y2d;
		}
		vx1.setValue(x1);
		vy1.setValue(y1);
		vx2.setValue(x2);
		vy2.setValue(y2);				
		lineWidth.setValue(Line.getWidth());				
	}
	
	void getVoxelSize() {
		Variable width = getFirstVariable();
		Variable height = getNextVariable();
		Variable depth = getNextVariable();
		Variable unit = getLastVariable();
		resetImage();
		ImagePlus imp = getImage();
		Calibration cal = imp.getCalibration();
		width.setValue(cal.pixelWidth);
		height.setValue(cal.pixelHeight);
		depth.setValue(cal.pixelDepth);
		unit.setString(cal.getUnits());
	}
	
	void getHistogram() {
		interp.getLeftParen();
		Variable values = null;
		if (interp.nextToken()==NUMBER)
			interp.getExpression();
		else
			values = getArrayVariable();
		Variable counts = getNextArrayVariable();
		interp.getComma();
		int nBins = (int)interp.getExpression();
		ImagePlus imp = getImage();
		double histMin=0.0, histMax=0.0;
		boolean setMinMax = false;
		int bitDepth = imp.getBitDepth();
		if (interp.nextToken()==',') {
			histMin = getNextArg();
			histMax = getLastArg();
			if (bitDepth==8 || bitDepth==24)
				interp.error("16 or 32-bit image required to set histMin and histMax");
			setMinMax = true;
		} else 
			interp.getRightParen();
		if ((bitDepth==8||bitDepth==24) && nBins!=256)
			interp.error("Bin count ("+nBins+") must be 256 for 8-bit and RGB images");
		if (nBins==65536 && bitDepth==16) {
			Variable[] array = counts.getArray();
			int[] hist = getProcessor().getHistogram();
			if (array!=null && array.length==nBins) {
				for (int i=0; i<nBins; i++)
					array[i].setValue(hist[i]);
			} else
				counts.setArray(new Variable(hist).getArray());
			return;
		}
		ImageStatistics stats;
		if (setMinMax)
			stats = imp.getStatistics(AREA+MEAN+MODE+MIN_MAX, nBins, histMin, histMax);
		else
			stats = imp.getStatistics(AREA+MEAN+MODE+MIN_MAX, nBins);
		if (values!=null) {
			Calibration cal = imp.getCalibration();
			double[] array = new double[nBins];
			double value = cal.getCValue(stats.histMin);
			double inc = 1.0;
			if (bitDepth==16 || bitDepth==32 || cal.calibrated())
				inc = (cal.getCValue(stats.histMax) - cal.getCValue(stats.histMin))/stats.nBins;
			for (int i=0; i<nBins; i++) {
				array[i] = value;
				value += inc;
			}
			values.setArray(new Variable(array).getArray());
		}
		Variable[] array = counts.getArray();
		if (array!=null && array.length==nBins) {
			for (int i=0; i<nBins; i++)
				array[i].setValue(stats.histogram[i]);
		} else
			counts.setArray(new Variable(stats.histogram).getArray());
	}
	
	void getLut() {
		Variable reds = getFirstArrayVariable();
		Variable greens = getNextArrayVariable();
		Variable blues = getLastArrayVariable();
		resetImage();
		ImageProcessor ip = getProcessor();
		if (ip instanceof ColorProcessor)
			interp.error("Non-RGB image expected");
		IndexColorModel cm = (IndexColorModel)ip.getColorModel();
		int mapSize = cm.getMapSize();
		byte[] rLUT = new byte[mapSize];
		byte[] gLUT = new byte[mapSize];
		byte[] bLUT = new byte[mapSize];
		cm.getReds(rLUT); 
		cm.getGreens(gLUT); 
		cm.getBlues(bLUT); 
		reds.setArray(new Variable(rLUT).getArray());
		greens.setArray(new Variable(gLUT).getArray());
		blues.setArray(new Variable(bLUT).getArray());
	}

	void setLut() {
		double[] reds = getFirstArray();
		double[] greens = getNextArray();
		double[] blues = getLastArray();
		int length = reds.length;		
		if (greens.length!=length || blues.length!=length)
			interp.error("Arrays are not the same length");
		resetImage();
		ImagePlus imp = getImage();
		if (imp.getBitDepth()==24)
			interp.error("Non-RGB image expected");
		ImageProcessor ip = getProcessor();
		byte[] r = new byte[length];
		byte[] g = new byte[length];
		byte[] b = new byte[length];
		for (int i=0; i<length; i++) {
			r[i] = (byte)reds[i];
			g[i] = (byte)greens[i];
			b[i] = (byte)blues[i];
		}		
		ip.setColorModel(new IndexColorModel(8, length, r, g, b));
		imp.updateAndDraw();
		updateNeeded = false;
	}

	void getThreshold() {
		Variable lower = getFirstVariable();
		Variable upper = getLastVariable();
		ImagePlus imp = getImage();
		ImageProcessor ip = getProcessor();
		double t1 = ip.getMinThreshold();
		double t2 = ip.getMaxThreshold();
		if (t1==ImageProcessor.NO_THRESHOLD) {
			t1 = -1;
			t2 = -1;
		} else if (imp.getBitDepth()==16) {
			Calibration cal = imp.getCalibration();
			t1 = cal.getCValue(t1); 
			t2 = cal.getCValue(t2); 
		}

		lower.setValue(t1);
		upper.setValue(t2);
	}

	void getPixelSize() {
		Variable unit = getFirstVariable();
		Variable width = getNextVariable();
		Variable height = getNextVariable();
		Variable depth = null;
		if (interp.nextToken()==',')
			depth = getNextVariable();
		interp.getRightParen();
		Calibration cal = getImage().getCalibration();
		unit.setString(cal.getUnits());
		width.setValue(cal.pixelWidth);
		height.setValue(cal.pixelHeight);
		if (depth!=null)
		depth.setValue(cal.pixelDepth);
	}

	void makeSelection() {
        String type = null;
        int roiType = -1;
        interp.getLeftParen();
		if (isStringArg()) {
			type = getString().toLowerCase();
			roiType = Roi.POLYGON;
			if (type.indexOf("free")!=-1)
				roiType = Roi.FREEROI;
			if (type.indexOf("traced")!=-1)
				roiType = Roi.TRACED_ROI;
			if (type.indexOf("line")!=-1) {
				if (type.indexOf("free")!=-1)
					roiType = Roi.FREELINE;
				else
					roiType = Roi.POLYLINE;
			}
			if (type.indexOf("angle")!=-1)
				roiType = Roi.ANGLE;
			if (type.indexOf("point")!=-1)
				roiType = Roi.POINT;
		} else {
			roiType = (int)interp.getExpression();
			if (roiType<0 || roiType==Roi.COMPOSITE)
				interp.error("Invalid selection type ("+roiType+")");
			if (roiType==Roi.RECTANGLE) roiType = Roi.POLYGON;		
			if (roiType==Roi.OVAL) roiType = Roi.FREEROI;		
		}
		double[] x = getNextArray();
		double[] y = getLastArray();
		int n = x.length;		
		if (y.length!=n)
			interp.error("Arrays are not the same length");
		resetImage();
		ImagePlus imp = getImage();
		int[] xcoord = new int[n];
		int[] ycoord = new int[n];
		int height = imp.getHeight();
		for (int i=0; i<n; i++) {
			xcoord[i] = (int)Math.round(x[i]);
			ycoord[i] = (int)Math.round(y[i]);
		}
		Roi roi = null;
		if (roiType==Roi.LINE) {
			if (xcoord.length!=2)
				interp.error("2 element arrays expected");
			roi = new Line(xcoord[0], ycoord[0], xcoord[1], ycoord[1]);
		} else if (roiType==Roi.POINT)
			roi = new PointRoi(xcoord, ycoord, n);
		else
			roi = new PolygonRoi(xcoord, ycoord, n, roiType);
		Roi previousRoi = imp.getRoi();
		if (shiftKeyDown||altKeyDown) imp.saveRoi();
		imp.setRoi(roi);
		if (roiType==Roi.POLYGON || roiType==Roi.FREEROI) {
			roi = imp.getRoi();
			if (previousRoi!=null && roi!=null)
				updateRoi(roi); 
		}
		updateNeeded = false;
	}

	void doPlot() {
		interp.getToken();
		if (interp.token!='.')
			interp.error("'.' expected");
		interp.getToken();
		if (!(interp.token==WORD || interp.token==PREDEFINED_FUNCTION))
			interp.error("Function name expected: ");
		String name = interp.tokenString;
		if (name.equals("create")) {
			newPlot();
			return;
		}
		if (plot==null)
			interp.error("No plot defined");
		if (name.equals("show")) {
		    showPlot();
		    return;
		} else if (name.equals("update")) {
		    updatePlot(); 
		    return;
		} else if (name.equals("setLimits")) {
			plot.setLimits(getFirstArg(), getNextArg(), getNextArg(), getLastArg());
		    return;
		} else if (name.equals("addText") || name.equals("drawLabel")) {
		    addPlotText(); 
		    return;
		} else if (name.equals("setColor")) {
		    setPlotColor(); 
		    return;
		} else if (name.equals("add")) {
			String arg = getFirstString();
			arg = arg.toLowerCase(Locale.US);
			int what = Plot.CIRCLE;
			if (arg.indexOf("curve")!=-1 || arg.indexOf("line")!=-1)
				what = Plot.LINE;
			else if (arg.indexOf("box")!=-1)
				what = Plot.BOX;
			else if (arg.indexOf("triangle")!=-1)
				what = Plot.TRIANGLE;
			else if (arg.indexOf("cross")!=-1)
				what = Plot.CROSS;		
			else if (arg.indexOf("dot")!=-1)
				what = Plot.DOT;		
			else if (arg.indexOf("x")!=-1)
				what = Plot.X;
			else if (arg.indexOf("error")!=-1)
				what = -1;
		    addToPlot(what); 
		    return;
		} else if (name.startsWith("setLineWidth")) {
		    plot.setLineWidth((int)getArg()); 
		    return;
		} else if (name.startsWith("setJustification")) {
		    doFunction(SET_JUSTIFICATION); 
		    return;
		} else
			interp.error("Unrecognized plot function");
	}

	void newPlot() {
		String title = getFirstString();
		String xLabel = getNextString();
		String yLabel = getNextString();
		double[] x, y;
		if (interp.nextToken()==')')
			x = y = null;
		else {
			x = getNextArray();
			if (interp.nextToken()==')') {
				y = x;
				x = new double[y.length];
				for (int i=0; i<y.length; i++)
					x[i] = i;
			} else
				y = getNextArray();
		}
		interp.getRightParen();
		plot = new Plot(title, xLabel, yLabel, x, y);							
	}
	
	void showPlot() {
		if (plot!=null) {
			PlotWindow plotWindow = plot.show();
			if (plotWindow!=null)
				plotID = plotWindow.getImagePlus().getID();
		}
		plot = null;
		interp.getParens();			
	}

	void updatePlot() {
		if (plot!=null) {
			ImagePlus plotImage = WindowManager.getImage(plotID);
			ImageWindow win = plotImage!=null?plotImage.getWindow():null;
			if (win!=null)
				((PlotWindow)win).drawPlot(plot);
			else {
				PlotWindow plotWindow = plot.show();
				plotID = plotWindow.getImagePlus().getID();
			}
		}
		plot = null;
		interp.getParens();			
	}
	
	void addPlotText() {
		String str = getFirstString();
		double x = getNextArg();
		double y = getLastArg();
		plot.setJustification(justification);
		plot.addLabel(x, y, str);
	}

	void setPlotColor() {
		interp.getLeftParen();
		plot.setColor(getColor());
		interp.getRightParen();
	}

	void addToPlot(int what) {
		double[] x = getNextArray();
		double[] y;
		if (interp.nextToken()==')') {
			y = x;
			x = new double[y.length];
			for (int i=0; i<y.length; i++)
				x[i] = i;
		} else {
			interp.getComma();
			y = getNumericArray();
		}
		interp.getRightParen();
		if (what==-1)
			plot.addErrorBars(y);
		else			
			plot.addPoints(x, y, what);		
	}
	
	void getBounds() {
		Variable x = getFirstVariable();
		Variable y = getNextVariable();
		Variable width = getNextVariable();
		Variable height = getLastVariable();
		resetImage();
		ImagePlus imp = getImage();
		Roi roi = imp.getRoi();
		if (roi!=null) {
			Rectangle r = roi.getBounds();
			x.setValue(r.x);
			y.setValue(r.y);
			width.setValue(r.width);
			height.setValue(r.height);
		} else {
			x.setValue(0);
			y.setValue(0);
			width.setValue(imp.getWidth());
			height.setValue(imp.getHeight());
		}
	}

	String substring() {
		String s = getFirstString();
		int index1 = (int)getNextArg();
		int index2 = (int)getLastArg();
		if (index1>index2)
			interp.error("beginIndex>endIndex");
		checkIndex(index1, 0, s.length());
		checkIndex(index2, 0, s.length());
		return s.substring(index1, index2);
	}

	int indexOf() {
		String s1 = getFirstString();
		String s2 = getNextString();
		int fromIndex = 0;
		if (interp.nextToken()==',') {
			fromIndex = (int)getLastArg();
			checkIndex(fromIndex, 0, s1.length()-1);
		} else
			interp.getRightParen();			
		if (fromIndex==0)
			return s1.indexOf(s2);
		else
			return s1.indexOf(s2, fromIndex);
	}

	int startsWithEndsWith(int type) {
		String s1 = getFirstString();
		String s2 = getLastString();
		if (type==STARTS_WITH)
			return s1.startsWith(s2)?1:0;
		else
			return s1.endsWith(s2)?1:0;
	}

	double isActive() {
		int id = (int)getArg();
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp==null || imp.getID()!=id)
			return 0.0; //false
		else
			return 1.0; //true
	}
	
	double isOpen() {
        interp.getLeftParen();
		if (isStringArg()) {
			String title = getString();
			interp.getRightParen();
			boolean open = WindowManager.getFrame(title)!=null;
			if (open)
				return 1.0;
			else if (Interpreter.isBatchMode() && Interpreter.imageTable!=null) {
				for (Enumeration en=Interpreter.imageTable.elements(); en.hasMoreElements();) {
					ImagePlus imp = (ImagePlus)en.nextElement();
					if (imp!=null && imp.getTitle().equals(title))
						return 1.0;
				}
			}
			return 0.0;
		} else {
			int id = (int)interp.getExpression();
			interp.getRightParen();
			return WindowManager.getImage(id)==null?0.0:1.0;
		}
	}

	boolean isStringArg() {
		int nextToken = pgm.code[interp.pc+1];
		int tok = nextToken&0xff;
		if (tok==STRING_CONSTANT||tok==STRING_FUNCTION) return true;
		if (tok!=WORD) return false;
		Variable v = interp.lookupVariable(nextToken>>TOK_SHIFT);
		if (v==null) return false;
		int type = v.getType();
		if (type!=Variable.ARRAY)
			return v.getType()==Variable.STRING;
		Variable[] array = v.getArray();
		if (array.length==0) return false;
		return array[0].getType()==Variable.STRING;
	}

	void exit() {
		String msg = null;
		if (interp.nextToken()=='(') {
			interp.getLeftParen();
			if (isStringArg())
				msg = getString();
			interp.getRightParen();
		}
		interp.finishUp();
		if (msg!=null)
			IJ.showMessage("Macro", msg);
		throw new RuntimeException(Macro.MACRO_CANCELED);
	}
	
	void showProgress() {
		ij.gui.ProgressBar progressBar = IJ.getInstance().getProgressBar();
		interp.getLeftParen();
		double arg1 = interp.getExpression();
		if (interp.nextToken()==',') {
			interp.getComma();
			double arg2 = interp.getExpression();
			if (progressBar!=null) progressBar.show((arg1+1.0)/arg2, true);						
		} else 
			if (progressBar!=null) progressBar.show(arg1, true);
		interp.getRightParen();
		interp.showingProgress = true; 	
	}
	
	void saveSettings() {
		interp.getParens();
		usePointerCursor = Prefs.usePointerCursor;
		hideProcessStackDialog = IJ.hideProcessStackDialog;
		divideByZeroValue = FloatBlitter.divideByZeroValue;
		jpegQuality = JpegWriter.getQuality();
		lineWidth = Line.getWidth();
		doScaling = ImageConverter.getDoScaling();
		weightedColor = Prefs.weightedColor;
		weights = ColorProcessor.getWeightingFactors();
		interpolateScaledImages = Prefs.interpolateScaledImages;
		open100Percent = Prefs.open100Percent;
		blackCanvas = Prefs.blackCanvas;
		useJFileChooser = Prefs.useJFileChooser;
		debugMode = IJ.debugMode;
		foregroundColor =Toolbar.getForegroundColor();
		backgroundColor =Toolbar.getBackgroundColor();
		roiColor = Roi.getColor();
		pointAutoMeasure = Prefs.pointAutoMeasure;
		requireControlKey = Prefs.requireControlKey;
		useInvertingLut = Prefs.useInvertingLut;
		doubleBuffer = Prefs.doubleBuffer;
		saveSettingsCalled = true;
		measurements = Analyzer.getMeasurements();
		decimalPlaces = Analyzer.getPrecision();
		blackBackground = Prefs.blackBackground;
	}
	
	void restoreSettings() {
		interp.getParens();
		if (!saveSettingsCalled)
			interp.error("saveSettings() not called");
		Prefs.usePointerCursor = usePointerCursor;
		IJ.hideProcessStackDialog = hideProcessStackDialog;
		FloatBlitter.divideByZeroValue = divideByZeroValue;
		JpegWriter.setQuality(jpegQuality);
		Line.setWidth(lineWidth);
		ImageConverter.setDoScaling(doScaling);
		if (weightedColor!=Prefs.weightedColor) {
			ColorProcessor.setWeightingFactors(weights[0], weights[1], weights[2]);
			Prefs.weightedColor = !(weights[0]==1d/3d && weights[1]==1d/3d && weights[2]==1d/3d);
		}
		Prefs.interpolateScaledImages = interpolateScaledImages;
		Prefs.open100Percent = open100Percent;
		Prefs.blackCanvas = blackCanvas;
		Prefs.useJFileChooser = useJFileChooser;
		Prefs.useInvertingLut = useInvertingLut;
		Prefs.doubleBuffer = doubleBuffer;
		IJ.debugMode = debugMode;
		Toolbar.setForegroundColor(foregroundColor);
		Toolbar.setBackgroundColor(backgroundColor);
		Roi.setColor(roiColor);
		Analyzer.setMeasurements(measurements);
		Analyzer.setPrecision(decimalPlaces);
		ColorProcessor.setWeightingFactors(weights[0], weights[1], weights[2]);
		Prefs.blackBackground = blackBackground;
	}
	
	void setKeyDown() {
		String keys = getStringArg();
		keys = keys.toLowerCase(Locale.US);
		altKeyDown = keys.indexOf("alt")!=-1;
		if (altKeyDown)
			IJ.setKeyDown(KeyEvent.VK_ALT);
		else
			IJ.setKeyUp(KeyEvent.VK_ALT);
		shiftKeyDown = keys.indexOf("shift")!=-1;
		if (shiftKeyDown)
			IJ.setKeyDown(KeyEvent.VK_SHIFT);
		else
			IJ.setKeyUp(KeyEvent.VK_SHIFT);		
		if (keys.equals("space"))
			IJ.setKeyDown(KeyEvent.VK_SPACE);
		else
			IJ.setKeyUp(KeyEvent.VK_SPACE);
		if (keys.indexOf("esc")!=-1)
			abortPluginOrMacro();
		else
			interp.keysSet = true;
	}
	
	void abortPluginOrMacro() {
		Interpreter.abortPrevious();
		IJ.setKeyDown(KeyEvent.VK_ESCAPE);
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp!=null) {
			ImageWindow win = imp.getWindow();
			if (win!=null) {
				win.running = false;
				win.running2 = false;
			}
		}
		//Macro.abort();
	}
	
	void open() {
		interp.getLeftParen();
		if (interp.nextToken()==')') {
			interp.getRightParen();
			IJ.open();
		} else {
			String path = getString();
			interp.getRightParen();
			IJ.open(path);
		}
		resetImage();
	}
	
	double roiManager() {
		String cmd = getFirstString();
		cmd = cmd.toLowerCase();
		String path = null;
		int index=0;
		double countOrIndex=Double.NaN;
		boolean twoArgCommand = cmd.equals("open")||cmd.equals("save")||cmd.equals("rename");
		boolean select = cmd.equals("select");
		if (twoArgCommand)
			path = getLastString();
		else if (select)
			index = (int)getLastArg();
		else
			interp.getRightParen();
		if (RoiManager.getInstance()==null)
			IJ.run("ROI Manager...");
		RoiManager rm = RoiManager.getInstance();
		if (rm==null)
			interp.error("ROI Manager not found");
		if (twoArgCommand)
			rm.runCommand(cmd, path);
		else if (select) {
			int n = rm.getList().getItemCount();
			checkIndex(index, 0, n-1);
			if (shiftKeyDown || altKeyDown) {
				rm.select(index, shiftKeyDown, altKeyDown);
				shiftKeyDown = altKeyDown = false;
			} else
				roiManagerSelect(rm, index);
		} else if (cmd.equals("count"))
			countOrIndex = rm.getList().getItemCount();
		else if (cmd.equals("index"))
			countOrIndex = rm.getList().getSelectedIndex();
		else {
			if (!rm.runCommand(cmd))
				interp.error("Invalid ROI Manager command");
		}
		return countOrIndex;			
	}
	
	void roiManagerSelect(RoiManager rm, int index) {
		int delay = 1;
		long start = System.currentTimeMillis();
		while (true) {
			rm.select(index);
			if (delay>1) IJ.wait(delay);
			if (rm.getList().isIndexSelected(index))
				break;
			//IJ.log(index+" "+delay);
			rm.select(-1); // deselect all
			IJ.wait(delay);
			delay *= 2; if (delay>32) delay=32;
			if ((System.currentTimeMillis()-start)>1000L)
				interp.error("Failed to select");
		}
	}
	
	void setFont() {
		String name = getFirstString();
		int size = (int)getNextArg();
		int style = 0;
		antialiasedText = false;
		if (interp.nextToken()==',') {
			String styles = getLastString().toLowerCase();
			if (styles.indexOf("bold")!=-1) style += Font.BOLD;
			if (styles.indexOf("italic")!=-1) style += Font.ITALIC;
			if (styles.indexOf("anti")!=-1) antialiasedText = true;
		} else
			interp.getRightParen();
		font = new Font(name, style, size);
		fontSet = false;
	}

	void getMinAndMax() {
		Variable min = getFirstVariable();
		Variable max = getLastVariable();
		ImagePlus imp = getImage();
		ImageProcessor ip = imp.getProcessor();
		double v1 = ip.getMin();
		double v2 = ip.getMax();
		if (imp.getBitDepth()==16) {
			Calibration cal = imp.getCalibration();
			v1 = cal.getCValue(v1); 
			v2 = cal.getCValue(v2); 
		}
		min.setValue(v1);
		max.setValue(v2);
	}
	
	void selectImage() {
        interp.getLeftParen();
		if (isStringArg()) {
			selectImage(getString());
			interp.getRightParen();
		} else {
			int id = (int)interp.getExpression();
			IJ.selectWindow(id);
			interp.getRightParen();
		}
		resetImage();
	}
	
	void selectImage(String title) {
		if (Interpreter.isBatchMode()) {
			if (Interpreter.imageTable!=null) {
				for (Enumeration en=Interpreter.imageTable.elements(); en.hasMoreElements();) {
					ImagePlus imp = (ImagePlus)en.nextElement();
					if (imp!=null) {
						if (imp.getTitle().equals(title)) {
							ImagePlus imp2 = WindowManager.getCurrentImage();
							if (imp2!=null && imp2!=imp) imp2.saveRoi();
							WindowManager.setTempCurrentImage(imp);
							return;
						}
					}
				}
			}
			selectWindowManagerImage(title);
		} else
			selectWindowManagerImage(title);
	}
	
	void notFound(String title) {
		interp.error(title+" not found");
	}

	void selectWindowManagerImage(String title) {
		long start = System.currentTimeMillis();
		while (System.currentTimeMillis()-start<4000) { // 4 sec timeout
			int[] wList = WindowManager.getIDList();
			int len = wList!=null?wList.length:0;
			for (int i=0; i<len; i++) {
				ImagePlus imp = WindowManager.getImage(wList[i]);
				if (imp!=null) {
					if (imp.getTitle().equals(title)) {
						IJ.selectWindow(imp.getID());
						return;
					}
				}
			}
			IJ.wait(10);
		}
		notFound(title);
	}

	void close() {
		interp.getParens();
		ImagePlus imp = getImage();
		ImageWindow win = imp.getWindow();
		if (win!=null) {
			imp.changes = false;
			win.close();
		} else {
			imp.saveRoi();
			WindowManager.setTempCurrentImage(null);
			interp.removeBatchModeImage(imp);
		}
		resetImage();
	}
	
	void setBatchMode() {
		boolean enterBatchMode = false;
		String sarg = null;
		interp.getLeftParen();
		if (isStringArg())
			sarg = getString();
		else {
			double arg = interp.getBooleanExpression();
			interp.checkBoolean(arg);
			enterBatchMode = arg==1.0;
		}
		interp.getRightParen();
		if (!interp.isBatchMode())
			interp.calledMacro = false;
		resetImage();
		if (enterBatchMode)  { // true
			interp.setBatchMode(true);
			ImagePlus tmp = WindowManager.getTempCurrentImage();
			if (tmp!=null)
				Interpreter.addBatchModeImage(tmp);
			return;
		}
		IJ.showProgress(0, 0);
		ImagePlus imp2 = WindowManager.getCurrentImage();
		WindowManager.setTempCurrentImage(null);
		if (sarg==null) {  //false
			interp.setBatchMode(false);
			displayBatchModeImage(imp2);
		} else {
			Vector v = Interpreter.imageTable;
			if (v==null) return;
			interp.setBatchMode(false);
			for (int i=0; i<v.size(); i++) {
				imp2 = (ImagePlus)v.elementAt(i);
				if (imp2!=null) 
					displayBatchModeImage(imp2);
			}
		}
	}
	
	void displayBatchModeImage(ImagePlus imp2) {
		if (imp2!=null) {
			ImageWindow win = imp2.getWindow();
			if (win==null)
				imp2.show();
			else {
				if (!win.isVisible()) win.show(); 
				imp2.updateAndDraw();
			}
			Roi roi = imp2.getRoi();
			if (roi!=null) imp2.setRoi(roi);
		}
	}
	
	void setLocation() {
		int x = (int)getFirstArg();
		int y = (int)getLastArg();
		ImagePlus imp = getImage();
		ImageWindow win =imp.getWindow();
		if (win!=null)
			win.setLocation(x, y);
	}
	
	void setSlice() {
		int n = (int)getArg();
		ImagePlus imp = getImage();
		int nSlices = imp.getStackSize();
		if (n==1 && nSlices==1)
			return;
		else if (n<1 || n>nSlices)
			interp.error("Argument must be >=1 and <="+nSlices);
		else
			imp.setSlice(n);
		resetImage();
	}
	
	void newImage() {
		String title = getFirstString();
		String type = getNextString();
		int width = (int)getNextArg();
		int height = (int)getNextArg();
		int depth = (int)getLastArg();
		IJ.newImage(title, type, width, height, depth);
		resetImage();
	}

	void saveAs() {
		String format = getFirstString();
		String path =  null;
		if (interp.nextNonEolToken()==',')
			path = getLastString();
		else
			interp.getRightParen();
		IJ.saveAs(format, path);
	}

	double getZoom() {
		interp.getParens();
		ImagePlus imp = getImage();
		ImageCanvas ic = imp.getCanvas();
		if (ic==null)
			{interp.error("Image not displayed"); return 0.0;}
		else
			return ic.getMagnification();
	}
	
	void setAutoThreshold() {
		interp.getParens();
		ImagePlus imp = getImage();
		ImageProcessor ip = getProcessor();
		double min=0.0, max=0.0;
		boolean notByteData = !(ip instanceof ByteProcessor);
		if (notByteData) {
			if (ip instanceof ColorProcessor)
				interp.error("Non-RGB image expected");
			ip.resetMinAndMax();
			min = ip.getMin(); max = ip.getMax();
			ip = new ByteProcessor(ip.createImage());
		}
		ip.setRoi(imp.getRoi());
		ImageStatistics stats = ImageStatistics.getStatistics(ip, AREA+MIN_MAX+MODE, null);
		int threshold = ip.getAutoThreshold(stats.histogram);
		int count1=0, count2=0;
		for (int i=0; i<256; i++) {
			if (i<threshold)
				count1 += stats.histogram[i];
			else
				count2 += stats.histogram[i];
		}
		boolean unbalanced = (double)count1/count2>1.25 || (double)count2/count1>1.25;
		//IJ.log(unbalanced+"  "+count1+"  "+count2);
		double lower, upper;
		if (unbalanced) {
			if ((stats.max-stats.dmode)>(stats.dmode-stats.min))
				{lower=threshold; upper=255.0;}
			else
				{lower=0.0; upper=threshold;}
		} else {
			if (ip.isInvertedLut())
				{lower=threshold; upper=255.0;}
			else
				{lower=0.0; upper=threshold;}
		}
		if (notByteData) {
			if (max>min) {
				lower = min + (lower/255.0)*(max-min);
				upper = min + (upper/255.0)*(max-min);
			} else
				lower = upper = min;
		}
		if (imp.getBitDepth()==16) {
			Calibration cal = imp.getCalibration();
			lower = cal.getCValue(lower); 
			upper = cal.getCValue(upper); 
		}
		IJ.setThreshold(lower, upper);
		resetImage();
	}
	
	double parseDouble(String s) {
			if (s==null) return 0.0;
			s = s.trim();
			if (s.indexOf(' ')!=-1) s = s.substring(0, s.indexOf(' '));
			return Tools.parseDouble(s);
	}
	
	double parseInt() {
		String s = getFirstString();
		int radix = 10;
		if (interp.nextToken()==',') {
			interp.getComma();
			radix = (int)interp.getExpression();
			if (radix<2||radix>36) radix = 10;
		}
		interp.getRightParen();
		double n;
		try {
			if (radix==10) {
				n = parseDouble(s);
				if (!Double.isNaN(n)) n = Math.round(n);
			} else
				n = Integer.parseInt(s, radix);
		} catch (NumberFormatException e) {
			n = NaN;
		}
		return n;			
	}

	void print() {
		interp.inPrint = true;
		String s = getFirstString();
		if (interp.nextNonEolToken()==',') {
			if (s.startsWith("[") && s.endsWith("]")) {
				printToWindow(s);
				return;
			} else if (s.equals("~0~")) {
				if (writer==null)
					interp.error("File not open");
                String s2 = getLastString();
                if (s2.endsWith("\n"))
                    writer.print(s2);
                else
                    writer.println(s2);
				interp.inPrint = false;
				return;
			}
			StringBuffer sb = new StringBuffer(s);
			do {
				sb.append(" ");
				sb.append(getNextString());
			} while (interp.nextNonEolToken()==',');
			s = sb.toString();
		}
		interp.getRightParen();
		IJ.log(s);
		interp.inPrint = false;
	}
	
	void printToWindow(String s) {
		String title = s.substring(1, s.length()-1);
		String s2 = getLastString();
		boolean isCommand = s2.startsWith("\\");
		Frame frame = WindowManager.getFrame(title);
		if (frame==null) {
			if (isCommand) {
				interp.done = true;
				return;
			} else
				interp.error("Window not found");
		}
		boolean isEditor = frame instanceof Editor;
		if (!(isEditor || frame instanceof TextWindow))
			interp.error("Window is not text window");
		if (isEditor) {
			Editor ed = (Editor)frame;
			ed.setIsMacroWindow(true);
			if (isCommand)
				handleEditorCommand(ed, s2);
			else
				ed.append(s2);
		} else {
			TextWindow tw = (TextWindow)frame;
			if (isCommand)
				handleTextWindowCommand(tw, s2);
			else
				tw.append(s2);
		}
	}
	
	void handleEditorCommand(Editor ed, String s) {
		if (s.startsWith("\\Update:")) {
			TextArea ta = ed.getTextArea();
			ta.setText(s.substring(8, s.length()));
			ta.setEditable(false);
		} else if (s.equals("\\Close"))
			ed.close();
		else
			ed.append(s);
	}

	void handleTextWindowCommand(TextWindow tw, String s) {
		TextPanel tp = tw.getTextPanel();
		if (s.startsWith("\\Update:")) {
			int n = tp.getLineCount();
			String s2 = s.substring(8, s.length());
			if (n==0)
				tp.append(s2);
			else
				tp.setLine(n-1, s2);
		} else if (s.startsWith("\\Update")) {
			int cindex = s.indexOf(":");
			if (cindex==-1)
				{tp.append(s); return;}
			String nstr = s.substring(7, cindex);
			int line = (int)Tools.parseDouble(nstr, -1);
			if (line<0 || line>25)
				{tp.append(s); return;}
			int count = tp.getLineCount();
			while (line>=count) {
				tp.append("");
				count++;
			}
			String s2 = s.substring(cindex+1, s.length());
			tp.setLine(line, s2);
		} else if (s.equals("\\Clear"))
			tp.clear();
		else if (s.equals("\\Close"))
			tw.close();
		else if (s.startsWith("\\Headings:"))
			tp.setColumnHeadings(s.substring(10));
		else
			tp.append(s);
	}

	
	double isKeyDown() {
		double value = 0.0;
		String key = getStringArg().toLowerCase(Locale.US);
		if (key.indexOf("alt")!=-1) value = IJ.altKeyDown()==true?1.0:0.0;
		else if (key.indexOf("shift")!=-1) value = IJ.shiftKeyDown()==true?1.0:0.0;
		else if (key.indexOf("space")!=-1) value = IJ.spaceBarDown()==true?1.0:0.0;
		else interp.error("Invalid key");
		return value;
	}

	String runMacro(boolean eval) {
		interp.getLeftParen();
		String name = getString();
		String arg = null;
		if (interp.nextNonEolToken()==',') {
			interp.getComma();
			arg = getString();
		}
		interp.getRightParen();
		if (eval)
			return IJ.runMacro(name, arg);
		else
			return IJ.runMacroFile(name, arg);
	}

	void setThreshold() {
		double lower = getFirstArg();
		double upper = getNextArg();
		String mode = null;
		if (interp.nextNonEolToken()==',') {
			interp.getComma();
			mode = getString();
		}
		interp.getRightParen();
		IJ.setThreshold(lower, upper, mode);
		resetImage();
	}

	void drawOrFill(int type) {
		int x = (int)getFirstArg();
		int y = (int)getNextArg();
		int width = (int)getNextArg();
		int height = (int)getLastArg();
		ImageProcessor ip = getProcessor();
		if (!colorSet) setForegroundColor(ip);
		switch (type) {
			case DRAW_RECT: ip.drawRect(x, y, width, height); break;
			case FILL_RECT: ip.setRoi(x, y, width, height); ip.fill(); break;
			case DRAW_OVAL: ip.drawOval(x, y, width, height); break;
			case FILL_OVAL: ip.fillOval(x, y, width, height); break;
		}
		updateAndDraw(defaultImp);
	}

	double getScreenDimension(int type) {
		interp.getParens();
		Dimension screen = IJ.getScreenSize();
		if (type==SCREEN_WIDTH)
			return screen.width;
		else
			return screen.height;
	}

	void getStatistics(boolean calibrated) {
		Variable count = getFirstVariable();
		Variable mean=null, min=null, max=null, std=null, hist=null;
		int params = AREA+MEAN+MIN_MAX;
		interp.getToken();
		int arg = 1;
		while (interp.token==',') {
			arg++;
			switch (arg) {
				case 2: mean = getVariable(); break;
				case 3: min = getVariable(); break;
				case 4: max = getVariable(); break;
				case 5: std = getVariable(); params += STD_DEV; break;
				case 6: hist = getArrayVariable(); break;
				default: interp.error("')' expected");
			}
			interp.getToken();
		}
		if (interp.token!=')') interp.error("')' expected");
		resetImage();
		ImagePlus imp = getImage();
		Calibration cal = calibrated?imp.getCalibration():null;
		ImageProcessor ip = getProcessor();
		ip.setRoi(imp.getRoi());
		ImageStatistics stats = ImageStatistics.getStatistics(ip, params, cal);
		if (calibrated)
			count.setValue(stats.area);
		else
			count.setValue(stats.pixelCount);
		if (mean!=null) mean.setValue(stats.mean);
		if (min!=null) min.setValue(stats.min);
		if (max!=null) max.setValue(stats.max);
		if (std!=null) std.setValue(stats.stdDev);
		if (hist!=null) {
			boolean is16bit = !calibrated && ip instanceof ShortProcessor;
			int[] histogram = is16bit?stats.histogram16:stats.histogram;
		    int bins = is16bit?(int)(stats.max+1):histogram.length;
			Variable[] array = new Variable[bins];
			int hmax = is16bit?(int)stats.max:255;
			for (int i=0; i<=hmax; i++)
				array[i] = new Variable(histogram[i]);
			hist.setArray(array);
		}
	}
	
	String replace() {
		String s1 = getFirstString();
		String s2 = getNextString();
		String s3 = getLastString();
		if (s2.length()==1 && s3.length()==1)
			return s1.replace(s2.charAt(0), s3.charAt(0));
		else if (IJ.isJava14()) {
			try {
				// Call replaceAll() using reflection so this class can be compiled with Java 1.3
				Class StringClass = s1.getClass();
				Method replaceAll = StringClass.getDeclaredMethod("replaceAll", new Class[] {String.class, String.class});
				Object arglist[]=new Object[2]; arglist[0]=s2; arglist[1]=s3;
 				return (String)replaceAll.invoke(s1, arglist);
			} catch (Exception e) {
				interp.error("Regular expression error:\n \n"+e);
			}
		} else
			interp.error("Java 1.4 or later required for multi-character replace");
		return null;
	}
	
	void floodFill() {
		int x = (int)getFirstArg();
		int y = (int)getNextArg();
		boolean fourConnected = true;
		if (interp.nextNonEolToken()==',') {
			String s = getLastString();
			if (s.indexOf("8")!=-1)
				fourConnected = false;
		} else
			interp.getRightParen();
		ImageProcessor ip = getProcessor();
		if (!colorSet) setForegroundColor(ip);
		FloodFiller ff = new FloodFiller(ip);
		if (fourConnected)
			ff.fill(x, y);
		else
			ff.fill8(x, y);
		updateAndDraw(defaultImp);
		if (Recorder.record && pgm.hasVars)
			Recorder.record("floodFill", x, y);
	}
	
	void restorePreviousTool() {
		interp.getParens();
		Toolbar tb = Toolbar.getInstance();
		if (tb!=null) tb.restorePreviousTool();
	}
	
	void setVoxelSize() {
		double width = getFirstArg();
		double height = getNextArg();
		double depth = getNextArg();
		String unit = getLastString();
		resetImage();
		ImagePlus imp = getImage();
		Calibration cal = imp.getCalibration();
		cal.pixelWidth = width;
		cal.pixelHeight = height;
		cal.pixelDepth = depth;
		cal.setUnit(unit);
		imp.repaintWindow();
	}

	void getLocationAndSize() {
		Variable v1 = getFirstVariable();
		Variable v2 = getNextVariable();
		Variable v3 = getNextVariable();
		Variable v4 = getLastVariable();
		ImagePlus imp = getImage();
		int x=0, y=0, w=0, h=0;
		ImageWindow win = imp.getWindow();
		if (win!=null) {
			Point loc = win.getLocation();
			Dimension size = win.getSize();
			x=loc.x; y=loc.y; w=size.width; h=size.height;
		}
		v1.setValue(x);
		v2.setValue(y);
		v3.setValue(w);
		v4.setValue(h);
	}
	
	String doDialog() {
		interp.getToken();
		if (interp.token!='.')
			interp.error("'.' expected");
		interp.getToken();
		if (!(interp.token==WORD || interp.token==STRING_FUNCTION || interp.token==NUMERIC_FUNCTION))
			interp.error("Function name expected: ");
		String name = interp.tokenString;
		try {
			if (name.equals("create")) {
				gd = new GenericDialog(getStringArg());
				return null;
			}
			if (gd==null) {
				interp.error("No dialog created with Dialog.create()"); 
				return null;
			}
			if (name.equals("addString")) {
				String label = getFirstString();
				String defaultStr = getNextString();
				int columns = 8;
				if (interp.nextNonEolToken()==',')
					columns = (int)getNextArg();
				interp.getRightParen();
				gd.addStringField(label, defaultStr, columns);
			} else if (name.equals("addNumber")) {
				int columns = 6;
				String units = null;
				String prompt = getFirstString();
				double defaultNumber = getNextArg();
				int decimalPlaces = (int)defaultNumber==defaultNumber?0:3;
				if (interp.nextNonEolToken()==',') {
					decimalPlaces = (int)getNextArg();
					columns = (int)getNextArg();
					units = getLastString();
				} else
					interp.getRightParen();
				gd.addNumericField(prompt, defaultNumber, decimalPlaces, columns, units);
			} else if (name.equals("addCheckbox")) {
				gd.addCheckbox(getFirstString(), getLastArg()==1?true:false);
			} else if (name.equals("addMessage")) {
				gd.addMessage(getStringArg());
			} else if (name.equals("addChoice")) {
				String prompt = getFirstString();
				interp.getComma();
				String[] choices = getStringArray();
				String defaultChoice = null;
				if (interp.nextNonEolToken()==',') {
					interp.getComma();
					defaultChoice = getString();
				} else
					defaultChoice = choices[0];
				interp.getRightParen();
				gd.addChoice(prompt, choices, defaultChoice);
			} else if (name.equals("show")) {
				interp.getParens();
				gd.showDialog();
				if (gd.wasCanceled()) {
					interp.finishUp();
					throw new RuntimeException(Macro.MACRO_CANCELED);
				}
			} else if (name.equals("getString")) {
				interp.getParens();
				return gd.getNextString();
			} else if (name.equals("getNumber")) {
				interp.getParens();
				return ""+gd.getNextNumber();
			} else if (name.equals("getCheckbox")) {
				interp.getParens();
				return gd.getNextBoolean()==true?"1":"0";
			} else if (name.equals("getChoice")) {
				interp.getParens();
				return gd.getNextChoice();
			} else
				interp.error("Unrecognized Dialog function "+name);
		} catch (IndexOutOfBoundsException e) {
			interp.error("Dialog error");
		}
		return null;
	}
	
	void getDateAndTime() {
		Variable year = getFirstVariable();
		Variable month = getNextVariable();
		Variable dayOfWeek = getNextVariable();
		Variable dayOfMonth = getNextVariable();
		Variable hour = getNextVariable();
		Variable minute = getNextVariable();
		Variable second = getNextVariable();
		Variable millisecond = getLastVariable();
		Calendar date = Calendar.getInstance();
		year.setValue(date.get(Calendar.YEAR));
		month.setValue(date.get(Calendar.MONTH));
		dayOfWeek.setValue(date.get(Calendar.DAY_OF_WEEK)-1);
		dayOfMonth.setValue(date.get(Calendar.DAY_OF_MONTH));
		hour.setValue(date.get(Calendar.HOUR_OF_DAY));
		minute.setValue(date.get(Calendar.MINUTE));
		second.setValue(date.get(Calendar.SECOND));
		millisecond.setValue(date.get(Calendar.MILLISECOND));
	}
	
	void setMetadata() {
		String metadata = getStringArg();
		ImagePlus imp = getImage();
		boolean isImageMetaData = false;
		if (metadata.startsWith("Info:")) {
			metadata = metadata.substring(5);
			isImageMetaData = true;
		}
		if (imp.getStackSize()==1 || isImageMetaData)
			imp.setProperty("Info", metadata);
		else {
			imp.getStack().setSliceLabel(metadata, imp.getCurrentSlice());
			if (!Interpreter.isBatchMode())
				imp.repaintWindow();
		}
	}

	String getMetadata() {
		interp.getParens();
		ImagePlus imp = getImage();
		String metadata;
		if (imp.getStackSize()==1) 
			metadata = (String)imp.getProperty("Info");
		else
			metadata = imp.getStack().getSliceLabel(imp.getCurrentSlice());
		if (metadata==null) metadata = "";
		return metadata;
	}

	ImagePlus getImageArg() {
		ImagePlus img = null;
		if (isStringArg()) {
			String title = getString();
			img = WindowManager.getImage(title);
		} else {
			int id = (int)interp.getExpression();
			img = WindowManager.getImage(id);
		}
		if (img==null) interp.error("Image not found");
		return img;
	}

	void imageCalculator() {
		String operator = getFirstString();
		interp.getComma();
		ImagePlus img1 = getImageArg();
		interp.getComma();
		ImagePlus img2 = getImageArg();
		interp.getRightParen();
		ImageCalculator ic = new ImageCalculator();
		ic.calculate(operator, img1, img2);
	}

	void setRGBWeights() {
		double r = getFirstArg();
		double g = getNextArg();
		double b = getLastArg();
		if (interp.rgbWeights==null)
			interp.rgbWeights = ColorProcessor.getWeightingFactors();
		ColorProcessor.setWeightingFactors(r, g, b);
	}

	void makePolygon() {
		int max = 200;
		int[] x = new int[max];
		int[] y = new int[max];
		x[0] = (int)Math.round(getFirstArg());
		y[0] = (int)Math.round(getNextArg());
		interp.getToken();
		int n = 1;
		while (interp.token==',' && n<max) {
			x[n] = (int)Math.round(interp.getExpression());
			interp.getComma();
			y[n] = (int)Math.round(interp.getExpression());
			interp.getToken();
			n++;
		}
		if (n<3)
			interp.error("Fewer than 3 points");
		if (n==max && interp.token!=')')
			interp.error("More than "+max+" points");
		ImagePlus imp = getImage();
		Roi previousRoi = imp.getRoi();
		if (shiftKeyDown||altKeyDown) imp.saveRoi();
		imp.setRoi(new PolygonRoi(x, y, n, Roi.POLYGON));
		Roi roi = imp.getRoi();
		if (previousRoi!=null && roi!=null)
			updateRoi(roi); 
		resetImage(); 
	}
	
	void updateRoi(Roi roi) {
		if (shiftKeyDown || altKeyDown)
			roi.update(shiftKeyDown, altKeyDown);
		shiftKeyDown = altKeyDown = false;
	}
	
	String doFile() {
		interp.getToken();
		if (interp.token!='.')
			interp.error("'.' expected");
		interp.getToken();
		if (!(interp.token==WORD || interp.token==STRING_FUNCTION || interp.token==NUMERIC_FUNCTION || interp.token==PREDEFINED_FUNCTION))
			interp.error("Function name expected: ");
		String name = interp.tokenString;
		if (name.equals("open"))
			return openFile();
		else if (name.equals("openAsString"))
			return openAsString();
		else if (name.equals("openUrlAsString"))
			return openUrlAsString();
		else if (name.equals("close"))
			return closeFile();
		else if (name.equals("separator")) {
			interp.getParens();
			return File.separator;
		} else if (name.equals("directory")) {
			interp.getParens();
			String lastDir = OpenDialog.getLastDirectory();
			return lastDir!=null?lastDir:"";
		} else if (name.equals("name")) {
			interp.getParens();
			String lastName = OpenDialog.getLastName();
			return lastName!=null?lastName:"";
		} else if (name.equals("rename")) {
			File f1 = new File(getFirstString());
			File f2 = new File(getLastString());
			if (isValid(f1) && isValid(f2)) 
				return f1.renameTo(f2)?"1":"0";
			else
				return "0";
		}
		File f = new File(getStringArg());
		if (name.equals("getLength")||name.equals("length"))
			return ""+f.length();
		else if (name.equals("getName"))
			return f.getName();
		else if (name.equals("getAbsolutePath"))
			return f.getAbsolutePath();
		else if (name.equals("getParent"))
			return f.getParent();
		else if (name.equals("exists"))
			return f.exists()?"1":"0";
		else if (name.equals("isDirectory"))
			return f.isDirectory()?"1":"0";
		else if (name.equals("makeDirectory")||name.equals("mkdir")) {
			f.mkdir(); return null;
		} else if (name.equals("lastModified"))
			return ""+f.lastModified();
		else if (name.equals("dateLastModified"))
			return (new Date(f.lastModified())).toString();
		else if (name.equals("delete")) {
			if (isValid(f)) 
				f.delete();
			return null;
		} else
			interp.error("Unrecognized File function "+name);
		return null;
	}
	
	boolean isValid(File f) {
		String path = f.getPath();
		if (path.equals("0") || path.equals("NaN") )
				interp.error("Invalid path");
		path = f.getAbsolutePath();
		if (!(path.indexOf("ImageJ")!=-1
		||path.startsWith(System.getProperty("java.io.tmpdir"))
		||path.startsWith(System.getProperty("user.home")))) {
			interp.error("File must be in ImageJ, home or tmp directory");
			return false;
		} else
			return true;
	}
	
	void setSelectionName() {
		Roi roi = getImage().getRoi();
		if (roi==null)
			interp.error("No selection");
		else
			roi.setName(getStringArg());
	}

	String selectionName() {
		Roi roi = getImage().getRoi();
		String name = null;
		if (roi==null)
			interp.error("No selection");
		else
			name = roi.getName();
		return name!=null?name:"";
	}
	
	String openFile() {
		String path = getFirstString();
		String defaultName = null;
		if (interp.nextToken()==')')
			interp.getRightParen();
		else
			defaultName = getLastString();
		if (path.equals("") || defaultName!=null) {
			String title = defaultName!=null?path:"openFile";
			defaultName = defaultName!=null?defaultName:"log.txt";
			SaveDialog sd = new SaveDialog(title, defaultName, ".txt");
			if(sd.getFileName()==null) return "";
			path = sd.getDirectory()+sd.getFileName();
		} else {
			File file = new File(path);
			if (file.exists() && !(path.endsWith(".txt")||path.endsWith(".java")||path.endsWith(".xls")||path.endsWith(".ijm")))
				interp.error("File exists and suffix is not '.txt', '.java', etc.");
		}
		if (writer!=null) writer.close();
		writer = null;
		try {
			FileOutputStream fos = new FileOutputStream(path);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			writer = new PrintWriter(bos);
		}
		catch (IOException e) {
			interp.error("File open error \n\""+e.getMessage()+"\"\n");
			return "";
		}
		return "~0~";
	}

	String openAsString() {
		String path = getStringArg();
		if (path.equals("")) {
			OpenDialog od = new OpenDialog("OpenAsString...", "");
			String directory = od.getDirectory();
			String name = od.getFileName();
			if (name==null) return "";
			path = directory + name;
		}
		String str = "";
		File file = new File(path);
		if (!file.exists())
			interp.error("File not found");
		try {
			StringBuffer sb = new StringBuffer(5000);
			BufferedReader r = new BufferedReader(new FileReader(file));
			while (true) {
				String s=r.readLine();
				if (s==null)
					break;
				else
					sb.append(s+"\n");
			}
			r.close();
			str = new String(sb);
		}
		catch (Exception e) {
			interp.error("File open error \n\""+e.getMessage()+"\"\n");
		}
		return str;
	}
	
	String closeFile() {
		String f = getStringArg();
		if (!f.equals("~0~"))
			interp.error("Invalid file variable");
		if (writer!=null) {
			writer.close();
			writer = null;
		}
		return null;
	}
	
	// Calls a public static method with an arbitrary number
	// of String parameters, returning a String.
	// Contributed by Johannes Schindelin
	String call() {
		// get class and method name
		String fullName = getFirstString();
		int dot = fullName.lastIndexOf('.');
		if(dot<0)
			interp.error("Expected 'classname.methodname'");
		String className = fullName.substring(0,dot);
		String methodName = fullName.substring(dot+1);

		// get optional string arguments
		Object[] args = null;
		if (interp.nextNonEolToken()==',') {
			Vector vargs = new Vector();
			do
				vargs.add(getNextString());
			while (interp.nextNonEolToken()==',');
			args = vargs.toArray();
		}
		interp.getRightParen();
		if (args==null) args = new Object[0];

		// get the class
		Class c;
		try {
			c = IJ.getClassLoader().loadClass(className);
		} catch(Exception ex) {
			interp.error("Could not load class "+className);
			return null;
		}

		// get method
		Method m;
		try {
			Class[] argClasses = null;
			if(args.length>0) {
				argClasses = new Class[args.length];
				for(int i=0;i<args.length;i++)
					argClasses[i] = args[i].getClass();
			}
			m = c.getMethod(methodName,argClasses);
		} catch(Exception ex) {
			interp.error("Could not find the method "+methodName+" with "+
				     args.length+" parameter(s) in class "+className);
			return null;
		}

		try {
			Object obj = m.invoke(null, args);
			return obj!=null?obj.toString():null;
		} catch(Exception e) {
			IJ.log("Call error ("+e+")");
			return null;
		}
			
 	}
 	
 	Variable[] getFontList() {
		interp.getParens();
		String fonts[] = null;
		if (IJ.isJava2()) {
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			fonts = ge.getAvailableFontFamilyNames();
		} else
			fonts = Toolkit.getDefaultToolkit().getFontList();
		if (fonts==null) return null;
    	Variable[] array = new Variable[fonts.length];
    	for (int i=0; i<fonts.length; i++)
    		array[i] = new Variable(0, 0.0, fonts[i]);
    	return array;
	}
	
	String openUrlAsString() {
		String urlString = getStringArg();
		StringBuffer sb = null;
		try {
			URL url = new URL(urlString);
			InputStream in = url.openStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			sb = new StringBuffer() ;
			String line;
			while ((line=br.readLine()) != null)
				sb.append (line + "\n");
			in.close ();
		} catch (IOException e) {
			String msg = ""+e;
			if (msg.indexOf("UnknownHost")!=-1 || msg.indexOf("FileNotFound")!=-1)
				return "";
			else
				interp.error(msg);
			sb = null;
		}
		if (sb!=null)
			return new String(sb);
		else
			return null;
	}
	
	void setOption() {
		String arg1 = getFirstString();
		interp.getComma();
		double arg2 = interp.getBooleanExpression();
		interp.checkBoolean(arg2);
		interp.getRightParen();
		boolean state = arg2==0?false:true;
		arg1 = arg1.toLowerCase(Locale.US);
		if (arg1.equals("disablepopupmenu")) {
			ImageCanvas ic = getImage().getCanvas();
			if (ic!=null) ic.disablePopupMenu(state);
		} else if (arg1.equals("show all")) {
			ImagePlus img = getImage();
			ImageCanvas ic = img.getCanvas();
			if (ic!=null) {
				boolean previousState = ic.getShowAllROIs();
				ic.setShowAllROIs(state);
				if (state!=previousState) img.draw();
			}
		} else if (arg1.equals("changes"))
			getImage().changes = state;
		else if (arg1.equals("debugmode"))
			IJ.debugMode = state;
		else if (arg1.equals("openusingplugins"))
			Opener.setOpenUsingPlugins(state);
		else if (arg1.equals("queuemacros"))
			pgm.queueCommands = state;
		else if (arg1.equals("disableundo"))
			Prefs.disableUndo = state;
		else
			interp.error("Invalid option");
	}
	
	void showText() {
		String title = getFirstString();
		String text = getLastString();
		Editor ed = new Editor();
		ed.setSize(350, 300);
		ed.create(title, text);
	}
	
	Variable[] newMenu() {
        String name = getFirstString();
        interp.getComma();
        String[] commands = getStringArray();
        interp.getRightParen();
        if (pgm.menus==null)
            pgm.menus = new Hashtable();
        pgm.menus.put(name, commands);
    	Variable[] commands2 = new Variable[commands.length];
    	for (int i=0; i<commands.length; i++)
    		commands2[i] = new Variable(0, 0.0, commands[i]);
    	return commands2;
	}
	
	void setSelectionLocation() {
		int x = (int)Math.round(getFirstArg());
		int y = (int)Math.round(getLastArg());
		resetImage();
		ImagePlus imp = getImage();
		Roi roi = imp.getRoi();
		if (roi==null)
			interp.error("Selection required");
		roi.setLocation(x, y);
		imp.draw();
	}
	
	double is() {
		double state = 0.0;
		String arg = getStringArg();
		arg = arg.toLowerCase(Locale.US);
		if (arg.equals("locked"))
			state = getImage().isLocked()?1.0:0.0;
		else if (arg.indexOf("invert")!=-1)
			state = getImage().isInvertedLut()?1.0:0.0;
		else
			interp.error("Argument must be 'locked' or 'Inverted LUT'");
		return state;
	}

	Variable[] getList() {
		String key = getStringArg();
		if (key.equals("java.properties")) {
			Properties props = System.getProperties();
			Vector v = new Vector();
			for (Enumeration en=props.keys(); en.hasMoreElements();)
				v.addElement((String)en.nextElement());
			Variable[] array = new Variable[v.size()];
			for (int i=0; i<array.length; i++)
				array[i] = new Variable(0, 0.0, (String)v.elementAt(i));
			return array;
		} else if (key.equals("window.titles")) {
			Frame[] list = WindowManager.getNonImageWindows();
			Variable[] array = new Variable[list.length];
			for (int i=0; i<list.length; i++) {
				Frame frame = list[i];
				array[i] = new Variable(0, 0.0, frame.getTitle());
			}
			return array;
		} else {
			interp.error("Unvalid key");
			return null;
		}
	}
	
	String doString() {
		interp.getToken();
		if (interp.token!='.')
			interp.error("'.' expected");
		interp.getToken();
		if (interp.token!=WORD)
			interp.error("Function name expected: ");
		String name = interp.tokenString;
		if (name.equals("append"))
			return appendToBuffer();
		else if (name.equals("copy"))
			return copyStringToClipboard();
		else if (name.equals("copyResults"))
			return copyResults();
		else if (name.equals("paste"))
			return getClipboardContents();
		else if (name.equals("resetBuffer"))
			return resetBuffer();
		else if (name.equals("buffer"))
			return getBuffer();
		else
			interp.error("Unrecognized String function");
		return null;
	}
	
	String appendToBuffer() {
		String text = getStringArg();
		if (buffer==null)
			buffer = new StringBuffer(256);
		buffer.append(text);
		return null;
	}

	String copyStringToClipboard() {
		String text = getStringArg();
		StringSelection ss = new StringSelection(text);
		java.awt.datatransfer.Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(ss, null);
		return null;
	}
		  
	String getClipboardContents() {
		interp.getParens();
		java.awt.datatransfer.Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable data = clipboard.getContents(null);
		String s = null;
		try {s = (String)data.getTransferData(DataFlavor.stringFlavor);} 
		catch (Exception e) {s = data.toString();}
		return s;
	}
    	
	String copyResults() {
		interp.getParens();
		if (!IJ.isResultsWindow())
			interp.error("No results");
		TextPanel tp = IJ.getTextPanel();
		StringSelection ss = new StringSelection(tp.getText());
		java.awt.datatransfer.Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(ss, null);
		return null;
	}

	String resetBuffer() {
		interp.getParens();
		buffer = new StringBuffer(256);
		return null;
	}

	String getBuffer() {
		interp.getParens();
		if (buffer==null)
			buffer = new StringBuffer(256);
		return buffer.toString();
	}
	
	void doCommand() {
		String arg = getStringArg();
		if (arg.equals("Start Animation"))
			arg = "Start Animation [\\]";
		IJ.doCommand(arg);
	}
	
	void getDimensions() {
		Variable width = getFirstVariable();
		Variable height = getNextVariable();
		Variable channels = getNextVariable();
		Variable slices = getNextVariable();
		Variable frames = getLastVariable();
		ImagePlus imp = getImage();
		int[] dim = imp.getDimensions();
		width.setValue(dim[0]);
		height.setValue(dim[1]);
		channels.setValue(dim[2]);
		slices.setValue(dim[3]);
		frames.setValue(dim[4]);
	}

} // class Functions

