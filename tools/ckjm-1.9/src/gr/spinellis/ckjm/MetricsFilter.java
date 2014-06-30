/*
 * $Id: \\dds\\src\\Research\\ckjm.RCS\\src\\gr\\spinellis\\ckjm\\MetricsFilter.java,v 1.9 2005/08/10 16:53:36 dds Exp $
 *
 * (C) Copyright 2005 Diomidis Spinellis
 *
 * Permission to use, copy, and distribute this software and its
 * documentation for any purpose and without fee is hereby granted,
 * provided that the above copyright notice appear in all copies and that
 * both that copyright notice and this permission notice appear in
 * supporting documentation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND WITHOUT ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTIES OF
 * MERCHANTIBILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 */

package gr.spinellis.ckjm;

import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import org.apache.bcel.Repository;
import org.apache.bcel.Constants;
import org.apache.bcel.util.*;
import java.io.*;
import java.util.*;

/**
 * Convert a list of classes into their metrics.
 * Process standard input lines or command line arguments
 * containing a class file name or a jar file name,
 * followed by a space and a class file name.
 * Display on the standard output the name of each class, followed by its
 * six Chidamber Kemerer metrics:
 * WMC, DIT, NOC, CBO, RFC, LCOM
 *
 * @see ClassMetrics
 * @version $Revision: 1.9 $
 * @author <a href="http://www.spinellis.gr">Diomidis Spinellis</a>
 */
public class MetricsFilter {
    /** True if the measurements should include calls to the Java JDK into account */
    private static boolean includeJdk = false;

    /** True if the reports should only include public classes */
    private static boolean onlyPublic = false;

    /** Return true if the measurements should include calls to the Java JDK into account */
    public static boolean isJdkIncluded() { return includeJdk; }
    /** Return true if the measurements should include all classes */
    public static boolean includeAll() { return !onlyPublic; }

    /**
     * Load and parse the specified class.
     * The class specification can be either a class file name, or
     * a jarfile, followed by space, followed by a class file name.
     */
    static void processClass(ClassMetricsContainer cm, String clspec) {
	int spc;
	JavaClass jc = null;

	if ((spc = clspec.indexOf(' ')) != -1) {
	    String jar = clspec.substring(0, spc);
	    clspec = clspec.substring(spc + 1);
	    try {
		jc = new ClassParser(jar, clspec).parse();
	    } catch (IOException e) {
		System.err.println("Error loading " + clspec + " from " + jar + ": " + e);
	    }
	} else {
	    try {
		jc = new ClassParser(clspec).parse();
	    } catch (IOException e) {
		System.err.println("Error loading " + clspec + ": " + e);
	    }
	}
	if (jc != null) {
	    ClassVisitor visitor = new ClassVisitor(jc, cm);
	    visitor.start();
	    visitor.end();
	}
    }

    /**
     * The interface for other Java based applications.
     * Implement the outputhandler to catch the results
     *
     * @param files Class files to be analyzed
     * @param outputHandler An implementation of the CkjmOutputHandler interface
     */
    public static void runMetrics(String[] files, CkjmOutputHandler outputHandler) {
        ClassMetricsContainer cm = new ClassMetricsContainer();

        for (int i = 0; i < files.length; i++)
            processClass(cm, files[i]);
        cm.printMetrics(outputHandler);
    }

    /** The filter's main body.
     * Process command line arguments and the standard input.
     */
    public static void main(String[] argv) {
	int argp = 0;

	if (argv.length > argp && argv[argp].equals("-s")) {
	    includeJdk = true;
	    argp++;
	}
	if (argv.length > argp && argv[argp].equals("-p")) {
	    onlyPublic = true;
	    argp++;
	}
	ClassMetricsContainer cm = new ClassMetricsContainer();

	if (argv.length == argp) {
	    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
	    try {
		String s;
		while ((s = in.readLine()) != null)
		    processClass(cm, s);
	    } catch (Exception e) {
		System.err.println("Error reading line: " + e);
		System.exit(1);
	    }
	}

	for (int i = argp; i < argv.length; i++)
	    processClass(cm, argv[i]);

	CkjmOutputHandler handler = new PrintPlainResults(System.out);
	cm.printMetrics(handler);
    }
}
