package ij.plugin.frame;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import ij.*;
import ij.plugin.*;
import ij.plugin.frame.*;
import ij.text.*;
import ij.gui.*;
import ij.util.*;
import ij.io.*;
import ij.process.*;
import ij.measure.*;

/** This is ImageJ's macro recorder. */
public class Recorder extends PlugInFrame implements PlugIn, ActionListener, ItemListener {

	public static boolean record;
	private Button makePlugin;
	private Checkbox recordCB;
	private String fitTypeStr = CurveFitter.fitList[0];
	private static TextArea textArea;
	private static Frame instance;
	private static String commandName;
	private static String commandOptions;

	public Recorder() {
		super("Recorder");
		if (instance!=null) {
			instance.toFront();
			return;
		}
		instance = this;
		Panel panel = new Panel();
		record = true;
		recordCB = new Checkbox("Record", record);
		recordCB.addItemListener(this);
		panel.add(recordCB);
		makePlugin = new Button("Create Plugin");
		makePlugin.addActionListener(this);
		panel.add(makePlugin);
		add("North", panel);
		textArea = new TextArea("",20,80,TextArea.SCROLLBARS_VERTICAL_ONLY);
		textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
		add("Center", textArea);
		pack();
		GUI.center(this);
		setVisible(true);
		IJ.register(Recorder.class);
	}

	public static void record(String method) {
		if (textArea==null)
			return;
		textArea.append(method+"();\n");
	}

	public static void setCommand(String command) {
		if (textArea==null)
			return;
		commandName = command;
		commandOptions = null;
		//IJ.write("setCommand: "+command);
	}

	public static void record(String method, String arg) {
		if (textArea==null)
			return;
		textArea.append(method+"(\""+arg+"\");\n");
	}

	static String fixPath (String path) {
		StringBuffer sb = new StringBuffer();
		char c;
		for (int i=0; i<path.length(); i++) {
			sb.append(c=path.charAt(i));
			if (c=='\\')
				sb.append("\\");
		}
		return new String(sb);
	}

	public static void record(String method, int a1, int a2) {
		if (textArea==null)
			return;
		textArea.append(method+"("+a1+", "+a2+");\n");
	}

	public static void record(String method, int a1, int a2, int a3) {
		if (textArea==null)
			return;
		textArea.append(method+"("+a1+", "+a2+", "+a3+");\n");
	}

	public static void record(String method, String args, int a1, int a2) {
		if (textArea==null)
			return;
		method = "//"+method;
		textArea.append(method+"(\""+args+"\", "+a1+", "+a2+");\n");
	}

	public static void record(String method, int a1, int a2, int a3, int a4) {
		if (textArea==null)
			return;
		textArea.append(method+"("+a1+", "+a2+", "+a3+", "+a4+");\n");
	}

	public static void record(String method, String path, String args, int a1, int a2, int a3, int a4, int a5) {
		if (textArea==null)
			return;
		path = fixPath(path);
		method = "//"+method;
		textArea.append(method+"(\""+path+"\", "+"\""+args+"\", "+a1+", "+a2+", "+a3+", "+a4+", "+a5+");\n");
	}
	
	public static void recordOption(String key, String value) {
		key = trimKey(key);
		value = addQuotes(value);
		if (commandOptions==null)
			commandOptions = key+"="+value;
		else
			commandOptions += " "+key+"="+value;
		//IJ.write("  "+key+"="+value);
	}

	public static void recordPath(String path) {
		String key = "path";
		path = fixPath(path);
		path = addQuotes(path);
		if (commandOptions==null)
			commandOptions = key+"="+path;
		else
			commandOptions += " "+key+"="+path;
		//IJ.write("  "+key+"="+value);
	}

	public static void recordOption(String key) {
		key = trimKey(key);
		if (commandOptions==null)
			commandOptions = key;
		else
			commandOptions += " "+key;
		//IJ.write("  "+key+"="+value);
	}
	
	static String trimKey(String key) {
		int index = key.indexOf(" ");
		if (index>-1)
			key = key.substring(0,index);
		index = key.indexOf(":");
		if (index>-1)
			key = key.substring(0,index);
		key = key.toLowerCase();
		return key;
	}

	public static void saveCommand() {
		if (commandName!=null) {
			if (commandOptions!=null)
				textArea.append("run(\""+commandName+"\", \""+commandOptions+"\");\n");
			else
				textArea.append("run(\""+commandName+"\");\n");
		}
		commandName = null;
		commandOptions = null;
	}

	static String addQuotes(String value) {
		int index = value.indexOf(' ');
		if (index>-1)
			value = "'"+value+"'";
		return value;
	}

	void createPlugin() {
		String text = textArea.getText();
		StringBuffer sb = new StringBuffer();
		int i = 0;
		if (text.startsWith("//")) {
			sb.append("\t\t//IJ.");
			i += 2;
		} else
			sb.append("\t\tIJ.");
		char c;
		int len = text.length();
		while (i<text.length()) {
			sb.append(c=text.charAt(i));
			if (c=='\n' && i<(len-1)) {
				sb.append("\t\t");
				if (text.charAt(i+1)!='/')
					sb.append("IJ.");
				else {
					sb.append("//IJ.");
					i += 2;
				}
			}
			i++;
		}
		//sb.append("\n");
		IJ.runPlugIn("ij.plugin.NewPlugin", new String(sb));
	}
	
	public void itemStateChanged(ItemEvent e) {
		record = recordCB.getState();
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource()==makePlugin)
			createPlugin();
	}

    public void windowClosing(WindowEvent e) {
		super.windowClosing(e);
		record = false;
		textArea = null;
		instance = null;	
	}

}