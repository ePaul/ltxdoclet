package paul.ltxdoclet;

import com.sun.javadoc.*;
import java.io.*;

public class PackageWriter
	extends LaTeXWriter
{

	public PackageWriter(PackageDoc pd)
		throws IOException
	{
		super(new File(configuration.toOutputFileName(pd), "package-doc.tex"));
		doc = pd;
	}
	
	private PackageDoc doc;

	public void writeDoc()
	{
		configuration.root.printNotice("ltxdoclet: package-doc.tex für \"" + pd +
												 "\" wird erstellt ...");
		ClassDoc[] classes = doc.allClasses();
		writeClasses(classes);
		chapter("Package " + doc);
		section("Übersicht");
		println(referenceTarget(doc));
		writeTags(doc.firstSentenceTags());
		section("Klassen-Liste");
		println("\\begin{itemize}");
		for (int i = 0; i < classes.length; i++)
		{
			ClassDoc cd = classes[i];
			print("\\item ");
			if (cd.isInterface())
				italic(cd.typeName());
			else 
				bold(cd.typeName());
			println(referenceTo(cd));
			writeTags(cd.firstSentenceTags());
		}
		println("\\end{itemize}");
		section("Beschreibung");
		writeTags(doc.inlineTags());
		Tag[] tags = doc.tags();
		for (int i = 0; i < tags.length; i++)
		{
			print("[" + tags[i]+ "| kind:" + tags[i].kind()+ "| name: "
			 + tags[i].name() + "|text: "+ tags[i].text() + "]");
			newLine(); 
		}
		writeClassImports(classes);
		close();
	}        // of PackageWriter.writeDoc()

	public void writeClasses(ClassDoc[] classes)
	{
		for (int i = 0; i < classes.length; i++)
		{
			final ClassDoc cd = classes[i];
			Thread tr = new Thread(cd + "-Writer")
				{
					public void run()
					{
						try 
						{
							new ClassWriter(cd).writeDoc();
						}
						catch(IOException io)
						{
							configuration.root.printError("Ausgabe für " + cd + " konnte nicht " +
																	"geschrieben werden.");
						}
						configuration.threads.remove(Thread.currentThread());
					}
				};       // of Thread
			configuration.threads.add(tr);
			tr.start();
		}        // of for
	}


	public void writeClassImports(ClassDoc[] classes)
	{
		String pkgDir = configuration.toInputFilename() + "/";
		for (int i = 0; i< classes.length; i++)
		{
			println("\\input{" + pkgDir + classes[i] + "}");
		}
	}

}
