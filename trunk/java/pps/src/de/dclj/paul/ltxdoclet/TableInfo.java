package de.dclj.paul.ltxdoclet;

import java.util.*;

/**
 * Unterstützung für die Umwandlung von HTML-Tabellen in LaTeX-Tabellen.
 * (Aus dem <a href="https://texdoclet.dev.java.net/">TexDoclet</a> von Gregg Wonderly,
 *  leicht angepasst an unser LaTeX-Doclet.)
 * <hr/>
 *  This class provides support for converting HTML tables into LaTeX tables.
 *  Some of the things <b>NOT</b> implemented include the following:
 *  <ul>
 *  <li>valign attributes are not procesed, but align= is.
 *  <li>rowspan attributes are not processed, but colspan= is.
 *  <li>the argument to border= in the table tag is not used to control line size
 *  </ul>
 *  <br>
 *  Here is an example table.
 *  <p>
 *  <table border>
 *  <tr><th>Column 1 Heading<th>Column two heading<th>Column three heading
 *  <tr><td>data<td colspan=2>Span two columns
 *  <tr><td><i>more data</i><td align=right>right<td align=left>left
 *  <tr><td colspan=3><table border>
 *  <tr><th colspan=3>A nested table example
 *  <tr><th>Column 1 Heading</th><th>Column two heading</th><th>Column three heading</th>
 *  <tr><td>data</td><td colspan=2>Span two columns</td>
 *  <tr><td><i>more data</i></td><td align=right>right</td><td align=left>left</td>
 *  <tr><td><pre>
 *    1
 *  2
 *  3
 *  4
 *  </pre></td>
 *  <td><pre>
 *    first line
 *  second line
 *  third line
 *  fourth line
 *  </pre></td>
 *  </table>
 *  </table>
 *
 *  @version $Id$
 *  @author <a href="mailto:gregg.wonderly@pobox.com">Gregg Wonderly</a>
 *        (small changes by Paul Ebermann to integrate it in my LaTeX-Doclet.)
 * @see HtmlKonverter
 */
public class TableInfo {
    private int rowcnt = 0;
    private int colcnt = 0; 
    private boolean border = false;
    private boolean colopen = false;
    private Map<String,String> props;
    private int bordwid;
    private boolean parboxed;
    private boolean rowopen;
    static int tblcnt;
    int tblno;
    String tc;


    int hasNumProp( String prop, Map<String,String> p ) {
	String val = p.get(prop) ;
	if( val == null )
	    return -1;
	try { return Integer.parseInt( val ); }
	catch( Exception ex) { return -1; }
    }

    /**
     *  Constructs a new table object and starts processing of the table by
     *  scanning the <code>&lt;table&gt;</code> passed to count columns.
     *
     *  @param p properties found on the <code>&lt;table&gt;</code> tag
     *  @param ret the result buffer that will contain the output
     *  @param table the input string that has the entire table definition in it.
     *  @param off the offset into <code>&lt;table&gt;</code> where scanning should start
     */
    public TableInfo( Map<String,String> p, StringBuilder ret, String table, int off ) {
	props = p;
	tblno = tblcnt++;
	tc = countName(tblno);
	if( p == null )
	    return;
	String val = p.get("border");
	border = false;
	if( val != null ) {
	    border = true;
	    bordwid = 2;
	    if( val.equals("") == false ) {
		try {bordwid = Integer.parseInt( val ); } catch( Exception ex ) {}
		if( bordwid == 0 )
		    border = false;
	    }
	}
	ret.append("\n% Table #"+tblno+"\n");
	byte[]b = table.getBytes();
	int col = 0;
	int row = 0;
	for( int i = off; i < b.length; ++i ) {
	    if( b[i] == '<' ) {
		if( table.substring( i, i+7 ) .equalsIgnoreCase("</table") ){
		    break;
		} else if( table.substring( i, i+4 ) .equalsIgnoreCase("</tr") ){
		    break;
		} else if( table.substring( i, i+3 ) .equalsIgnoreCase("<tr") ){
		    if( row++ > 0 )
			break;
		} else if( table.substring( i, i+3 ) .equalsIgnoreCase("<td") ){
		    Map<String,String> pp = new HashMap<String, String>();
		    int idx = HtmlKonverter.getTagAttrs( table, pp, i+3 );
		    int v = hasNumProp( "colspan",pp );
		    if( v > 0 )
			col += v;
		    else
			col++;
		    i = idx-1;
		} else if( table.substring( i, i+3 ) .equalsIgnoreCase("<th") ){
		    Map<String,String> pp = new HashMap<String, String>();
		    int idx = HtmlKonverter.getTagAttrs( table, pp, i+3 );
		    int v = hasNumProp( "colspan", pp );
		    if( v > 0 )
			col += v;
		    else
			col++;
		    i = idx-1;
		}
	    }
	}
	if( col == 0 )
	    col = 1;
	for( int i = 0; i < col; ++i ) {
	    String cc = countName(i);
	    ret.append("\\newlength{\\tbl"+tc+"c"+cc+"w}\n");
	    ret.append("\\setlength{\\tbl"+tc+"c"+cc+"w}{"+(1.0/col)+"\\hsize}\n");
	}
	ret.append("\\begin{tabular}{");
	if( border )
	    ret.append("|");
	for( int i = 0; i < col; ++i ) {
	    String cc = countName(i);
	    ret.append("p{\\tbl"+tc+"c"+cc+"w}");
	    if( border )
		ret.append("|");
	}
	ret.append("}\n");
    }

    /**
     * Wandelt eine Nummer in die dazugehörige
     * Zeichenkette im 26-er-System (a-z) um.
     */
    public String countName(int i) {
	return "" +
	    (char)('a' + ((i/26)/26))+
	    (char)('a' + ((i/26)%26))+
	    (char)('a' + (i%26));
    }
    
    /**
     * Gibt zu einer Spaltennummer (in der aktuellen Tabelle)
     * die dazugehörige Breiten-Kontrollsequenz zurück.
     */
    public String colSize(int colNum) {
	return "\\tbl" + tc + "c" +
	    countName(colNum) + "w";
    }

    /**
     *  Starts a new column, possibly closing the current column if needed
     *
     *  @param ret the output buffer to put LaTeX into
     *  @param p the properties from the <code>&lt;td&gt;</code> tag
     */
    public void startCol( StringBuilder ret, Map<String,String> p ) {
	endCol(ret);
	int span = hasNumProp("colspan", p);
	if( colcnt > 0 ) {
	    ret.append(" & " );
	}
	String align = p.get("align");
	if( align != null && span < 0 )
	    span = 1;
	if( span > 0 ) {
	    StringBuilder spanSize =
		new StringBuilder();
	    spanSize.append("\\dimexpr");
	    spanSize.append(colSize(colcnt));
	    for(int i = 1; i < span; i++) {
		spanSize.append(" + ");
		spanSize.append(colSize(colcnt+i));
	    }
	    spanSize.append(" - 2ex");
	    spanSize.append("\\relax");

	    ret.append("\\multicolumn{"+span+"}{" );
	    if( border && colcnt == 0)
		ret.append("|");
	    String cc = countName(colcnt);
	    if( align != null ) {
		String h = align.substring(0,1);
		if( "rR".indexOf(h) >= 0 )
		    ret.append("r");
		else if( "lL".indexOf(h) >= 0 )
		    ret.append("p{"+spanSize+"}");
		else if( "cC".indexOf(h) >= 0 )
		    ret.append("p{"+spanSize+"}");
	    } else {
		ret.append("p{"+spanSize+"}");
	    }
	    if( border )
		ret.append("|");
	    ret.append("}");
	}
	String wid=p.get("texwidth");
	ret.append("{");
	if( wid != null ) {
	    ret.append("\\parbox{"+wid+"}{\\vskip 1ex ");
	    parboxed = true;
	}
	colcnt++;
	colopen = true;
    }
		
		
    /**
     *  Starts a new Heading column, possibly closing the current column
     *  if needed.  A Heading column has a Bold Face font directive around
     *  it.
     *
     *  @param ret the output buffer to put LaTeX into
     *  @param p the properties from the <code>&lt;th&gt;</code> tag
     */
    public void startHeadCol( StringBuilder ret, Map<String,String> p ) {
	startCol( ret, p );
	ret.append("\\bf ");
    }
	
		
    /**
     *  Ends the current column.
     *
     *  @param ret the output buffer to put LaTeX into
     */
    public void endCol( StringBuilder ret ) {
	if( colopen ) {
	    colopen = false;
	    if(parboxed)
		ret.append("\\vskip 1ex}");
	    parboxed = false;
	    ret.append("}");
	}
    }
    
		
    /**
     *  Starts a new row, possibly closing the current row if needed
     *
     *  @param ret the output buffer to put LaTeX into
     *  @param p the properties from the <code>&lt;tr&gt;</code> tag
     */
    public void startRow( StringBuilder ret, Map<String,String> p ) {
	endRow(ret);
	if( rowcnt == 0 ) {
	    if( border )
		ret.append(" \\hline " );
	}
	colcnt = 0;
	++rowcnt;
	rowopen = true;
    }
	
		
    /**
     *  Ends the current row.
     *
     *  @param ret the output buffer to put LaTeX into
     */
    public void endRow( StringBuilder ret ) {
	if( rowopen ) {
	    endCol(ret);
	    ret.append( " \\tabularnewline" );
	    if( border )
		ret.append( " \\hline" );
	    rowopen = false;
	    ret.append("\n");
	}
    }
		
		
    /**
     *  Ends the table, closing the last row as needed
     *
     *  @param ret the output buffer to put LaTeX into
     */
    public void endTable( StringBuilder ret ) {
	endRow( ret );
	ret.append("\\end{tabular}\n");
    }
}
