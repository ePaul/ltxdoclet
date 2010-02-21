package de.dclj.paul.ltxdoclet;

import com.sun.javadoc.*;
import java.io.*;
import java.util.*;
import java.awt.Color;

/**
 * Ein Konverter von HTML zu LaTeX.
 * Hergestellt auf Basis von Teilen
 * von TeXDoclet von Gregg Wonderly, im Original zu finden unter
 *  <a href="https://texdoclet.dev.java.net/">texdoclet.dec.java.net</a>.
 * <hr/>
 *  <p>
 *  Supported HTML tags within comments include the following
 *  <dl>
 *  <dt>&lt;dl&gt;
 *  <dd>with the associated &lt;dt&gt;&lt;dd&gt;&lt;/dl&gt; tags
 *  <dt>&lt;p&gt;
 *  <dd>but not align=center...yet
 *  <dt>&lt;br&gt;
 *  <dd>but not clear=xxx
 *  <dt>&lt;table&gt;
 *  <dd>including all the associcated
 *      &lt;td&gt;&lt;th&gt;&lt;tr&gt;&lt;/td&gt;&lt;/th&gt;&lt;/tr&gt;
 *  <dt>&lt;ol&gt;
 *  <dd>ordered lists
 *  <dt>&lt;ul&gt;
 *  <dd>unordered lists
 *  <dt>&lt;font&gt;
 *  <dd>font coloring
 *  <dt>&lt;pre&gt;
 *  <dd>preformatted text
 *  <dt>&lt;code&gt;
 *  <dd>fixed point fonts
 *  <dt>&lt;i&gt;
 *  <dd>italized fonts
 *  <dt>&lt;b&gt;
 *  <dd>bold fonts
 *      </dl>
 *
 *  @version $Id$
 *  @author <a href="mailto:gregg.wonderly@pobox.com">Gregg Wonderly</a> (TeXDoclet),
 *      Paul Ebermann (Umwandelung zu HtmlKonverter, einige Anpassungen)
 *  @see TableInfo
 */
public class HtmlKonverter {
    /**
     * Die zur Zeit offenen und von inneren Tabellen verdeckten Tabellen.
     */
    Deque<TableInfo> tblstk;


    /**
     * Die aktuell geöffnete Tabelle.
     */
    TableInfo tblinfo;

    
    /**
     * Wie viele verschachtelte &lt;pre>/&lt;code>-Umgebungen sind gerade offen?
     */
    int verbat = 0;

    /**
     * Die im aktuellen Dokument definierten Farben.
     * Schlüssel sind die im HTML definierten Farben (red, blue, ...) sowie
     * hexadezimal kodierte Farben (aab732), Werte die jeweils dazugehörigen
     * Farbnamen im LaTeX-Dokument.
     */
    static Map<String, String> colors;
    
    /**
     * Die Nummer der nächsten zu definierenden Farbe.
     */
    static int colorIdx = 0;
        
    /*
     *  Testing entry point for testing tables
     public static void main( String args[] ) {
     init();
     os.println( "\\documentstyle{book}" );
     os.println( "\\begin{document}");
     os.println( fixText(
     "<table border> "+
     "<tr><th>Heading 1<th>Heading 2<th>Heading 3"+
     "<tr><td>Column 1<td colspan=2 align=right>2 columns here"+
     "<tr><td colspan=2>two here as well<td>just 1"+
     "<tr><td align=left>1<td>2<td align=right>3"+
     "</table>" ) );
     os.println( "\\end{document}");
     }
    */

    /**
     * Konstruktor.
     */
    public HtmlKonverter() {
        tblinfo = new TableInfo( null, null, "", 0 );
        tblstk = new ArrayDeque<TableInfo>();
    }

        
    /**
     * Initialisiert die Farb-Tabelle.
     * Dies wird automatisch vor der ersten Verwendung der Klasse aufgerufen,
     * ist also nur notwendig, falls innerhalb einer Ausführung des Programmes
     * (ohne Neuladen dieser Klasse) mehrere LaTeX-Dokumente erstellt werden
     * sollen.
     */
    static void init() {
	colors = new HashMap<String,String>(13);
	/*
	 * Die Standard-Farben von HTML, die netterweise
	 * auch in LaTeX (color) so heißen. 
	 */
	for(String colName : new String[]{"red", "green", "blue",
					  "white", "yellow", "black",
					  "cyan", "magenta"}) {
	    colors.put(colName, colName);
	}
    }

    static {
        init();
    }
        
        
    static int labno = 0;
    static Hashtable<String,String> refs = new Hashtable<String,String>();

    static String refName( String key ) {
        String lab;
        if( (lab = refs.get(key)) == null ) {
            lab = "l"+labno++;
            refs.put( key, lab );
        }
        return lab;
    }                   
        
        
    void stackTable( Map<String,String> p, StringBuilder ret, String txt, int off ) {
        tblstk.push( tblinfo );
        tblinfo = new TableInfo( p, ret, txt, off );
    }
        

    void processBlock( String block, StringBuilder ret ) {
        if( block.substring(0,6).equalsIgnoreCase("@link ") ) {
            block = block.substring(6).trim();

            StringTokenizer st = new StringTokenizer(block," \n\r\t");
            String key = st.nextToken();
            String text = key;
            if( st.hasMoreTokens() )
                text = st.nextToken("\001").trim();
            ret.append( toTeX(text.trim())+
                        "\\refdefined{"+refName(makeRefKey(key))+"} ");
        } else {
            ret.append("{"+block+"}");
        }
    }

    static String makeRefKey( String key ) {
        return key;
    }

    static String block = "";
    static String refurl = "";
    static String refimg = "";
    static boolean collectBlock;
    static int chapt = 0;
    static int textdepth = 0;


    String toTeX( String str ) {
        StringBuilder ret = new StringBuilder(str.length());
        long start  = System.currentTimeMillis();
        boolean svcoll = false;
        String svblock = null;
        if( textdepth > 0 ) {
            svcoll = collectBlock;
            svblock = block;
        }
        ++textdepth;
        for( int i = 0 ; i < str.length(); ++i ) { /* { */
            int c = str.charAt(i);
            if( collectBlock == true && c != '}') {
                block += str.charAt(i);
                continue;
            }
            switch(c) {
            case ' ':
                if( verbat > 0 ) {
                    ret.append("\\phantom{ }");
                } else {
                    ret.append(' ');
                }
                break;
            case '"':
                ret.append("\\textquotedbl ");
                break;
            case '_':
            case '%':
            case '$':
            case '#':
                ret.append( '\\' );
                ret.append( (char)c );
                break;
            case '^': /* { */
                ret.append("$\\wedge$");
                break;
            case '}':
                if( collectBlock == false ) {
                    ret.append("$\\}$");
                    break;
                }
                collectBlock = false;
                processBlock( block, ret );
                break;
            case '{':
                if( str.length() > i+5 && str.substring(i,i+6).equalsIgnoreCase("{@link") ) {
                    block = "@link";
                    collectBlock = true;
                    i += 5;
                } else {
                    ret.append("$\\{$");
                }
                break;
            case '<':
                if( str.length() > i+4 && str.substring(i,i+5).equalsIgnoreCase("<pre>") ){

                    ret.append( "{\\tt\n");
                    verbat++;
                    i+=4;
                    if(str.charAt(i+1) == '\r') {
                        i++;
                    }
                    if(str.charAt(i+1) == '\n') {
                        i++;
                    }
                } else if( str.length() > i+5 && str.substring(i,i+6).equalsIgnoreCase("</pre>") ){

                    verbat--;
                    ret.append( "}\n" );
                    i+=5;
                } else if( str.length() > i+3 && str.substring(i,i+4).equalsIgnoreCase("<h1>") ){
                    ret.append("\\headref{1}{\\Huge}{");
                    i+=3;
                } else if( str.length() > i+4 && str.substring(i,i+5).equalsIgnoreCase("</h1>") ){
                    ret.append("}\\bl ");
                    i+=4;
                } else if( str.length() > i+3 && str.substring(i,i+4).equalsIgnoreCase("<h2>") ){
                    ret.append("\\headref{2}{\\huge}{");
                    i+=3;
                } else if( str.length() > i+4 && str.substring(i,i+5).equalsIgnoreCase("</h2>") ){
                    ret.append("}\\bl ");
                    i+=4;
                } else if( str.length() > i+3 && str.substring(i,i+4).equalsIgnoreCase("<h3>") ){
                    ret.append("\\headref{3}{\\Large}{");
                    i+=3;
                } else if( str.length() > i+4 && str.substring(i,i+5).equalsIgnoreCase("</h3>") ){
                    ret.append("}\\bl ");
                    i+=4;
                } else if( str.length() > i+3 && str.substring(i,i+4).equalsIgnoreCase("<h4>") ){
                    ret.append("\\headref{4}{\\normalsize}{");
                    i+=3;
                } else if( str.length() > i+4 && str.substring(i,i+5).equalsIgnoreCase("</h4>") ){
                    ret.append("}\\bl ");
                    i+=4;
                } else if( str.length() > i+3 && str.substring(i,i+4).equalsIgnoreCase("<h5>") ){
                    ret.append("\\headref{5}{\\small}{");
                    i+=3;
                } else if( str.length() > i+4 && str.substring(i,i+5).equalsIgnoreCase("</h5>") ){
                    ret.append("}\\bl ");
                    i+=4;
                } else if( str.length() > i+3 && str.substring(i,i+4).equalsIgnoreCase("<h6>") ){
                    ret.append("\\headref{6}{\\footnotesize}{");
                    i+=3;
                } else if( str.length() > i+4 && str.substring(i,i+5).equalsIgnoreCase("</h6>") ){
                    ret.append("}\\bl ");
                    i+=4;
                } else if( str.length() > i+3 && str.substring(i,i+4).equalsIgnoreCase("<h7>") ){
                    ret.append("\\headref{7}{\\scriptsize}{");
                    i+=3;
                } else if( str.length() > i+4 && str.substring(i,i+5).equalsIgnoreCase("</h7>") ){
                    ret.append("}\\bl ");
                    i+=4;
                } else if( str.length() > i+3 && str.substring(i,i+4).equalsIgnoreCase("<h8>") ){
                    ret.append("\\headref{8}{\\tiny}{");
                    i+=3;
                } else if( str.length() > i+4 && str.substring(i,i+5).equalsIgnoreCase("</h8>") ){
                    ret.append("}\\bl ");
                    i+=4;
                } else if( str.length() > i+5 && str.substring(i,i+6).equalsIgnoreCase("<html>") ){
                    i+=5;
                } else if( str.length() > i+6 && str.substring(i,i+7).equalsIgnoreCase("</html>") ){
                    if( chapt > 0 ) {
                        ret.append("}");
                        --chapt;
                    }
                    i+=6;
                } else if( str.length() > i+5 && str.substring(i,i+6).equalsIgnoreCase("<head>") ){
                    i+=5;
                } else if( str.length() > i+6 && str.substring(i,i+7).equalsIgnoreCase("</head>") ){
                    i+=6;
                } else if( str.length() > i+7 && str.substring(i,i+8).equalsIgnoreCase("<center>") ){
                    ret.append("\\makebox[\\hsize]{ ");
                    i+=7;
                } else if( str.length() > i+8 && str.substring(i,i+9).equalsIgnoreCase("</center>") ){
                    ret.append("}");
                    i+=8;
                } else if( str.length() > i+4 && str.substring(i,i+5).equalsIgnoreCase("<meta") ){
                    Map<String,String> p = new HashMap<String, String>();
                    int idx = getTagAttrs( str, p, i+3 );
                    i = idx;
                } else if( str.length() > i+6 && str.substring(i,i+7).equalsIgnoreCase("<title>") ){
                    i+=6;
                    ret.append("\\chapter{");
                } else if( str.length() > i+7 && str.substring(i,i+8).equalsIgnoreCase("</title>") ){
                    ret.append("}{");
                    i+=7;
                } else if( str.length() > i+4 && str.substring(i,i+5).equalsIgnoreCase("<form") ){
                    Map<String,String> p = new HashMap<String, String>();
                    int idx = getTagAttrs( str, p, i+3 );
                    i = idx;
                } else if( str.length() > i+6 && str.substring(i,i+7).equalsIgnoreCase("</form>") ){
                    i+=6;
                } else if( str.length() > i+5 && str.substring(i,i+6).equalsIgnoreCase("<input") ){
                    Map<String,String> p = new HashMap<String, String>();
                    int idx = getTagAttrs( str, p, i+3 );
                    i = idx;
                } else if( str.length() > i+7 && str.substring(i,i+8).equalsIgnoreCase("</input>") ){
                    i+=7;
                } else if( str.length() > i+4 && str.substring(i,i+5).equalsIgnoreCase("<body") ){
                    Map<String,String> p = new HashMap<String, String>();
                    int idx = getTagAttrs( str, p, i+3 );
                    i = idx;
                } else if( str.length() > i+6 && str.substring(i,i+7).equalsIgnoreCase("</body>") ){
                    i+=6;
                } else if( str.length() > i+5 && str.substring(i,i+6).equalsIgnoreCase("<code>") ){
		    verbat++;
                    ret.append( "{\\ttfamily " );
                    i+=5;
                } else if( str.length() > i+6 && str.substring(i,i+7).equalsIgnoreCase("</code>") ){
		    verbat--;
                    ret.append( "}" );
                    i+=6;
                } else if( str.length() > i+4 && str.substring(i,i+5).equalsIgnoreCase("</br>") ){
                    i+=4;
                } else if( str.length() > i+3 && str.substring(i,i+4).equalsIgnoreCase("<br>") ){
                    ret.append( "\\mbox{}\\newline\n" );
                    i+=3;
                } else if( str.length() > i+3 && str.substring(i,i+4).equalsIgnoreCase("</p>") ){
		    ret.append("\\par ");
                    i+=3;
                } else if( str.length() > i+2 && str.substring(i,i+3).equalsIgnoreCase("<p>") ){
                    ret.append( "\\par " );
                    i+=2;
                } else if( str.length() > i+2 && str.substring(i,i+3).equalsIgnoreCase("<hr") ){
                    Map<String,String> p = new HashMap<String, String>();
                    int idx = getTagAttrs( str, p, i+3 );
                    String sz = p.get("size");
                    int size = 1;
                    if( sz != null )
                        size = Integer.parseInt(sz);
                    ret.append( "\\newline\\rule[2mm]{\\hsize}{"+(1*size*.5)+"mm}\\newline\n" );
                    i = idx;
                } else if( str.length() > i+2 && str.substring(i,i+3).equalsIgnoreCase("<b>") ){
                    ret.append( "{\\bf " );
                    i+=2;
                } else if( str.length() > i+3 && str.substring(i,i+4).equalsIgnoreCase("</b>") ){
                    ret.append( "}" );
                    i+=3;
                } else if( str.length() > i+6 && str.substring(i,i+7).equalsIgnoreCase("<strong>") ){
                    ret.append( "{\\bf " );
                    i+=6;
                } else if( str.length() > i+7 && str.substring(i,i+8).equalsIgnoreCase("</strong>") ){
                    ret.append( "}" );
                    i+=7;
                } else if( str.length() > i+5 && str.substring(i,i+6).equalsIgnoreCase("</img>") ){
                    i+=5;
                } else if( str.length() > i+4 && str.substring(i,i+4).equalsIgnoreCase("<img") ){
		    // TODO: includegraphics
                    Map<String,String> p = new HashMap<String, String>();
                    int idx = getTagAttrs( str, p, i+4 );
                    refimg = p.get("src");
                    ret.append( "(see image at "+toTeX(refimg)+")" );
                    i = idx;
                } else if( str.length() > i+3 && str.substring(i,i+4).equalsIgnoreCase("</a>") ){
                    if( refurl != null ) {
                        ret.append( "} " );
                        if( refurl.charAt(0) == '#' ) {
			    // TODO: interner Link
                            ret.append("\\refdefined{"+refName(makeRefKey(refurl.substring(1)))+"}" );
			}
                        else {
			    // TODO: externer Link
                            ret.append("(at "+toTeX(refurl)+")" );
			}
                    }
                    i+=3;
                } else if( str.length() > i+2 && str.substring(i,i+2).equalsIgnoreCase("<a") ){
                    Map<String,String> p = new HashMap<String, String>();
                    int idx = getTagAttrs( str, p, i+3 );
                    refurl = p.get("href");
                    String refname = p.get("href");
                    i = idx;
                    if( refurl != null )
                        ret.append( "{\\bf " );
                    else if( refname != null )
                        ret.append("\\label{"+refName(makeRefKey(refname))+"}" );
                } else if( str.length() > i+3 && str.substring(i,i+3).equalsIgnoreCase("<ol") ){
                    Map<String,String> p = new HashMap<String, String>();
                    int idx = getTagAttrs( str, p, i+3 );
                    i = idx;
                    ret.append( "\\begin{enumerate}\n" );
                } else if( str.length() > i+2 && str.substring(i,i+3).equalsIgnoreCase("<dl") ){
                    Map<String,String> p = new HashMap<String, String>();
                    int idx = getTagAttrs( str, p, i+3 );
                    i = idx;
                    ret.append( "\\begin{itemize}\n" );
                } else if( str.length() > i+3 && str.substring(i,i+4).equalsIgnoreCase("<li>") ){
		    ret.append( "\n");
                    ret.append( "\\item " );
                    i+=3;
                } else if( str.length() > i+3 && str.substring(i,i+4).equalsIgnoreCase("<dt>") ){
		    ret.append( "\n");
                    ret.append( "\\item[{" );
                    i+=3;
                } else if( str.length() > i+3 && str.substring(i,i+4).equalsIgnoreCase("<dd>") ){
                    ret.append( "}]" );
                    i+=3;
                } else if ( str.length() > i+4 && str.substring(i,i+5).equalsIgnoreCase("</dd>")) {
		    i += 4;
                } else if ( str.length() > i+4 && str.substring(i,i+5).equalsIgnoreCase("</dt>")) {
		    i += 4;
		}else if( str.length() > i+4 && str.substring(i,i+5).equalsIgnoreCase("</dl>") ){
                    ret.append( "\n\\end{itemize}" );
                    i+=4;
                } else if( str.length() > i+4 && str.substring(i,i+5).equalsIgnoreCase("</ol>") ){
                    ret.append( "\n\\end{enumerate}" );
                    i+=4;
                } else if( str.length() > i+3 && str.substring(i,i+3).equalsIgnoreCase("<ul") ){
                    Map<String,String> p = new HashMap<String, String>();
                    int idx = getTagAttrs( str, p, i+3 );
                    i = idx;
                    ret.append( "\\begin{itemize}" );
                } else if( str.length() > i+4 && str.substring(i,i+5).equalsIgnoreCase("</ul>") ){
                    ret.append( "\n\\end{itemize}\n" );
                    i+=4;
                } else if( str.length() > i+2 && str.substring(i,i+3).equalsIgnoreCase("<i>") ){
                    ret.append( "{\\it " );
                    i+=2;
                } else if( str.length() > i+3 && str.substring(i,i+4).equalsIgnoreCase("</i>") ){
                    ret.append( "}" );
                    i+=3;
                } else if( str.length() > i+7 && str.substring(i,i+8).equalsIgnoreCase("</table>") ){
                    tblinfo.endTable(ret);
                    tblinfo = tblstk.pop();
                    i+=7;
                } else if( str.length() > i+4 && str.substring(i,i+5).equalsIgnoreCase("</th>") ){
                    tblinfo.endCol(ret);
                    i+=4;
                } else if( str.length() > i+4 && str.substring(i,i+5).equalsIgnoreCase("</td>") ){
                    tblinfo.endCol(ret);
                    i+=4;
                } else if( str.length() > i+4 && str.substring(i,i+5).equalsIgnoreCase("</tr>") ){
                    tblinfo.endRow(ret);
                    i+=4;
                } else if( str.length() > i+5 && str.substring(i,i+6).equalsIgnoreCase("<table") ){
                    Map<String,String> p = new HashMap<String, String>();
                    int idx = getTagAttrs( str, p, i+6 );
                    i = idx;
                    stackTable( p, ret, str, i );
                } else if( str.length() > i+2 && str.substring(i,i+3).equalsIgnoreCase("<tr") ){
                    Map<String,String> p = new HashMap<String, String>();
                    int idx = getTagAttrs( str, p, i+3 );
                    i = idx;
                    tblinfo.startRow(ret,p);
                } else if( str.length() > i+2 && str.substring(i,i+3).equalsIgnoreCase("<td") ){
                    Map<String,String> p = new HashMap<String, String>();
                    int idx = getTagAttrs( str, p, i+3 );
                    i = idx;
                    tblinfo.startCol(ret, p);
                } else if( str.length() > i+2 && str.substring(i,i+3).equalsIgnoreCase("<th") ){
                    Map<String,String> p = new HashMap<String, String>();
                    int idx = getTagAttrs( str, p, i+3 );
                    i = idx;
                    tblinfo.startHeadCol(ret,p);
                } else if( str.length() > i+4 && str.substring(i,i+5).equalsIgnoreCase("<font") ){
                    Map<String,String> p = new HashMap<String, String>();
                    int idx = getTagAttrs( str, p, i+5 );
                    i = idx;
                    String sz = p.get("size");
                    String col = p.get("color");
                    ret.append( "{" );
                    if( col != null ) {
			String colName = colors.get(col);
			if(colName == null) {
			    colName = "color"+colorIdx;
			    colorIdx++;

			    Color cc = new Color( (int)Long.parseLong( col, 16 ) );
			    ret.append("\\definecolor{"+colName+"}[rgb]{"+(cc.getRed()/255.0)+","+
				       (cc.getBlue()/255.0)+","+(cc.getGreen()/255.0)+"}");
			    colors.put(col, colName);
			}
			ret.append("\\color{"+colName+"}" );
		    }

                } else if( str.length() > i+6 && str.substring(i,i+7).equalsIgnoreCase("</font>") ){
                    ret.append( "}" );
                    i+=6;
                } else {
                    ret.append("\\textless ");
                }
                break;
            case '\r':
            case '\n':
                if( tblstk.size() > 0 ) {
                    // Swallow new lines while tables are in progress,
                    // <tr> controls new line emission.
                    if( verbat > 0 ) {
                        ret.append( "\\newline\n" );
                    } else
                        ret.append(" ");
                } else {
                    if( (i+1) < str.length() && str.charAt(i+1) == 10 ) {
                        ret.append("\\bl ");
                        ++i;
                    } else {
                        if( verbat > 0 )
                            ret.append( "\\mbox{}\\newline\n" );
                        else
                            ret.append( (char)c );
                    }
                }
                break;
            case '/':
                ret.append("$/$");
                break;
            case '&':
                if( str.length() > i+4 && str.substring(i,i+2).equals("&#") ) {
                    String it = str.substring(i+2);
                    int stp = it.indexOf(';');
                    if( stp > 0 ) {
                        String v = it.substring(0,stp);
                        int ch = -1;
                        try {
                            ch = Integer.parseInt( v );
                        } catch( NumberFormatException ex ) {
                            ch = -1;
                        }
                        if( ch >= 0 && ch < 128 ) {
                            ret.append("\\verb"+((char)(ch+1))+((char)ch)+((char)(ch+1)));
                        } else {
                            ret.append( "\\&\\#"+v );
                        }
                        i+=v.length()+2;
                    } else {
                        ret.append( "\\&\\#" );
                        i++;
                    }
                } else if( str.length() > i+4 && str.substring(i,i+5).equalsIgnoreCase("&amp;") ) {
                    ret.append("\\&");
                    i+=4;
                } else if( str.length() > i+5 && str.substring(i,i+6).equalsIgnoreCase("&nbsp;") ) {
                    ret.append("\\phantom{ }");
                    i+=5;
                } else if( str.length() > i+3 && str.substring(i,i+4).equalsIgnoreCase("&lt;") ) {
                    ret.append("\\textless ");
                    i += 3;
                } else if( str.length() > i+3 && str.substring(i,i+4).equalsIgnoreCase("&gt;") ) {
                    ret.append("\\textgreater ");
                    i += 3;
                } else
                    ret.append("\\&");
                break;
            case '>':
                ret.append("\\textgreater ");
                break;
            case '\\':
                ret.append("\\bslash ");
                break;
            default:
                ret.append( (char)c );
                break;
            }
        }
        if( textdepth > 0 ) {
            collectBlock = svcoll;
            block = svblock;
        }
        --textdepth;
        long to = System.currentTimeMillis()-start;
        if( to > 1000 ) {
            System.out.print("(text @"+to+" msecs)" );
            System.out.flush();
        }
        return ret.toString();
    }
        
    /**
     *  This method parses HTML tags to extract the tag attributes and place
     *  them into a Map<String,String> object.
     *
     *  @param str the string that is the whole HTML start tag (at least)
     *  @param i the offset in the string where the tag starts
     *  @return the offset in the String after the end of the tag (not element).
     */
    static int getTagAttrs( String str, Map<String,String> p, int i ) {
        //      static Map<String,String> getTagAttrs( String str, int i ) {
        byte b[] = str.getBytes();
        String name = "";
        String value = "";
        int state = 0;
        while( i < b.length ) {
            switch((char)b[i]) {
            case ' ':
                if( state == 2 ) {
                    p.put( name.toLowerCase(), value );
                    state = 1;
                    name = "";
                    value = "";
                } else if( state == 3 ) {
                    value += " ";
                }
                break;
            case '=':
                if( state == 1 ) {
                    state = 2;
                    value = "";
                } else if( state > 1 ) {
                    value += '=';
                }
                break;
            case '"':
                if( state == 2 ) {
                    state = 3;
                } else if( state == 3 ) {
                    state = 1;
                    p.put( name.toLowerCase(), value );
                    name = "";
                    value = "";
                }
                break;          
            case '>':
                if( state == 1 ) {
                    p.put(name.toLowerCase(),"" );
                } else if( state == 2 ) {
                    p.put(name.toLowerCase(),value);
                }
                return i;
            default:
                if( state == 0 )
                    state = 1;
                if( state == 1 ) {
                    name = name + (char)b[i];
                } else {
                    value = value + (char)b[i];
                }
            }
            ++i;
        }
        return i;
    }
}
