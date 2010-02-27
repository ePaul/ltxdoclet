package de.dclj.paul.ltxdoclet;

//import com.sun.tools.doclets.internal.toolkit.*;
//import com.sun.tools.doclets.formats.html.*;
import com.sun.tools.doclets.*;
import com.sun.javadoc.*;
import java.util.*;
import java.io.*;

// Compiler- und Tree-API.
import javax.tools.*;
import com.sun.source.util.*;
import com.sun.source.tree.*;


import java.nio.charset.Charset;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import javax.tools.StandardJavaFileManager;
import java.util.Set;
import java.util.HashSet;
import javax.tools.JavaFileObject;
import javax.tools.JavaCompiler.CompilationTask;
import java.util.Arrays;
import java.util.List;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import javax.lang.model.util.Elements;
import java.util.Map;

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


    /**
     * Sollen Quelltexte mit aufgenommen werden?
     * Falls kein Compiler gefunden wird, wird das
     * automatisch auf false gesetzt, auch wenn die
     * entsprechende Option gesetzt war.
     */
    public boolean includeSource = false;

        
    public List<Thread> threads = Collections.synchronizedList(new ArrayList<Thread>());
    public boolean wasError;


    Set<String> javacOptions =
	new HashSet<String>(Arrays.asList(new String[]{
		    "-classpath", "-sourcepath",
		    "-encoding", "-source"
		}));

    

//     /**
//      * Ansatz hier: ein JavacTask pro Datei.
//      * Vorteil: Wir können den starten, wenn
//      * wir ihn brauchen, anstatt erst eine
//      * Liste aller Dateien zusammenzusammeln.
//      *
//      * Nachteil: wahrscheinlich dauert es so deutlich
//      * länger als gleich einen Task für alle Dateien
//      * zu starten, und nachher nur darauf zuzugreifen.
//      *
//      * Hmm, mal sehen.
//      */
//     Map<File, JavacTask> compilationMap =
// 	new HashMap<File, JavacTask>();

    /**
     * Startet den Java-Compiler, um Quelltexte einfügen zu können.
     * Diese Methode wird nur aufgerufen, wenn die entsprechende
     * Option gesetzt war.
     *
     * Falls es beim Compilieren einen Fehler gibt, wird
     * das Quelltextanzeigen abgeschaltet und ein Fehler ausgegeben.
     */
    void startCompiler() {
	JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
	if (compiler == null) {
	    // kein Compiler da => kein Quelltext!
	    root.printError("Es gibt keinen Java-Compiler.");
	    this.includeSource = false;
	    return;
	}
	StandardJavaFileManager fileManager =
	    // TODO: eventuell Encoding setzen?
	    compiler.getStandardFileManager(null, null, null);

	Set<File> files = new HashSet<File>();
	for (ClassDoc klasse : root.classes()) {
	    files.add(klasse.position().file());
	}
	for (PackageDoc pckg : root.specifiedPackages()) {
	    if (pckg.position() != null) {
		files.add(pckg.position().file());
	    }
	}
	Iterable<? extends JavaFileObject> jFiles =
	    fileManager.getJavaFileObjectsFromFiles(files);
	List<String> params = new ArrayList<String>();
	for(String[] option : root.options()) {
	    if (javacOptions.contains(option[0])) {
		params.addAll(Arrays.asList(option));
	    }
	}
	
	CompilationTask task =
	    compiler.getTask(null /* TODO: ein Writer, der die Daten
				     an root.print... weitergibt. */,
			     fileManager /* TODO: ein FileManager, der zu
					    schreibende Daten einfach
					    wegwirft. */,
			     null /* DiagnosticsListener */,
			     params /* options - TODO: auswahl an Optionen
				     von javadoc weitergeben. */,
			     null /* annotation processors */,
			     jFiles /* Dateien, die zu kompilieren sind.*/
			     );
	if(! (task instanceof JavacTask)) {
	    // irgend ein anderer Compiler als Javac,
	    // ohne die passenden Methoden
	    root.printError("Der CompilerTask " + task + " ist kein "+
			    "JavacTask, damit können wir leider "+
			    "die Tree-API nicht verwenden und daher "+
			    "keinen Quelltext ausdrucken.");
	    this.includeSource = false;
	    return;
	}
	JavacTask jtask = (JavacTask)task;
	try {
	    jtask.parse();
	    jtask.analyze();
	}
	catch(IOException io) {
	    throw new RuntimeException(io);
	}
	
	this.pp = new PrettyPrinter(jtask.getElements(),
				    jtask.getTypes(),
				    Trees.instance(jtask));
				    
	root.printNotice("Javac-Task: " + jtask);
    }

    PrettyPrinter pp;


//     public void /* TODO */ getSource(MemberDoc member)
//     {
// 	try {
// 	SourcePosition pos = member.position();
// 	File file = pos.file();
// 	JavacTask task = compilationMap.get(file);
// 	if (task == null) {
// 	    // TODO
// 	}
// 	// Hmm, ist unser CompilationUnitTree wirklich der erste?
// 	CompilationUnitTree cuTree = task.parse().next();
	


// 	}
// 	catch (IOException io) {
// 	    StringBuilder b = new StringBuilder();
// 	    b.append(io);
// 	    for(StackTraceElement ste : io.getStackTrace()) {
// 		b.append("\n");
// 		b.append(ste);
// 	    }
// 	    root.printError(b.toString());
// 	}
//     }



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
	    if (op[0].equals("-includesource")) {
		this.includeSource = true;
	    }
	}
	this.packages = rd.specifiedPackages();
	// TODO
	root.printNotice("... Optionen gelesen.");
    }


    private Map<String, Integer> optionLengths;

    {
	optionLengths = new HashMap<String,Integer>();
	optionLengths.put("-d", 2);
	optionLengths.put("-includesource", 1);
	optionLengths.put("-docencoding", 2);
	optionLengths.put("-doctitle", 2);
	optionLengths.put("-link", 2);
	optionLengths.put("-linkhtml", 2);
	optionLengths.put("-linkoffline", 3);
	optionLengths.put("-linkofflinehtml", 3);
	optionLengths.put("-linkfootnotehtml", 4);
	optionLengths.put("-linkendhtml", 4);
	optionLengths.put("-linkofflinefootnotehtml", 3);
	optionLengths.put("-linkofflineendhtml", 3);
	optionLengths.put("-linkpdf", 2);
	optionLengths.put("-linkofflinepdf", 3);
	optionLengths.put("-linkfootnotepdf", 4);
	optionLengths.put("-linkendpdf", 4);
	for(String op : javacOptions) {
	    optionLengths.put(op, 2);
	}
	
    }





    /**
     * Ermittelt, ob dieses Doclet eine Option annimmt, und
     * wenn ja, wie viele Argumente sie nimmt.
     * @return die Anzahl der Kommandozeilenargumente, die
     *    diese Option darstellen, inklusive der Option selbst.
     */
    public int optionLength(String option) {
	if ("-help".equals(option)) {
	    System.out.println(optionHelp());
	    return 1;
	}
	Integer r = optionLengths.get(option);
	return r == null ? 0 : r;
    }

    public String optionHelp() {
	String text =
	    "Optionen des LaTeX-Doclets:"
	    +"\n---------------------------"
	    +"\n"
	    +"\nAusgabe-Optionen:"
	    +"\n"
	    +"\n -d 〈dir〉           Verzeichnis, in dem die erzeugten Dateien"
	    +"\n                    abgelegt werden sollen."
	    +"\n -docencoding 〈enc〉 Name der Kodierung für die LaTeX-Dateien."
	    +"\n                    Default ist die Default-Kodierung des"
	    +"\n                    Systems (zur Zeit "+Charset.defaultCharset()
	    + ")."
	    +"\n -doctitle 〈title〉  Titel des Dokumentes."
	    +"\n"
	    +"\nLink-Optionen:"
	    +"\n"
	    +"\n -linkhtml 〈url〉    Erstelle externe Links zu einer"
	    +"\n                    Javadoc-HTML-Doku."
	    +"\n -link 〈url〉        Synonym für -linkhtml"
	    +"\n -linkfootnotehtml 〈url〉 〈linktitle〉"
	    +"\n                    Erstellt externe Links zu einer"
	    +"\n                    Javadoc-HTML-Doku als Fußnoten,"
	    +"\n                    mit gegebenen Titel für den Verweis."
	    +"\n -linkendhtml 〈url〉 〈linktitle〉"
	    +"\n                    Erstellt externe Links am Ende zu einer"
	    +"\n                    Javadoc-HTML-Doku, mit gegebenen Titel"
	    +"\n                    für den Verweis."
	    +"\n -linkofflinehtml 〈url〉 〈package-list-url〉"
	    +"\n                    wie -linkhtml, aber sucht die Package-Liste"
	    +"\n                    nicht bei 〈url〉, sondern bei"
	    +"\n                    〈package-list-url〉."
	    +"\n -linkoffline 〈url〉 〈package-list-url〉"
	    +"\n                    Synonym für -linkofflinehtml〉"
	    +"\n -linkofflinefootnotehtml 〈url〉 〈package-list-url〉"
	    +"\n                    wie -linkfootnotehtml, aber sucht die"
	    +"\n                    Package-Liste nicht bei 〈url〉, sondern bei"
	    +"\n                    〈package-list-url〉."
	    +"\n -linkofflineendhtml 〈url〉 〈package-list-url〉"
	    +"\n                    wie -linkendhtml, aber sucht die"
	    +"\n                    Package-Liste nicht bei 〈url〉, sondern bei"
	    +"\n                    〈package-list-url〉."
	    +"\n -linkpdf 〈pdf-url〉 Erstellt Links zu einer anderen PDF-Datei"
	    +"\n                    (mit diesem Doclet erzeugt). (Eine"
	    +"\n                    Package-Liste sollte im selben Verzeichnis"
	    +"\n                    liegen.)"
	    +"\n -linkofflinepdf 〈pdf-url〉 〈pkglst-url〉"
	    +"\n                    Erstellt Links zu einer anderen PDF-Datei"
	    +"\n                    (mit diesem Doclet erzeugt), wobei die"
	    +"\n                    Package-Liste bei 〈pkglst-url〉 gesucht wird."
	    +"\n -linkfootnotepdf 〈pdf-url〉 〈idx-url〉 〈linktitle〉"
	    +"\n                    Erstellt Links zu einer anderen PDF-Datei"
	    +"\n                    (mit diesem Doclet erzeugt) als Fußnoten,"
	    +"\n                    mit Seitennummern aus der gegebenen"
	    +"\n                    Index-Datei und gegebenen Titel für den"
	    +"\n                    Verweis."
	    +"\n -linkendpdf 〈pdf-url〉 〈idx-url〉 〈linktitle〉"
	    +"\n                    Erstellt links zu einer anderen PDF-Datei"
	    +"\n                    (mit diesem Doclet erzeugt) am Ende,"
	    +"\n                    mit Seitennummern aus der gegebenen"
	    +"\n                    Index-Datei und gegebenen Titel für den"
	    +"\n                    Verweis."
	    +"\n"
	    +"\nQuelltext-Optionen:"
	    +"\n"
	    +"\n -includesource     Nimmt auch Quelltext in die erzeugte Doku"
	    +"\n                    mit auf. Es wird Quelltext nur für"
	    +"\n                    dokumentierte Elemente erzeugt, also sollte"
	    +"\n                    meist auch -private gewählt werden, um den"
	    +"\n                    kompletten Quelltext zu erhalten."
	    +"\n -classpath 〈path〉  Der Klassen-Suchpfad für den eingebauten"
	    +"\n                    Compiler-Aufruf. (Dies ist auch eine"
	    +"\n                    javadoc-Option, die wir weiterverwenden.)"
	    +"\n -sourcepath 〈path〉 Der Quelltext-Suchpfad für den eingebauten"
	    +"\n                    Compiler-Aufruf. (Dies ist auch eine"
	    +"\n                    javadoc-Option, die wir weiterverwenden.)"
	    +"\n -encoding 〈path〉   Die Quelltext-Kodierung für den eingebauten"
	    +"\n                    Compiler-Aufruf. (Dies ist auch eine"
	    +"\n                    javadoc-Option, die wir weiterverwenden.)"
	    +"\n -source 〈version〉  Die Java-Version, zu der dieser Quelltext"
	    +"\n                    kompatibel ist. (Dies ist auch eine"
	    +"\n                    javadoc-Option, die wir weiterverwenden.)"
	    +"\n";
	return text;
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
