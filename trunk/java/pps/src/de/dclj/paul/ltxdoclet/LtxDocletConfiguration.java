package de.dclj.paul.ltxdoclet;

//import com.sun.tools.doclets.internal.toolkit.*;
//import com.sun.tools.doclets.formats.html.*;
import com.sun.tools.doclets.*;
import com.sun.javadoc.*;
import java.util.*;
import java.io.*;

import java.nio.charset.Charset;

/**
 * Konfiguration für unser Doclet.
 */
public class LtxDocletConfiguration

{

    /**
     * Das Verzeichnis, in dem alle erzeugten Daten abgelegt werden sollen.
     *
     * Wird durch "-d" festgelegt.
     */
    public File destdir;
    /**
     * Die zu dokumentierende Software ist in diesem {@link RootDoc}-Objekt versteckt.
     */
    public RootDoc root;

    /**
     * Die Liste der Packages.
     */
    public PackageDoc[] packages;

    /**
     * Der Titel des Dokumentes.
     * Wird durch "-doctitle" festgelegt.
     */
    public String doctitle;

    /**
     * Das zu verwendende Encoding für die Ausgabe-Dateien.
     * Wird durch "-docencoding" festgelegt, wie auch beim Standarddoclet.
     */
    public Charset docencoding;

        
    public List<Thread> threads = Collections.synchronizedList(new ArrayList<Thread>());
    public boolean wasError;
    



    public void setOptions(RootDoc rd)
    {
	this.docencoding = Charset.defaultCharset();
	this.destdir = new File(System.getProperty("user.dir"));
	this.doctitle = "Die Package-Sammlung";

	this.root = rd;
	root.printNotice("Lese Optionen ...");
	for(String[] op : rd.options()) {
	    root.printNotice("Option: " + Arrays.toString(op));
	    if (op[0].equals("-d")) {
		this.destdir = new File(op[1]);
		//		root.printNotice("  destdirname: " + destdirname);
		destdir.mkdirs();
	    }
	    if (op[0].equals("-doctitle")) {
		this.doctitle = op[1];
	    }
	    if (op[0].equals("-docencoding")) {
		this.docencoding = Charset.forName(op[1]);
	    }
	}
	this.packages = rd.specifiedPackages();
	// TODO
	root.printNotice("... Optionen gelesen.");
    }

    public int optionLength(String option) {
	if ("-d".equals(option))
	    return 2;
	// TODO: weitere Optionen
	return 0;
    }

    public boolean validOptions(String[][] options, DocErrorReporter rep) {
	// TODO
	return true;
    }


    public void setSpecificDocletOptions(RootDoc root)
    {
        if (doctitle == null)
            {
                doctitle = "Die Package-Sammlung";
            }
        //...
    }
        
    //     public WriterFactory getWriterFactory() {
    //  return null;
    //     }

    //     public Comparator getMemberComparator() {
    //  return null;
    //     }

    public int specificDocletOptionLength(String option)
    {

        if (option.equals("-d"))
            return 2;
        if (option.equals("-doctitle"))
            return 2;
        return 0;
    }


    public boolean specificDocletValidOptions(String[][] ops, DocErrorReporter err)
    {
        //...
        return true;
    }


    String toInputFileName(PackageDoc d)
    {
        return d.toString().replace('.', '/');
    }
        
    File toOutputFileName(PackageDoc d)
    {
        String newName = d.toString().replace('.', '/');
        File dir =  new File(destdir, newName);
        dir.mkdirs();
        return dir;
    }



}
