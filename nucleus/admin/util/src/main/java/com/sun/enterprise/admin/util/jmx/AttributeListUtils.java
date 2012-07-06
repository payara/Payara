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
 * Indentation Information:
 * 0. Please (try to) preserve these settings.
 * 1. Tabs are preferred over spaces.
 * 2. In vi/vim -
 *		:set tabstop=4 :set shiftwidth=4 :set softtabstop=4
 * 3. In S1 Studio -
 *		1. Tools->Options->Editor Settings->Java Editor->Tab Size = 4
 *		2. Tools->Options->Indentation Engines->Java Indentation Engine->Expand Tabs to Spaces = False.
 *		3. Tools->Options->Indentation Engines->Java Indentation Engine->Number of Spaces per Tab = 4.
 */
package com.sun.enterprise.admin.util.jmx;

import java.util.*;
import javax.management.Attribute;
import javax.management.AttributeList;

/**
 * A class that has some useful utility methods to deal with <ul> <li> Jmx
 * Attributes </li> <li> Jmx AttributeLists. </li> </ul> It is expected to
 * enhance this class with more utility methods.
 *
 * @author Kedar.Mhaswade@Sun.Com
 * @since Sun Java System Application Server 8
 */
public class AttributeListUtils {

    private AttributeListUtils() {
        //disallow
    }

    /**
     * Checks whether this list contains a JMX {@link Attribute} with given
     * name. Note that this method will return true if there is at least one
     * attribute with given name.
     *
     * @param al an instance of {@link AttributeList}
     * @param name a String representing the name of the attribute. The name may
     * not be null.
     * @return true if there is at least one attribute in the list with given
     * name, false otherwise
     * @throws IllegalArgumentException if the attribute list or name is null
     */
    public static boolean containsNamedAttribute(final AttributeList al,
            final String name) {
        if (al == null || name == null) {
            throw new IllegalArgumentException("null arg");
        }
        boolean contains = false;
        final Iterator it = al.iterator();
        while (it.hasNext()) {
            final Attribute at = (Attribute) it.next();
            if (name.equals(at.getName())) { //attribute name may not be null - guaranteed
                contains = true;
                break;
            }
        }
        return (contains);
    }

    /**
     * Checks whether an attribute with the name same as that of the given
     * attribute exists in this list. The given name may not be null.
     *
     * @param al an instance of {@link AttributeList}
     * @param a an Attribute with a name and a value
     * @return true if there exists at least one attribute with same name, false
     * otherwise
     * @throws IllegalArgumentException if the attribute list or name is null
     */
    public static boolean containsNamedAttribute(final AttributeList al,
            final Attribute a) {
        if (al == null || a == null) {
            throw new IllegalArgumentException("null arg");
        }
        return (containsNamedAttribute(al, a.getName()));
    }

    /**
     * Returns the given list as a map of attributes, keyed on the names of the
     * attribute in the list. The passed argument may not be null. The mappings
     * are between the names of attributes and attributes themselves.
     *
     * @param al the list of attributes that need to be mapped
     * @return an instance of {@link Map}
     * @throws IllegalArgumentException if the argument is null
     */
    public static Map asNameMap(final AttributeList al) {
        if (al == null) {
            throw new IllegalArgumentException("null arg");
        }
        final Map m = new HashMap();
        final Iterator it = al.iterator();
        while (it.hasNext()) {
            final Attribute a = (Attribute) it.next();
            m.put(a.getName(), a);
        }
        return (m);
    }

    /**
     * JMX 1.2 specification had a weird limitation that a Dynamic MBean may not
     * have an attribute whose name is <b> not a valid Java identifier <b>. This
     * method is a utility method to convert the any arbitrary name into a
     * String that can be a valid JMX 1.2 attribute. Every character in the
     * string passed that is neither a Character.isJavaIdentifierStart nor a
     * Character.isJavaIdentifierPart is replace with a valid character '_'.
     *
     * @param name a String that represents any non null name
     * @return a String that represents a name valid for a JMX 1.2 MBean.
     * @throws IllegalArgumentException if the parameter is null or is of zero
     * length
     */
    public static String toJmx12Attribute(final String name) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("invalid arg");
        }
        final char rc = '_';
        assert (Character.isJavaIdentifierStart(rc) && Character.isJavaIdentifierPart(rc));
        final char[] chars = new char[name.length()];
        name.getChars(0, name.length(), chars, 0);
        if (!Character.isJavaIdentifierStart(chars[0])) {
            chars[0] = rc;
        }
        for (int i = 1; i < name.length(); i++) { //note the index
            if (!Character.isJavaIdentifierPart(chars[i])) {
                chars[i] = rc;
            }
        }
        return (new String(chars));
    }

    /**
     * Returns a String representation of an attribute list such that: <ul> <li>
     * Each attribute is a name and value separated by a ','. toString() on the
     * value is called. </li> <li> Each pair is separated by a new line
     * character '\n'. </li> </ul>
     *
     * @param al the list of attributes - may be null, in which case an empty
     * String is returned.
     * @return a String representing the parameter passed
     */
    public static String toString(final AttributeList al) {
        final StringBuffer sb = new StringBuffer();
        final char SEP = ',';
        final char NL = '\n';
        if (al != null) {
            final Iterator it = al.iterator();
            while (it.hasNext()) {
                final Attribute a = (Attribute) it.next();
                sb.append(a.getName()).append(SEP).append(a.getValue().toString()).append(NL);
            }
        }
        return (sb.toString());
    }

    public static String dash2CamelCase(String dashed) {
        /*
         * This algorithm is obviously not accurate, as I have not written a
         * generic parser/lexical analyzer, nor have I written a BNF. All it
         * does is it converts Strings like abc-def-ghi to AbcDefGhi. A hyphen,
         * if followed by an alphabet, will be removed and the alphabet is
         * capitalized. No more complications. The passed String is converted
         * into lower case by default.
         */
        if (dashed == null) {
            throw new IllegalArgumentException("Null Arg");
        }
        dashed = dashed.toLowerCase(Locale.ENGLISH);
        final ArrayList list = new ArrayList();
        final StringTokenizer tz = new StringTokenizer(dashed, "-");
        while (tz.hasMoreTokens()) {
            list.add(tz.nextToken());
        }
        final String[] tmp = new String[list.size()];
        final String[] strings = getCamelCaseArray((String[]) list.toArray(tmp));
        return (strings2String(strings));
    }

    private static String[] getCamelCaseArray(final String[] from) {
        final String[] humps = new String[from.length];
        for (int i = 0; i < from.length; i++) {
            final StringBuffer sb = new StringBuffer(from[i]);
            sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
            humps[i] = sb.toString();
        }
        return (humps);
    }

    private static String strings2String(final String[] a) {
        final StringBuffer sb = new StringBuffer();
        for (int i = 0; i < a.length; i++) {
            sb.append(a[i]);
        }
        return (sb.toString());
    }
}
