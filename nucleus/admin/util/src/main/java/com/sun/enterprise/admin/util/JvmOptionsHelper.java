/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

/*
 * $Id: JvmOptionsHelper.java,v 1.3 2007/04/03 01:13:42 llc Exp $
 */
package com.sun.enterprise.admin.util;

import com.sun.enterprise.util.i18n.StringManager;
import java.util.*;

/**
 * A helper class to facilitate the add/delete/get jvm options.
 */
final public class JvmOptionsHelper {

    /**
     * First in the chain of responsibility
     */
    private final JvmOptionsElement head;

    /*
     * Constructs a new JvmOptionsHelper object. Stores the options as a chain
     * of JvmOptionElements. @param options @throws InvalidJvmOptionException If
     * any option is invalid. For example, an option that does not start with
     * '-'. @throws IllegalArgumentException If the options param is null.
     */
    public JvmOptionsHelper(String[] options) throws InvalidJvmOptionException {
        if (null == options) {
            throw new IllegalArgumentException();
        }
        if (options.length == 0) {
            head = new JvmOptionsElement("");
        } else {
            head = new JvmOptionsElement(options[0]);
        }
        JvmOptionsElement current = head;
        for (int i = 1; i < options.length; i++) {
            JvmOptionsElement next = new JvmOptionsElement(options[i]);
            current.setNext(next);
            current = next;
        }
    }

    /**
     * Adds the options to its current set. Omits options that already exist.
     * Note :- This method depends on the exact String comparision of the
     * options. Hence an option "a=b c=d" will be added even if individual
     * options already exist.
     *
     * @param options
     * @return Returns an array of options that <bold>could not</bold> be added.
     * The array will be atleast of 0 length. An array of length > 0 indicates
     * that some options haven't been added successfully.
     * @throws InvalidJvmOptionException If any option is invalid. For example,
     * an option that does not start with '-'.
     * @throws IllegalArgumentException If options param is null.
     */
    public String[] addJvmOptions(String[] options) throws InvalidJvmOptionException {
        if (null == options) {
            throw new IllegalArgumentException();
        }
        final Set alreadyExist = new HashSet();
        JvmOptionsElement last = last();
        for (int i = 0; i < options.length; i++) {
            if (!head.hasOption(options[i])) {
                JvmOptionsElement x = new JvmOptionsElement(options[i]);
                last.setNext(x);
                last = x;
            } else {
                alreadyExist.add(options[i]);
            }
        }
        return toStringArray(alreadyExist);
    }

    /**
     * Returns the last JvmOptionsElement in the chain of responsibility.
     */
    public JvmOptionsElement last() {
        JvmOptionsElement current = head;
        while (current.hasNext()) {
            current = current.next();
        }
        return current;
    }

    /**
     * Deletes the options from its current set.
     *
     * @param options
     * @return Returns an array of options that <bold>could not</bold> be
     * deleted. The array will be atleast of 0 length. An array of length > 0
     * indicates that some options haven't been deleted successfully.
     * @throws IllegalArgumentException If options param is null.
     */
    public String[] deleteJvmOptions(String[] options) {
        if (null == options) {
            throw new IllegalArgumentException();
        }
        
        final Set donotExist = new HashSet();
        for (int i = 0; i < options.length; i++) {
            if (!head.deleteJvmOption(options[i])) {
                donotExist.add(options[i]);
            }
        }
        return toStringArray(donotExist);
    }

    /**
     * Returns the current set of Jvm options.
     */
    public String[] getJvmOptionsAsStoredInXml() {
        Set s = new LinkedHashSet();
        JvmOptionsElement current = head;
        while (!JvmOptionsElement.isLast(current)) {
            String options = current.getJvmOptionsAsStoredInXml();
            if ((options != null) && (options.length() > 0)) {
                s.add(options);
            }
            current = current.next();
        }
        return toStringArray(s);
    }

    /**
     * Returns the current set of Jvm options.
     */
    public String[] getJvmOptions() {
        Set s = new LinkedHashSet();
        JvmOptionsElement current = head;
        while (!JvmOptionsElement.isLast(current)) {
            ArrayList options = current.getJvmOptions();
            if ((options != null) && (options.size() > 0)) {
                s.addAll(options);
            }
            current = current.next();
        }
        return toStringArray(s);
    }

    public static String[] toStringArray(Collection c) {
        final String[] s = new String[c.size()];
        final Iterator it = c.iterator();
        int i = 0;
        while (it.hasNext()) {
            s[i] = (String) it.next();
            i++;
        }
        return s;
    }
}

/**
 * Represents individual handlers in the chain of responsibility. Executes the
 * methods such as hasNext(), deleteJvmOption(), hasOption() on its options set
 * and then invokes the next in the chain.
 */
class JvmOptionsElement {

    private static final StringManager strMgr =
            StringManager.getManager(JvmOptionsElement.class);
    /**
     * Used to indicate the last element in the chain.
     */
    private static final JvmOptionsElement DEFAULT =
            new JvmOptionsElement() {

                @Override
                boolean hasOption(String option) {
                    return false;
                }

                @Override
                boolean deleteJvmOption(String option) {
                    return false;
                }

                @Override
                String getJvmOptionsAsStoredInXml() {
                    return "";
                }

                @Override
                ArrayList getJvmOptions() {
                    return new ArrayList();
                }

                @Override
                boolean hasNext() {
                    return false;
                }

                @Override
                void setNext(JvmOptionsElement element) {
                    throw new UnsupportedOperationException();
                }
            };
    private final Set jvmOptions = new LinkedHashSet();
    private JvmOptionsElement next;

    static boolean isLast(JvmOptionsElement e) {
        return (e == DEFAULT);
    }

    /**
     * private default ctor. To be used only within the scope of this class.
     */
    private JvmOptionsElement() {
    }

    /**
     * Constructs a new JvmOptionsElement object.
     *
     * @param options Tokenizes the options and stores them as a Set. Spaces are
     * used as delimiter.
     * @throws InvalidJvmOptionException If any option is invalid. For example,
     * an option that does not start with '-'.
     * @throws IllegalArgumentException If options is null.
     */
    JvmOptionsElement(String options) throws InvalidJvmOptionException {
        if (null == options) {
            throw new IllegalArgumentException();
        }
        //Need to exclude the gogo shell args that was added for issue 14173.
        //Otherwise performance tuner breaks saying that noop=true is not valid
        //because it does not begin with a '-'.
        String gogoArgs = "-Dgosh.args=--noshutdown -c noop=true";
        if (!options.equals(gogoArgs)) {
            //4923404
            QuotedStringTokenizer strTok = new QuotedStringTokenizer(options, " \t");
            //4923404
            while (strTok.hasMoreTokens()) {
                String option = strTok.nextToken();
                checkValidOption(option);
                jvmOptions.add(option);
            }
        } else {
            jvmOptions.add(gogoArgs);
        }
        next = DEFAULT;
    }

    /**
     * Sets the next element.
     *
     * @throws IllegalArgumentException If element is null.
     */
    void setNext(JvmOptionsElement element) {
        if (null == element) {
            throw new IllegalArgumentException();
        }
        this.next = element;
    }

    boolean hasNext() {
        return (DEFAULT != next);
    }

    JvmOptionsElement next() {
        return next;
    }

    boolean hasOption(String option) {
        boolean exists = jvmOptions.contains(option);
        if (!exists) {
            exists = next.hasOption(option);
        }
        return exists;
    }

    /**
     * Deletes the option from its set of jvm options and then invokes the next
     * in the chain.
     *
     * @param option
     * @return Returns true if the option exists in at least one element in the
     * chain.
     */
    boolean deleteJvmOption(String option) {
        boolean b1 = jvmOptions.remove(option);
        boolean b2 = next().deleteJvmOption(option);
        return (b1 || b2);
    }

    /**
     */
    String getJvmOptionsAsStoredInXml() {
        if (jvmOptions.isEmpty()) {
            return "";
        }
        final StringBuffer sb = new StringBuffer();
        final Iterator it = jvmOptions.iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(SEP);
            }
        }
        return sb.toString();
    }

    /**
     */
    ArrayList getJvmOptions() {
        final ArrayList arr = new ArrayList();
        if (!jvmOptions.isEmpty()) {
            final Iterator it = jvmOptions.iterator();
            while (it.hasNext()) {
                String nextOption = (String) it.next();
                if (nextOption.length() > 2 && nextOption.startsWith("\"") && nextOption.endsWith("\"")) {
                    nextOption = nextOption.substring(1, nextOption.length() - 1);
                }
                arr.add(nextOption);
            }
        }
        return arr;
    }
    static final char SEP = ' ';

    @Override
    public String toString() {
        return getJvmOptionsAsStoredInXml();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 19 * hash + (this.jvmOptions != null ? this.jvmOptions.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        boolean isEqual = false;
        if (o instanceof JvmOptionsElement) {
            JvmOptionsElement that = (JvmOptionsElement) o;
            return this.jvmOptions.containsAll(that.jvmOptions);
        }
        return isEqual;
    }

    private void checkValidOption(String option)
            throws InvalidJvmOptionException {
        if ((null == option) || option.equals("")) {
            throw new InvalidJvmOptionException(strMgr.getString(
                    "jvmOptions.invalid_option", option));
        }
        if (!option.startsWith("-")
                && !(option.startsWith("\"-") && option.endsWith("\""))) {
            throw new InvalidJvmOptionException(strMgr.getString(
                    "jvmOptions.no_dash", option));
        }
        //4923404
        checkQuotes(option);
        //4923404
    }

    //4923404
    void checkQuotes(String option) throws InvalidJvmOptionException {
        int length = option.length();
        int numQuotes = 0;
        int index = 0;

        while (index < length
                && (index = option.indexOf('\"', index)) != -1) {
            numQuotes++;
            index++;
        }
        if ((numQuotes % 2) != 0) {
            throw new InvalidJvmOptionException(strMgr.getString(
                    "jvmOptions.incorrect_quotes", option));
        }
    }
    //4923404
}
