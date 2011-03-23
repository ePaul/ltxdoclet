package de.dclj.paul.ltxdoclet;

import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;

import com.sun.source.tree.Tree.Kind;

/**
 * Java-Token, die eine spezielle Behandlung benötigen.
 */
public enum SpecialToken {

    DUMMY("üäöü"),

        // UnaryTree

        POSTFIX_INCREMENT ("++", Kind.POSTFIX_INCREMENT),
        PREFIX_INCREMENT  ("++", Kind.PREFIX_INCREMENT ),
        POSTFIX_DECREMENT ("--", Kind.POSTFIX_DECREMENT),
        PREFIX_DECREMENT  ("--", Kind.PREFIX_DECREMENT ),
        UNARY_PLUS        ("+" , Kind.UNARY_PLUS       ),
        UNARY_MINUS       ("-" , Kind.UNARY_MINUS      ),
        BITWISE_COMPLEMENT( "~", Kind.BITWISE_COMPLEMENT, 2, " \\clap{$\\sim$} "),
        LOGICAL_COMPLEMENT( "!", Kind.LOGICAL_COMPLEMENT),

        // BinaryTree

        MULTIPLY             ("*"  , Kind.MULTIPLY            , 2, " \\clap{$\\cdot$} "), 
        DIVIDE               ("/"  , Kind.DIVIDE              , 2, " \\clap{$/$} "), 
        REMAINDER            ("%"  , Kind.REMAINDER           ,3, " \\% "), 
        PLUS                 ("+"  , Kind.PLUS                ,2, " \\clap{$+$} "), 
        MINUS                ("-"  , Kind.MINUS               ,2, " \\clap{$-$} "), 
        LEFT_SHIFT           ("<<" , Kind.LEFT_SHIFT          ,4, " << "),
        RIGHT_SHIFT          (">>" , Kind.RIGHT_SHIFT         ,4, " >> "),
        UNSIGNED_RIGHT_SHIFT (">>>", Kind.UNSIGNED_RIGHT_SHIFT,5, " >>> "),
        LESS_THAN            ("<"  , Kind.LESS_THAN           , 2, " \\clap{$<$} "), 
        GREATER_THAN         (">"  , Kind.GREATER_THAN        , 2, " \\clap{$>$} "), 
        LESS_THAN_EQUAL      ("<=" , Kind.LESS_THAN_EQUAL     , 2, " \\clap{$\\leq$} "),
        GREATER_THAN_EQUAL   (">=" , Kind.GREATER_THAN_EQUAL  , 2, " \\clap{$\\geq$} "),
        EQUAL_TO             ("==" , Kind.EQUAL_TO            , 3, "\\clapon{$\\equiv$}{   }"),
        NOT_EQUAL_TO         ("!=" , Kind.NOT_EQUAL_TO        , 3, "\\clapon{$\\neq$}{   }"),
        AND                  ("&"  , Kind.AND                 , 2, " \\clap{\\&} "), 
        XOR                  ("^"  , Kind.XOR                 , 2, " \\clap{\\barwedge} "), 
        OR                   ("|"  , Kind.OR                  , 2, " \\clap{|} "), 
        CONDITIONAL_AND      ("&&" , Kind.CONDITIONAL_AND     , 4, " \\&\\& "),
        CONDITIONAL_OR       ("||" , Kind.CONDITIONAL_OR      , 4, " || "),

        // AssignmentTree

        ASSIGNMENT           ("=",  Kind.ASSIGNMENT,   4, "  \\clap{$\\leftarrow$}  "),

        // CompoundAssignmentTree

        MULTIPLY_ASSIGNMENT            ("*="  , Kind.MULTIPLY_ASSIGNMENT            ),
        DIVIDE_ASSIGNMENT              ("/="  , Kind.DIVIDE_ASSIGNMENT              ),
        REMAINDER_ASSIGNMENT           ("%="  , Kind.REMAINDER_ASSIGNMENT           ),
        PLUS_ASSIGNMENT                ("+="  , Kind.PLUS_ASSIGNMENT                ),
        MINUS_ASSIGMENT                ("-="  , Kind.MINUS_ASSIGNMENT               ),
        LEFT_SHIFT_ASSIGNMENT          ("<<=" , Kind.LEFT_SHIFT_ASSIGNMENT          ),
        RIGHT_SHIFT_ASSIGNMENT         (">>=" , Kind.RIGHT_SHIFT_ASSIGNMENT         ),
        UNSIGNED_RIGHT_SHIFT_ASSIGNMENT(">>>=", Kind.UNSIGNED_RIGHT_SHIFT_ASSIGNMENT),
        AND_ASSIGNMENT                 ("&="  , Kind.AND_ASSIGNMENT                 ),
        XOR_ASSIGNMENT                 ("^="  , Kind.XOR_ASSIGNMENT                 ),
        OR_ASSIGNMENT                  ("|="  , Kind.OR_ASSIGNMENT                  ),

        // sonstige Symbole

        LEFT_ANGLE("<", " \\llap{$\\langle$}"),
        RIGHT_ANGLE(">", "\\rlap{$\\rangle$} "),
        RIGHT_ANGLE_SPACE("> ", "\\rlap{$\\rangle$}  "),
        SEMIKOLON(";", 2, "~\\clap{\\textbf{;}} "),
        KOMMA(","),
        QUESTION_MARK("?", 3, " \\textbf{?} "),
        COLON(":", 3, " \\textbf{:} "),
        KOMMA_SPACE(", "),
        //      BIT_UND("&", 3, " \\& "),
        //      BIT_ODER("|", 3, " \\textbar\\ "),
        //      UND("&&", 4, " \\&\\& "),
        //      ODER("||", 4, " \\textbar\\textbar\\ "),

        /**
         * Linke geschweifte Klammer.
         */
        LEFT_BRACE("{", "\\{"),
        LEFT_BRACE_SPACE("{ ", "\\{ "),
        SPACE_LEFT_BRACE(" {", " \\{"),
        /**
         * rechte geschweifte Klammer.
         */
        RIGHT_BRACE("}", "\\}"),
        SPACE_RIGHT_BRACE(" }", " \\}");



    SpecialToken(String javaText, Kind expressionKind, int len, String TeXtext) {
        this.len = len;
        this.TeXtext = TeXtext;
        this.javaText = javaText;
        this.kind = expressionKind;
    }

    SpecialToken(String javaText, Kind kind, String TeXtext) {
        this(javaText, kind, javaText.length(), TeXtext);
    }


    SpecialToken(String javaText, int len, String TeXtext) {
        this(javaText, null, len, TeXtext);
    }

    SpecialToken(String jText, String lText) {
        this(jText, jText.length(), lText);
    }

    SpecialToken(String text, Kind kind) {
        this(text, kind, text.length(), text);
    }

    SpecialToken(String text) {
        this(text, text.length(), text);
    }


    private int len;
    private String TeXtext;
    private String javaText;

    private Kind kind;

    public int getLength() {
        return len;
    }

    /**
     * Eine Abbildung von den Java-Token zu unseren Token-Objekten.
     */
    private static Map<String, SpecialToken> javaMap =
        new HashMap<String, SpecialToken>();

    private static Map<Kind, SpecialToken> opMap =
        new EnumMap<Kind, SpecialToken>(Kind.class);

    static {
        for(SpecialToken tok : values()) {
            javaMap.put(tok.javaText, tok);
            if (tok.kind != null) {
                opMap.put(tok.kind, tok);
            }
        }
    }

    public static SpecialToken getToken(Kind kind) {
        return opMap.get(kind);
    }

    public static SpecialToken getToken(String jText) {
        return javaMap.get(jText);
    }

    public String getReplacement() {
        return TeXtext;
    }

}