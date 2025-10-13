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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
 *
 * Portions Copyright [2017-2019] Payara Foundation and/or affiliates
 */
package com.sun.enterprise.util;

/* WBN Valentine's Day, 2000 -- place for handy String utils.
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.*;

public class StringUtils {
    public static final String NEWLINE = System.getProperty("line.separator");
    public static final String EOL = NEWLINE;

    private StringUtils() {
    }

    ////////////////////////////////////////////////////////////////////////////
    /**
     * return the length of the String - or 0 if it's null
     * @param s
     * @return
     */
    public static int safeLength(String s) {
        if (s == null) {
            return 0;
        }

        return s.length();
    }

    ////////////////////////////////////////////////////////////////////////////
    /**
     * Returns true if a string is not null and has a non-zero length, false otherwise
     * @param s
     * @return
     */
    public static boolean ok(String s) {
        return s != null && s.length() > 0;
    }

    ////////////////////////////////////////////////////////////////////////////
    /**
     * Returns a String containing SQLState, message and error code of exception and all sub-exceptions
     * @param ex the exception to format
     * @returnformatted exception
     */
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
    /**
     * Find longest String in a List of Strings...
     * @param strings the list of strings
     * @returnthe index of the longest string
     */
    public static int maxWidth(List strings) {
        int max = 0;

        if (strings == null || strings.isEmpty() || !(strings.get(0) instanceof String)) {
            return 0;
        }

        for (int i = strings.size() - 1; i >= 0; i--) {
            int len = ((String) strings.get(i)).length();

            if (len > max) {
                max = len;
            }
        }

        return max;
    }

    ////////////////////////////////////////////////////////////////////////////
    /** Is this the String representation of a valid hex number?
    "5", "d", "D", "F454ecbb" all return true..
     * @param s.**/
    // p.s. there MUST be a better and faster way of doing this...
    public static boolean isHex(String s) {

        if (s == null) return false;

        final int slen = s.length();

        for (int i = 0; i < slen; i++) {
            if (!isHex(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////
    private static final String HEX_CHARS = "0123456789abcdefABCDEF";
    /**
     * Is this the char a valid hex digit?
     * <p>
     * Can be upper or lower case
     * @param c
     * @return
     */
    public static boolean isHex(char c) {
        return HEX_CHARS.indexOf(c) != -1;
    }

    ////////////////////////////////////////////////////////////////////////////
    /**
     * e.g.  input: "a/b/c/d/foobar.txt"   output: "d"
     * @param s
     * @return
     */
    public static String getPenultimateDirName(String s) {

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
    /**
     * Returns the classname without package info
     * <p>
     * i.e. java.lang.String would return String
     * @param className The classname to convert. Note that there is no checking that this is a valid classname.
     * @return
     */
    public static String toShortClassName(String className) {
        int index = className.lastIndexOf('.');

        if (index >= 0 && index < className.length() - 1) {
            return className.substring(index + 1);
        }

        return className;
    }

    ////////////////////////////////////////////////////////////////////////////
    /**
     * Adds spaces to the end of a string to make it reach the specified length.
     * <p>
     * If the string is longer than the padded length then this function will return the original string.
     * @param s String to pad
     * @param len The length of the string with added padding
     * @return The padded string
     */
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
    /**
     * Adds spaces to the front of a string to make it reach the specified length.
     * <p>
     * If the string is longer than the padded length then this function will return the original string.
     * @param s String to pad
     * @param len The length of the string with added padding
     * @return The padded string
     */
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
    /**
     * Converts a String into an array where every \n is a new used to signal a new element in the array
     * @param s string to split into lines
     * @returnthe resulting lines array
     */
    public static String[] toLines(String s) {
        if (s == null) {
            return new String[0];
        }

        List<String> lines = new ArrayList<>();

        int start = 0;
        int end = 0;

        for (end = s.indexOf('\n', start); end >= 0 && start < s.length(); end = s.indexOf('\n', start)) {
            lines.add(s.substring(start, end));	// does NOT include the '\n'
            start = end + 1;
        }

        if (start < s.length()) {
            lines.add(s.substring(start));
        }

        return lines.toArray(new String[0]);
    }

    ////////////////////////////////////////////////////////////////////////////
    /**
     * Puts a string before every element in an array
     * <p>
     * i.e. {@code prepend(new String["foo", "bar"],"not ")} would result in {@code String["not foo", "not bar"]}
     * @param ss
     * @param what
     */
    public static void prepend(String[] ss, String what) {
        for (int i = 0; i < ss.length; i++) {
            ss[i] = what + ss[i];
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    /**
     * Converts the first letter of a string to upper case
     * @param s
     * @return
     */
    public static String upperCaseFirstLetter(String s) {
        if (s == null || s.length() <= 0) {
            return s;
        }

        return s.substring(0, 1).toUpperCase(Locale.getDefault()) + s.substring(1);
    }

    ////////////////////////////////////////////////////////////////////////////
    /**
     * Replaces the first instance of a token within a string
     * @param s The string to operate on
     * @param token the token to be replaced
     * @param replace the new value
     * @return
     * @deprecated Now part of {@link String} since JDK 1.5
     * @see String#replace(CharSequence, CharSequence)
     * @see String#replaceFirst(String, String)
     */
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
    /**
     * Converts a {@link Properties} object to string
     * <p>
     * If there it is empty then this will return "No entries".
     * Otherwise it will be in the form of "key= value" with each property on a new line.
     * @param props
     * @return
     */
    public static String toString(Properties props) {
        if (props == null || props.size() <= 0) {
            return "No entries";
        }

        Set<Map.Entry<Object, Object>> entries = props.entrySet();
        StringBuilder sb = new StringBuilder();

        // first -- to line things up nicely -- find the longest key...
        int keyWidth = 0;
        for (Map.Entry<Object, Object> me : entries) {
            String key = (String) me.getKey();
            int len = key.length();

            if (len > keyWidth) {
                keyWidth = len;
            }
        }

        ++keyWidth;

        // now make the strings...
        for (Map.Entry<Object, Object> me : entries) {
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
        StringBuilder path = new StringBuilder();
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

        List<String> tokens = new ArrayList<>();
        while (st.hasMoreTokens()) {
            String token = st.nextToken().trim();
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

    /**
     * Gets a String version of the stack trace of an exception
     * @param t
     * @return
     */
    public static String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }

    /**
     * Returns true is the given string is of the form "${text}"
     * @param s
     * @return
     */
    public static final boolean isToken(String s) {
        return s != null && s.startsWith("${") && s.endsWith("}") && s.length() > 3;
    }

    /**
     * Removes preceding <code>${</code> and trailing <code>}</code> from a String
     * @param s
     * @return
     */
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
     * @param quoteChar
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
     * @param str
     * @return
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
     * @param str
     * @return
     */
    public static String nvl(String str) {
        return str == null ? "" : str;
    }

    public static String trimQuotes(String value) {
        final int length = value.length();
        if (length > 1
                && ((value.startsWith("\"") && value.endsWith("\"") && value.substring(1, length - 1).indexOf('"') == -1)
                || (value.startsWith("'") && value.endsWith("'") && value.substring(1, length - 1).indexOf('\'') == -1))) {
            value = value.substring(1, length - 1);
        }
        return value;
    }
}
