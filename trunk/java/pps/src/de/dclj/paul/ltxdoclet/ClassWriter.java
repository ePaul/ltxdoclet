package de.dclj.paul.ltxdoclet;

import com.sun.javadoc.*;
import java.io.*;

/**
 * Ein Writer zum Schreiben einer Klasse (inklusive der Methoden).
 */
public class ClassWriter
	extends LaTeXWriter
{

    private ClassDoc doc;

    public ClassWriter(ClassDoc cd)
	throws IOException
    {
	super(new File(configuration.toOutputFileName(cd.containingPackage()),  cd.name()+".tex"));
	this.doc = cd;
    }

    public void writeDoc() {
	configuration.root.printNotice("ltxdoclet: Klassen-Doku für \"" + doc +
				       "\" wird erstellt ...");
	println("   % Api-Dokumentation für Klasse " + doc + " (noch nicht fertig). ");
	
	if (doc.isInterface()) {
	    section("Interface " + doc);
	} else if (doc.isOrdinaryClass()) {
	    section("Klasse " + doc);
	} else if (doc.isException()) {
	    section("Exception " + doc);
	} else if (doc.isError()) {
	    section("Error " + doc);
	} else if (doc.isEnum()) {
	    section("Enum " + doc);
	}
	println(referenceTarget(doc));


	Type[] interfaces = doc.interfaceTypes();
	Type superClass = doc.superclassType();

	subsection("Übersicht");
	// TODO: Deklaration
	writeDescription(doc);
	
	ConstructorDoc[] konstr = doc.constructors();
	MethodDoc[] meth = doc.methods();
	FieldDoc[] fields = doc.fields();
	

	subsection("Inhaltsverzeichnis");
	// TODO

	writeMemberList(fields, "Variablen");
	writeMemberList(konstr, "Konstruktoren");
	writeMemberList(meth, "Methoden");

	close();
    }


    public <X extends MemberDoc> void writeMemberList(X[] liste, String titel) {
	if (liste.length > 0) {
	    subsection(titel);
	    println("\\begin{description}");
	    for (X d : liste) {
		writeMemberDoc(d);
	    }
	    println("\\end{description}");
	}
    }

    public <X extends MemberDoc> void writeMemberDoc(X d) {
	println("\\item[" + d.name() + "]"); 
	println(referenceTarget(d));
	print("~"); // damit newPAragraph() unten auch wirklich eine neue Zeile anfängt.
	// TODO: Signatur
	writeDescription(d);
	newParagraph();
	if (d.isField()) {
	    writeDeclaration((FieldDoc) d);
	}
    }


    public void writeDeclaration(FieldDoc d)
    {
	print("\\texttt{");
	print(d.modifiers());
	print(" ");
	Type t = d.type();
	print(typeRef(t));
	print(" ");
	print(d.name());
	print(";");
	print("}");
    }


}
