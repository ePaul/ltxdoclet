package de.dclj.paul.ltxdoclet;

import java.util.*;
import java.io.Flushable;
import java.util.ArrayDeque;
import java.util.Deque;
import javax.lang.model.element.Element;

import com.sun.source.tree.Tree.Kind;

import com.sun.javadoc.Doc;


/**
 * Ein Formatierer für Quelltext.
 *
 *
 * @author <a href="mailto:paulo@heribert.local">Paul Ebermann</a>
 * @version $Id$
 */
public class SourceFormatter
    implements java.io.Flushable
{

    /**
     * Hierhin geben wir die Daten aus.
     */
    private LaTeXWriter writer;

    /**
     * Creates a new <code>SourceFormatter</code> instance.
     *
     */
    public SourceFormatter(LaTeXWriter wri) {
	this.writer = wri;
	this.currentLine = new StringBuilder(80);
	this.indentStack = new ArrayDeque<Integer>();
	this.indentStack.push(0);
	this.currentLineLength = 0;
    }

    /**
     * Sorgt dafür, dass alles an den unterliegenden Stream
     * ausgegeben wird.
     */
    public void flush() {
	if (hasIndent) {
	    // es wurde bereits eine Zeile angefangen
	    writer.print(currentLine);
	}
    }


    private Deque<Integer> indentStack;

    private StringBuilder currentLine;
    private boolean hasIndent = false;

    private int currentLineLength;

    /**
     * Hängt an die aktuelle Zeile einen Text
     * an, dessen Drucklänge gleich seiner
     * Länge ist.
     */
    private void appendInLine(String text) {
	appendInLine(text, text.codePointCount(0, text.length()));
    }

    /**
     * Hängt an die aktuelle Zeile einen
     * Text an und registriert dessen Länge.
     * @param text der auszugebende Text.
     * @param len die Druck-Länge dieses Textes
     *       in Zeichen.
     */
    private void appendInLine(String text, int len) {
	indent();
	currentLine.append(text);
	currentLineLength += len;
    }


    /**
     * Beendet eine Zeile.
     * Der aktuelle Inhalt des Zeilenpuffers wird
     * ausgegeben und danach der Zeilenpuffer geleert.
     */
    private void finishLine() {
	indent();
	writer.println(currentLine);
	currentLine.setLength(0);
	hasIndent = false;
	// TODO
    }


    /**
     * Beginnt eine neue Einrückungsebene, welche die
     * aktuelle "Cursorposition" (d.h. die bisherige Länge
     * aktuellen Zeile) als neue Einrücktiefe hat.
     *
     * Alle bis zum nächsten {@link #popIndent}
     * begonnenen Zeilen erhalten eine Einrückung dieser Länge.
     */
    public void pushIndent() {
	currentLine.append("\\ltdSetIndent{"+currentLineLength+"}");
	this.indentStack.push(currentLineLength);
    }

    /**
     * Beginnt eine neue Einrückungsebene.
     *
     * Alle bis zum nächsten {@link #popIndent}
     * begonnenen Zeilen erhalten eine Einrückung, die 4 Zeichen
     * mehr ist als die aktuelle Einrückung.
     */
    public void addIndent() {
	int newIndent = this.indentStack.peek() + 4;
	currentLine.append("\\ltdSetIndent{" +newIndent+"}");
	this.indentStack.push(newIndent);
    }

    /**
     * Beendet eine Einrückungsebene.
     *
     * Alle in Zukunft begonnenen Zeilen haben wieder
     * die Einrückung, die vor dem letzten {@link #addIndent} oder
     * {@link #pushIndent} aktiv war.
     */
    public void popIndent() {
	this.indentStack.pop();
	currentLine.append("\\ltdSetIndent{"+this.indentStack.peek()+"}");
    }


    /**
     * stellt sicher, dass die aktuelle
     * Zeile eingerückt ist.
     */
    private void indent() {
	if (hasIndent)
	    return;
	currentLine.append("\\ltdIndent");
	int count = indentStack.peek();
	for (int i = 0; i < count; i++)
	    currentLine.append(' ');
	currentLine.append(".");
	currentLineLength = count;
	hasIndent = true;
    }

    /**
     * gibt puren Text aus (d.h. Text, dessen Drucklänge
     * gleich seiner Länge ist).
     */
    public void print(String text) {
	// bei leerem String müssen wir keine Einrückung erzeugen.
	if (text.length() == 0)
	    return;
	int index = text.indexOf("\n");
	if(index < 0) {
	    // ohne Zeilenumbruch
	    appendInLine(text);
	    return;
	}
	// mindestens ein Zeilenumbruch enthalten
	appendInLine(text.substring(0, index));
	finishLine();
	// rekursiver Aufruf mit dem Rest-String
	print(text.substring(index+1));
    }

    /**
     * gibt puren Text aus und beendet danach die Zeile.
     */
    public void println(String text) {
	print(text);
	finishLine();
    }

    /**
     * gibt die String-Darstellung des Objektes als puren Text aus.
     */
    public void print(Object o) {
	print(o.toString());
    }

    /**
     * gibt die String-Darstellung des Objektes als puren Text aus
     * und beendet danach die Zeile.
     */
    public void println(Object o) {
	println(o.toString());
    }

    /**
     * beendet die aktuelle Zeile.
     */
    public void println() {
	finishLine();
    }

    /**
     * Druckt ein Schlüsselwort.
     */
    public void printKeyword(String keyword) {
	appendInLine("\\markKeyword{" + keyword + "}",
		     keyword.length());
    }


    /**
     * druckt einen Identifier (dabei werden {@code _} escapet.)
     */
    public void printId(CharSequence name) {
	indent();
	int len = name.length();
	for(int i = 0; i < len; i++) {
	    char c = name.charAt(i);
	    if (c == '_') {
		this.currentLine.append("\\_");
	    }
	    else {
		this.currentLine.append(c);
	    }
	}
	this.currentLineLength += len;
    }


    //    private Map<String, 


    /**
     * Druckt ein spezielles Token.
     */
    public void printSpecial(String token) {
	if(token.equals(""))
	    return;
	if(token.charAt(0) == '\n') {
	    println();
	    token = token.substring(1);
	    if(token.equals(""))
		return;
	}
	SpecialToken tok = SpecialToken.getToken(token);
	if (tok == null) {
	    appendInLine("\\textbf{" + token + "}", token.length());
	}
	else {
	    appendInLine(tok.getReplacement(), tok.getLength());
	}
    }

    public void printSpecial(Kind token) {
	SpecialToken tok = SpecialToken.getToken(token);
	if (tok == null) {
	    print(token);
	}
	else {
	    appendInLine(tok.getReplacement(), tok.getLength());
	}
    }

//     /**
//      * Druckt ein spezielles Token und beendet dann die Zeile.
//      */
//     public void printlnSpecial(String token) {
// 	printSpecial(token);
// 	println();
//     }

    private void printAsNumber(String text) {
	appendInLine("{\\markNumber ", 0);
	appendInLine(text);
	appendInLine("}", 0);
    }


    /**
     * Druckt ein Literal für einen Wert.
     */
    public void printLiteral(Object value, Kind type) {
	switch(type) {
	case BOOLEAN_LITERAL:
	    String val = value.toString();
	    appendInLine("{\\markLiteralKeyword " + val + "}", val.length());
	    return;
	case INT_LITERAL:
	case DOUBLE_LITERAL:
	    printAsNumber(value.toString());
	    return;
	case LONG_LITERAL:
	    printAsNumber(value.toString() + "L");
	    return;
	case FLOAT_LITERAL:
	    printAsNumber(value.toString() + "f");
	    return;
	case CHAR_LITERAL: {

	    String javaString = 
		escapeJavaString(value.toString());
	    int len = javaString.length() + 2;
	    String lString = 
		escapeLaTeXString("'" + javaString + "'");
	    appendInLine("{\\markNumber " + lString + "}", len);
	    return;
	}
	case STRING_LITERAL: {
	    String javaString = 
		escapeJavaString(value.toString());
	    int len = javaString.length() + 2;
	    String lString = 
		escapeLaTeXString('"' + javaString + '"');
	    appendInLine("{\\markString "+lString+"}", len);
	    return;
	}
	case NULL_LITERAL:
	    appendInLine("{\\markLiteralKeyword null}", 4);
	    return;
	}
	throw new IllegalArgumentException("Kein Literal: " + type + " (" + value + ")");
    }

    private final static char[] verbDelim = "#'+*&-.,:;<>|!/()=[]1234567890".toCharArray();



    private String escapeLaTeXString(String org) {
	char delim = 0;
	for (char c : verbDelim) {
	    if (org.indexOf(c)< 0) {
		delim = c;
		break;
	    } 
	}
	if (delim == 0) {
	    // kein gemeinsamter Delimiter für den
	    // ganzen String gefunden => wir halbieren
	    // den String und versuchen es für die
	    // Hälften noch einmal. Irgendwann ist der
	    // String kürzer als unsere Liste der Delimiter,
	    // dann muss es klappen.
	    int div = org.length()/2;
	    return
		escapeLaTeXString(org.substring(0, div)) +
		escapeLaTeXString(org.substring(div));
	}
	return "\\verb" + delim + org + delim;
    }

    private String escapeJavaString(String org) {
	StringBuilder b = new StringBuilder(org);
	for (int i = 0; i < b.length(); i++) {
	    char c = b.charAt(i);
	    String replace;
	    switch(c) {
	    case '\n': replace = "\\n"; break;
	    case '\r': replace = "\\r"; break;
	    case '\t': replace = "\\t"; break;
	    case '\f': replace = "\\f"; break;
	    case '\b': replace = "\\b"; break;
	    case '\\': replace = "\\\\"; break;
	    case '\'': replace = "\\'"; break;
	    case '\"': replace = "\\\""; break;
	    default:
		continue;
	    }
	    b.replace(i, i+1, replace);
	    i++;
	}
	return b.toString();
    }


    /**
     * Druckt einen Identifier als Link.
     * @param text der Text des Links (purer Text).
     * @param el das Element, zu dem gelinkt wird.
     */
    public void printLinkedId(String text, Doc el)
    {
	// TODO: Links für Klassen/Packages an die richtige Stelle
	// TODO: Links für nicht enthaltene Elemente nicht setzen.
	appendInLine("\\hyperlink{" + writer.toRefLabel(el) +"}{", 0);
	printId(text);
	appendInLine("}", 0);
    }


}
