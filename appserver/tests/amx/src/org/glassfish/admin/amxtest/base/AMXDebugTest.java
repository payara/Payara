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

/*
* $Header: /cvs/glassfish/admin/mbeanapi-impl/tests/org.glassfish.admin.amxtest/base/AMXDebugTest.java,v 1.5 2007/05/05 05:23:53 tcfujii Exp $
* $Revision: 1.5 $
* $Date: 2007/05/05 05:23:53 $
*/
package org.glassfish.admin.amxtest.base;

import com.sun.appserv.management.base.AMXDebug;
import com.sun.appserv.management.util.misc.Output;

import java.io.File;

/**
 */
public final class AMXDebugTest
        extends junit.framework.TestCase {
    public AMXDebugTest() {
        getAMXDebug().setDefaultDebug(true);
        getAMXDebug().setAll(true);
    }

    private String
    getID(final String uniquifier) {
        return this.getClass().getName() + "." + uniquifier;
    }

    private Output
    getOutput(final String id) {
        return getAMXDebug().getOutput(id);
    }

    private AMXDebug
    getAMXDebug() {
        return AMXDebug.getInstance();
    }

    public synchronized void
    testCreateFile() {
        // multiple iterations require that we choose a new file each time
        final String id = getID("testCreateFile" + System.currentTimeMillis());
        final Output output = getOutput(id);

        final File outputFile = getAMXDebug().getOutputFile(id);
        outputFile.delete();
        assert (!outputFile.exists());

        output.printDebug("test");
        assert (outputFile.exists());
    }

    public synchronized void
    testToggleDebug() {
        final String id = getID("testToggleDebug");
        final Output output = getOutput(id);

        getAMXDebug().setDebug(id, false);
        assert (!getAMXDebug().getDebug(id));
        getAMXDebug().setDebug(id, true);
        assert (getAMXDebug().getDebug(id));
    }

    public synchronized void
    testReset() {
        final String id = getID("testReset");
        final Output output = getOutput(id);

        getAMXDebug().reset(id);
        final File outputFile = getAMXDebug().getOutputFile(id);
        outputFile.delete();
        assert (!outputFile.exists());
        output.printDebug("test");
        assert (outputFile.exists());

        // make sure we can call it repeatedly
        getAMXDebug().reset(id);
        getAMXDebug().reset(id);
        getAMXDebug().reset(id);
    }


    public synchronized void
    testPrint() {
        final String id = getID("testPrint");
        final Output output = getOutput(id);

        output.printDebug("printDebug");
        output.printError("printError");
        output.println("println");
        output.print("print");
        output.print("...");
        output.print("END");
    }


    public synchronized void
    testClose() {
        final String id = getID("testClose");
        final Output output = getOutput(id);
        final File outputFile = getAMXDebug().getOutputFile(id);

        output.println("hello");
        assert (outputFile.exists());

        output.close();
        outputFile.delete();
        assert (!outputFile.exists());

        output.println("hello");
        assert (outputFile.exists());
    }

    public synchronized void
    testToggleDefaultDebug() {
        final String id = getID("testToggleDefaultDebug");
        final Output output = getOutput(id);

        getAMXDebug().setDefaultDebug(false);
        assert (!getAMXDebug().getDefaultDebug());

        getAMXDebug().setDefaultDebug(true);
        assert (getAMXDebug().getDefaultDebug());
    }


    public synchronized void
    testSetAll() {
        final String id = getID("testSetAll");
        final Output output = getOutput(id);

        getAMXDebug().setAll(false);
        getAMXDebug().setAll(false);
        getAMXDebug().setAll(true);
        getAMXDebug().setAll(true);
        getAMXDebug().setAll(false);
        getAMXDebug().setAll(true);
        getAMXDebug().setAll(true);
    }


    public synchronized void
    testMark() {
        final String id = getID("testMark");
        final Output output = getOutput(id);

        getAMXDebug().mark(id);
        getAMXDebug().mark(id, null);
        getAMXDebug().mark(id, "marker 1");
        getAMXDebug().mark(id, "marker 2");
        getAMXDebug().mark(output, null);
        getAMXDebug().mark(output, "marker 3");
    }
}

