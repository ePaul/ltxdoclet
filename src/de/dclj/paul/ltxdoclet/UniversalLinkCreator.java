package de.dclj.paul.ltxdoclet;


import com.sun.javadoc.*;
import java.util.*;
import java.io.*;
import java.net.URL;

/**
 * Universeller Link-Creator.
 * Er delegiert je nach Package an einen spezielleren Link-Creator.
 *
 * @author <a href="mailto:paulo@heribert.local">Paul Ebermann</a>
 * @version $Id$
 */
public class UniversalLinkCreator implements LinkCreator {

    /**
     * Link-Creator für interne Links.
     */
    private LinkCreator intern;
    /**
     * Map von Package-Namen zu den entsprechenden LinkCreators
     * für externe Links.
     */
    private Map<String, LinkCreator> extern;
    /**
     * Fallback-Linkcreator: wenn in extern keiner
     * für das Package passt, wird hier einer genommen.
     */
    private LinkCreator noLink;

    /**
     * Konstruktor.
     */
    UniversalLinkCreator() {
        this.intern = new InternLinkCreator();
        this.noLink = new NoLinkCreator();
        this.extern = new HashMap<String, LinkCreator>();
    }

    /* **** Implementation of de.dclj.paul.ltxdoclet.LinkCreator *** */

    /**
     * Describe <code>createLink</code> method here.
     *
     * @param string a <code>String</code> value
     * @param doc a <code>Doc</code> value
     * @return a <code>String</code> value
     */
    public final String createLink(final String label, final Doc doc) {
        if(doc.isIncluded()) {
            // interner Link
            return intern.createLink(label, doc);
        }
        LinkCreator lc = extern.get(packageName(doc));
        if (lc != null) {
            // externer Link
            return lc.createLink(label, doc);
        }
        // kein Link
        return noLink.createLink(label, doc);
    }


    /* ************* sonstige öffentliche Methoden ********* */

    /**
     * Fügt eine Kommandozeilenoption hinzu.
     * @param args die Option mitsamt ihren Argumenten in jeweils
     *      einem String.
     */
    public void addOption(String... args)
    {
        LinkCreator lc;
        String packageURL;
        if(args[0].equals("-link") ||
           args[0].equals("-linkhtml")) {
            packageURL = args[1];
            lc = new HTMLInlineLinkCreator(args[1]);
        }
        else if(args[0].equals("-linkoffline") ||
                args[0].equals("-linkofflinehtml")) {
            lc = new HTMLInlineLinkCreator(args[1]);
            packageURL = args[2];
        }
        else if(args[0].equals("-linkpdf")) {
            packageURL = args[1].substring(0, args[1].lastIndexOf("/")+1);
            lc = new PDFInlineLinkCreator(args[1]);
        }
        else if(args[0].equals("-linkofflinepdf")) {
            lc = new PDFInlineLinkCreator(args[1]);
            packageURL = args[2];
        }
        // TODO: andere Optionen
        else {
            return;
        }
        parsePackageList(packageURL, lc);
    }

    
    /* ************** private Methoden ************ */

    private void parsePackageList(String packageURL, LinkCreator lc) {
        try {
            URL url = new URL(packageURL+"package-list");
            InputStream in = url.openStream();
            // TODO: Encoding der Liste festlegen?
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                extern.put(line, lc);
            }
            reader.close();
            in.close();
        }
        catch(IOException io) {
            io.printStackTrace();
            // TODO: was tun?
        }
    }  // parsePackageList

    private String packageName(Doc d) {
        if(d instanceof PackageDoc) {
            return d.name();
        }
        if(d instanceof ProgramElementDoc) {
            return ((ProgramElementDoc)d).containingPackage().name();
        }
        return "-";
    }
  
    /* ************ Innere Klassen ********** */


    private class HTMLInlineLinkCreator
        implements LinkCreator
    {
        private String url;

        HTMLInlineLinkCreator(String adresse) {
            this.url = adresse;
        }
        public String createLink(String label, Doc target)
        {
            String linkTarget = htmlAdress(this.url, target);
            return "\\href{"+linkTarget+"}{"+label+"}";
        }  // createLink
    }  // class HTMLInlineLinkCreator

    private String packageHtmlAdress(String baseURL, Doc target) {
        return baseURL + "?" + packageName(target).replace(".", "/");
    }

    private String htmlAdress(String baseURL, Doc target) {
        if(target instanceof PackageDoc) {
            return packageHtmlAdress(baseURL, target) +
                "/package-summary.html";
        }
        else if (target instanceof ClassDoc) {
            return packageHtmlAdress(baseURL, target) +  "/" +
                target.name() + ".html";
        } else {
            MemberDoc md = (MemberDoc)target;
            return packageHtmlAdress(baseURL, md) + "/" +
                md.containingClass().name() + ".html#" +
                md.name();  // TODO: ist da die Signatur mit drin?
        }
    }


    /**
     * Link-Creator für Inline-Links zu anderen PDF-Dateien, die mit diesem
     * Doclet erstellt wurden (oder zumindest entsprechende interne Link-Anker
     * haben).
     */
    private class PDFInlineLinkCreator
        implements LinkCreator
    {
        PDFInlineLinkCreator(String pdf) {
            this.pdfURL = pdf;
        }

        private String pdfURL;
        public String createLink(String label, Doc target) {
            String targetName = LaTeXWriter.configuration.toRefLabel(target);
            String url = pdfURL + "#" + targetName;
            return "\\href{"+url+"}{"+label+"}";            
        }


    }  // class PDFInlineLinkCreator




    /**
     * Ein Link-Creator für interne Links.
     */
    private class InternLinkCreator implements LinkCreator {
        /**
         * {@inheritDoc}
         */
        public String createLink(String label, Doc target) {
            String targetName = LaTeXWriter.configuration.toRefLabel(target);
            return "\\hyperlink{" + targetName + "}{" + label + "}";
        }

    }

    /**
     * Ein Link-Creator, der keine Links erstellt.
     */
    private class NoLinkCreator implements LinkCreator {
        public String createLink(String label, Doc d) {
            return label;
        }
    }

}
