/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package com.sun.enterprise.admin.cli.optional;

import com.sun.enterprise.admin.cli.ClassPathBuilder;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.util.OS;
import static com.sun.enterprise.util.SystemPropertyConstants.DERBY_ROOT_PROPERTY;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.glassfish.api.admin.CommandException;

/**
 *
 * @author jGauravGupta
 */
public class DerbyManager extends DBManager {

    private static final LocalStringsImpl strings
            = new LocalStringsImpl(DerbyManager.class);

    @Override
    public Class<? extends DBControl> getDBControl() {
        return DerbyControl.class;
    }

    @Override
    public String getLogFileName() {
        return DerbyControl.DB_LOG_FILENAME;
    }

    /**
     * Note that when using Darwin (Mac), the property,
     * "-Dderby.storage.fileSyncTransactionLog=True" is defined. See:
     * http://www.jasonbrome.com/blog/archives/2004/12/05/apache_derby_on_mac_os_x.html
     * https://issues.apache.org/jira/browse/DERBY-1
     *
     * @return
     */
    @Override
    public List<String> getSystemProperty() {
        List<String> properties = new ArrayList<>();
        if (OS.isDarwin()) {
            properties.add("-Dderby.storage.fileSyncTransactionLog=True");
        }
        return properties;
    }

    @Override
    public String getRootProperty() {
        return DERBY_ROOT_PROPERTY;
    }

    /**
     * Check if database is installed.
     *
     * @param dbLocation
     * @throws org.glassfish.api.admin.CommandException
     */
    @Override
    public void checkIfDbInstalled(final File dbLocation) throws CommandException {
        if (!dbLocation.exists()) {
            throw new CommandException("dblocation not found: " + dbLocation);
        }
        File dbJar = new File(new File(dbLocation, "lib"), "derbyclient.jar");
        if (!dbJar.exists()) {
            throw new CommandException("derbyclient.jar not found in " + dbLocation);
        }
    }

    @Override
    public void buildDatabaseClasspath(File dbLocation, ClassPathBuilder sDatabaseClasspath) throws CommandException {
        sDatabaseClasspath
                .add(dbLocation, "lib", "derby.jar")
                .add(dbLocation, "lib", "derbytools.jar")
                .add(dbLocation, "lib", "derbynet.jar")
                .add(dbLocation, "lib", "derbyclient.jar");
    }

}
