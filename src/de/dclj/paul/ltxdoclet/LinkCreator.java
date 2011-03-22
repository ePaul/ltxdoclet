package de.dclj.paul.ltxdoclet;


import com.sun.javadoc.*;

/**
 * Eine Schnittstelle für Objekte, welche Links auf
 * Programmelemente erstellen können.
 *
 * @author <a href="mailto:paulo@heribert.local">Paul Ebermann</a>
 * @version $Id$
 */
public interface LinkCreator {

    public String createLink(String label, Doc target);


}
