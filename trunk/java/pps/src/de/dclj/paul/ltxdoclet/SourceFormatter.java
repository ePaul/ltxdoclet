package de.dclj.paul.ltxdoclet;

import java.util.*;


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
	this.indentStack.push(this.indentStack.peek() + 4);
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
    }


    /**
     * stellt sicher, dass die aktuelle
     * Zeile eingerückt ist.
     */
    private void indent() {
	if (hasIndent) return;
	int count = indentStack.peek();
	for (int i = 0; i < count; i++)
	    currentLine.append(' ');
	currentLineLength = count;
	hasIndent = true;
    }

    /**
     * gibt puren Text aus (d.h. Text, dessen Drucklänge
     * gleich seiner Länge ist).
     */
    public void print(String text) {
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

    public void print(Object o) {
	print(o.toString());
    }

    public void println(Object o) {
	println(o.toString());
    }

    /**
     * beendet die aktuelle Zeile.
     */
    public void println() {
	finishLine();
    }

}
