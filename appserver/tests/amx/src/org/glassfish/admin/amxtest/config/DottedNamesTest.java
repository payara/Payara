/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.amxtest.config;

import com.sun.appserv.management.base.DottedNames;
import org.glassfish.admin.amxtest.AMXTestBase;

import javax.management.Attribute;

/**
 */
public final class DottedNamesTest
        extends AMXTestBase {
    public DottedNamesTest() {
    }

    private void
    checkAttribute(final Attribute attr) {
        assert (attr != null);

        final Object value = attr.getValue();
        if (value instanceof Attribute) {
            warning("Is value of " + attr.getName() + " really another Attribute? => " +
                    toString(value));
        }
    }

    private void
    checkResultsFromWildGet(
            final Object[] results) {
        for (int i = 0; i < results.length; ++i) {
            final Object result = results[i];

            if (result == null) {
                warning("null result from dottedNameGet( \"*\" )");
            } else if (!(result instanceof Attribute)) {
                warning("non-Attribute result from dottedNameGet( \"*\" ): " + result);
            } else {
                // it's an Attribute
                final Attribute attr = (Attribute) result;
                checkAttribute((Attribute) result);
            }
        }
    }

    private void
    checkResultsFromGet(
            final String[] names,
            final Object[] results) {
        for (int i = 0; i < results.length; ++i) {
            final Object result = results[i];

            if (result == null) {
                warning("Dotted name has null result: " + names[i]);
            } else if (!(result instanceof Attribute)) {
                warning("Dotted name " + names[i] + " could not be obtained: " + result);
            }
        }
    }

    private String[]
    getAllNames(final DottedNames dottedNames) {
        final Attribute[] attrs = (Attribute[]) dottedNames.dottedNameGet("*");
        final String[] names = new String[attrs.length];
        for (int i = 0; i < names.length; ++i) {
            names[i] = attrs[i].getName();
        }

        return (names);
    }


    public void
    testGetAllConfigDottedNames() {
        final long start = now();
        final DottedNames dottedNames = getDomainRoot().getDottedNames();

        final String[] names = getAllNames(dottedNames);

        final Object[] results = dottedNames.dottedNameGet(names);

        checkResultsFromGet(names, results);
        printElapsed("testGetAllConfigDottedNames", start);
    }

/*    public void
    testGetAllMonitoringDottedNames() {
        if (checkNotOffline("testMonitoringRefresh")) {
            final MonitoringDottedNames dottedNames = getDomainRoot().getMonitoringDottedNames();
            final long start = now();
            final String[] names = getAllNames(dottedNames);

            final Object[] results = dottedNames.dottedNameGet(names);

            checkResultsFromGet(names, results);
            printElapsed("testGetAllMonitoringDottedNames", start);
        }
    }*/

    public void
    testWildGetAllDottedNames() {
        final long start = now();
        final DottedNames dottedNames = getDomainRoot().getDottedNames();

        final Attribute[] results = (Attribute[]) dottedNames.dottedNameGet("*");
        checkResultsFromWildGet(results);
        printElapsed("testWildGetAllConfigDottedNames", start);
    }
/*


    public void
    testWildGetAllMonitoringDottedNames() {
        if (checkNotOffline("testMonitoringRefresh")) {
            final long start = now();
            final MonitoringDottedNames dottedNames = getDomainRoot().getMonitoringDottedNames();
            final Attribute[] results = (Attribute[]) dottedNames.dottedNameGet("*");
            checkResultsFromWildGet(results);
            printElapsed("testWildGetAllMonitoringDottedNames", start);
        }
    }
*/

    /**
     Test that we can set (change) a dotted name.
     */
    public void
    testDottedNameSet() {
        final long start = now();

        final DottedNames dottedNames = getDomainRoot().getDottedNames();

        final String target = "domain.locale";
        final Object result = dottedNames.dottedNameGet(target);

        final Attribute localeAttr = (Attribute) dottedNames.dottedNameGet(target);
        checkAttribute(localeAttr);

        final String locale = (String) localeAttr.getValue();

        // set to a new value
        Object[] results = dottedNames.dottedNameSet(new String[]{target + "=dummy_locale"});
        assert (results.length == 1);
        checkAttribute((Attribute) results[0]);

        // change back to previous value
        final String restoreString = target + "=" + (locale == null ? "" : locale);
        results = dottedNames.dottedNameSet(new String[]{restoreString});

        final Attribute finalAttr = (Attribute) dottedNames.dottedNameGet(target);
        assert (
                (finalAttr.getValue() == null && localeAttr.getValue() == null) ||
                        finalAttr.getValue().equals(localeAttr.getValue()));
        printElapsed("testConfigDottedNameSet", start);
    }

    private int
    testList(
            final DottedNames dottedNames,
            final String dottedName) {
        final Object[] results = dottedNames.dottedNameList(new String[]{dottedName});

        //trace( dottedName + ": " + toString( results ) );
        for (int i = 0; i < results.length; ++i) {
            testList(dottedNames, (String) results[i]);
        }

        return (results.length);
    }

        public void
    testRecursiveDottedNameList() {
        final long start = now();
        final DottedNames dottedNames = getDomainRoot().getDottedNames();

        final int numFound = testList(dottedNames, "domain");
        assert (numFound >= 4);    // should be at least 4.
        printElapsed("testRecursiveConfigDottedNameList", start);
    }

    /*

    public void
    testRecursiveMonitoringDottedNameList() {
        if (checkNotOffline("testRecursiveMonitoringDottedNameList")) {
            final MonitoringDottedNames dottedNames = getDomainRoot().getMonitoringDottedNames();

            final long start = now();

            final int numFound = testList(dottedNames, "server");
            assert (numFound >= 4);    // should be at least 4.\

            testList(dottedNames, "*");

            printElapsed("testRecursiveMonitoringDottedNameList", start);
        }
    }*/
}











