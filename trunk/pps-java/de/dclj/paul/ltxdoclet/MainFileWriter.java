package paul.ltxdoclet;

import java.io.*;
import com.sun.javadoc.*;
import java.util.*;

public class MainFileWriter
	extends LaTeXWriter
{

	public MainFileWriter()
		throws IOException
	{
		super (new File(configuration.destdirname, "doku-main.tex"));
	}

	public void writeDoku()
	{
		writePackages();
		configuration.root.printNotice("ltxdoclet: doku-main.tex wird erstellt ...");
		println("   % Damit beim Compilieren nicht bei jedem Fehler angehalten wird");
		println("\\scrollmode");
		println();
		println("   % Report scheint für eine API jedenfalls besser als Artikel");
		println("\\documentclass[final, 11pt, a4paper]{report}");
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
			catch(InterruptedException ex){}
		}     // of while
	}        // of MainFileWriter.waitForAllThreads()
	
	private void writePreamble()
	{
		println("\\usepackage{hyperref}");
		if (Locale.getDefault().getLanguage().equals("de"))
		{
			println("  % Neue deutsche Silbentrennung");
			println("\\usepackage{ngerman}");
			println();
		}
		println("\\usepackage{enumerate}");
		println();
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
			writeTags(pd.firstSentenceTags());
			
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
						configuration.threads.remove(Thread.currentThread());
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
