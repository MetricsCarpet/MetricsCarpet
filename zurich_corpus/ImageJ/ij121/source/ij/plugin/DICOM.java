package ij.plugin;
import java.io.*;
import java.util.*;
import ij.*;
import ij.io.*;
import ij.util.Tools;
import ij.measure.Calibration;

/** This plug-in decodes DICOM files. If 'arg' is empty, it
	displays a file open dialog and opens and displays the 
	image selected by the user. If 'arg' is a path, it opens the 
	specified image and the calling routine can display it using
	"((ImagePlus)IJ.runPlugIn("ij.plugin.DICOM", path)).show()".
	*/

/* RAK (Richard Kirk, rak@cre.canon.co.uk) changes 14/7/99

   InputStream.skip() looped to check the actual number of
   bytes is read.

   Big/little-endian options on element length.

   Explicit check for each known VR to make mistaken identifications
   of explicit VR's less likely.

   Variables b1..b4 renamed as b0..b3.

   Increment of 4 to offset on (7FE0,0010) tag removed.

   Queries on some other unrecognized tags.
   Anyone want to claim them?

   RAK changes 15/7/99

   Bug fix on magic values for explicit VRs with 32-bit lengths.

   Various bits of tidying up, including...
   'location' incremented on read using getByte() or getString().
   simpler debug mode message generation (values no longer reported).

   Added z pixel aspect ratio support for multi-slice DICOM volumes.
   Michael Abramoff, 31-10-2000
   */

public class DICOM extends ImagePlus implements PlugIn {

	public void run(String arg) {
		OpenDialog od = new OpenDialog("Open Dicom...", arg);
		String directory = od.getDirectory();
		String fileName = od.getFileName();
		if (fileName==null)
			return;
		IJ.showStatus("Opening: " + directory + fileName);
		DicomDecoder dd = new DicomDecoder(directory, fileName);
		FileInfo fi = null;
		try {fi = dd.getFileInfo();}
		catch (IOException e) {
			String msg = e.getMessage();
			IJ.showStatus("");
			if (msg.indexOf("EOF")<0) {
				IJ.showMessage("DicomDecoder", msg);
				return;
			} else if (!dd.dicmFound()) {
				msg = "This does not appear to be a valid\n"
				+ "DICOM file. It does not have the\n"
				+ "characters 'DICM' at offset 128.";
				IJ.showMessage("DicomDecoder", msg);
				return;
			}
		}
		if (fi!=null && fi.width>0 && fi.height>0 && fi.offset>0) {
			FileOpener fo = new FileOpener(fi);
			ImagePlus imp = fo.open(false);
			if (fi.fileType==FileInfo.GRAY16_SIGNED) {
				Calibration cal = imp.getCalibration();
				double[] coeff = new double[2];
				coeff[0] = -32768.0;
				coeff[1] = 1.0;
				cal.setFunction(Calibration.STRAIGHT_LINE, coeff, "gray value");
			}
			if (imp.getStackSize()>1)
				setStack(fileName, imp.getStack());
			else
				setProcessor(fileName, imp.getProcessor());
			setCalibration(imp.getCalibration());
			setProperty("Info", dd.getDicomInfo());
			if (arg.equals("")) show();
		} else
			IJ.showMessage("DicomDecoder","Unable to decode DICOM header.");
		IJ.showStatus("");
	}
}


class DicomDecoder {

	private static final int PIXEL_REPRESENTATION = 0x00280103;
	private static final int TRANSFER_SYNTAX_UID = 0x00020010;
	private static final int SLICE_SPACING = 0x00180088;
	private static final int NUMBER_OF_FRAMES = 0x00280008;
	private static final int ROWS = 0x00280010;
	private static final int COLUMNS = 0x00280011;
	private static final int PIXEL_SPACING = 0x00280030;
	private static final int BITS_ALLOCATED = 0x00280100;
	private static final int RED_PALETTE = 0x00281201;
	private static final int GREEN_PALETTE = 0x00281202;
	private static final int BLUE_PALETTE = 0x00281203;
	private static final int PIXEL_DATA = 0x7FE00010;

	private static final int AE=0x4145, AS=0x4153, AT=0x4154, CS=0x4353, DA=0x4441, DS=0x4453, DT=0x4454,
		FD=0x4644, FL=0x464C, IS=0x4953, LO=0x4C4F, LT=0x4C54, PN=0x504E, SH=0x5348, SL=0x534C, 
		SS=0x5353, ST=0x5354, TM=0x544D, UI=0x5549, UL=0x554C, US=0x5553, UT=0x5554,
		OB=0x4F42, OW=0x4F57, SQ=0x5351, UN=0x554E, QQ=0x3F3F;
		
	private static Properties dictionary;

	private String directory, fileName;
	private static final int ID_OFFSET = 128;  //location of "DICM"
	private static final String DICM = "DICM";
	
	private BufferedInputStream f;
	private int location = 0;
	private boolean littleEndian = true;
	
	private int elementLength;
	private int vr;  // Value Representation
	private static final int IMPLICIT_VR = 0x2D2D; // '--' 
	private byte[] vrLetters = new byte[2];
 	private int previousGroup;
 	private StringBuffer dicomInfo = new StringBuffer(1000);
 	private boolean dicmFound; // "DICM" found at offset 128
 	private boolean oddLocations;  // one or more tags at odd locations

	public DicomDecoder(String directory, String fileName) {
		this.directory = directory;
		this.fileName = fileName;
		if (dictionary==null) {
			DicomDictionary d = new DicomDictionary();
			dictionary = d.getDictionary();
		}
			
		IJ.register(DICOM.class);
	}
  
	String getString(int length) throws IOException {
		byte[] buf = new byte[length];
		int pos = 0;
		while (pos<length) {
			int count = f.read(buf, pos, length-pos);
			pos += count;
		}
		location += length;
		return new String(buf);
	}
  
	int getByte() throws IOException {
		int b = f.read();
		if (b ==-1) throw new IOException("unexpected EOF");
		++location;
		return b;
	}

	int getShort() throws IOException {
		int b0 = getByte();
		int b1 = getByte();
		if (littleEndian)
			return ((b1 << 8) + b0);
		else
			return ((b0 << 8) + b1);
	}
  
	final int getInt() throws IOException {
		int b0 = getByte();
		int b1 = getByte();
		int b2 = getByte();
		int b3 = getByte();
		if (littleEndian)
			return ((b3<<24) + (b2<<16) + (b1<<8) + b0);
		else
			return ((b0<<24) + (b1<<16) + (b2<<8) + b3);
	}
  
	byte[] getLut(int length) throws IOException {
		if ((length&1)!=0) { // odd
			String dummy = getString(length);
			return null;
		}
		length /= 2;
		byte[] lut = new byte[length];
		for (int i=0; i<length; i++)
			lut[i] = (byte)(getShort()>>>8);
		return lut;
	}
  
  	int getLength() throws IOException {
		int b0 = getByte();
		int b1 = getByte();
		int b2 = getByte();
		int b3 = getByte();
		
		// We cannot know whether the VR is implicit or explicit
		// without the full DICOM Data Dictionary for public and
		// private groups.
		
		// We will assume the VR is explicit if the two bytes
		// match the known codes. It is possible that these two
		// bytes are part of a 32-bit length for an implicit VR.
		
		vr = (b0<<8) + b1;
		
		switch (vr) {
			case OB: case OW: case SQ: case UN:
				// Explicit VR with 32-bit length if other two bytes are zero
					if ( (b2 == 0) || (b3 == 0) ) return getInt();
				// Implicit VR with 32-bit length
				vr = IMPLICIT_VR;
				if (littleEndian)
					return ((b3<<24) + (b2<<16) + (b1<<8) + b0);
				else
					return ((b0<<24) + (b1<<16) + (b2<<8) + b3);		
			case AE: case AS: case AT: case CS: case DA: case DS: case DT:  case FD:
			case FL: case IS: case LO: case LT: case PN: case SH: case SL: case SS:
			case ST: case TM:case UI: case UL: case US: case UT: case QQ:
				// Explicit vr with 16-bit length
				if (littleEndian)
					return ((b3<<8) + b2);
				else
					return ((b2<<8) + b3);
			default:
				// Implicit VR with 32-bit length...
				vr = IMPLICIT_VR;
				if (littleEndian)
					return ((b3<<24) + (b2<<16) + (b1<<8) + b0);
				else
					return ((b0<<24) + (b1<<16) + (b2<<8) + b3);
		}
	}

	int getNextTag() throws IOException {
		int groupWord = getShort();
		int elementWord = getShort();
		int tag = groupWord<<16 | elementWord;
		elementLength = getLength();
		
		// hack needed to read some GE files
		// The element length must be even!
		if (elementLength==13 && !oddLocations) elementLength = 10; 
		
		// "Undefined" element length.
		// This is a sort of bracket that encloses a sequence of elements.
		if (elementLength==-1)
 			elementLength = 0;
		return tag;
	}
  
	FileInfo getFileInfo() throws IOException {
		long skipCount;
		
		FileInfo fi = new FileInfo();
		int bitsAllocated = 16;
		fi.fileFormat = fi.RAW;
		fi.fileName = fileName;
		fi.directory = directory;
		fi.width = 0;
		fi.height = 0;
		fi.offset = 0;
		fi.intelByteOrder = true;
		fi.fileType = FileInfo.GRAY16_UNSIGNED;
		
		f = new BufferedInputStream(new FileInputStream(directory + fileName));
		if (IJ.debugMode) {
			IJ.write("");
			IJ.write("DicomDecoder: decoding "+fileName);
		}
		
		skipCount = (long)ID_OFFSET;
		while (skipCount > 0) skipCount -= f.skip( skipCount );
		location += ID_OFFSET;
		
		if (!getString(4).equals(DICM)) {
			f.close();
			f = new BufferedInputStream(new FileInputStream(directory + fileName));
			location = 0;
			if (IJ.debugMode) IJ.write(DICM + " not found at offset "+ID_OFFSET+"; reseting to offset 0");
		} else if (IJ.debugMode) {
			dicmFound = true;
			IJ.write(DICM + " found at offset " + ID_OFFSET);
		}
		
		boolean inSequence = true;
		
		while (true) {
			int tag = getNextTag();
			if ((location&1)!=0) { // DICOM tags must be at even locations
				oddLocations = true;
				if (dicmFound)
					break;
			}
			if (tag==TRANSFER_SYNTAX_UID) {
				String s = getString(elementLength);
				addInfo(tag, s);
				if (s.indexOf("1.2.4")>-1||s.indexOf("1.2.5")>-1) {
					f.close();
					String msg = "ImageJ cannot open compressed DICOM images.\n \n";
					msg += "Transfer Syntax UID = "+s;
					throw new IOException(msg);
				}
			} else if (tag==NUMBER_OF_FRAMES) {
				String s = getString(elementLength);
				addInfo(tag, s);
				double frames = s2d(s);
				if (frames>1.0)
					fi.nImages = (int)frames;
			} else if (tag==ROWS) {
				fi.height = getShort();
				addInfo(tag, Integer.toString(fi.height));
			} else if (tag==COLUMNS) {
				fi.width = getShort();
				addInfo(tag, Integer.toString(fi.width));
			} else if (tag==PIXEL_SPACING) {
				String scale = getString(elementLength);
				getSpatialScale(fi, scale);
				addInfo(tag, scale);
			} else if (tag==SLICE_SPACING) {
				String spacing = getString(elementLength);
				fi.pixelDepth = s2d(spacing);
				addInfo(tag, spacing);
			} else if (tag==BITS_ALLOCATED) {
				bitsAllocated = getShort();
				if (bitsAllocated==8)
					fi.fileType = FileInfo.GRAY8;
				addInfo(tag, Integer.toString(bitsAllocated));
			} else if (tag==PIXEL_REPRESENTATION) {
				int pixelRepresentation = getShort();
				if (pixelRepresentation==1)
					fi.fileType = FileInfo.GRAY16_SIGNED;
				addInfo(tag, Integer.toString(pixelRepresentation));
			} else if (tag==RED_PALETTE) {
				fi.reds = getLut(elementLength);
				addInfo(tag, Integer.toString(elementLength/2));
			} else if (tag==GREEN_PALETTE) {
				fi.greens = getLut(elementLength);
				addInfo(tag, Integer.toString(elementLength/2));
			} else if (tag==BLUE_PALETTE) {
				fi.blues = getLut(elementLength);
				addInfo(tag, Integer.toString(elementLength/2));
			} else if (tag==PIXEL_DATA && elementLength!=0) {
				// Start of image data...
				fi.offset = location;
				addInfo(tag, Integer.toString(location));
				break;
			} else if (tag==0x7F880010 && elementLength!=0) {
				// What is this? - RAK
				fi.offset = location+4;
				break;
			} else {
				// Not used, skip over it...
				addInfo(tag, null);
			}
		} // while(true)
		
		if (fi.fileType==FileInfo.GRAY8) {
			if (fi.reds!=null && fi.greens!=null && fi.blues!=null
			&& fi.reds.length==fi.greens.length
			&& fi.reds.length==fi.blues.length) {
				fi.fileType = FileInfo.COLOR8;
				fi.lutSize = fi.reds.length;
				
			}
		}
		
		if (IJ.debugMode) {
			IJ.write("width: " + fi.width);
			IJ.write("height: " + fi.height);
			IJ.write("images: " + fi.nImages);
			IJ.write("bits allocated: " + bitsAllocated);
			IJ.write("offset: " + fi.offset);
		}
	
		f.close();
		return fi;
	}
	
	String getDicomInfo() {
		return new String(dicomInfo);
	}

	void addInfo(int tag, String value) throws IOException {
		String info = getHeaderInfo(tag, value);
		if (info!=null) {
			int group = tag>>>16;
			if (group!=previousGroup) dicomInfo.append("\n");
			previousGroup = group;
			dicomInfo.append(tag2hex(tag)+info+"\n");
		}
		if (IJ.debugMode) {
			if (info==null) info = "";
			vrLetters[0] = (byte)(vr >> 8);
			vrLetters[1] = (byte)(vr & 0xFF);
			String VR = new String(vrLetters);
			IJ.write("(" + tag2hex(tag) + VR
			+ " " + elementLength
			+ " bytes from "
			+ (location-elementLength)+") "
			+ info);
		}
	}

	String getHeaderInfo(int tag, String value) throws IOException {
		String key = i2hex(tag);
		//while (key.length()<8)
		//	key = '0' + key;
		String id = (String)dictionary.get(key);
		if (id!=null) {
			if (vr==IMPLICIT_VR && id!=null)
				vr = (id.charAt(0)<<8) + id.charAt(1);
			id = id.substring(2);
		}
		if (value!=null)
			return id+": "+value;
		switch (vr) {
			case AE: case AS: case AT: case CS: case DA: case DS: case DT:  case IS: case LO: 
			case LT: case PN: case SH: case ST: case TM: case UI:
				value = getString(elementLength);
				break;
			case US:
				if (elementLength==2)
					value = Integer.toString(getShort());
				else {
					value = "";
					int n = elementLength/2;
					for (int i=0; i<n; i++)
						value += Integer.toString(getShort())+" ";
				}
				break;
			default:
				long skipCount = (long)elementLength;
				while (skipCount > 0) skipCount -= f.skip(skipCount);
				location += elementLength;
				value = "";
		}
		if (id==null)
			return null;
		else
			return id+": "+value;
	}

	static char[] buf8 = new char[8];
	
	/** Converts an int to an 8 byte hex string. */
	String i2hex(int i) {
		for (int pos=7; pos>=0; pos--) {
			buf8[pos] = Tools.hexDigits[i&0xf];
			i >>>= 4;
		}
		return new String(buf8);
	}

	char[] buf10;
	
	String tag2hex(int tag) {
		if (buf10==null) {
			buf10 = new char[11];
			buf10[4] = ',';
			buf10[9] = ' ';
		}
		int pos = 8;
		while (pos>=0) {
			buf10[pos] = Tools.hexDigits[tag&0xf];
			tag >>>= 4;
			pos--;
			if (pos==4) pos--; // skip coma
		}
		return new String(buf10);
	}
	
 	double s2d(String s) {
		Double d;
		try {d = new Double(s);}
		catch (NumberFormatException e) {d = null;}
		if (d!=null)
			return(d.doubleValue());
		else
			return(0.0);
	}
  
	void getSpatialScale(FileInfo fi, String scale) {
		double xscale=0, yscale=0;
		int i = scale.indexOf('\\');
		if (i>0) {
			xscale = s2d(scale.substring(0, i));
			yscale = s2d(scale.substring(i+1));
		}
		if (xscale!=0.0 && yscale!=0.0) {
			fi.pixelWidth = xscale;
			fi.pixelHeight = yscale;
			fi.unit = "mm";
		}
	}
	
	boolean dicmFound() {
		return dicmFound;
	}

}


class DicomDictionary {

	Properties getDictionary() {
		Properties p = new Properties();
		for (int i=0; i<dict.length; i++) {
			p.put(dict[i].substring(0,8), dict[i].substring(9));
		}
		return p;
	}

	String[] dict = {

		"00020010=UITransfer Syntax UID",
		"00080005=CSSpecific Character Set",
		"00080008=CSImage Type",
		"00080012=DAInstance Creation Date",
		"00080013=TMInstance Creation Time",
		"00080014=UIInstance Creator UID",
		"00080016=UISOP Class UID",
		"00080018=UISOP Instance UID",
		"00080020=DAStudy Date",
		"00080021=DASeries Date",
		"00080022=DAAcquisition Date",
		"00080023=DAImage Date",
		"00080024=DAOverlay Date",
		"00080025=DACurve Date",
		"00080030=TMStudy Time",
		"00080031=TMSeries Time",
		"00080032=TMAcquisition Time",
		"00080033=TMImage Time",
		"00080034=TMOverlay Time",
		"00080035=TMCurve Time",
		"00080042=CSNuclear Medicine Series Type",
		"00080050=SHAccession Number",
		"00080052=CSQuery/Retrieve Level",
		"00080054=AERetrieve AE Title",
		"00080058=AEFailed SOP Instance UID List",
		"00080060=CSModality",
		"00080064=CSConversion Type",
		"00080070=LOManufacturer",
		"00080080=LOInstitution Name",
		"00080081=STInstitution Address",
		"00080082=SQInstitution Code Sequence",
		"00080090=PNReferring Physician's Name",
		"00080092=STReferring Physician's Address",
		"00080094=SHReferring Physician's Telephone Numbers",
		"00080100=SHCode Value",
		"00080102=SHCoding Scheme Designator",
		"00080104=LOCode Meaning",
		"00081010=SHStation Name",
		"00081030=LOStudy Description",
		"00081032=SQProcedure Code Sequence",
		"0008103E=LOSeries Description",
		"00081040=LOInstitutional Department Name",
		"00081050=PNAttending Physician's Name",
		"00081060=PNName of Physician(s) Reading Study",
		"00081070=PNOperator's Name",
		"00081080=LOAdmitting Diagnoses Description",
		"00081084=SQAdmitting Diagnosis Code Sequence",
		"00081090=LOManufacturer's Model Name",
		"00081100=SQReferenced Results Sequence",
		"00081110=SQReferenced Study Sequence",
		"00081111=SQReferenced Study Component Sequence",
		"00081115=SQReferenced Series Sequence",
		"00081120=SQReferenced Patient Sequence",
		"00081125=SQReferenced Visit Sequence",
		"00081130=SQReferenced Overlay Sequence",
		"00081140=SQReferenced Image Sequence",
		"00081145=SQReferenced Curve Sequence",
		"00081150=UIReferenced SOP Class UID",
		"00081155=UIReferenced SOP Instance UID",
		"00082111=STDerivation Description",
		"00082112=SQSource Image Sequence",
		"00082120=SHStage Name",
		"00082122=ISStage Number",
		"00082124=ISNumber of Stages",
		"00082129=ISNumber of Event Timers",
		"00082128=ISView Number",
		"0008212A=ISNumber of Views in Stage",
		"00082130=DSEvent Elapsed Time(s)",
		"00082132=LOEvent Timer Name(s)",
		"00082142=ISStart Trim",
		"00082143=ISStop Trim",
		"00082144=ISRecommended Display Frame Rate",
		"00082200=CSTransducer Position",
		"00082204=CSTransducer Orientation",
		"00082208=CSAnatomic Structure",

		"00100010=PNPatient's Name",
		"00100020=LOPatient ID",
		"00100021=LOIssuer of Patient ID",
		"00100030=DAPatient's Birth Date",
		"00100032=TMPatient's Birth Time",
		"00100040=CSPatient's Sex",
		"00101000=LOOther Patient IDs",
		"00101001=PNOther Patient Names",
		"00101005=PNPatient's Maiden Name",
		"00101010=ASPatient's Age",
		"00101020=DSPatient's Size",
		"00101030=DSPatient's Weight",
		"00101040=LOPatient's Address",
		"00102150=LOCountry of Residence",
		"00102152=LORegion of Residence",
		"00102180=SHOccupation",
		"001021A0=CSSmoking Status",
		"001021B0=LTAdditional Patient History",
		"00104000=LTPatient Comments",

		"00180010=LOContrast/Bolus Agent",
		"00180015=CSBody Part Examined",
		"00180020=CSScanning Sequence",
		"00180021=CSSequence Variant",
		"00180022=CSScan Options",
		"00180023=CSMR Acquisition Type",
		"00180024=SHSequence Name",
		"00180025=CSAngio Flag",
		"00180030=LORadionuclide",
		"00180031=LORadiopharmaceutical",
		"00180032=DSEnergy Window Centerline",
		"00180033=DSEnergy Window Total Width",
		"00180034=LOIntervention Drug Name",
		"00180035=TMIntervention Drug Start Time",
		"00180040=ISCine Rate",
		"00180050=DSSlice Thickness",
		"00180060=DSKVP",
		"00180070=ISCounts Accumulated",
		"00180071=CSAcquisition Termination Condition",
		"00180072=DSEffective Series Duration",
		"00180080=DSRepetition Time",
		"00180081=DSEcho Time",
		"00180082=DSInversion Time",
		"00180083=DSNumber of Averages",
		"00180084=DSImaging Frequency",
		"00180085=SHImaged Nucleus",
		"00180086=ISEcho Numbers(s)",
		"00180087=DSMagnetic Field Strength",
		"00180088=DSSpacing Between Slices",
		"00180089=ISNumber of Phase Encoding Steps",
		"00180090=DSData Collection Diameter",
		"00180091=ISEcho Train Length",
		"00180093=DSPercent Sampling",
		"00180094=DSPercent Phase Field of View",
		"00180095=DSPixel Bandwidth",
		"00181000=LODevice Serial Number",
		"00181004=LOPlate ID",
		"00181010=LOSecondary Capture Device ID",
		"00181012=DADate of Secondary Capture",
		"00181014=TMTime of Secondary Capture",
		"00181016=LOSecondary Capture Device Manufacturer",
		"00181018=LOSecondary Capture Device Manufacturer's Model Name",
		"00181019=LOSecondary Capture Device Software Version(s)",
		"00181020=LOSoftware Versions(s)",
		"00181022=SHVideo Image Format Acquired",
		"00181023=LODigital Image Format Acquired",
		"00181030=LOProtocol Name",
		"00181040=LOContrast/Bolus Route",
		"00181041=DSContrast/Bolus Volume",
		"00181042=TMContrast/Bolus Start Time",
		"00181043=TMContrast/Bolus Stop Time",
		"00181044=DSContrast/Bolus Total Dose",
		"00181045=ISSyringe Counts",
		"00181050=DSSpatial Resolution",
		"00181060=DSTrigger Time",
		"00181061=LOTrigger Source or Type",
		"00181062=ISNominal Interval",
		"00181063=DSFrame Time",
		"00181064=LOFraming Type",
		"00181065=DSFrame Time Vector",
		"00181066=DSFrame Delay",
		"00181070=LORadionuclide Route",
		"00181071=DSRadionuclide Volume",
		"00181072=TMRadionuclide Start Time",
		"00181073=TMRadionuclide Stop Time",
		"00181074=DSRadionuclide Total Dose",
		"00181080=CSBeat Rejection Flag",
		"00181081=ISLow R-R Value",
		"00181082=ISHigh R-R Value",
		"00181083=ISIntervals Acquired",
		"00181084=ISIntervals Rejected",
		"00181085=LOPVC Rejection",
		"00181086=ISSkip Beats",
		"00181088=ISHeart Rate",
		"00181090=ISCardiac Number of Images",
		"00181094=ISTrigger Window",
		"00181100=DSReconstruction Diameter",
		"00181110=DSDistance Source to Detector",
		"00181111=DSDistance Source to Patient",
		"00181120=DSGantry/Detector Tilt",
		"00181030=DSTable Height",
		"00181131=DSTable Traverse",
		"00181140=CSRotation Direction",
		"00181141=DSAngular Position",
		"00181142=DSRadial Position",
		"00181143=DSScan Arc",
		"00181144=DSAngular Step",
		"00181145=DSCenter of Rotation Offset",
		"00181146=DSRotation Offset",
		"00181147=CSField of View Shape",
		"00181149=ISField of View Dimensions(s)",
		"00181150=ISExposure Time",
		"00181151=ISX-ray Tube Current",
		"00181152=ISExposure",
		"00181160=SHFilter Type",
		"00181170=ISGenerator Power",
		"00181180=SHCollimator/grid Name",
		"00181181=CSCollimator Type",
		"00181182=ISFocal Distance",
		"00181183=DSX Focus Center",
		"00181184=DSY Focus Center",
		"00181190=DSFocal Spot(s)",
		"00181200=DADate of Last Calibration",
		"00181201=TMTime of Last Calibration",
		"00181210=SHConvolution Kernel",
		"00181242=ISActual Frame Duration",
		"00181243=ISCount Rate",
		"00181250=SHReceiving Coil",
		"00181151=SHTransmitting Coil",
		"00181160=SHScreen Type",
		"00181261=LOPhosphor Type",
		"00181300=ISScan Velocity",
		"00181301=CSWhole Body Technique",
		"00181302=ISScan Length",
		"00181310=USAcquisition Matrix",
		"00181312=CSPhase Encoding Direction",
		"00181314=DSFlip Angle",
		"00181315=CSVariable Flip Angle Flag",
		"00181316=DSSAR",
		"00181318=DSdB/dt",
		"00181400=LOAcquisition Device Processing Description",
		"00181401=LOAcquisition Device Processing Code",
		"00181402=CSCassette Orientation",
		"00181403=CSCassette Size",
		"00181404=USExposures on Plate",
		"00181405=ISRelative X-ray Exposure",
		"00185000=SHOutput Power",
		"00185010=LOTransducer Data",
		"00185012=DSFocus Depth",
		"00185020=LOPreprocessing Function",
		"00185021=LOPostprocessing Function",
		"00185022=DSMechanical Index",
		"00185024=DSThermal Index",
		"00185026=DSCranial Thermal Index",
		"00185027=DSSoft Tissue Thermal Index",
		"00185028=DSSoft Tissue-focus Thermal Index",
		"00185029=DSSoft Tissue-surface Thermal Index",
		"00185050=ISDepth of Scan Field",
		"00185100=CSPatient Position",
		"00185101=CSView Position",
		"00185210=DSImage Transformation Matrix",
		"00185212=DSImage Translation Vector",
		"00186000=DSSensitivity",
		"00186011=SQSequence of Ultrasound Regions",
		"00186012=USRegion Spatial Format",
		"00186014=USRegion Data Type",
		"00186016=ULRegion Flags",
		"00186018=ULRegion Location Min X0",
		"0018601A=ULRegion Location Min Y0",
		"0018601C=ULRegion Location Max X1",
		"0018601E=ULRegion Location Max Y1",
		"00186020=SLReference Pixel X0",
		"00186022=SLReference Pixel Y0",
		"00186024=USPhysical Units X Direction",
		"00186026=USPhysical Units Y Direction",
		"00181628=FDReference Pixel Physical Value X",
		"0018602A=FDReference Pixel Physical Value Y",
		"0018602C=FDPhysical Delta X",
		"0018602E=FDPhysical Delta Y",
		"00186030=ULTransducer Frequency",
		"00186031=CSTransducer Type",
		"00186032=ULPulse Repetition Frequency",
		"00186034=FDDoppler Correction Angle",
		"00186036=FDSterring Angle",
		"00186038=ULDoppler Sample Volume X Position",
		"0018603A=ULDoppler Sample Volume Y Position",
		"0018603C=ULTM-Line Position X0",
		"0018603E=ULTM-Line Position Y0",
		"00186040=ULTM-Line Position X1",
		"00186042=ULTM-Line Position Y1",
		"00186044=USPixel Component Organization",
		"00186046=ULPixel Component Organization",
		"00186048=ULPixel Component Range Start",
		"0018604A=ULPixel Component Range Stop",
		"0018604C=USPixel Component Physical Units",
		"0018604E=USPixel Component Data Type",
		"00186050=ULNumber of Table Break Points",
		"00186052=ULTable of X Break Points",
		"00186054=FDTable of Y Break Points",

		"0020000D=UIStudy Instance UID",
		"0020000E=UISeries Instance UID",
		"00200010=SHStudy ID",
		"00200011=ISSeries Number",
		"00200012=ISAcquisition Number",
		"00200013=ISImage Number",
		"00200014=ISIsotope Number",
		"00200015=ISPhase Number",
		"00200016=ISInterval Number",
		"00200017=ISTime Slot Number",
		"00200018=ISAngle Number",
		"00200020=CSPatient Orientation",
		"00200022=USOverlay Number",
		"00200024=USCurve Number",
		"00200032=DSImage Position (Patient)",
		"00200037=DSImage Orientation (Patient)",
		"00200052=UIFrame of Reference UID",
		"00200060=CSLaterality",
		"00200080=UIMasking Image UID",
		"00200100=ISTemporal Position Identifier",
		"00200105=ISNumber of Temporal Positions",
		"00200110=DSTemporal Resolution",
		"00201000=ISSeries in Study",
		"00201002=ISImages in Acquisition",
		"00201004=ISAcquisition in Study",
		"00201040=LOPosition Reference Indicator",
		"00201041=DSSlice Location",
		"00201070=ISOther Study Numbers",
		"00201200=ISNumber of Patient Related Studies",
		"00201202=ISNumber of Patient Related Series",
		"00201204=ISNumber of Patient Related Images",
		"00201206=ISNumber of Study Related Series",
		"00201208=ISNumber of Study Related Images",
		"00204000=LTImage Comments",

		"00280002=USSamples per Pixel",
		"00280004=CSPhotometric Interpretation",
		"00280006=USPlanar Configuration",
		"00280008=ISNumber of Frames",
		"00280009=ATFrame Increment Pointer",
		"00280010=USRows",
		"00280011=USColumns",
		"00280030=DSPixel Spacing",
		"00280031=DSZoom Factor",
		"00280032=DSZoom Center",
		"00280034=ISPixel Aspect Ratio",
		"00280051=CSCorrected Image",
		"00280100=USBits Allocated",
		"00280101=USBits Stored",
		"00280102=USHigh Bit",
		"00280103=USPixel Representation",
		"00280106=USSmallest Image Pixel Value",
		"00280107=USLargest Image Pixel Value",
		"00280108=USSmallest Pixel Value in Series",
		"00280109=USLargest Pixel Value in Series",
		"00280120=USPixel Padding Value",
		"00281050=DSWindow Center",
		"00281051=DSWindow Width",
		"00281052=DSRescale Intercept",
		"00281053=DSRescale Slope",
		"00281054=LORescale Type",
		"00281055=LOWindow Center & Width Explanation",
		"00281101=USRed Palette Color Lookup Table Descriptor",
		"00281102=USGreen Palette Color Lookup Table Descriptor",
		"00281103=USBlue Palette Color Lookup Table Descriptor",
		"00281201=USRed Palette Color Lookup Table Data",
		"00281202=USGreen Palette Color Lookup Table Data",
		"00281203=USBlue Palette Color Lookup Table Data",
		"00283000=SQModality LUT Sequence",
		"00283002=USLUT Descriptor",
		"00283003=LOLUT Explanation",
		"00283004=LOMadality LUT Type",
		"00283006=USLUT Data",
		"00283010=SQVOI LUT Sequence",

		"7FE00010=OXPixel Data",

		"FFFEE000=DLItem",
		"FFFEE00D=DLItem Delimitation Item",
		"FFFEE0DD=DLSequence Delimitation Item"
	};
}

