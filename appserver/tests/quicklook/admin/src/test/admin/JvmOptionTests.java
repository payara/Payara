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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.Map;
import java.util.jar.Manifest;
import org.testng.annotations.Test;
import test.admin.util.GeneralUtils;

/** Test related to creating/deleting/listing JVM options as supported by GlassFish.
 *
 * @author &#2325;&#2375;&#2342;&#2366;&#2352 (km@dev.java.net)
 * @since GlassFish v3 Prelude
 */
public class JvmOptionTests extends BaseAsadminTest {
    private static final String TEST_JOE    = "-Dname= joe blo"; //sufficiently unique
    private static final String CJ          = "create-jvm-options";
    private static final String DJ          = "delete-jvm-options";
    private static final String LJ          = "list-jvm-options";
    
    @Test(groups={"pulse"}) // test method
    public void createJoe() {
        Map<String, String> options = Collections.EMPTY_MAP;
        String operand = TEST_JOE;
        String up = GeneralUtils.toFinalURL(adminUrl, CJ, options, operand);
//        Reporter.log("url: " + up);
        Manifest man = super.invokeURLAndGetManifest(up);
        GeneralUtils.handleManifestFailure(man);
    }

    @Test(groups={"pulse"}, dependsOnMethods={"createJoe"})
    public void ensureCreatedJoeExists() {
        Manifest man = runListJoesCommand();
        GeneralUtils.handleManifestFailure(man);
        // we are past failure, now test the contents
        try {
            String children = URLDecoder.decode(GeneralUtils.getValueForTypeFromManifest(man, GeneralUtils.AsadminManifestKeyType.CHILDREN), "UTF-8");
            if (!children.contains(TEST_JOE)) {
                throw new RuntimeException("added JVM option: " + TEST_JOE + " does not exist in the list: " + children);
            }   
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    @Test(groups={"pulse"}, dependsOnMethods={"ensureCreatedJoeExists"})
    public void deleteJoe() {
        Map<String, String> options = Collections.EMPTY_MAP;
        String operand = TEST_JOE;
        String up = GeneralUtils.toFinalURL(adminUrl, DJ, options, operand);
//        Reporter.log("url: " + up);
        Manifest man = super.invokeURLAndGetManifest(up);
        GeneralUtils.handleManifestFailure(man);        
    }

    @Test(groups={"pulse"}, dependsOnMethods={"deleteJoe"})
    public void deletedJoeDoesNotExist() {
        Manifest man = runListJoesCommand();
        GeneralUtils.handleManifestFailure(man);
        // we are past failure, now test the contents
        try {
            String children = URLDecoder.decode(GeneralUtils.getValueForTypeFromManifest(man, GeneralUtils.AsadminManifestKeyType.CHILDREN), "UTF-8");
            if (children.contains(TEST_JOE)) {
                throw new RuntimeException("deleted JVM option: " + TEST_JOE + " exists in the list: " + children);
            } 
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
            
    }

    private Manifest runListJoesCommand() {
        Map<String, String> options = Collections.EMPTY_MAP;
        String operand = null;
        String up = GeneralUtils.toFinalURL(adminUrl, LJ, options, operand);
//        Reporter.log("url: " + up);
        Manifest man = super.invokeURLAndGetManifest(up);
        return ( man );
    }    
}
