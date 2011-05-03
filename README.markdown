ltxdoclet - a LaTeX generating doclet
=====================================

Similarly to [Texdoclet][1], this is a [Javadoc][2] doclet which generates
LaTeX code instead of HTML (like the standard doclet).

This LaTeX code can then converted to PDF with the XeLaTeX engine.
(I think LuaLaTeX should work, too.)

As a main difference to Texdoclet, this doclet can include pretty-printed
source code in the documentation - in effect creating a source listing with
nicely formatted javadoc comments.

More information and some documentation is available at the [homepage][3].


The source code (both Java and LaTeX) itself is documented in German.



[1] http://java.net/projects/texdoclet/
[2] http://download.oracle.com/javase/6/docs/technotes/guides/javadoc/
[3] http://epaul.github.com/ltxdoclet/