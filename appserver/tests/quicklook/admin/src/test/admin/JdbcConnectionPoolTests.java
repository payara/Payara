/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2011 Oracle and/or its affiliates. All rights reserved.
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

package test.admin;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.jar.Manifest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import test.admin.util.GeneralUtils;

/** Supposed to have JDBC connection pool and resource tests.
 *
 * @author &#2325;&#2375;&#2342;&#2366;&#2352 (km@dev.java.net)
 * @since GlassFish v3 Prelude
 */
public class JdbcConnectionPoolTests extends BaseAsadminTest {

    private File path;
    private static final String JAVADB_POOL = "javadb_pool"; //same as in resources.xml
    private static final String ADD_RES     = "add-resources";
    
    @Parameters({"resources.xml.relative.path"})
    @BeforeClass
    public void setupEnvironment(String relative) {
        String cwd = System.getProperty("BASEDIR");
        path = new File(cwd, relative);
    }
    @Test(groups={"pulse"}) // test method
    public void createPool() {
        Map<String, String> options = Collections.EMPTY_MAP;
        String operand = path.getAbsolutePath();
        String up = GeneralUtils.toFinalURL(adminUrl, ADD_RES, options, operand);
//        Reporter.log("url: " + up);
        Manifest man = super.invokeURLAndGetManifest(up);
        GeneralUtils.handleManifestFailure(man);
    }
    @Test(groups={"pulse"}, dependsOnMethods={"createPool"})
    public void pingPool() {
        String CMD = "ping-connection-pool";
        Map<String, String> options = Collections.EMPTY_MAP;
        String operand = JAVADB_POOL;
        String up = GeneralUtils.toFinalURL(adminUrl, CMD, options, operand);
        Manifest man = super.invokeURLAndGetManifest(up);
        GeneralUtils.handleManifestFailure(man);
        //ping succeeded!
    }

    @Test(groups={"pulse"}, dependsOnMethods={"pingPool"})
    public void ensureCreatedPoolExists() {
        Manifest man = runListPoolsCommand();
        GeneralUtils.handleManifestFailure(man);
        // we are past failure, now test the contents
        String children = GeneralUtils.getValueForTypeFromManifest(man, GeneralUtils.AsadminManifestKeyType.CHILDREN);
        if (!children.contains(JAVADB_POOL)) {
            throw new RuntimeException("deleted http listener: " + JAVADB_POOL + " exists in the list: " + children);
        }        
    }
    
    @Test(groups={"pulse"}, dependsOnMethods={"ensureCreatedPoolExists"})
    public void deletePool() {
        String CMD = "delete-jdbc-connection-pool";
        Map<String, String> options = Collections.EMPTY_MAP;
        String operand = JAVADB_POOL;
        String up = GeneralUtils.toFinalURL(adminUrl, CMD, options, operand);
//        Reporter.log("url: " + up);
        Manifest man = super.invokeURLAndGetManifest(up);
        GeneralUtils.handleManifestFailure(man);        
    }

    @Test(groups={"pulse"}, dependsOnMethods={"deletePool"})
    public void deletedPoolDoesNotExist() {
        Manifest man = runListPoolsCommand();
        GeneralUtils.handleManifestFailure(man);
        // we are past failure, now test the contents
        String children = GeneralUtils.getValueForTypeFromManifest(man, GeneralUtils.AsadminManifestKeyType.CHILDREN);
        if (children.contains(JAVADB_POOL)) {
            throw new RuntimeException("deleted http listener: " + JAVADB_POOL + " exists in the list: " + children);
        }         
    }

    private Manifest runListPoolsCommand() {
        String CMD = "list-jdbc-connection-pools";
        Map<String, String> options = Collections.EMPTY_MAP;
        String operand = null;
        String up = GeneralUtils.toFinalURL(adminUrl, CMD, options, operand);
//        Reporter.log("url: " + up);
        Manifest man = super.invokeURLAndGetManifest(up);
        return ( man );
    }    
}
