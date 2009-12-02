package paul.ltxdoclet;

import com.sun.tools.doclets.*;
import com.sun.javadoc.*;
import java.util.*;
import java.io.*;

public class LtxDocletConfiguration
	extends Configuration
{

	public String doctitle;

	public void setSpecificDocletOptions(RootDoc root)
	{
		if (doctitle == null)
		{
			doctitle = "Die Package-Sammlung";
		}
		//...
	}
	
	
	public List threads = Collections.synchronizedList(new ArrayList());
	
	
	public boolean wasError;
	
	public int specificDocletOptionLength(String option)
	{

		if (option.equals("-d"))
			return 2;
		if (option.equals("-doctitle"))
			return 2;
		return 0;
	}


	public boolean specificDocletValidOptions(String[][] ops, DocErrorReporter err)
	{
		//...
		return true;
	}


	String toInputFileName(PackageDoc d)
	{
		return d.toString().replace('.', '/');
	}
	
	File toOutputFileName(PackageDoc d)
	{
		String newName = destdirname + d.toString().replace('.', '\\');
		File dir =  new File(newName);
		dir.mkdirs();
		return dir;
	}



}
