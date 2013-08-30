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

package org.glassfish.common.util.admin;

import java.io.InputStream;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 *  A utility class that gets the plain text man page for the
 *  given command.  It searches (using Class.getResource()) for
 *  the pages, and returns the first one found.
 *
 *  For any given man page multiple instances of that page can exist.
 *  Man pages are come in sections (1 through 9, 1m through 9m),
 *  locales (language, country, variant), and by command version.
 *  These instances are ordered by section number (1 - 9, 1m *  - 9m),
 *  local specificity (most specific to least specific) and then by
 *  version (later versions before earlier versions).
 *
 *  This is probably <em>not</em> what is wanted (I think what is
 *  wanted is versions before sections before language specificity),
 *  but is this way because of the way the Java Class.getResource()
 *  mechanism works.
 *
 *  All methods will throw a NullPointerException if given null object
 *  arguments.
 *
 *  All methods will throw an IllegalArgumentException if their
 *  arguments are non-null but are otherwise meaningless.
 */

public class ManPageFinder {
    private static final String[] sections = {
        "1", "1m", "2", "2m", "3", "3m", "4", "4m", "5", "5m",
        "6", "6m", "7", "7m", "8", "8m", "9", "9m", "5asc" };

    private ManPageFinder() {
        // no instances allowed
    }

    /**
     * Get the man page for the given command for the given locale
     * using the given classloader.
     *
     * @param cmdName the command name
     * @param cmdClass the command class
     * @param locale the locale to be used to find the man page
     * @param classLoader the class loader to be used to find the man page
     */
    public static BufferedReader getCommandManPage(
                        String cmdName, String cmdClass,
                        Locale locale, ClassLoader classLoader, Logger logger) {

        InputStream s = null;
 
        Iterator it = getPossibleLocations(cmdName, cmdClass, locale, logger);
        while (s == null && it.hasNext()) {
            s = classLoader.getResourceAsStream((String)it.next());
        }
 
        if (s == null)
            return null;
        Reader r;
        try {
            r = new InputStreamReader(s, "utf-8");
        } catch (UnsupportedEncodingException ex) {
            r = new InputStreamReader(s);
        }
        return new BufferedReader(r);
    }

    private static Iterator getPossibleLocations(final String cmdName,
            final String cmdClass, final Locale locale, final Logger logger) {
        return new Iterator() {
            final String[] locales = getLocaleLocations(locale);
            private int i = 0;
            private int j = 0;
            private String helpdir = getHelpDir(cmdClass);
            private String commandName = cmdName;

            public boolean hasNext() {
                return i < locales.length && j < sections.length;
            }

            public Object next() throws NoSuchElementException{
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                final String result = helpdir + locales[i] + "/" +
                                        commandName + "." + sections[j++];

                if (j == sections.length) {
                    i++;
                    if (i < locales.length)
                        j = 0;
                }
                logger.log(Level.FINE, "Trying to get this manpage: {0}", result);
                return result;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static String getHelpDir(String cmdClass) {
        // The man page is assumed to be packaged with the
        // command class.
        String pkgname =
            cmdClass.substring(0, cmdClass.lastIndexOf('.'));
        return pkgname.replace('.', '/');
    }

    private static String[] getLocaleLocations(Locale locale) {
        String language = locale.getLanguage();
        String country = locale.getCountry();
        String variant = locale.getVariant();
        List<String> l = new ArrayList<String>();
        l.add("");

        if (language != null && language.length() > 0) {
            l.add("/" + language);
            if (country != null && country.length() > 0) {
                l.add("/" + language + "_" + country);
                if (variant != null && variant.length() > 0)
                    l.add("/" + language + "_" + country + "_" + variant);
            }
        }
        Collections.reverse(l);
        return l.toArray(new String[l.size()]);
    }
}
