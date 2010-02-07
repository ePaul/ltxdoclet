package de.dclj.paul.ltxdoclet;

import com.sun.source.tree.*;
import com.sun.source.util.*;

import com.sun.javadoc.*;

import javax.lang.model.element.*;
import javax.lang.model.util.*;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.element.VariableElement;
import javax.lang.model.element.TypeElement;
import java.util.Set;
import javax.lang.model.element.Element;
import java.util.List;
import javax.lang.model.util.Types;
import javax.lang.model.element.Modifier;
import javax.lang.model.util.Elements;
import java.io.PrintWriter;


/**
 * Ein Schön-Drucker für Quelltext.
 *
 * Wir implementieren {@link TreeVisitor}, um den Syntaxbaum
 * abzuarbeiten.
 *
 * Von außen müssen nur {@link #printSource} und der
 * Konstruktor verwendet werden.
 *
 * @author <a href="mailto:paulo@heribert.local">Paul Ebermann</a>
 * @version $Id$
 */
public class PrettyPrinter
    extends TreeScanner<Void, SourceFormatter>
{

    private int maxLineLength = 50;

    Elements elements;
    Types types;
    Trees trees;


    /**
     * Creates a new <code>PrettyPrinter</code> instance.
     *
     */
    public PrettyPrinter(Elements elements, Types types, Trees trees)
	{
	    this.elements = elements;
	    this.types = types;
	    this.trees = trees;
	}
    

    /**
     * Druckt den Quelltext zum angegebenen dokumentierten
     * Element aus.
     */
    public void printSource(ProgramElementDoc doc,
			    LaTeXWriter target) {

	Element elem = getElementForDoc(doc);
	Tree t = trees.getTree(elem);

	SourceFormatter f = new SourceFormatter(target);
	this.scan(t, f);
	f.flush();
    }


    /**
     * Sucht das passende {@link Element}-Objekt zu einem
     * dokumentierten Programmelement.
     */
    private Element getElementForDoc(ProgramElementDoc doc) {
// 	LaTeXWriter.configuration.root.printNotice("Suche Element für "
// 						   + doc + " ...");
	ClassDoc klasse = doc.containingClass();
	
	if (klasse == null) {
	    // wir haben schon eine Top-Level-Klasse
	    Element el = elements.getTypeElement(doc.qualifiedName());
	    if (el == null) {
		throw new RuntimeException("kein Element für " + doc + 
					   " gefunden.");
	    }
	    return el;
	}
	// rekursiver Aufruf.
	Element parent = getElementForDoc(klasse);

	return findInList(doc, parent.getEnclosedElements());
    }


    private Element findInList(ProgramElementDoc doc,
			      List<? extends Element> siblings) {
	//	System.err.println("findInList(" + doc + ", " + siblings + ") ...");
	if (doc.isConstructor()) {
	    List<ExecutableElement> konstruktoren = 
		ElementFilter.constructorsIn(siblings);
	    // TODO: passenden Konstruktor raussuchen (Signatur)
	    return konstruktoren.get(0);
	}
	if (doc.isMethod()) {
	    List<ExecutableElement> methoden =
		ElementFilter.methodsIn(siblings);
	    for(ExecutableElement methode : methoden) {
// 		System.err.println("methode: " + methode);
// 		System.err.println(" .simpleName(): " + methode.getSimpleName());
// 		System.err.println("doc: " + doc);
// 		System.err.println("  .name() " + doc.name());
		if (methode.getSimpleName().contentEquals(doc.name())) {
		    // TODO: Signatur vergleichen
		    return methode;
		}
	    }  // for
	}  // if method
	if (doc.isField())  {
	    List<VariableElement> fields =
		ElementFilter.fieldsIn(siblings);
	    for(VariableElement var : fields) {
		if (var.getSimpleName().contentEquals(doc.name())) {
		    return var;
		}
	    }
	} // if field
	if (doc instanceof ClassDoc) {
	    List<TypeElement> classes = 
		ElementFilter.typesIn(siblings);
	    for(TypeElement klasse : classes) {
		if (klasse.getSimpleName().contentEquals(doc.name())) {
		    return klasse;
		}
	    }
	} // if type
	throw new RuntimeException("unbekannter Doc-Typ: " + doc);
    }
    

    /* ****************** TreeVisitor-Methoden ***************** */

    
    /**
     * Druckt eine Variablendeklaration
     * (als Statement, nicht in Parameterdeklarationen,
     * also mit {@code ;} am Ende.
     */
    @Override
    public Void visitVariable(VariableTree var,
			      SourceFormatter target)
    {
	printVariableDecl(var, target);
	target.print(";");
	return null;
    }

    /**
     * Scannt einen Tree als Typ. (Dies hat eine
     * Sonderbehandlung bei Identifier, die andernfalls
     * als Expression aufgefasst würden.)
     */
    public void scanType(Tree tree, SourceFormatter target) {
	if (tree.getKind() == Tree.Kind.IDENTIFIER) {
	    // TODO: eventl. Link setzen
	    target.print("{" + tree + "}");
	}
	else {
	    //	    System.err.println("Scan als Type: " + tree + " : " + tree.getClass());
	    this.scan(tree, target);
	}
    }

    /**
     * Druckt eine Variablendeklaration.
     */
    public void printVariableDecl(VariableTree var, SourceFormatter target) {
	this.scan(var.getModifiers(), target);
	this.scanType(var.getType(), target);
	target.print(" ");
	target.print(var.getName());
	ExpressionTree init = var.getInitializer();
	if (init != null) {
	    target.print(" = ");
	    this.scan(init, target);
	}
    }

    /**
     * Schreibt eine Liste von Typparametern oder -Argumenten, falls
     * nicht leer, raus.
     * @param liste die Liste der Parameter. Falls die Liste leer ist,
     * wird gar nichts getan.
     * @param target dort schreiben wir die Daten hin.
     * @param space falls {@code true} und die Liste nicht leer ist,
     *    wird am Ende (nach dem {@code >}) noch ein Leerzeichen
     *    angehängt.
     */
    public void printTypeParameters(List<?extends Tree> liste,
				    SourceFormatter target, boolean space)
    {
	printTypeList(liste, target, "<", ", ", space ? "> " : ">");
    }


    public void printParameterList(List<? extends VariableTree> params,
				   SourceFormatter target)
    {
	if (params.isEmpty())
	    return;
	target.pushIndent();
	this.printVariableDecl(params.get(0), target);
	for(VariableTree arg : params.subList(1, params.size())) {
	    target.println(",");
	    this.printVariableDecl(arg, target);
	}
	target.popIndent();
    }

    public void printThrows(List<? extends ExpressionTree> exceptions,
			    SourceFormatter target) {
	this.printTypeList(exceptions, target,
			   "\n" + "        "+ "throws ",
			   ",\n"+"        " + "       ",
			   "");
    }


    @Override
    public Void visitMethod(MethodTree meth, SourceFormatter target)
    {
	this.scan(meth.getModifiers(), target);
	this.printTypeParameters(meth.getTypeParameters(), target, true);
	this.scan(meth.getReturnType(), target);
	target.print(" ");
	target.print(meth.getName());
	target.print("(");
	printParameterList(meth.getParameters(), target);
	target.print(")");
	printThrows(meth.getThrows(), target);
	// TODO: was passiert bei abstrakten Methoden ohne Body?
	target.println();
	this.scan(meth.getBody(), target);

	return null;
    }

    /**
     * Druckt eine Liste.
     * Falls die Liste leer ist, wird gar nichts getan,
     * ansonsten wird zunächst {@code prefix} ausgegeben,
     * dann werden die einzelnen Elemente gescannt,
     * getrennt durch {@code infix}, danach wird
     * {@code postfix} ausgegeben.
     * @param liste die zu druckenden Elemente.
     * @param target der Formatierer, auf dem der Inhalt gedruckt
     *         werden soll
     * @param prefix ein String, der am Anfang der Liste
     *        (falls nicht leer) gedruckt werden soll
     * @param infix ein String, der zwischen den einzelnen
     *        Listenelementen gedruckt werden soll.
     * @param postfix ein String, der am Ende der Liste
     *        (falls nicht leer) gedruckt werden soll.
     * @see printTypeList für eine analoge Methode,
     *         die die Einträge als Typen scannt.
     */
    public void printList(List<? extends Tree> liste,
		   SourceFormatter target,
		   String prefix,
		   String infix,
		   String postfix) 
    {
	if (liste.isEmpty()) 
	    return;
	target.print(prefix);
	this.scan(liste.get(0), target);
	for(Tree elem : liste.subList(1, liste.size())) {
	    target.print(infix);
	    this.scan(elem, target);
	}
	target.print(postfix);
    }

    /**
     * Druckt eine Liste als Typen.
     *
     * Falls die Liste leer ist, wird gar nichts getan,
     * ansonsten wird zunächst {@code prefix} ausgegeben,
     * dann werden die einzelnen Elemente als Typen gescannt,
     * getrennt durch {@code infix}, danach wird
     * {@code postfix} ausgegeben.
     * @param liste die zu druckenden Elemente.
     * @param // TODO
     * @see printList für eine analoge Methode,
     *         die die Einträge nicht als Typen scannt.
     */
    public void printTypeList(List<? extends Tree> liste,
		   SourceFormatter target,
		   String prefix,
		   String infix,
		   String postfix) 
    {
	if (liste.isEmpty()) 
	    return;
	target.print(prefix);
	this.scanType(liste.get(0), target);
	for(Tree elem : liste.subList(1, liste.size())) {
	    target.print(infix);
	    this.scanType(elem, target);
	}
	target.print(postfix);
    }


    public void printBounds(List<? extends Tree> liste,
			    SourceFormatter target)
    {
	printTypeList(liste, target, " extends ", " & ", "");
    }


    @Override
    public Void visitTypeParameter(TypeParameterTree param,
				   SourceFormatter target)
    {
	target.print(param.getName());
	printBounds(param.getBounds(), target);
	return null;
    }


    /**
     * Druckt die Modifiers aus, mit je einem Leerzeichen
     * dazwischen und einem danach.
     */
    @Override
    public Void visitModifiers(ModifiersTree mods,
			       SourceFormatter target)
    {
	// TODO: Annotations
	Set<Modifier> modSet = mods.getFlags();
	for(Modifier m : modSet) {
	    target.print(m);
	    target.print(" ");
	}
	return null;
    }


    /**
     * Druckt einen Block.
     */
    @Override
    public Void visitBlock(BlockTree block, SourceFormatter target) {
	target.println("{");
	target.println("  ...");
	// Rekursion erst einmal abgeschaltet.
	target.println("}");
	return null;
    }
    

    /**
     * Druckt einen Array-Typ.
     */
    @Override
    public Void visitArrayType(ArrayTypeTree arrayType, SourceFormatter target)
    {
	this.scan(arrayType.getType(), target);
	target.print("[]");
	return null;
    }

    @Override
    public Void visitParameterizedType(ParameterizedTypeTree pType,
				      SourceFormatter target)
    {
	//	target.print("<* " + pType.getType() + " *>");
	//	target.print("<*" + pType + "*>");
	this.scanType(pType.getType(), target);
	List<? extends Tree> params = pType.getTypeArguments();
	this.printTypeParameters(params, target, false);
	return null;
    }

    @Override
    public Void visitIdentifier(IdentifierTree id, SourceFormatter target) {
	target.print(id.getName());
	return null;
    }

    @Override
    public Void visitPrimitiveType(PrimitiveTypeTree prim,
				   SourceFormatter target) {
	target.print(prim.getPrimitiveTypeKind().toString().toLowerCase());
	return null;
    }

    //    public Void visit


    /**
     * Druckt eine Wildcard (ohne folgendes Leerzeichen).
     */
    @Override
    public Void visitWildcard(WildcardTree wildcard, SourceFormatter target)
    {
	target.print("?");
	switch(wildcard.getKind()) {
	case UNBOUNDED_WILDCARD:
	    return null;
	case SUPER_WILDCARD:
	    target.print(" super ");
	    break;
	case EXTENDS_WILDCARD:
	    target.print(" extends ");
	    break;
	default:
	    throw new IllegalArgumentException(wildcard + " hat Kind " +
					       wildcard.getKind());
	}
	this.scanType(wildcard.getBound(), target);
	return null;
    }


}
