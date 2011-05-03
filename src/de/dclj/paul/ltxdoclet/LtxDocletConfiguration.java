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

import javax.lang.model.element.Element;

import java.text.MessageFormat;

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
     * Wird durch »-d« festgelegt.
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
     * Wird durch »-doctitle« festgelegt.
     */
    public String doctitle;

    /**
     * Das zu verwendende Encoding für die Ausgabe-Dateien.
     * Wird durch »-docencoding« festgelegt, wie auch beim Standarddoclet.
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


    /**
     * Dieses Objekt erstellt Links.
     */
    public LinkCreator linker;
    

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


    /**
     * Merkt sich die Optionen aus {@code rd}.
     */
    public void setOptions(RootDoc rd)
    {
	this.docencoding = Charset.defaultCharset();
	this.destdir = new File(System.getProperty("user.dir"));
	this.doctitle = "Die Package-Sammlung";

	UniversalLinkCreator lc = new UniversalLinkCreator();
	this.linker = lc;

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
	    if(op[0].startsWith("-link")) {
		lc.addOption(op);
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


    /**
     * prints some help about the options.
     * We will load the text from the resource bundle.
     */
    public String optionHelp() {
        
        ResourceBundle bundle =
            ResourceBundle.getBundle("de.dclj.paul.ltxdoclet.help",
                                     new HelpTextBundleControl());
        Charset cs = Charset.defaultCharset();
        String text = bundle.getString("help");
        if(!"〈...〉".contentEquals(cs.decode(cs.encode("〈...〉")))) {
            text = text.replace('〈', '<').replace('〉', '>');
        }
        return MessageFormat.format(text, cs);
    }


    public boolean validOptions(String[][] options, DocErrorReporter rep) {
	// TODO
	return true;
    }


        
    //     public WriterFactory getWriterFactory() {
    //  return null;
    //     }

    //     public Comparator getMemberComparator() {
    //  return null;
    //     }


    /**
     * Erstellt den Label-Namen für das angegebene Programmelement.
     */
    public String toRefLabel(Doc doc) {
	if (doc instanceof PackageDoc) {
	    return doc + "-package";
	}
	if (doc instanceof ClassDoc) {
	    return doc + "-class";
	}
	if (doc instanceof RootDoc) {
	    return "over-view";
	}
	// TODO
	return removeSpaces(doc.toString());
    }

    private String removeSpaces(String t) {
	StringBuilder b = new StringBuilder(t);
	int index=0;
	while((index = b.indexOf(" ", index))>=0) {
	    b.deleteCharAt(index);
	}
	return b.toString();
    }


    /**
     * Erstellt den Label-Namen für das angegebene Programmelement.
     */
    public String toRefLabel(Element element) {
	switch(element.getKind()) {
	case CLASS:
	case INTERFACE:
	case ENUM:
	case ANNOTATION_TYPE:
	    return element + "-class";
	case PACKAGE:
	    return element + "-package";
	default:
	    return element.toString();
	}
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
