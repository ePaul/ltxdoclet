package de.dclj.paul.ltxdoclet;

import com.sun.tools.doclets.*;
import com.sun.javadoc.*;

import java.io.*;
import java.io.PrintWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
//import javax.print.Doc;
//import java.lang.reflect.ParameterizedType;
//import javax.swing.text.html.HTML.Tag;
import javax.lang.model.element.Element;
//import javax.lang.model.type.TypeVariable;
//import javax.lang.model.type.WildcardType;

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

/**
 * Einige generelle Methoden zum Schreiben von LaTeX-Dokumenten.
 * Unterklassen können diese dann verwenden.
 */
public class LaTeXWriter
    extends PrintWriter
{
    /*
     * Wenn im LaTeX-Quelltext ein "\" herauskommen soll, muss hier immer "\\" eingetippt
     * werden.
     */

    /**
     * Erstellt einen neuen LaTeXWriter, der in die angegebene
     * Datei schreibt.
     * @throws IOException falls die Datei nicht geöffnet bzw. beschrieben werden kann.
     */
    public LaTeXWriter(File filename)
        throws IOException
    {
        super(new OutputStreamWriter(new FileOutputStream(filename),
                                     configuration.docencoding));
        configuration.root.printNotice("Schreibe in " + filename + "...");
        println("   % /--------------------------------------------\\");
        println("   % | API-Dokumentation für einige Java-Packages |");
        println("   % |    (genaueres siehe doku-main.tex).        |");
        println("   % | LaTeX-Ausgabe erstellt von 'ltxdoclet'.    |");
        println("   % | Dieses Programm stammt von Paul Ebermann.  |");
        println("   % \\--------------------------------------------/");
        println();
    }

    private String replace(String org, String sub1, String sub2)
    {
        StringBuffer buf = new StringBuffer(org);
        int slen = sub1.length();
        for (int i = 0; i < buf.length(); i++)
            {
                if(buf.substring(i, i + slen).equals(sub1))
                    {
                        buf.replace(i, i+slen, sub2);
                    }
            }
        return buf.toString();
    }

    /**
     * Die Liste der Ersetzungen.
     */
    final static String[][] ltxsymb =
    {
        {"\\", "\\textbackslash "},
        {"\"", "{\\textquotedbl}"},
        {"_", "{\\textunderscore}"},
        {"¡", "!`"},
        {"£", "{\\pounds}"},
        {"§", "{\\S}"},
        {"©", "{\\copyright}"},
        {"±", "{\\pm}"},
        {"¶", "{\\P}"},
        {"·", "{\\cdot}"},
        {"¿", "?`"},
        {"À", "\\`{A}"},
        {"Á", "\\'{A}"},
        {"Â", "\\^{A}"},
        {"Ã", "\\~{A}"},
        {"Ä", "\\\"{A}"},
        {"Å", "{\\AA}"},
        {"Æ", "{\\AE}"},
        {"Ç", "\\c{C}"},
        {"È", "\\`{E}"},
        {"É", "\\'{E}"},
        {"Ê", "\\^{E}"},
        {"Ë", "\\\"{E}"},
        {"Ì", "\\`{I}"},
        {"Í", "\\'{I}"},
        {"Î", "\\^{I}"},
        {"Ï", "\\\"{I}"},
        {"Ñ", "\\~{N}"},
        {"Ò", "\\`{O}"},
        {"Ó", "\\'{O}"},
        {"Ô", "\\^{O}"},
        {"Õ", "\\~{O}"},
        {"Ö", "\\\"{O}"},
        {"×", "{\\times}"},
        {"Ø", "{\\O}"},
        {"Ù", "\\`{U}"},
        {"Ú", "\\'{U}"},
        {"Û", "\\^{U}"},
        {"Ü", "\\\"{U}"},
        {"Ý", "\\'{Y}"},
        {"ß", "{\\ss}"},
        {"à", "\\`{a}"},
        {"á", "\\'{a}"},
        {"â", "\\^{a}"},
        {"ã", "\\~{a}"},
        {"ä", "\\\"{a}"},
        {"å", "{\\aa}"},
        {"æ", "{\\ae}"},
        {"ç", "\\c{c}"},
        {"è", "\\`{e}"},
        {"é", "\\'{e}"},
        {"ê", "\\^{e}"},
        {"ë", "\\\"{e}"},
        {"ì", "\\`{\\i}"},
        {"í", "\\'{\\i}"},
        {"î", "\\^{\\i}"},
        {"ï", "\\\"{\\i}"},
        {"ñ", "\\~{n}"},
        {"ò", "\\`{o}"},
        {"ó", "\\'{o}"},
        {"ô", "\\^{o}"},
        {"õ", "\\~{o}"},
        {"ö", "\\\"{o}"},
        {"÷", "{\\div}"},
        {"ø", "{\\o}"},
        {"ù", "\\`{u}"},
        {"ú", "\\'{u}"},
        {"û", "\\^{u}"},
        {"ü", "\\\"{u}"},
        {"ý", "\\'{y}"},
        {"ÿ", "\\\"{y}"}
    };

    /**
     * wandelt einen Unicode-String in die entsprechenden
     * LaTeX-Symbole um.
     */
    public String asLaTeXString(String s)
    {
        return new HtmlKonverter().toTeX(s);
        //      for (int i = 0; i < ltxsymb.length; i++)
        //          {
        //              s = replace(s, ltxsymb[i][0], ltxsymb[i][1]);
        //          }
        //      return s;
    }

    /**
     * Wandelt den Namen eines Dokumentations-Elementes in einen
     * LaTeX-String um.
     */
    public String asLaTeXString(Doc d)
    {
        return asLaTeXString(d.toString());
    }

    /**
     * gibt den angegebenen Text, umgewandelt in LaTeX-Befehle, aus.
     * @param text der umzukodierende Text.
     */
    public void ltxwrite(String text)
    {
        print(asLaTeXString(text));
    }

    /**
     * Beginnt ein neues Kapitel.
     * @param name  Name bzw. Überschrift des Kapitels
     * @param num  Numerierung erwünscht?
     */
    public void chapter(String name, boolean num)
    {
        println("\\chapter" + (num? "" : "*") + "{" + asLaTeXString(name) + "}");
    }


    /**
     * Beginnt ein neues Kapitel mit Numerierung.
     * @param name  Name bzw. Überschrift des Kapitels
     */
    public void chapter(String name)
    {
        chapter(name, true);
    }

    /**
     * Eine Kapitelüberschrift mit Link-Ziel.
     */
    public void chapter(String prefix, Doc doc, String shortName)
    {
        String ref = configuration.toRefLabel(doc);
        println("\\chapter[" + asLaTeXString(shortName) + "]{" + prefix +
                "\\ltdHypertarget{" + ref + "}{"
                +asLaTeXString(doc.toString()) + "}}\\label{"+ref+"}");
    }

    public void chapter(String prefix, Doc doc) {
        chapter(prefix, doc, prefix + doc);
    }


    /**
     * Beginnt einen neuen Abschnitt.
     */
    public void section(String name)
    {
        println("\\section{" + asLaTeXString(name) + "}");
    }

    public void section(String prefix, Doc doc, String shortName)
    {
        String ref = configuration.toRefLabel(doc);
        println("\\section[" + asLaTeXString(shortName) + "]{" + prefix +
                "\\ltdHypertarget{" + ref + "}{"
                +asLaTeXString(doc.toString()) + "}}\\label{"+ref+"}");
    }

    /**
     * Beginnt einen neuen Abschnitt, der im Inhaltsverzeichnis einen
     * alternativen Namen hat.
     */
    public void section(String name, String shortName)
    {
        println("\\section[" + asLaTeXString(shortName) + "]{" +
                asLaTeXString(name) + "}");
    }


    /**
     * Beginnt einen neuen Unterabschnitt.
     */
    public void subsection(String name)
    {
        println("\\subsection{" + asLaTeXString(name) + "}");
    }

    /**
     * Beginnt einen neuen Unterunterabschnitt.
     */
    public void subsubsection(String name)
    {
        println("\\subsubsection{" + asLaTeXString(name) + "}");
    }

    /**
     * gibt den angegebenen Text kursiv aus.
     */
    public void italic(String name)
    {
        println("\\textit{" + asLaTeXString(name) + "}");
    }

    /**
     * gibt den angegebenen Text fett aus.
     */
    public void bold(String text)
    {
        println("\\textbf{" + asLaTeXString(text) + "}");
    }

    /**
     * Schreibt einen Typnamen (mit Verlinkungen) in den
     * angegebenen StringBuilder (am Ende).
     */
    public void typeRef(Type t, StringBuilder app) {
        if (t.isPrimitive()) {
            app.append(t.toString());
            return;
        }
        String dimension = t.dimension();
        TypeVariable tv = t.asTypeVariable();
        if (tv != null) {
            // TODO: interfaces etc.?
            if (tv.owner().isClass()) {
                app.append(createLink(tv.typeName(), tv.owner()));
            }
            else {
                app.append(tv.typeName());
            }
            app.append(dimension);
            return;
        }
        WildcardType wt = t.asWildcardType();
        if (wt != null) {
            // TODO: Bounds verlinken
            app.append(wt);
            app.append(dimension);
            return;
        }
        ParameterizedType pt = t.asParameterizedType();
        if (pt != null) {
            if (pt.containingType() != null) {
                app.append(typeRef(pt.containingType()));
                app.append(".");
            }
            // TODO: was passiert bei generischen inneren Klassen?
            app.append(typeRef(pt.asClassDoc()));
            app.append("<");
            for(Type ta : pt.typeArguments()) {
                app.append(typeRef(ta));
                app.append(", ");
            }
            // TODO: bessere Lösung ausdenken, dann können wir auch
            // direkt "Appendable" nutzen statt StringBuilder.
            if (pt.typeArguments().length > 0) {
                app.delete(app.length() - 2, app.length()); // letztes ", " wieder löschen
            }
            app.append(">");
            app.append(dimension);
            return;
        }
        ClassDoc cd = t.asClassDoc();
        if (cd != null) {
            if (cd.containingClass() != null) {
                typeRef(cd.containingClass(), app);
                app.append(".");
            }
            app.append(createLink(cd));
            app.append(dimension);
            return;
        }
        // noch etwas anderes?
        app.append(t);
    }


    /**
     * Erstellt eine verlinkte Version des Typnamens von t.
     */
    public String typeRef(Type t) {
        StringBuilder b = new StringBuilder();
        typeRef(t, b);
        return b.toString();
    }


    /**
     * Ermittelt eine Referenz zu dem angegebenen Programmelement.
     */
    public String referenceTo(Doc doc)
    {
        return "\\pageref{" + configuration.toRefLabel(doc) + "}";
    }


    /**
     * Erstellt ein LaTeX-Label für das angegebene Programmelement.
     */
    public String referenceTarget(Doc doc)
    {
        return referenceTarget(doc, doc.toString());
    }

    public String referenceTarget(Doc doc, String label) {
        String ref = configuration.toRefLabel(doc);
        return "\\ltdHypertarget{" + ref + "}{" + label + "}"+
            "\\label{"+ref +"}";
    }

    /**
     * Schreibt die Beschreibung dieses zu dokumentierenden Elementes.
     *
     * Zuerst werden die {@linkplain Doc#inlineTags Inline-Tags} des Elementes ausgegeben,
     * danach die »normalen« Tags.
     */
    public void writeDescription(Doc d) {
        writeInlineTags(d.inlineTags());
        Tag[] tags = d.tags();
        if (tags.length > 0) {
            println("\\begin{description}");
            if(d instanceof ExecutableMemberDoc) {
                ExecutableMemberDoc emd = (ExecutableMemberDoc)d;
                writeTypeParams(emd.typeParamTags());
                writeParams(emd.paramTags());
                writeThrows(emd.throwsTags());
            }
            writeNormalTags(normalTags(tags));
            writeSeeTags(seeTags(tags));
            println("\\end{description}");
        }
    }

    public void writeTypeParams(ParamTag[] tags) {
        if(tags.length > 0) {
            println("\\item[Typparameter] ~");
            println("\\begin{description}");
            for(ParamTag pt : tags) {
                writeParamTag(pt);
            }
            println("\\end{description}");
        }
    }

    public void writeParams(ParamTag[] tags) {
        if(tags.length > 0) {
            println("\\item[Parameter] ~");
            println("\\begin{description}");
            for(ParamTag pt : tags) {
                writeParamTag(pt);
            }
            println("\\end{description}");
        }
    }

    public void writeThrows(ThrowsTag[] tags) {
        if(tags.length > 0) {
            println("\\item[Exceptions] ~");
            println("\\begin{description}");
            for(ThrowsTag tt : tags) {
                writeThrowsTag(tt);
            }
            println("\\end{description}");
        }
    }

    public void writeNormalTags(Tag[] tags) {
        if (tags.length > 0) {
            for (Tag tag : tags) {
                println("\\item[" + tagName(tag) + "] ");
                writeInlineTags(tag.inlineTags());
            }
        }
    }

    public void writeSeeTags(SeeTag[] tags) {
        if(tags.length > 0) {
            println("\\item[Siehe auch] ~");
            print("\\noprint"); // um das erste Komma wieder aufzuessen.
            for(SeeTag t : tags) {
                print(", ");
                writeSeeTag(t);
            }
        }
    }


    /**
     * Filtert alle Tags heraus, die speziell behandelt werden, und gibt
     * die übrigen zurück.
     * Die herausgefilterten sind:
     * <ul>
     *    <li>@throws/@ecxeption
     *    <li>@param
     *    <li>@see
     * </ul>
     */
    public Tag[] normalTags(Tag[] tags) {
        Tag[] result = new Tag[tags.length];
        int j = 0;
        for(Tag t : tags) {
            if(!nonNormalTagKinds.contains(t.kind())) {
                result[j] = t;
                j++;
            }
        }
        return j == tags.length ? result :
            Arrays.copyOfRange(result, 0, j);
    }


    private final Set<String> nonNormalTagKinds =
        new HashSet<String>(Arrays.asList("@throws", "@see", "@param"));


    private SeeTag[] seeTags(Tag[] tags) {
        SeeTag[] result = new SeeTag[tags.length];
        int j = 0;
        for(Tag t : tags) {
            if("@see".equals(t.kind())) {
                result[j] = (SeeTag)t;
                j++;
            }
        }
        return j == tags.length ? result :
            Arrays.copyOfRange(result, 0, j);
    }


    /**
     * schreibt ein generisches Block-Tag.
     */
    public void writeTag(Tag tag) {
        println("\\item[" + tagName(tag) + "]");
        writeInlineTags(tag.inlineTags());
    }

    public void writeThrowsTag(ThrowsTag tag) {
        println("\\item[" + typeRef(tag.exceptionType()) + "]");
        writeInlineTags(tag.inlineTags());
    }


    public void writeParamTag(ParamTag tag) {
        println("\\item[" + tag.parameterName() + "]");
        writeInlineTags(tag.inlineTags());
    }

    public void writeSeeTag(SeeTag tag) {
        //      println("\\noprint{writeSeeTag(" + tag + ")}");
        String className = tag.referencedClassName();
        //      println("\\noprint{className=" + className+"}");
        if (className == null) {
            // expliziter HTML-Link
            // TODO
            print(asLaTeXString(tag.toString()));
        }
        else {
            writeLinkTag(tag);
        }
    }


    /**
     * Ermittelt den Namen eines Tags in einer Tag-Liste.
     */
    public String tagName(Tag t) {
        String kind = t.kind();
        if (kind.equals("@throws"))
            return "throws " + ((ThrowsTag)t).exceptionType();
        if (kind.equals("@see"))
            return "Siehe auch";
        if (kind.equals("@return"))
            return "Rückgabewert";
        if (kind.equals("@param")) {
            ParamTag pt = (ParamTag)t;
            if (pt.isTypeParameter())
                return "Typeparameter " + pt.parameterName();
            return "Parameter " + pt.parameterName();
        }
        return kind;
    }

    public String createLink(String labelText, String target) {
        return "\\hyperlink{" +target + "}{"+ labelText +"}"; //+ " [\\pageref*{" + targetName +"}]";
    }



    public String createExternalLink(String label, Doc target) {
        // TODO
        return null;
    }


    /**
     * Erstellt einen Link.
     */
    public String createLink(String label, Doc target) {
        if (label.equals("")) {
            label = target.name();
        }
        return configuration.linker.createLink(label, target);

        //      // TODO:  target.isIncluded überprüfen und andernfalls externen
        //      //        oder gar keinen Link setzen.  Oder einen Index mit allen
        //      //        solchen Klassen am Ende anlegen?
        //      String targetName = configuration.toRefLabel(target);
        //      return createLink(label, targetName);
    }

    /**
     * Erstellt einen Link.
     */
    public String createLink(Doc target) {
        return createLink("", target);
    }


    void writeLinkTag(SeeTag tag, Doc doc) {
        if(tag.name().equals("@linkplain")) {
            print(createLink(tag.label(), doc));
        }
        else {
            print("\\texttt{" + createLink(tag.label(), doc)+ "}");
        }
    }

    /**
     * Schreibt ein {@linkplain SeeTag Link-Tag} (bzw. den Inhalt
     * eines {@link SeeTag}s) hinaus.
     *
     * Hier noch ein {@linkplain PackageWriter Testlink}
     * und {@link ClassWriter}.
     * @see PrettyPrinter
     */
    public void writeLinkTag(SeeTag st) {
        print("\\noprint{"+st+"}");
       
        MemberDoc md = st.referencedMember();
        if (md != null)
            {
                writeLinkTag(st, md);
                return;
            }        // if
        // kein Member
        ClassDoc cd = st.referencedClass();
        if (cd != null)
            {
                writeLinkTag(st, cd);
                return;
            }
        // keine Klasse
        PackageDoc pd = st.referencedPackage();
        if (pd != null) {
            writeLinkTag(st, pd);
            return;
        }
        // kein Package - was dann?
        print("{«Link:" + st +"|" +st.referencedClassName() + "|"+ st.label() + "»}");
        // ...
        //      configuration.root.printNotice("Zur Zeit sind nur Links zu Klassen und Member implementiert.");
    }

    public void writeInlineTag(Tag t, HtmlKonverter konverter) {
        if (t.kind().equalsIgnoreCase("Text"))    // Puren Text umwandeln
            print(konverter.toTeX(t.text()));
        else if(t.name().equals("@LaTeX"))  // LaTeX-Quelltext direkt verwenden
            print(t.text());
        else if (t.name().equalsIgnoreCase("@code")) {
            // TODO: Code-Formatierung?
            print("\\verb!" + t.text() + "!");
        }
        else if(t instanceof SeeTag)        // Inline-See-Tag (@link)
            {
                SeeTag st = (SeeTag)t;
                writeLinkTag(st);
            }        // else
        else {
            configuration.root.printNotice("Unbekanntes Inline-Tag: " + t);
            print("<<"+t+">>");
        }
    }


    /**
     * Schreibt mehrere Tags nacheinander. Dies ist gedacht für
     * Inline- und Text-Tags.
     * Diese werden z.B. von Doc.inlineTags() und
     * Doc.firstSentenceTags() zurückgegeben.
     */
    public void writeInlineTags(Tag[] tags)
    {
        HtmlKonverter konverter = new HtmlKonverter();
        for (Tag tag : tags) {
            writeInlineTag(tag, konverter);
        }
        println();
    }  // writeInlineTags


    public void newParagraph() {
        println();
        println();
    }


    public void newLine()
    {
        println("\\\\");
    }

    /************/

    static LtxDocletConfiguration configuration;


}
