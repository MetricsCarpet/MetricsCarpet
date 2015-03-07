package ij.plugin;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import ij.*;
import ij.io.*;
import ij.process.*;


/** This plugin opens a tab-delimeted text file as an image. */
public class TextReader implements PlugIn {
	int words = 0, chars = 0, lines = 0;
	String directory, name, path;
	
	public void run(String arg) {
		if (showDialog()) {
			IJ.showStatus("Opening: " + path);
			ImageProcessor ip = open(path);
			if (ip!=null)
				new ImagePlus(name, ip).show();
		}
	}
	
	boolean showDialog() {
		OpenDialog od = new OpenDialog("Open Text Image...", null);
		directory = od.getDirectory();
		name = od.getFileName();
		if (name!=null)
			path = directory + name;
		return name!=null;
	}
	
	/** Displays a file open dialog and opens the specified
		text file as a float image. */
	public ImageProcessor open(){
		if (showDialog())
			return open(path);
		else
			return null;
	}
	
	/** Opens the specified text file as a float image. */
	public ImageProcessor open(String path){
		ImageProcessor ip = null;
		try {
			words = chars = lines = 0;
			Reader r = new BufferedReader(new FileReader(path));
			countLines(r);
			r.close();
			r = new BufferedReader(new FileReader(path));
			int width = words/lines;
			float[] pixels = new float[width*lines];
			ip = new FloatProcessor(width, lines, pixels, null);
			read(r, width*lines, pixels);
			ip.resetMinAndMax();
		}
		catch (Exception e) {
			IJ.showMessage("TextReader", e.getMessage());
			ip = null;
		}
		return ip;
	}
	
	/** Returns the file name. */
	public String getName() {
		return name;
	}

	void countLines(Reader r) throws IOException {
		StreamTokenizer tok = new StreamTokenizer(r);
		int width=1;

		tok.resetSyntax();
		tok.wordChars(33, 255);
		tok.whitespaceChars(0, ' ');
		tok.eolIsSignificant(true);

		while (tok.nextToken() != StreamTokenizer.TT_EOF) {
			switch (tok.ttype) {
				case StreamTokenizer.TT_EOL:
					lines++;
					if (lines==1)
						width = words;
					if (lines%20==0 && width>1 && lines<=width)
						IJ.showProgress(((double)lines/width)/2.0);
					break;
				case StreamTokenizer.TT_WORD:
					words++;
					break;
			}
		}
	}

	void read(Reader r, int size, float[] pixels) throws IOException {
		StreamTokenizer tok = new StreamTokenizer(r);
		tok.resetSyntax();
		tok.wordChars(33, 255);
		tok.whitespaceChars(0, ' ');
		tok.parseNumbers();

		int i = 0;
		int inc = size/20;
		while (tok.nextToken() != StreamTokenizer.TT_EOF) {
			if (tok.ttype==StreamTokenizer.TT_NUMBER) {
				pixels[i++] = (float)tok.nval;
				if (i%inc==0)
					IJ.showProgress(0.5+((double)i/size)/2.0);
			}
		}
		IJ.showProgress(1.0);
	}

}