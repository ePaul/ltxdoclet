package de.dclj.paul.ltxdoclet;

import com.sun.source.tree.*;
import com.sun.source.util.*;

import com.sun.javadoc.*;

import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.*;


import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.element.VariableElement;
import javax.lang.model.element.TypeElement;
import java.util.Set;
import java.util.Collections;
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
    extends TreePathScanner<Void, SourceFormatter>
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

    
    private ProgramElementDoc currentElement;

    /**
     * Druckt den Quelltext zum angegebenen dokumentierten
     * Element aus.
     */
    public void printSource(ProgramElementDoc doc,
			    LaTeXWriter target)
    {
	this.currentElement = doc;

	Element elem = getElementForDoc(doc);
	TreePath path = trees.getPath(elem);
// 	System.err.println("doc: " + doc);
// 	if (doc instanceof MemberDoc) {
// 	    if (((MemberDoc)doc).isSynthetic()) {
// 	    // kein Quelltext da
// 		return;
// 	    }
// 	    System.err.println("MemberDoc!");
// 	    System.err.println(" synthetic: "+
// 			       ((MemberDoc)doc).isSynthetic());
// 	}
// 	System.err.println("elem: " + elem);
// 	System.err.println("path : " + path);
	if (path == null)
	    return;

	target.println("\\begin{sourcecode}");
	SourceFormatter f = new SourceFormatter(target);
	this.scan(path, f);
	f.flush();
	target.println("\\end{sourcecode}");
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

    /**
     * Sucht das Doc für das genannte Element.
     */
    private Doc getDocForElement(ExecutableElement element,
				 ClassDoc classD)
    {
// 	System.err.println("getDocForElement(" + element + ", " +
// 			   classD + ")");
	if(element.getKind() == ElementKind.CONSTRUCTOR) {
	    ConstructorDoc[] cons = classD.constructors(false);
	    return findInList(element, cons);
	}
	else if (element.getKind() == ElementKind.METHOD){
	    MethodDoc[] meths = classD.methods(false);
	    return findInList(element, meths);
	}
	else {
	    throw new IllegalArgumentException(element + " (" +
					       element.getKind() +")");
	}
    }

    
    private Doc findInList(ExecutableElement exe,
			   ConstructorDoc[] cons) {
	for(ConstructorDoc doc : cons) {
	    if(gleicheSignatur(exe, doc)) {
		return doc;
	    }
	}
	return null;
    }

    private Doc findInList(ExecutableElement exe,
			   MethodDoc[] cons) {
	for(MethodDoc doc : cons) {
	    if(doc.name().contentEquals(exe.getSimpleName()) &&
	       gleicheSignatur(exe, doc)) {
		return doc;
	    }
	}
	return null;
    }


    private Element findInList(ProgramElementDoc doc,
			      List<? extends Element> siblings) {
	//	System.err.println("findInList(" + doc + ", " + siblings + ") ...");
	if (doc.isConstructor()) {
	    List<ExecutableElement> konstruktoren = 
		ElementFilter.constructorsIn(siblings);
	    // passenden Konstruktor raussuchen (Signatur)
	    for (ExecutableElement exe : konstruktoren) {
		if(gleicheSignatur(exe, (ConstructorDoc)doc)) {
		    return exe;
		}
	    }
	    LaTeXWriter.configuration.root
		.printWarning("Kein Konstruktor zu " + doc +" gefunden. "+
			      "Kandidaten waren: " + konstruktoren);
	    
	}
	if (doc.isMethod()) {
	    List<ExecutableElement> methoden =
		ElementFilter.methodsIn(siblings);
	    methodenSchleife:
	    for(ExecutableElement methode : methoden) {
		if (methode.getSimpleName().contentEquals(doc.name())
		    && gleicheSignatur(methode, (MethodDoc)doc)) {
		    return methode;
		}
	    }  // for
	    LaTeXWriter.configuration.root
		.printWarning("Keine Methode zu " + doc +" gefunden. "+
			      "Kandidaten waren: " + methoden);
	}  // if method
	if (doc.isField() || doc.isEnumConstant())  {
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
		if (klasse.getSimpleName()
		    .contentEquals(((Type)doc).simpleTypeName())) {
		    return klasse;
		}
	    }
	    LaTeXWriter.configuration.root
		.printWarning("Keine Klasse zu " + doc +" gefunden. "+
			      "Kandidaten waren: " + classes);
	} // if type
	throw new RuntimeException("unbekannter Doc-Typ: " + doc + " [" + doc.getClass() + "]");
    }

    /**
     * Untersucht, ob ein Element und das entsprechende
     * Dokumentationselement zusammenpassen.
     */
    private boolean gleicheSignatur(ExecutableElement methode,
				    ExecutableMemberDoc doc)
    {
	Parameter[] parameters =
	    doc.parameters();
	// Signatur vergleichen
	List<? extends VariableElement> args =
	    methode.getParameters();
	if (args.size() != parameters.length)
	    return false;
	for(int i = 0; i < parameters.length; i++) {
	    // die Typnamen in der Element-API enthalten hier eventuell
	    // Leerzeichen (nach Kommas), bei Javadoc nicht (oder umgekehrt).
	    // 
	    String argTtext = typeAsString(args.get(i).asType()).replace(", ", ",");
	    if (! argTtext.equals(parameters[i].type().toString().replace(", ", ",")))
		return false;
	}
	return true;
    }
				    
    /**
     * Wandelt einen Typ in einen String um, um Signaturen
     * vergleichen zu können.
     */
    private String typeAsString(TypeMirror argT) {
	if (argT.getKind() == TypeKind.TYPEVAR) {
	    TypeVariable argTVar = (TypeVariable)argT;
	    TypeMirror lowerBound =
		argTVar.getLowerBound();
	    if (lowerBound.getKind() != TypeKind.NULL){
		return argT + " super " + lowerBound;
	    }
	    else {
		TypeMirror upperBound =
		    argTVar.getUpperBound();
		if (upperBound.toString()
		    .equals("java.lang.Object")) {
		    // keine Beschränkung
		    return argT.toString();
		}
		else {
		    return argT + " extends " + upperBound;
		}
	    }
	}
	else {
	    return argT.toString();
	}
    }  // typeAsString()


    /* ******* Hilfsmethoden für die Tree-Visitor-Methoden. ******* */


    private void makeLink(String text, Element linkTarget,
			  SourceFormatter target)
    {
	RootDoc root = LaTeXWriter.configuration.root;
	Doc doc;
	switch(linkTarget.getKind()) {
	case PACKAGE: {
	    PackageElement pEl = (PackageElement)linkTarget;
	    doc = root.packageNamed(pEl.getQualifiedName().toString());
	    break;
	}
	case CLASS:
	case INTERFACE:
	case ENUM:
	case ANNOTATION_TYPE: {
	    TypeElement type = (TypeElement)linkTarget;
	    doc = root.classNamed(type.getQualifiedName().toString());
	    // TODO: innere Klassen
	    break;
	}
	case CONSTRUCTOR:
	case METHOD: {
	    ExecutableElement method = (ExecutableElement)linkTarget;
	    TypeElement type = (TypeElement)method.getEnclosingElement();
	    ClassDoc typeDoc =
		root.classNamed(type.getQualifiedName().toString());
	    // richtige Methode/konstruktor raussuchen
	    doc = getDocForElement(method, typeDoc);
// 	    System.err.println("Method-Doc: " + doc);
	    break;
	}
	case FIELD:
	case ENUM_CONSTANT: {
	    VariableElement field = (VariableElement)linkTarget;
	    TypeElement type = (TypeElement)field.getEnclosingElement();
	    ClassDoc typeDoc = root.classNamed(type.getQualifiedName().toString());
	    // TODO: richtiges Feld/Enum-Konstante heraussuchen
	    doc = null;
	    break;
	}
	case EXCEPTION_PARAMETER:
	case LOCAL_VARIABLE:
	case PARAMETER: {
	    // alle Varianten lokaler Variablen. Hier müssen
	    // wir eigentlich keinen Link setzen, oder?
	    // (TODO: vielleicht auch lokale Variablen verlinken)
	    target.printId(text);
	    return;
	}
	default:
	    // anderes Element?
	    target.print("«[" + text + ":" + linkTarget+"]»");
	    return;
	}  // switch
	makeLink(text, doc, linkTarget, target);
    }

    /**
     * setzt einen Link auf ein Programmelement.
     * @param text der Text des Links.
     * @param doc das Dokumentationselement zum verlinkten Element.
     *  Falls {@code null}, ist keine Dokumentation dazu vorhanden.
     * @param linkTarget das Element, auf das gelinkt wird.
     * @param target hier wird der Text hingeschrieben.
     */
    void makeLink(String text, Doc doc, Element linkTarget,
		   SourceFormatter target) {
	if (doc == null) {
	    // dieses Programm-Element hat keine Dokumentation, und wurde
	    // nicht in der API verwendet.
	    makeExternalLink(text, linkTarget, doc, target);
	}
	else/* if (!doc.isIncluded()) {
	    // dieses Element ist nicht in der aktuellen Doku, aber
	    // wurde irgendwo in der Doku erwähnt.
	    // => vielleicht eine Liste im Anhang?
	    makeExternalLink(text, linkTarget, doc, target);
	    } else*/ {
	    // dieses Element gehört zu den Elementen, die gerade
	    // dokumentiert werden. => interner Link.
	    target.printLinkedId(text, doc);
	}
    }


    /**
     * setzt einen externen Link zu einem Element, welches nicht
     * enthalten ist.
     */
    private void makeExternalLink(String text, Element linkTarget,
				  Doc linkedDoc, SourceFormatter target) {
	// TODO: externen link setzen, falls wir einen haben.
	target.print(text);
    }



    /**
     * Zählt, wie viele Array-Level in diesem Typ sind.
     */
    private int arrayLevel(Tree tree) {
	if(tree.getKind() != Tree.Kind.ARRAY_TYPE) {
	    return 0;
	}
	ArrayTypeTree array = (ArrayTypeTree)tree;
	return 1 + arrayLevel(array.getType());
    }

    /**
     * Sucht den Basistyp nach Entfernen aller Array-Level.
     */
    private Tree arrayBaseType(Tree tree) {
	if(tree.getKind() != Tree.Kind.ARRAY_TYPE) {
	    return tree;
	}
	ArrayTypeTree array = (ArrayTypeTree)tree;
	return arrayBaseType(array.getType());
    }

    /**
     * Sucht ein Element in einem Scope.
     */
    public Element findElement(Element scope, Name name, TypeMirror type)
    {
	if(scope.getKind() == ElementKind.TYPE_PARAMETER) {
	    List<? extends TypeMirror> bndlist = 
		((TypeParameterElement)scope).getBounds();
	    if(bndlist.isEmpty()) {
		bndlist =
		    Collections.singletonList(elements.getTypeElement("java.lang.Object").asType());
	    }
	    for(TypeMirror bnd: bndlist){
		Element subScope = types.asElement(bnd);
		Element found = findElement(subScope, name, type);
		if (found != null)
		    return found;
	    }
	    return null;
	}
	List<? extends Element> list =
	    (scope.getKind().isClass() || scope.getKind().isInterface()) ?
	    elements.getAllMembers((TypeElement)scope) :
	    scope.getEnclosedElements();
	for(Element el : list) {
	    if(el.getSimpleName().equals(name)) {
		// TODO: Typ überprüfen
		return el;
	    }
	}
	Element enclosing = scope.getEnclosingElement();
	if(enclosing == null)
	    return null;
	return findElement(enclosing, name, type);
	
    }  // findElement(Element, ...)


    /**
     * Sucht ein Element mit angegebenen Namen
     * und Typ in einem Scope.
     */
    public Element findElement(Scope scope, Name name, TypeMirror type) {
// 	System.err.println("findElement(" + scope + ", {" + name + "}, {" + type + "})");
	for(Element el: scope.getLocalElements()) {
// 	    System.err.println("lokal: " + el);
	    if(el.getSimpleName().equals(name)) {
		// TODO: Typ überprüfen
		return el;
	    }
	}
	TypeElement typeEl = scope.getEnclosingClass();
	if (type != null) {
	    Element inClass = findElement(typeEl, name, type);
	    if (inClass != null)
		return inClass;
	}
	Scope enclosing = scope.getEnclosingScope();
	if(enclosing == null)
	    return null;
	return findElement(enclosing, name, type);

    }  // findElement(Scope, ...)


    /**
     * Scannt einen Tree als Typ. (Dies hat eine
     * Sonderbehandlung bei Identifier, die andernfalls
     * als Expression aufgefasst würden.)
     */
    public void scanType(Tree tree, SourceFormatter target) {
	if (tree.getKind() == Tree.Kind.IDENTIFIER) {
	    TypeMirror t =
		trees.getTypeMirror(TreePath.
				    getPath(this.getCurrentPath(),
						tree));
	    Element el = types.asElement(t);
	    this.makeLink(tree.toString(), el, target);
	    // TODO: eventl. Link setzen
	    //	    target.print("{" + tree + "}");
	}
	else {
	    //	    System.err.println("Scan als Type: " + tree + " : " + tree.getClass());
	    this.scan(tree, target);
	}
    }  // scanType
    


    /**
     * Druckt eine Variablendeklaration (ohne das Semikolon am Ende).
     */
    public void printVariableDecl(VariableTree var, SourceFormatter target)
    {
	this.scan(var.getModifiers(), target);
	this.scanType(var.getType(), target);
	target.print(" ");
	this.printVariableInitializer(var, target);
    }

    /**
     * Druckt eine Variablendeklaration ohne Modifier und Typ
     * (d.h. nur Namen und eventuell Initializer).
     *
     * Dies ist für Variablendeklarationen, die mehrere Variablen
     * gleichzeitig deklarieren, notwendig (insbesondere bei
     * for-Schleifen).
     */
    public void printVariableInitializer(VariableTree var,
					 SourceFormatter target)
    {
	target.printId(var.getName());
	ExpressionTree init = var.getInitializer();
	if (init != null) {
	    target.printSpecial(" = ");
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


    /**
     * Druckt die Initialisierungsanweisungen einer For-Schleife.
     * Das ist entweder eine Variablendeklaration (eventuell mehrere
     * Variablen gleichen Typs), oder eine Reihe von
     * StatementExpressions (d.h. die Sorte Expressions, die in
     * ExpressionStatements vorkommen kann), verpackt in je ein
     * ExpressionStatement. Wir müssen beim Drucken aufpassen, dass
     * keine {@code ;}, sondern {@code ,} dazwischen sind.
     */
    public void printForInit(List<?extends StatementTree> liste,
			     SourceFormatter target) 
    {
	if(liste.isEmpty()) {
	    return;
	}
	StatementTree first = liste.get(0);
	switch(first.getKind()) {
	case VARIABLE: {
	    VariableTree var = (VariableTree)first;
	    this.printVariableDecl(var, target);
	    for(StatementTree stat : liste.subList(1, liste.size())) {
		var = (VariableTree)stat;
		target.printSpecial(", ");
		this.printVariableInitializer(var, target);
	    }
	    return;
	}
	case EXPRESSION_STATEMENT: {
	    // TODO: nachschauen, ob durch das Überspringen
	    // der ExpressionStatements der TreePath kaputtgeht.
	    ExpressionStatementTree exp = (ExpressionStatementTree)first;
	    this.scan(exp.getExpression(), target);
	    for(StatementTree stat : liste.subList(1, liste.size())) {
		exp = (ExpressionStatementTree)stat;
		target.printSpecial(", ");
		this.scan(exp.getExpression(), target);
	    }
	    return;
	}
	default:
	    throw new IllegalArgumentException(first.getClass().getName());
	}  // switch
    } // printForInit.

    /**
     * Druckt den Update-Teil eines For-Statements. Dies ist eine
     * Liste von StatementExpressions, für uns leider verpackt in je
     * ein ExpressionStatement.
     * Wir müssen Kommas dazwischensetzen.
     */
    public void printForUpdate(List<?extends ExpressionStatementTree>liste,
			       SourceFormatter target)
    {
	    // TODO: nachschauen, ob durch das Überspringen
	    // der ExpressionStatements der TreePath kaputtgeht.
	if (liste.isEmpty()) {
	    return;
	}
	ExpressionStatementTree exp = liste.get(0);
	this.scan(exp.getExpression(), target);
	for(ExpressionStatementTree stat : liste.subList(1,liste.size())) {
	    target.print(", ");
	    this.scan(stat.getExpression(), target);
	}
    }


    /**
     * Druckt eine Parameterliste (d.h. eine Liste von
     * Parameterdeklarationen einer Methode/eines Konstruktors).
     */
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


    /**
     * Druckt eine Liste von throws-Deklarationen
     * für eine Methode.
     */
    public void printThrows(List<? extends ExpressionTree> exceptions,
			    SourceFormatter target) {
	target.addIndent();
	this.printTypeList(exceptions, target,
			   "\n" + "throws ",
			   ",\n"+ "       ",
			   "");
	target.popIndent();
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
	target.printSpecial(prefix);
	this.scan(liste.get(0), target);
	for(Tree elem : liste.subList(1, liste.size())) {
	    target.printSpecial(infix);
	    this.scan(elem, target);
	}
	target.printSpecial(postfix);
    }

    /**
     * Druckt eine Liste als Typen.
     *
     * Falls die Liste leer ist, wird gar nichts getan,
     * ansonsten wird zunächst {@code prefix} ausgegeben,
     * dann werden die einzelnen Elemente als Typen gescannt,
     * getrennt durch {@code infix}, danach wird
     * {@code postfix} ausgegeben.
     * @param liste die zu druckenden Elemente. Sie werden als
     *       Typen aufgefasst.
     * @param target der Formatierer, auf dem der Inhalt gedruckt
     *         werden soll
     * @param prefix ein String, der am Anfang der Liste
     *        (falls nicht leer) gedruckt werden soll
     * @param infix ein String, der zwischen den einzelnen
     *        Listenelementen gedruckt werden soll.
     * @param postfix ein String, der am Ende der Liste
     *        (falls nicht leer) gedruckt werden soll.
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
	target.printSpecial(prefix);
	this.scanType(liste.get(0), target);
	for(Tree elem : liste.subList(1, liste.size())) {
	    target.printSpecial(infix);
	    this.scanType(elem, target);
	}
	target.printSpecial(postfix);
    }


    /**
     * Druckt die Liste der Grenzen für einen
     * Typparameter (eines Types oder einer Methode).
     */
    public void printBounds(List<? extends Tree> liste,
			    SourceFormatter target)
    {
	printTypeList(liste, target, " extends ", "&", "");
    }


    /**
     * Druckt ein Statement eingerückt.
     * <p>
     * Ist dies ein Block-Statement, muss nichts weiter
     * getan werden (außer dieses auszugeben), da es
     * ja selbst seinen Inhalt einrückt, und eine weitere
     * Einrückung überflüssig ist.
     *</p>
     * Andernfalls wird die Einrückung erhöht, eine neue Zeile
     * begonnen, das Statement gedruckt und die Einrückung wieder
     * zurückgesetzt.
     */
    private void printIndented(StatementTree statement,
			       SourceFormatter target) {
	if (statement instanceof BlockTree) {
	    this.scan(statement, target);
	} else {
	    target.addIndent();
	    target.println();
	    this.scan(statement, target);
	    target.popIndent();
	}
    }

    /**
     * Druckt eine Reihe von Anweisungen eingerückt.
     */
    private void printIndented(List<? extends StatementTree> statements,
			       SourceFormatter target)
    {
	switch(statements.size()) {
	case 0:
	    return;
	case 1:
	    printIndented(statements.get(0), target);
	    return;
	default:
	    target.addIndent();
	    this.printList(statements, target, "\n", "\n", "");
	    target.popIndent();
	}
    }


    /* ****************** TreeVisitor-Methoden ***************** */


    /* ******** Deklarationen ******** */


    /**
     * Druckt eine Methode (oder einen Konstruktor o.ä.).
     */
    @Override
    public Void visitMethod(MethodTree meth, SourceFormatter target)
    {
	this.scan(meth.getModifiers(), target);
	this.printTypeParameters(meth.getTypeParameters(), target, true);
	if (meth.getName().contentEquals("<init>")) {
	    // TODO: Klassenname herausfinden
	    target.print(currentElement.name());
	}
	else {
	    this.scanType(meth.getReturnType(), target);
	    target.print(" ");
	    target.print(meth.getName());
	}
	target.print("(");
	printParameterList(meth.getParameters(), target);
	target.print(")");
	printThrows(meth.getThrows(), target);
	// TODO: was passiert bei abstrakten Methoden ohne Body?
	target.println();
	this.scan(meth.getBody(), target);
	target.println();

	return null;
    }



    
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
     * Druckt eine Typdeklaration (Klasse, Interface, etc.)
     */
    @Override
    public Void visitClass(ClassTree klasse,
			   SourceFormatter target)
    {
	target.print("«Typdeklaration: " +klasse.getSimpleName() +"»");
	// TODO
	return null;
    }


    /* ******** "normale" Statements ********** */

    /**
     * Druckt ein Assert-Statement.
     */
    @Override
    public Void visitAssert(AssertTree tree, SourceFormatter target)
    {
	target.printSpecial("assert ");
	this.scan(tree.getCondition(), target);
	ExpressionTree detail = tree.getDetail();
	if (detail != null) {
	    target.printSpecial(":");
	    this.scan(detail, target);
	}
	target.printSpecial(";");
	return null;
    }


    /**
     * Druckt einen Block.
     */
    @Override
    public Void visitBlock(BlockTree block, SourceFormatter target)
    {
	target.printSpecial("{");
	target.addIndent();
	target.println();
	//	System.err.println("visitBlock(){ ...");
	this.printList(block.getStatements(), target,
		  "", "\n", "\n");
	//	System.err.println("... } // visitBlock() ");
	target.popIndent();
	target.printSpecial("}");
	return null;
    }
    

    /**
     * Druckt ein Break-Statement.
     */
    @Override
    public Void visitBreak(BreakTree br, SourceFormatter target)
    {
	Name label = br.getLabel();
	if (label == null)
	    target.printSpecial("break;");
	else {
	    target.printSpecial("break ");
	    target.printId(label);
	}
	return null;
    }


    /**
     * Druckt ein Continue-Statement.
     */
    @Override
    public Void visitContinue(ContinueTree br, SourceFormatter target)
    {
	Name label = br.getLabel();
	if (label == null)
	    target.printSpecial("break;");
	else {
	    target.printSpecial("break ");
	    target.printId(label);
	}
	return null;
    }

    /**
     * Druckt eine Do-While-Schleife.
     */
    @Override
    public Void visitDoWhileLoop(DoWhileLoopTree loop,
				 SourceFormatter target)
    {
	StatementTree stat = loop.getStatement();
	target.printSpecial("do ");
	this.printIndented(stat, target);
	target.print(stat.getKind() == Tree.Kind.BLOCK ? "\n" : " ");
	target.printSpecial("while");
	this.scan(loop.getCondition(), target);
	target.printSpecial(";");
	return null;
    }


    /**
     * Druckt ein leeres Statement.
     */
    @Override
    public Void visitEmptyStatement(EmptyStatementTree empty,
				    SourceFormatter target)
    {
	target.printSpecial(";");
	return null;
    }

    /**
     * Druckt eine for-each-Schleife.
     */
    @Override
    public Void visitEnhancedForLoop(EnhancedForLoopTree loop,
				     SourceFormatter target)
    {
	target.printSpecial("for ");
	target.printSpecial("(");
	this.printVariableDecl(loop.getVariable(), target);
	target.printSpecial(":"); // TODO: a element b (\in)?
	this.scan(loop.getExpression(), target);
	target.printSpecial(") ");
	this.printIndented(loop.getStatement(), target);
	return null;
    }


    /**
     * Druckt ein Expression-Statement.
     */
    @Override
    public Void visitExpressionStatement(ExpressionStatementTree stat,
					 SourceFormatter target) {
	this.scan(stat.getExpression(), target);
	target.printSpecial(";");
	return null;
    }


    /**
     * Druckt eine for-Schleife.
     */
    @Override
    public Void visitForLoop(ForLoopTree loop,
			     SourceFormatter target)
    {
	target.printSpecial("for");
	target.printSpecial("(");
	// TODO: verhindern, dass hier zusätzliche ";" mit
	// reinkommen.
// 	System.err.println(loop.getInitializer());
// 	for (int i = 0,  j = 7; j<i ; i++) {
// 	    break;
// 	}
// 	for("Hallo".toString(), "Welt".charAt(2) ; ; ) {
// 	    break;
// 	}

// 	for(StatementTree init: loop.getInitializer()) {
// 	    System.err.println("initializer: " + init + " (" + init.getClass()+")");
// 	}
	this.printForInit(loop.getInitializer(), target);
	target.printSpecial(";");
	this.scan(loop.getCondition(), target);
	target.printSpecial(";");
	// TODO: verhindern, dass hier zusätzliche ";" mit
	// reinkommen.
	
	this.printForUpdate(loop.getUpdate(), target);
	target.printSpecial(")");
	this.printIndented(loop.getStatement(), target);
	return null;
    }



    /**
     * Druckt ein if-Statement.
     */
    @Override
    public Void visitIf(IfTree ifS, SourceFormatter target)
    {
	target.printSpecial("if ");
	//	target.printSpecial("(");
	//	System.out.println("condition: " + ifS.getCondition());
	this.scan(ifS.getCondition(), target);
	//	target.printSpecial(")");
	target.print(" ");
	this.printIndented(ifS.getThenStatement(), target);
	StatementTree elseTree = ifS.getElseStatement();
	if (elseTree != null) {
	    target.println();
	    target.printSpecial("else ");
	    if (elseTree instanceof IfTree) {
		// Sonderbehandlung für else-if-Kaskaden:
		//  nicht weiter einrücken, sondern auf der
		//  selben Zeile weitermachen.
		this.scan(elseTree, target);
	    }
	    else {
		this.printIndented(elseTree, target);
	    }
	}
	return null;
    }


    /**
     * Druckt ein benanntes Statement.
     */
    @Override
    public Void visitLabeledStatement(LabeledStatementTree stat,
				      SourceFormatter target)
    {
	target.printId(stat.getLabel());
	target.printSpecial(":");
	target.addIndent();
	target.println();
	this.scan(stat.getStatement(), target);
	target.popIndent();
	return null;
    }

    /**
     * Druckt ein Return-Statement.
     */
    @Override
    public Void visitReturn(ReturnTree ret, SourceFormatter target)
    {
	ExpressionTree exp = ret.getExpression();
	if (exp == null) {
	    target.printSpecial("return;");
	}
	else {
	    target.printSpecial("return ");
	    this.scan(exp, target);
	    target.printSpecial(";");
	}
	return null;
    }

    /**
     * Druckt ein Switch-Statement.
     */
    @Override
    public Void visitSwitch(SwitchTree st, SourceFormatter target)
    {
	target.printSpecial("switch");
	// target.printSpecial("(");
	this.scan(st.getExpression(), target);
	// target.printSpecial(")");
	target.print(" ");
	target.printSpecial("{");
	target.addIndent();
	target.println();
	this.scan(st.getCases(), target);
	target.popIndent();
	target.printSpecial("}");
	return null;
    }

    /**
     * Druckt ein synchronized-Statement.
     */
    @Override
    public Void visitSynchronized(SynchronizedTree syn,
				  SourceFormatter target)
    {
	target.printSpecial("synchronized");
	target.printSpecial("(");
	this.scan(syn.getExpression(), target);
	target.printSpecial(")");
	target.print(" ");
	this.scan(syn.getBlock(), target);
	return null;
    }

    /**
     * Druckt ein Throw-Statement.
     */
    @Override
    public Void visitThrow(ThrowTree tree, SourceFormatter target)
    {
	target.printSpecial("throw ");
	this.scan(tree.getExpression(), target);
	target.printSpecial(";");
	return null;
    }

    /**
     * Druckt ein Try-Statement.
     */
    @Override
    public Void visitTry(TryTree tree, SourceFormatter target)
    {
	target.printSpecial("try ");
	this.scan(tree.getBlock(), target);
	printList(tree.getCatches(), target, "\n", "\n", "");
	BlockTree fin = tree.getFinallyBlock();
	if (fin != null) {
	    target.println();
	    target.printSpecial("finally ");
	    this.scan(fin, target);
	}
	return null;
    }


    /**
     * Druckt eine While-Schleife.
     */
    @Override
    public Void visitWhileLoop(WhileLoopTree loop,
				 SourceFormatter target)
    {
	target.printSpecial("while");
	//	target.printSpecial("(");
	this.scan(loop.getCondition(), target);
	//	target.printSpecial(")");
	this.printIndented(loop.getStatement(), target);
	return null;
    }



    /* ******** Expressions ************* */



    /**
     * Druckt eine Annotation.
     */
    @Override
    public Void visitAnnotation(AnnotationTree tree,
				SourceFormatter target)
    {
	target.printSpecial("@");
	this.scanType(tree.getAnnotationType(), target);
	this.printList(tree.getArguments(), target, "(", ",", ")");
	return null;
    }


    /**
     * Druckt einen Array-Zugriff.
     */
    @Override
    public Void visitArrayAccess(ArrayAccessTree tree,
				 SourceFormatter target)
    {
	this.scan(tree.getExpression(), target);
	target.printSpecial("[");
	this.scan(tree.getIndex(), target);
	target.printSpecial("]");
	return null;
    }


    /**
     * Druckt eine Zuweisung.
     */
    @Override
    public Void visitAssignment(AssignmentTree tree,
				SourceFormatter target)
    {
	this.scan(tree.getVariable(), target);
	target.printSpecial(" = ");
	this.scan(tree.getExpression(), target);
	return null;
    }


    /**
     * Druckt einen binären Ausdruck.
     */
    @Override
    public Void visitBinary(BinaryTree bin, SourceFormatter target)
    {
	this.scan(bin.getLeftOperand(), target);
	target.printSpecial(bin.getKind());
	this.scan(bin.getRightOperand(), target);

	return null;
    }


    /**
     * Druckt einen Rechnung-und-Zuweisungs-Ausdruck.
     */
    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree tree,
					SourceFormatter target)
    {
	this.scan(tree.getVariable(), target);
	target.printSpecial(tree.getKind());
	this.scan(tree.getExpression(), target);
	return null;
    }

    /**
     * Druckt eine bedingten Ausdruck {@code a ? b : c }.
     */
    @Override
    public Void visitConditionalExpression(ConditionalExpressionTree tree,
					   SourceFormatter target)
    {
	this.scan(tree.getCondition(), target);
	target.printSpecial("?");
	this.scan(tree.getTrueExpression(), target);
	target.printSpecial(":");
	this.scan(tree.getFalseExpression(), target);

	return null;
    }



    /**
     * Druckt einen Identifier (als Expression).
     */
    @Override
    public Void visitIdentifier(IdentifierTree id, SourceFormatter target)
    {
	target.printId(id.getName());
	return null;
    }

    /**
     * Druckt einen instanceof-Ausdruck.
     */
    @Override
    public Void visitInstanceOf(InstanceOfTree tree,
				SourceFormatter target)
    {
	this.scan(tree.getExpression(), target);
	target.printSpecial(" instanceof ");
	this.scanType(tree.getType(), target);
	return null;
    }

    /**
     * Druckt ein Literal.
     */
    @Override
    public Void visitLiteral(LiteralTree literal,
			     SourceFormatter target)
    {
	target.printLiteral(literal.getValue(), literal.getKind());
	return null;
    }


    /**
     * Druckt einen Variablen-Zugriffs-Ausdruck.
     */
    @Override
    public Void visitMemberSelect(MemberSelectTree tree,
				  SourceFormatter target)
    {
	// TODO: Link setzen
	this.scan(tree.getExpression(), target);
	target.printSpecial(".");
	target.printId(tree.getIdentifier());
	return null;
    }


    /**
     * Druckt einen Methodenaufruf.
     * <p>
     *  Hier liegt das Problem darin, dass wir einen Selector,
     *  einige Typparameter und eine Argumentenliste haben, und
     *  die Typparameter, falls vorhanden, syntaktisch eigentlich
     *  in der Mitte des Selectors (d.h. vor seinem letzten
     *  Identifier) stehen müssen.
     */
    @Override
    @SuppressWarnings("ungültig")
    public Void visitMethodInvocation(MethodInvocationTree tree,
				      SourceFormatter target)
    {
// 	System.err.println("visitMethodInvocation() { ... ");

	// Ja, die Typparameter müssen eigentlich in den Selector
	// eingebaut werden. Hmm.
	ExpressionTree selector = tree.getMethodSelect();
	Name identifier;
	TypeMirror containing;
	Element methodElement;

	TreePath selectorPath =
	    TreePath.getPath(this.getCurrentPath(), selector);

// 	System.out.println("--");
// 	System.out.println("selector : " + selector);
	TypeMirror t = trees.getTypeMirror(selectorPath);
// 	System.out.println("type : " + t);
// 	Element el = trees.getElement(selectorPath);
// 	System.out.println("element: " + el);

// 	System.err.println("selector: " + selector + " (" +
// 			   selector.getKind()+")");
	switch(selector.getKind()) {
	case MEMBER_SELECT:
	    MemberSelectTree msTree = (MemberSelectTree)selector;
	    ExpressionTree expr = msTree.getExpression();
	    TreePath exprPath = TreePath.getPath(selectorPath, expr);
	    containing = trees.getTypeMirror(exprPath);
	    //	    System.out.println("expression type : " + containing);

	    this.scan(expr, target);
	    target.printSpecial(".");
	    identifier = msTree.getIdentifier();

	    methodElement =
		findElement(types.asElement(containing), identifier, t);
	    break;
	case IDENTIFIER:
	    identifier = ((IdentifierTree)selector).getName();
	    //	    System.err.println("identifier: " + identifier);
	    Scope s;
// 	    if(identifier.contentEquals("this") ||
// 	       identifier.contentEquals("super")) {
// 		// TODO: passenden Konstruktor raussuchen und verlinken.
// 		TreePath path = this.getCurrentPath();
// 		System.err.println("path: " + path);
// //  		for(Tree tr : path) {
// //  		    System.err.print(tr+", ");
// //  		}
// 		for (TreePath tp = selectorPath;
// 		     tp != null ;
// 		     tp = tp.getParentPath()) {
// 		    System.err.println("{" + tp.getLeaf().getKind() + "}, ");
// 		    s = trees.getScope(tp);
// 		}
// 		s = trees.getScope(path);
// 		System.err.println("scope: " + s);
// 	    }
// 	    else {
	    s = trees.getScope(selectorPath);
// 	    }
	    methodElement = findElement(s, identifier, t);
	    //	    System.err.println("methodElement: " + methodElement);
	    break;
	default:
	    throw new IllegalArgumentException("Methoden-Selector: [" + selector + "] (" + selector.getKind() +")");
	}
	this.printTypeList(tree.getTypeArguments(), target, "<", ", ", ">");
	this.makeLink(identifier.toString(), methodElement, target);
	//	System.out.println("methodElement: " + methodElement);
	target.print("(");
	this.printList(tree.getArguments(), target, "", ", ", "");
	target.print(")");

// 	System.err.println("... } // visitMethodInvocation() ");
	
	return null;
    }

    /**
     * Druckt einen Array-Erstellungs-Ausdruck.
     */
    @Override
    public Void visitNewArray(NewArrayTree tree,
			      SourceFormatter target)
    {

	int[][] beispiel = new int[2][];
	int[][] beispiel1 = new int[2][3];
	int[][] beispiel2 = new int[][]{ { 1, 1}, {2,2}};

// 	System.err.println("tree:              " + tree);
// 	System.err.println("tree.type:         " + tree.getType());
// 	System.err.println("tree.dimensions:   " + tree.getDimensions());
// 	System.err.println("tree.initializers: " + tree.getInitializers());

	Tree type = tree.getType();

	if(type != null) {
	    target.printSpecial("new ");
	    Tree baseType = arrayBaseType(type);
	    int level = arrayLevel(type) + 1;
	    List<? extends ExpressionTree> dims = tree.getDimensions();
	    this.scanType(baseType, target);
	    this.printList(dims, target, "[", "][", "]");
	    for(int i = dims.size(); i < level; i++) {
		target.printSpecial("[]");
	    }
	}
	List<? extends ExpressionTree> init = tree.getInitializers();
	if (init != null) {
	    this.printList(init, target, "{ ", ", ", " }");
	}
	return null;
    }

    /**
     * Druckt einen Exemplarerstellungsausdruck.
     */
    @Override
    public Void visitNewClass(NewClassTree tree,
			      SourceFormatter target)
    {
	ExpressionTree enclosing =
	    tree.getEnclosingExpression();
	if (enclosing != null) {
	    this.scan(enclosing, target);
	    target.printSpecial(".");
	}
	target.printSpecial("new ");
	this.printTypeList(tree.getTypeArguments(), target, "<", ", ", "> ");
// 	if (enclosing != null) {
// 	    this.scan(tree.getIdentifier(), target);
// 	} else {
	this.scanType(tree.getIdentifier(), target);
// 	}
	target.printSpecial("(");
	this.printList(tree.getArguments(), target, "", ", ", "");
	target.printSpecial(")");
	ClassTree body = tree.getClassBody();
	if (body != null) {
	    // TODO: vielleicht eine andere Methode dafür nehmen,
	    // um von echten Klassen-Typen unterscheiden zu können.
	    this.scan(body, target);
	}
	return null;
    }
    

    /**
     * Druckt einen eingeklammerten Ausdruck.
     */
    @Override
    public Void visitParenthesized(ParenthesizedTree tree,
				   SourceFormatter target)
    {
	target.printSpecial("(");
	target.pushIndent();
	this.scan(tree.getExpression(), target);
	target.printSpecial(")");
	target.popIndent();
	return null;
    }

    /**
     * Druckt einen Type-Cast-Ausdruck.
     */
    @Override
    public Void visitTypeCast(TypeCastTree tree, SourceFormatter target)
    {
	target.printSpecial("(");
	this.scanType(tree.getType(), target);
	target.printSpecial(")");
	this.scan(tree.getExpression(), target);
	
	return null;
    }


    /**
     * Druckt einen unären Ausdruck.
     */
    @Override
    public Void visitUnary(UnaryTree tree, SourceFormatter target)
    {
	switch(tree.getKind()) {
	case POSTFIX_DECREMENT:
	case POSTFIX_INCREMENT:
	    // postfix
	    this.scan(tree.getExpression(), target);
	    target.printSpecial(tree.getKind());
	    return null;
	}
	// alle anderen sind Prefix.
	target.printSpecial(tree.getKind());
	this.scan(tree.getExpression(), target);
	
	return null;
    }


    /* ********* Typen ********* */



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



    /**
     * Druckt einen parametrisierten Typ.
     */
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


    /**
     * Druckt den Namen eines primitiven Typs.
     */
    @Override
    public Void visitPrimitiveType(PrimitiveTypeTree prim,
				   SourceFormatter target) {
	target.print(prim.getPrimitiveTypeKind().toString().toLowerCase());
	return null;
    }



    /**
     * Druckt einen Wildcard-Typ (ohne folgendes Leerzeichen).
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





    /* ******** sonstiges ********** */


    /**
     * Druckt die Modifiers aus, mit je einem Leerzeichen
     * dazwischen und einem danach.
     */
    @Override
    public Void visitModifiers(ModifiersTree mods,
			       SourceFormatter target)
    {
	// TODO: Annotations
	this.printList(mods.getAnnotations(), target, "", ", ", "\n");
	Set<Modifier> modSet = mods.getFlags();
	for(Modifier m : modSet) {
	    target.printSpecial(m.toString());
	    target.print(" ");
	}
	return null;
    }

    /**
     * Druckt einen Zweig eines Switch-Statements.
     */
    @Override
    public Void visitCase(CaseTree cTree, SourceFormatter target)
    {
	ExpressionTree exp = cTree.getExpression();
	if (exp == null) {
	    target.printSpecial("default: ");
	}
	else {
	    target.printSpecial("case ");
	    this.scan(exp, target);
	    target.printSpecial(": ");
	}
	printIndented(cTree.getStatements(), target);
	target.println();
	return null;
    }

    /**
     * Druckt einen Catch-Zweig eines Throw-Statements.
     */
    @Override
    public Void visitCatch(CatchTree cat, SourceFormatter target) {
	target.printSpecial("catch");
	target.printSpecial("(");
	this.printVariableDecl(cat.getParameter(), target);
	target.printSpecial(")");
	this.scan(cat.getBlock(), target);
	return null;
    }


    /**
     * Druckt eine Typparameterdeklaration.
     */
    @Override
    public Void visitTypeParameter(TypeParameterTree param,
				   SourceFormatter target)
    {
	target.print(param.getName());
	printBounds(param.getBounds(), target);
	return null;
    }


}
