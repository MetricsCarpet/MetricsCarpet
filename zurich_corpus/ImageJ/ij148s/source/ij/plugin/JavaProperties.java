package ij.plugin;
import ij.*;
import ij.text.*;
import ij.io.OpenDialog;
import ij.gui.GUI;
import java.awt.*;
import java.util.*;
import java.applet.Applet;

/** Displays the Java system properties in a text window. */
public class JavaProperties implements PlugIn {

	StringBuffer sb = new StringBuffer();
	
	public void run(String arg) {
		show("java.version");
		show("java.vendor");
		if (IJ.isMacintosh()) show("mrj.version");
		show("os.name");
		show("os.version");
		show("os.arch");
		show("file.separator");
		show("path.separator");
		
		String s = System.getProperty("line.separator");
		char ch1, ch2;
		String str1, str2="";
		ch1 = s.charAt(0);
		if (ch1=='\r')
			str1 = "<cr>";
		else
			str1 = "<lf>";
		if (s.length()==2) {
			ch2 = s.charAt(1);
			if (ch2=='\r')
				str2 = "<cr>";
			else
				str2 = "<lf>";
		}
		sb.append("  line.separator: " + str1 + str2+"\n");
			
		Applet applet = IJ.getApplet();
		if (applet!=null) {
			sb.append("\n");
			sb.append("  code base: "+applet.getCodeBase()+"\n");
			sb.append("  document base: "+applet.getDocumentBase()+"\n");
			sb.append("  sample images dir: "+Prefs.getImagesURL()+"\n");
			TextWindow tw = new TextWindow("Properties", new String(sb), 400, 400);
			return;
		}
		sb.append("\n");
		show("user.name");
		show("user.home");
		show("user.dir");
		show("user.country");
		show("file.encoding");
		show("java.home");
		show("java.compiler");
		show("java.class.path");
		show("java.ext.dirs");
		show("java.io.tmpdir");
		
		sb.append("\n");
		String userDir = System.getProperty("user.dir");
		String userHome = System.getProperty("user.home");
		String osName = System.getProperty("os.name");
		sb.append("  IJ.getVersion: "+IJ.getVersion()+"\n");
		sb.append("  IJ.getFullVersion: "+IJ.getFullVersion()+"\n");
		sb.append("  IJ.isJava2: "+IJ.isJava2()+"\n");
		sb.append("  IJ.isJava15: "+IJ.isJava15()+"\n");
		sb.append("  IJ.isJava16: "+IJ.isJava16()+"\n");
		sb.append("  IJ.isLinux: "+IJ.isLinux()+"\n");
		sb.append("  IJ.isMacintosh: "+IJ.isMacintosh()+"\n");
		sb.append("  IJ.isMacOSX: "+IJ.isMacOSX()+"\n");
		sb.append("  IJ.isWindows: "+IJ.isWindows()+"\n");
		sb.append("  IJ.is64Bit: "+IJ.is64Bit()+"\n");
		sb.append("\n");
		sb.append("  IJ.getDirectory(\"imagej\"): "+ IJ.getDirectory("imagej") +"\n");
		sb.append("  IJ.getDirectory(\"home\"): "+ IJ.getDirectory("home") +"\n");
		sb.append("  IJ.getDirectory(\"plugins\"): "+ IJ.getDirectory("plugins") +"\n");
		sb.append("  IJ.getDirectory(\"macros\"): "+ IJ.getDirectory("macros") +"\n");
		sb.append("  IJ.getDirectory(\"luts\"): "+ IJ.getDirectory("luts") +"\n");
		sb.append("  IJ.getDirectory(\"current\"): "+ IJ.getDirectory("current") +"\n");
		sb.append("  IJ.getDirectory(\"temp\"): "+ IJ.getDirectory("temp") +"\n");
		sb.append("  IJ.getDirectory(\"default\"): "+ IJ.getDirectory("default") +"\n");
		sb.append("  IJ.getDirectory(\"image\"): "+ IJ.getDirectory("image") +"\n");
		sb.append("\n");
		sb.append("  Menus.getPlugInsPath: "+Menus.getPlugInsPath()+"\n");
		sb.append("  Menus.getMacrosPath: "+Menus.getMacrosPath()+"\n");
		sb.append("  Prefs.getImageJDir: "+Prefs.getImageJDir()+"\n");	
		sb.append("  Prefs.getThreads: "+Prefs.getThreads()+cores());	
		sb.append("  Prefs.open100Percent: "+Prefs.open100Percent+"\n");		
		sb.append("  Prefs.blackBackground: "+Prefs.blackBackground+"\n");		
		sb.append("  Prefs.useJFileChooser: "+Prefs.useJFileChooser+"\n");		
		sb.append("  Prefs.weightedColor: "+Prefs.weightedColor+"\n");		
		sb.append("  Prefs.blackCanvas: "+Prefs.blackCanvas+"\n");		
		sb.append("  Prefs.pointAutoMeasure: "+Prefs.pointAutoMeasure+"\n");		
		sb.append("  Prefs.pointAutoNextSlice: "+Prefs.pointAutoNextSlice+"\n");		
		sb.append("  Prefs.requireControlKey: "+Prefs.requireControlKey+"\n");		
		sb.append("  Prefs.useInvertingLut: "+Prefs.useInvertingLut+"\n");		
		sb.append("  Prefs.antialiasedTools: "+Prefs.antialiasedTools+"\n");		
		sb.append("  Prefs.useInvertingLut: "+Prefs.useInvertingLut+"\n");		
		sb.append("  Prefs.intelByteOrder: "+Prefs.intelByteOrder+"\n");		
		sb.append("  Prefs.doubleBuffer: "+Prefs.doubleBuffer+"\n");		
		sb.append("  Prefs.noPointLabels: "+Prefs.noPointLabels+"\n");		
		sb.append("  Prefs.disableUndo: "+Prefs.disableUndo+"\n");		
		sb.append("  Prefs dir: "+Prefs.getPrefsDir()+"\n");
		sb.append("  Current dir: "+OpenDialog.getDefaultDirectory()+"\n");
		sb.append("  Sample images dir: "+Prefs.getImagesURL()+"\n");
		sb.append("  Memory in use: "+IJ.freeMemory()+"\n");	
		Dimension d = IJ.getScreenSize();
		sb.append("  Screen size: " + d.width + "x" + d.height+"\n");
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		String b1 = toString(GUI.getMaxWindowBounds());
		String b2 = toString(ge.getMaximumWindowBounds());
		if (!b2.equals(b1))
			b1 += " (" + b2 + ")";
		sb.append("  Max window bounds: " + b1 + "\n");
		listMonitors(ge, sb);
		System.gc();
		doFullDump();
		if (IJ.getInstance()==null)
			IJ.log(new String(sb));
		else
			new TextWindow("Properties", new String(sb), 400, 500);
	}
	
	private void listMonitors(GraphicsEnvironment ge, StringBuffer sb) {
		int max = 10;
		String[] str = new String[max];
		int n = 0;
		Rectangle bounds2 = null;
		GraphicsDevice[] gs = ge.getScreenDevices();
		for (int j=0; j<gs.length; j++) {
			GraphicsDevice gd = gs[j];
			GraphicsConfiguration[] gc = gd.getConfigurations();
			for (int i=0; i<gc.length; i++) {
				Rectangle bounds = gc[i].getBounds();
				if (bounds!=null && !bounds.equals(bounds2) && n<max) {
					str[n++] = toString(bounds);
					bounds2 = bounds;
				}
			}
		}
		if (n>1) {
			Rectangle ub = GUI.getUnionOfBounds();
			if (ub!=null)
				sb.append("  Union of bounds: " + toString(ub) + "\n");
			for (int i=0; i<n; i++)
				sb.append("  Monitor"+(i+1)+": " + str[i] + "\n");
		}
	}

	private String toString(Rectangle r) {
		if (r==null) return "";
		String s = r.toString();
		return s.substring(19, s.length()-1);
	}
	
	String cores() {
		int cores = Runtime.getRuntime().availableProcessors();
		if (cores==1)
			return " (1 core)\n";
		else
			return " ("+cores+" cores)\n";
	}
	
	void show(String property) {
		String p = System.getProperty(property);
		if (p!=null)
			sb.append("  " + property + ": " + p+"\n");
	}
	
	void doFullDump() {
		sb.append("\n");
		sb.append("All Java Properties\n");
		Properties props = System.getProperties();
		for (Enumeration en=props.keys(); en.hasMoreElements();) {
			String key = (String)en.nextElement();
			sb.append("  "+key+": "+(String)props.get(key)+"\n");
		}
	}

}
