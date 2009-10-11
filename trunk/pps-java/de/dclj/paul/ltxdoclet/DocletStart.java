package paul.ltxdoclet;

/*
 * (c) Paul Ebermann, 2001
 *
 */
 
 
import com.sun.javadoc.*;


/**
 * Eintrittspunkt für das Doclet.
 */
public class DocletStart
{

		/**
		 * Diese Methode wird beim Start aufgerufen.
		 */
	public static boolean start(RootDoc root)
	{
		System.out.println("ltxdoclet, (c) Paul Ebermann 2001");
		try
		{
			configuration().setOptions(root);
			DocletStart s = new DocletStart();
			new MainFileWriter().writeDoku();
			return !configuration().wasError;
		}
		catch(Exception ex)
		{
			return false;
		}
	}

	public static int optionLength(String option)
	{
		return configuration().optionLength(option);
	}
	
	public static boolean validOptions(String[][] ops, DocErrorReporter rep)
	{
		return configuration().validOptions(ops, rep);
	}
	
	/* ****************** */
	
	
	static LtxDocletConfiguration configuration()
	{
		if (LaTeXWriter.configuration == null)
		{
			LaTeXWriter.configuration = new LtxDocletConfiguration();
		}
		return LaTeXWriter.configuration;
	}
	
	/* ******************* */
	
	
	private DocletStart()
	{
	}

/*   
	private void readOptions()
	{
		String[][] ops = root.options();
		for (int i = 0; i < ops.length; i++)
		{
			if(ops[i][0].equals("-d"))
				targetDir = new File(cutQuotes(ops[i][1]));
			else if(ops[i][0].equals("-doctitle")
				doctitle = cutQuotes(ops[i][1]);
		}
	}


	private static String cutQuotes(String org)
	{
		if (org.startsWith("""") && org.endsWith(""""))
			return org.substring(1, org.length - 1);
		if (org.startsWith("'") && org.endsWith("'"))
			return org.substring(1, org.length - 1);
		return org;
	}
	
	private RootDoc root;
	private File targetDir = new File(".");
	private String doctitle;

*/

}
