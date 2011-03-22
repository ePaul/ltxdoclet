/**
 * Pauls LaTeX-Doclet ist ein Doclet, welches als Ausgabe
 * LaTeX-Quelltext produziert, um eine schön formatierte
 * Ausgabe (etwa als PDF oder gedruckt) zu erhalten.
 *<ul>
 * <li>Die Haupt-Klasse (deren statische Methoden von javadoc aufgerufen werden)
 *     ist {@link de.dclj.paul.ltxdoclet.DocletStart}.</li>
 * <li>Ein Quelltextformatierer ist in {@link de.dclj.paul.ltxdoclet.PrettyPrinter} gegeben.
 * <li>Umwandlung von HTML in LaTeX ist in der Klasse {@link de.dclj.paul.ltxdoc.HtmlKonverter},
 *    welche eine abgespeckte Version des <a href="http://texdoclet.dev.java.net/">TexDoclet</a>s
 *    von Gregg Wonderly ist.</li>
 * </ul>
 * @author Paul Ebermann
 * @version $Id$
 */
package de.dclj.paul.ltxdoclet;