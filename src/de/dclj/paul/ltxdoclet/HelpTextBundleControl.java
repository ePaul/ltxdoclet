package de.dclj.paul.ltxdoclet;
import java.util.*;
import java.io.*;


/**
 * A resource bundle control implementation for loading
 * a help text in a UTF-8 encoded ".txt" file as a single-key
 * ResourceBundle. (The key is {@code "help"}).
 */
class HelpTextBundleControl extends ResourceBundle.Control {
    public List<String> getFormats(String basename) {
        return Arrays.asList("helptext");
    }
    public ResourceBundle newBundle(String baseName,
                                    Locale loc,
                                    String format,
                                    ClassLoader loader,
                                    boolean reload)
        throws IOException
    {
        if(!format.equals("helptext")) {
            return null;
        }
        String fileName =
            toResourceName(toBundleName(baseName, loc), "txt");

        InputStream stream = loader.getResourceAsStream(fileName);
        if(stream == null) {
            return null;
        }
        StringBuilder b = new StringBuilder(stream.available());
        Reader r =
            new BufferedReader(new InputStreamReader(stream,
                                                     "UTF-8"));
        char[] c = new char[500];
        int l;
        while((l = r.read(c)) > 0) {
            b.append(c, 0, l);
        }
        final String val = b.toString();
        return new ListResourceBundle() {
            protected Object[][] getContents() {
                return new Object[][]{ { "help", val } };
            }
        };
    }  // newBundle()
}
