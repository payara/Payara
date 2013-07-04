/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package com.sun.enterprise.util;

/* WBN Valentine's Day, 2000 -- place for handy String utils.
 */
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.sql.SQLException;
import java.text.StringCharacterIterator;

public class StringUtils {
    public static final String NEWLINE = System.getProperty("line.separator");
    public static final String EOL = NEWLINE;

    private StringUtils() {
    }

    ////////////////////////////////////////////////////////////////////////////
    /**
     * return the length of the String - or 0 if it's null
     */
    public static int safeLength(String s) {
        if (s == null) {
            return 0;
        }

        return s.length();
    }

    ////////////////////////////////////////////////////////////////////////////
    public static boolean ok(String s) {
        return s != null && s.length() > 0;
    }

    ////////////////////////////////////////////////////////////////////////////
    public static String formatSQLException(SQLException ex) {
        assert ex != null;

        StringBuilder sb = new StringBuilder("SQLException:\n");

        do {
            sb.append("SQLState: ").append(ex.getSQLState()).append('\n');
            sb.append("Message:  ").append(ex.getMessage()).append('\n');
            sb.append("Vendor:   ").append(ex.getErrorCode()).append('\n');
            sb.append('\n');
        }
        while ((ex = ex.getNextException()) != null);

        return sb.toString();
    }

    ////////////////////////////////////////////////////////////////////////////
    public static int maxWidth(Vector v) {
        // find longest String in a vector of Strings...
        int max = 0;

        if (v == null || v.size() <= 0 || !(v.elementAt(0) instanceof String)) {
            return 0;
        }

        for (int i = v.size() - 1; i >= 0; i--) {
            int len = ((String) v.elementAt(i)).length();

            if (len > max) {
                max = len;
            }
        }

        return max;
    }

    ////////////////////////////////////////////////////////////////////////////
    public static boolean isHex(String s) {
        // is this the String representation of a valid hex number?
        // "5", "d", "D", "F454ecbb" all return true...
        // p.s. there MUST be a better and faster way of doing this...

        final int slen = s.length();

        for (int i = 0; i < slen; i++) {
            if (isHex(s.charAt(i)) == false) {
                return false;
            }
        }
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////
    public static boolean isHex(char c) {
        // is this the char a valid hex digit?

        String hex = "0123456789abcdefABCDEF";
        int hexlen = hex.length();

        for (int i = 0; i < hexlen; i++) {
            if (hex.charAt(i) == c) {
                return true;
            }
        }

        return false;
    }

    ////////////////////////////////////////////////////////////////////////////
    public static String getPenultimateDirName(String s) {
        // e.g.  input: "a/b/c/d/foobar.txt"   output: "d"

        if (s == null || s.length() <= 0) {
            return s;
        }

        // must be a plain file name -- return empty string...
        if ((s.indexOf('/') < 0) && (s.indexOf('\\') < 0)) {
            return "";
        }

        s = s.replace('\\', '/');	// make life easier for the next steps...

        int index = s.lastIndexOf('/');

        if (index < 0) {
            return "";	// can't happen!!!
        }
        s = s.substring(0, index);	// this will truncate the last '/'

        index = s.lastIndexOf('/');

        if (index >= 0) {
            s = s.substring(index + 1);
        }

        return s;
    }

    ////////////////////////////////////////////////////////////////////////////
    public static String toShortClassName(String className) {
        int index = className.lastIndexOf('.');

        if (index >= 0 && index < className.length() - 1) {
            return className.substring(index + 1);
        }

        return className;
    }

    ////////////////////////////////////////////////////////////////////////////
    public static String padRight(String s, int len) {
        if (s == null || s.length() >= len) {
            return s;
        }

        StringBuilder sb = new StringBuilder(s);

        for (int i = len - s.length(); i > 0; --i) {
            sb.append(' ');
        }

        return sb.toString();
    }

    ////////////////////////////////////////////////////////////////////////////
    public static String padLeft(String s, int len) {
        if (s == null || s.length() >= len) {
            return s;
        }

        StringBuilder sb = new StringBuilder();

        for (int i = len - s.length(); i > 0; --i) {
            sb.append(' ');
        }

        return sb.append(s).toString();
    }

    ////////////////////////////////////////////////////////////////////////////
    public static String[] toLines(String s) {
        if (s == null) {
            return new String[0];
        }

        Vector v = new Vector();

        int start = 0;
        int end = 0;

        for (end = s.indexOf('\n', start); end >= 0 && start < s.length(); end = s.indexOf('\n', start)) {
            v.addElement(s.substring(start, end));	// does NOT include the '\n'
            start = end + 1;
        }

        if (start < s.length()) {
            v.addElement(s.substring(start));
        }

        String[] ss = new String[v.size()];

        v.copyInto(ss);

        return ss;
    }

    ////////////////////////////////////////////////////////////////////////////
    public static void prepend(String[] ss, String what) {
        for (int i = 0; i < ss.length; i++) {
            ss[i] = what + ss[i];
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    public static String upperCaseFirstLetter(String s) {
        if (s == null || s.length() <= 0) {
            return s;
        }

        return s.substring(0, 1).toUpperCase(Locale.getDefault()) + s.substring(1);
    }

    ////////////////////////////////////////////////////////////////////////////
    public static String replace(String s, String token, String replace) {
        if (s == null || s.length() <= 0 || token == null || token.length() <= 0) {
            return s;
        }

        int index = s.indexOf(token);

        if (index < 0) {
            return s;
        }

        int tokenLength = token.length();
        String ret = s.substring(0, index);
        ret += replace;
        ret += s.substring(index + tokenLength);

        return ret;
    }

    ////////////////////////////////////////////////////////////////////////////
    public static String toString(Properties props) {
        if (props == null || props.size() <= 0) {
            return "No entries";
        }

        Set entries = props.entrySet();
        StringBuffer sb = new StringBuffer();

        // first -- to line things up nicely -- find the longest key...
        int keyWidth = 0;
        for (Iterator it = entries.iterator(); it.hasNext();) {
            Map.Entry me = (Map.Entry) it.next();
            String key = (String) me.getKey();
            int len = key.length();

            if (len > keyWidth) {
                keyWidth = len;
            }
        }

        ++keyWidth;

        // now make the strings...
        for (Iterator it = entries.iterator(); it.hasNext();) {
            Map.Entry me = (Map.Entry) it.next();
            String key = (String) me.getKey();
            String val = (String) me.getValue();

            sb.append(padRight(key, keyWidth));
            sb.append("= ");
            sb.append(val);
            sb.append('\n');
        }

        return sb.toString();
    }

    //  Test Code...
    public static void main(String[] args) {
        final int len = args.length;

        if ((len == 1) && args[0].equalsIgnoreCase("toLine")) {
            testToLine();
        }
        else if ((len > 1) && args[0].equalsIgnoreCase("isHex")) {
            testHex(args);
        }
        else {
            usage();
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    private static void usage() {
        System.out.println("StringUtils -- main() for testing usage:\n");
        System.out.println("java netscape.blizzard.util.StringUtils toLine");
        System.out.println("java netscape.blizzard.util.StringUtils isHex number1 number2 ...");
    }

    ////////////////////////////////////////////////////////////////////////////
    private static void testHex(String[] args) {
        System.out.println("StringUtils -- Testing Hex");

        for (int i = 1; i < args.length; i++) {
            System.out.println(padRight(args[i], 16) + "  " + (isHex(args[i]) ? "yesHex" : "notHex"));
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    private static void testToLine() {
        System.out.println("StringUtils -- Testing toLine()");
        String[] ss = {
            null,
            "",
            "abc\ndef\n",
            "abc\ndef",
            "abc",
            "abc\n",
            "abc\n\n",
            "q",
            "\n\nk\n\nz\n\n",
            "sd.adj;ld"
        };

        for (int k = 0; k < ss.length; k++) {
            String[] s2 = StringUtils.toLines(ss[k]);
            System.out.println("String #" + k + ", Number of Lines:  " + s2.length);

            for (int i = 0; i < s2.length; i++) {
                System.out.println(s2[i]);
            }
        }
    }

    public static void testUpperCase() {
        String[] test = new String[]{"xyz", "HITHERE", "123aa", "aSSS", "yothere"};//NOI18N

        for (int i = 0; i < test.length; i++) {
            System.out.println(test[i] + " >>> " + upperCaseFirstLetter(test[i]));//NOI18N
        }
    }

    /**
    A utility to get the Operating System specific path from given array
    of Strings.
    @param strings an array of Strings participating in the path.
    @param addTrailing a boolean that determines whether the returned
    String should have a trailing File Separator character. None of
    the strings may be null or empty String. An exception is thrown.
    @return a String that concatenates these Strings and gets a path. Returns
    a null if the array is null or contains no elements.
    @throws IllegalArgumentException if any of the arguments is null or is
    an empty string.
     */
    public static String makeFilePath(String[] strings, boolean addTrailing) {
        StringBuffer path = new StringBuffer();
        String separator = System.getProperty("file.separator");
        if (strings != null) {
            for (int i = 0; i < strings.length; i++) {
                String element = strings[i];
                if (element == null || element.length() == 0) {
                    throw new IllegalArgumentException();
                }
                path.append(element);
                if (i < strings.length - 1) {
                    path.append(separator);
                }
            }
            if (addTrailing) {
                path.append(separator);
            }
        }
        return (path.toString());
    }

    /**
     * Parses a string containing substrings separated from
     * each other by the standard separator characters and returns
     * a list of strings.
     *
     * Splits the string <code>line</code> into individual string elements
     * separated by the field separators, and returns these individual
     * strings as a list of strings. The individual string elements are
     * trimmed of leading and trailing whitespace. Only non-empty strings
     * are returned in the list.
     *
     * @param line The string to split
     * @return     Returns the list containing the individual strings that
     *             the input string was split into.
     */
    public static List<String> parseStringList(String line) {
        return parseStringList(line, null);
    }

    /**
     * Parses a string containing substrings separated from
     * each other by the specified set of separator characters and returns
     * a list of strings.
     *
     * Splits the string <code>line</code> into individual string elements
     * separated by the field separators specified in <code>sep</code>,
     * and returns these individual strings as a list of strings. The
     * individual string elements are trimmed of leading and trailing
     * whitespace. Only non-empty strings are returned in the list.
     *
     * @param line The string to split
     * @param sep  The list of separators to use for determining where the
     *             string should be split. If null, then the standard
     *             separators (see StringTokenizer javadocs) are used.
     * @return     Returns the list containing the individual strings that
     *             the input string was split into.
     */
    public static List<String> parseStringList(String line, String sep) {
        if (line == null) {
            return null;
        }

        StringTokenizer st;
        if (sep == null) {
            st = new StringTokenizer(line);
        }
        else {
            st = new StringTokenizer(line, sep);
        }

        String token;

        List<String> tokens = new Vector();
        while (st.hasMoreTokens()) {
            token = st.nextToken().trim();
            if (token.length() > 0) {
                tokens.add(token);
            }
        }

        return tokens;
    }

    /**
     * Get a system propety given a property name, possibly trying all combination
     * of upercase, name mangling to get a value.
     *
     * @param propName the approximate system property name
     * @return the property value if found, null otherwise
     */
    public static String getProperty(String propName) {
        // xxx.yyy
        String value = System.getProperty(propName);
        if (value != null) {
            return value;
        }
        // XXX.YYY
        value = System.getProperty(propName.toUpperCase(Locale.getDefault()));
        if (value != null) {
            System.setProperty(propName, value);
            return value;
        }
        // xxx_yyy
        value = System.getProperty(propName.replace('.', '_'));
        if (value != null) {
            System.setProperty(propName, value);
            return value;
        }
        // XXX_YYY
        value = System.getProperty(propName.toUpperCase(Locale.getDefault()).replace('.', '_'));
        if (value != null) {
            System.setProperty(propName, value);
        }
        return value;
    }

    /**
     * Remove a character from a String
     *
     * @param strOrig original string
     * @param c character to remove from the string
     * @return String with specified characters removed
     */
    public static String removeChar(String strOrig, char c) {
        StringBuilder strNew = new StringBuilder();
        for (int i = 0; i < strOrig.length(); i++) {
            if (strOrig.charAt(i) != c) {
                strNew.append(strOrig.charAt(i));
            }
        }
        return strNew.toString();
    }

    public static String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }

    public static final boolean isToken(String s) {
        return s != null && s.startsWith("${") && s.endsWith("}") && s.length() > 3;
    }

    public static final String stripToken(String s) {
        if (isToken(s))
            // NO possible wrong assumptions here -- see isToken()
            return s.substring(2, s.length() - 1);
        else
            return s;   // GIGO
    }

    /**
     * Concatenate a list of strings, putting a separator in between each one.
     * If the list is one string, then the separator is not used.
     * The separator will never be added to the start or end of the returned
     * string.
     * When empty or null strings are encountered in the list of strings
     * they are ignore.
     *
     * @param separator Separator to use between concatenated strings
     * @param list      List of strings to concatenate together
     * @return          String created by concatenating provided strings
     */
    public static String cat(String separator, String... list) {
        StringBuilder sb = new StringBuilder();

        boolean first = true;
        for (String s : list) {
            // Skip empty or null strings
            if (!StringUtils.ok(s)) {
                continue;
            }
            if (!first) {
                sb.append(separator);
            }
            else {
                first = false;
            }
            sb.append(s);
        }
        return sb.toString();
    }

    /**
     * Removes the quoting around a String.
     * @param s The String that may have enclosing quotes
     * @return The String resulting from removing the enclosing quotes
     */
    public static String removeEnclosingQuotes(String s) {
        if (s == null)
            return null;

        if (isDoubleQuoted(s) || isSingleQuoted(s)) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    /**
     * Nightmares can result from using a path with a space in it!
     * This method will enclose in double-quotes if needed.
     * @param path
     * @return
     */
    public static String quotePathIfNecessary(String path) {
        return quotePathIfNecessary(path, '"');
    }
    /**
     * Nightmares can result from using a path with a space in it!
     * This method will enclose in the specified quote characters if needed.
     * @param path
     * @return
     */
    public static String quotePathIfNecessary(String path, char quoteChar) {
        if (!ok(path)
                || !needsQuoting(path)
                || isDoubleQuoted(path)
                || isSingleQuoted(path))
            return path;
        // needs quoting!
        StringBuilder sb = new StringBuilder();
        sb.append(quoteChar);
        sb.append(path);
        sb.append(quoteChar);
        return sb.toString();
    }

    private static boolean needsQuoting(String path) {
        return ok(path)
                && (path.indexOf(' ') >= 0 || path.indexOf('\t') >= 0);
    }

    private static boolean isDoubleQuoted(String s) {
        return s.startsWith("\"") && s.endsWith("\"") && s.length() > 1;
    }

    private static boolean isSingleQuoted(String s) {
        return s.startsWith("'") && s.endsWith("'") && s.length() > 1;
    }

    /** Escape characters to use result in html.
     * <table border='1' cellpadding='3' cellspacing='0'>
     * <tr><th> Chars </th><th>Escape sequence</th></tr>
     * <tr><td> < </td><td> &lt; </td></tr>
     * <tr><td> > </td><td> &gt; </td></tr>
     * <tr><td> & </td><td> &amp; </td></tr>
     * <tr><td> " </td><td> &quot;</td></tr>
     * <tr><td> \t </td><td> &#009;</td></tr>
     * </table>
     */
    public static String escapeForHtml(String str) {
        if (str == null) {
            return null;
        }
        StringBuilder result = new StringBuilder(str.length() + 16);
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            switch (ch) {
                case '<':
                    result.append("&lt;");
                    break;
                case '>':
                    result.append("&gt;");
                    break;
                case '&':
                    result.append("&amp;");
                    break;
                case '\"':
                    result.append("&quot;");
                    break;
                case '\t':
                    result.append("&#009;");
                    break;
                default:
                    result.append(ch);
            }
        }
        return result.toString();
    }
    
    /** If given {@code String} is {@code null} then returns empty {@code String}
     * otherwise returns given {@code String}
     */
    public static String nvl(String str) {
        return str == null ? "" : str;
    }
}
