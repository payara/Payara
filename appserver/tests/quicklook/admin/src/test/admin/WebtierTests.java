/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2010 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Manifest;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import test.admin.util.GeneralUtils;

/**
 * Provides web-tier configuration tests. We should also use this class to test out the runtime behavior of web-tier.
 * e.g. Are the webtier components really available the moment we create them?
 *
 * @author &#2325;&#2375;&#2342;&#2366;&#2352 (km@dev.java.net)
 * @since GlassFish v3 Prelude
 */
public class WebtierTests extends BaseAsadminTest {
    private final String LISTENER_NAME = "ls12345"; //sufficiently unique, I believe

    @BeforeClass
    private void setup() {
    }

    @Test(groups = {"pulse"})
    public void createListener() {
        if (!getListeners().contains(LISTENER_NAME)) {
            String CMD = "create-http-listener";
            Map<String, String> options = getCreateOptions();
            String operand = LISTENER_NAME;
            String up = GeneralUtils.toFinalURL(adminUrl, CMD, options, operand);
            //Reporter.log("url: " + up);
            Manifest man = super.invokeURLAndGetManifest(up);
            String ec = GeneralUtils.getValueForTypeFromManifest(man, GeneralUtils.AsadminManifestKeyType.EXIT_CODE);
            GeneralUtils.handleManifestFailure(man);
        }
    }

    @Test(groups = {"pulse"}, dependsOnMethods = {"ensureDeletedListenerDoesNotExist"})
    public void createListenerWithOldParam() {
        String operand = LISTENER_NAME + "2";
        if (!getListeners().contains(operand)) {
            String CMD = "create-http-listener";
            Map<String, String> options = getCreateOptions();
            options.put("defaultvs", options.get("default-virtual-server"));
				options.remove("default-virtual-server");
            String up = GeneralUtils.toFinalURL(adminUrl, CMD, options, operand);
            //Reporter.log("url: " + up);
            Manifest man = super.invokeURLAndGetManifest(up);
            String ec = GeneralUtils.getValueForTypeFromManifest(man, GeneralUtils.AsadminManifestKeyType.EXIT_CODE);
            GeneralUtils.handleManifestFailure(man);
        }
    }

    @Test(groups = {"pulse"}, dependsOnMethods = {"createListener"})
    public void ensureCreatedListenerExists() { //should be run after createListener method
        if (!getListeners().contains(LISTENER_NAME)) {
            throw new RuntimeException("created http listener: " + LISTENER_NAME + " does not exist in the list");
        }
    }

    private String getListeners() {
        Manifest man = runListHttpListenersCommand();
        GeneralUtils.handleManifestFailure(man);
        // we are past failure, now test the contents
        return GeneralUtils.getValueForTypeFromManifest(man, GeneralUtils.AsadminManifestKeyType.CHILDREN);
    }

    @Test(groups = {"pulse"}, dependsOnMethods = {"createListener", "ensureCreatedListenerExists"})
    public void deleteListener() {
        String CMD = "delete-http-listener";
        Map<String, String> options = Collections.EMPTY_MAP;
        String operand = LISTENER_NAME;
        String up = GeneralUtils.toFinalURL(adminUrl, CMD, options, operand);
//        Reporter.log("url: " + up);
        Manifest man = super.invokeURLAndGetManifest(up);
        String ec = GeneralUtils.getValueForTypeFromManifest(man, GeneralUtils.AsadminManifestKeyType.EXIT_CODE);
        GeneralUtils.handleManifestFailure(man);
    }

    @Test(groups = {"pulse"}, dependsOnMethods = {"createListenerWithOldParam"})
    public void deleteListener2() {
        String CMD = "delete-http-listener";
        Map<String, String> options = Collections.EMPTY_MAP;
        String operand = LISTENER_NAME + "2";
        String up = GeneralUtils.toFinalURL(adminUrl, CMD, options, operand);
//        Reporter.log("url: " + up);
        Manifest man = super.invokeURLAndGetManifest(up);
        String ec = GeneralUtils.getValueForTypeFromManifest(man, GeneralUtils.AsadminManifestKeyType.EXIT_CODE);
        GeneralUtils.handleManifestFailure(man);
    }

    @Test(groups = {"pulse"}, dependsOnMethods = {"deleteListener"})
    public void ensureDeletedListenerDoesNotExist() {
        if (getListeners().contains(LISTENER_NAME)) {
            throw new RuntimeException("deleted http listener: " + LISTENER_NAME + " exists in the list");
        }
    }

    private Map<String, String> getCreateOptions() {
        Map<String, String> opts = new HashMap<String, String>();
        opts.put("listeneraddress", "0.0.0.0");
        opts.put("listenerport", "1234");
        opts.put("default-virtual-server", "server");
        return (opts);
    }

    private Manifest runListHttpListenersCommand() {
        String CMD = "list-http-listeners";
        Map<String, String> options = Collections.EMPTY_MAP;
        String operand = null;
        String up = GeneralUtils.toFinalURL(adminUrl, CMD, options, operand);
//        Reporter.log("url: " + up);
        Manifest man = super.invokeURLAndGetManifest(up);
        return (man);
    }
}
