package de.dclj.paul.ltxdoclet;

import java.io.*;
import com.sun.javadoc.*;
import java.util.*;

import  java.nio.charset.Charset;

/**
 * Ein Writer für die Haupt-Datei.
 */
public class MainFileWriter
    extends LaTeXWriter
{

    /**
     *
     */
    public MainFileWriter()
	throws IOException
    {
	super (new File(configuration.destdir, "doku-main.tex"));
    }


    /**
     * Erstellt die komplette Doku für das Projekt.
     * 
     * Dazu werden in parallelen Threads {@link PackageWriter} für die einzelnen Packages
     * aufgerufen und dann die Main-Datei erstellt.
     */
    public void writeDoku()
    {
	writePackages();
	configuration.root.printNotice("ltxdoclet: doku-main.tex wird erstellt ...");
	println("   % Damit beim Compilieren nicht bei jedem Fehler angehalten wird");
	println("\\scrollmode");
	println();
	writePreamble();
	println("\\begin{document}");
	println();
	chapter("Übersicht", false);
	ltxwrite( configuration.doctitle + " besteht aus den folgenden Packages. Eine");
	ltxwrite( " kurze Beschreibung folgt danach.");
	section("Package-Liste");
	writePackageList();
	section("Beschreibung");
	writeOverview();
	//      println("\renewcommand{\thechapter}{\ara{chapter}}");
	println("\\setcounter{chapter}{0}");
	writePackageImports();
	println("\\appendix");
	//...
	println("\\end{document}");
	close();
	configuration.root.printNotice("ltxdoclet: ... doku-main.tex fertig.");
	configuration.root.printNotice("ltxdoclet: warte auf Beendigung der anderen Dateien ...");
	waitForAllThreads();
	configuration.root.printNotice("ltxdoclet: Fertig!");
    }
	
	
    /**
     * Wartet, bis alle Threads in LaTeXWriter.configuration.threads
     * beendet wurden und sich aus der Liste streichen.
     */
    public void waitForAllThreads()
    {
	List threads = configuration.threads;
	while(true)
	    {
		Thread akt;
		synchronized(threads)
		    {
			if (!configuration.threads.isEmpty())
			    akt =(Thread)threads.get(0);
			else 
			    break;
		    }
		try 
		    {
			akt.join();
		    }
		catch(InterruptedException ex){
		    Thread.currentThread().interrupt();
		    configuration.wasError = true;
		    return;
		}
	    }     // of while
    }        // of MainFileWriter.waitForAllThreads()

    /**
     * Kopiert eine Datei (z.B. ein LaTeX-Paket) aus unseren
     * Programm-Ressourcen in das Ausgabeverzeichnis.
     * @param packageName der Name der Datei, z.B. {@code ltxdoclet.sty}.
     */
    private void copyPackage(String packageName)
    {
	try {
	InputStream in =
	    new BufferedInputStream(MainFileWriter.class.getResourceAsStream(packageName));
	OutputStream out =
	    new BufferedOutputStream(new FileOutputStream(new File(configuration.destdir, packageName)));
	pipeInToOut(in, out);
	in.close();
	out.close();
	}
	catch(IOException io) {
	    throw new RuntimeException("konnte das Package nicht kopieren: ",
				       io);
	}
    }

    /**
     * Leitet den kompletten Inhalt einen InputStreams
     * in einen OutputStream um.
     *
     * Diese Methode liest und schreibt jedes byte einzeln - man sollte
     * also gepufferte Streams verwenden, damit es nicht zu langsam wird.
     */
    private void pipeInToOut(InputStream in, OutputStream out)
	throws IOException
    {
	int r;
	while(0 <= (r = in.read())) {
	    out.write(r);
	}
    }

	
    private void writePreamble()
    {
	println("   % Report scheint für eine API jedenfalls besser als Artikel");
	println("\\documentclass[final, 11pt, a4paper]{scrreprt}");
	println();
	println("\\usepackage[" +
		translateEncoding(configuration.docencoding) +
		"]{inputenc}%  Kodierung der Eingabedateien");
	println("\\usepackage[T1]{fontenc}%  Kodierung der Schriften");
	println("\\usepackage{textcomp}");
	println("\\usepackage[scaled]{luximono}% andere Schriftart");
	if (Locale.getDefault().getLanguage().equals("de"))
	    {
		println("  % Neue deutsche Silbentrennung");
		println("\\usepackage{ngerman}");
		println();
	    }
	println("\\usepackage[pdfborderstyle={/S/U/W 1}]{hyperref}");
	println("\\usepackage{enumerate}");
	println();
	println("\\usepackage[dvipsnames]{color}");
	println();
	println("\\usepackage{ltxdoclet}");
	println();
	;
	copyPackage("ltxdoclet.sty");
    }

    /**
     * Übersetzt einen Java-Encoding-Namen in den passenden Namen für
     * das inputenc-Paket.
     */
    private String translateEncoding(Charset javaName) {
	if(javaName.toString().equals("UTF-8")) {
	    return "utf8";
	}
	return javaName.toString().toLowerCase();
    }

	
    private void writeOverview()
    {
	//... 
    }
	
    private void writePackageList()
    {
	println("   % Liste der Packages:");
	println("\\begin{enumerate}[1.]");
	PackageDoc[] pkgs = configuration.packages;
	for (int i = 0; i < pkgs.length; i++)
	    {
		PackageDoc pd = pkgs[i];
		println("\\item " + asLaTeXString(pd) + "\\dotfill " + referenceTo(pd));
		newLine();
		writeInlineTags(pd.firstSentenceTags());
			
	    }
	println("\\end{enumerate}");
    }

    private void writePackageImports()
    {
	PackageDoc[] pkgs = configuration.packages;
	for (int i = 0; i < pkgs.length; i++)
	    {
		String pkgName = configuration.toInputFileName(pkgs[i]);
		println("\\input{"+pkgName+"/package-doc.tex"+ "}");
	    }        // of for
    }
	
    private void writePackages()
    {
	configuration.root.printNotice("Package-Dokus werden erstellt ...");
	PackageDoc[] pkgs = configuration.packages;
	for (int i = 0; i < pkgs.length; i++)
	    {
		final PackageDoc pd = pkgs[i];
		Thread thread = new Thread(pd+"-Writer")
		    {
			public void run()
			{
			    try
				{
				    new PackageWriter(pd).writeDoc();
				}
			    catch(IOException io)
				{
				    configuration.root.printError("Ausgabe für " + pd + " konnte " +
								  "nicht geschrieben werden!");
				}
			    catch(RuntimeException ex) {
				configuration.wasError = true;
				throw ex;
			    }
			    finally {
				configuration.threads.remove(this);
			    }
			}        // of run()
		    };
		configuration.threads.add(thread);
		thread.start();
	    }        // of for
    }        // of MainFileWriter.writePackages()
	
    private void writeIndex()
    {
    }


  
	

}
