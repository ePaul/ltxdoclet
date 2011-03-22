package de.dclj.paul.ltxdoclet;

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
     * Diese Methode wird von Javadoc aufgerufen, um
     * unser Doclet arbeiten zu lassen.
     * @param root alle Infos, die wir brauchen.
     * @return {@code true} falls erfolgreich, 
     *   {@code false} falls es einen Fehler gab.
     */
    public static boolean start(RootDoc root)
    {
	root.printNotice("ltxdoclet, (c) Paul Ebermann 2001, 2010");
	try
	    {
		configuration().setOptions(root);
		// DocletStart s = new DocletStart();
		//		configuration().root.printNotice("Lade MainFileWriter ...");
		if (configuration().includeSource) {
		    configuration().startCompiler();
		}
		new MainFileWriter().writeDoku();
		configuration().root.printNotice("Fertig.");
		configuration().root.printNotice("Fehler: " + configuration().wasError);
		return !configuration().wasError;
	    }
	catch(Exception ex)
	    {
		ex.printStackTrace();
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
	
    public static LanguageVersion languageVersion()
    {
	return LanguageVersion.JAVA_1_5;
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
