package paul.ltxdoclet;


import paul.ltxdoclet.PackageWriter;
import paul.ltxdoclet.LaTeXWriter;
import paul.ltxdoclet.MainFileWriter;
import paul.ltxdoclet.LtxDocletConfiguration;
import paul.ltxdoclet.DocletStart;



public class Alles
{
	public Alles()
	{
		new MainFileWriter();
		new PackageWriter((com.sun.javadoc.PackageDoc)null);
		new LtxDocletConfiguration();
		DocletStart.start(null);
		new LaTeXWriter(new java.io.File(""));
	}
}
