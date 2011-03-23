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
        try {
            configuration.root.printNotice("ltxdoclet: Klassen-Doku für \"" + doc +
                                           "\" wird erstellt ...");
            println("   % Api-Dokumentation für Klasse " + doc + " (noch nicht fertig). ");
        
            String refTarget = referenceTarget(doc);
            if (doc.isInterface()) {
                section("Interface ", doc, doc.name());
            } else if (doc.isOrdinaryClass()) {
                section("Klasse ", doc, doc.name());
            } else if (doc.isException()) {
                section("Exception ", doc, doc.name());
            } else if (doc.isError()) {
                section("Error ", doc, doc.name());
            } else if (doc.isEnum()) {
                section("Enum ", doc, doc.name());
            }
            //  println(referenceTarget(doc));


            Type[] interfaces = doc.interfaceTypes();
            Type superClass = doc.superclassType();

            subsection("Übersicht");
            // TODO: Deklaration
            writeDescription(doc);

            ConstructorDoc[] konstr = doc.constructors();
            MethodDoc[] meth = doc.methods();
            FieldDoc[] fields = doc.fields();
            FieldDoc[] consts = doc.enumConstants();


            subsection("Inhaltsverzeichnis");
            // TODO

            writeMemberList(consts, "Enum-Konstanten");
            writeMemberList(fields, "Variablen");
            writeMemberList(konstr, "Konstruktoren");
            writeMemberList(meth, "Methoden");

        }
        finally {
            configuration.root.printNotice("ltxdoclet: ... Klassen-Doku für \"" + doc +
                                           "\" beendet.");          
            close();
        }
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
        println("\\item[{" + referenceTarget(d, asLaTeXString(d.name())) + "}]"); 
        print("~ "); // damit newParagraph() unten auch wirklich eine neue Zeile anfängt.
        // TODO: Signatur
        writeDescription(d);
        //      if (d.isField()) {
        //          writeDeclaration((FieldDoc) d);
        //      }
        if (configuration.includeSource) {
            try{
                configuration.pp.printSource(d, this);
            }
            //      catch(RuntimeException ex) {
            //          configuration.root.printError("bei printSource("
            //                                        + d + "):");
            //          configuration.wasError = true;
            //          ex.printStackTrace();
            //      }
            finally {
                newParagraph();
            }
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
