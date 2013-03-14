/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.config.serverbeans.customvalidators;

import com.sun.enterprise.config.serverbeans.*;

import java.util.Arrays;
import java.util.List;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.UnexpectedTypeException;

import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.config.Dom;


/**
 * Validation logic for ContextRootCheck constraint
 *
 * @author Amy Roh
 */
public class ContextRootCheckValidator implements ConstraintValidator<ContextRootCheck, Application> {

    @Override
    public void initialize(ContextRootCheck constraintAnnotation) {
        //no initialization needed
    }

    @Override
    public boolean isValid(Application app, ConstraintValidatorContext cvc) throws UnexpectedTypeException {

        if (app == null) {
            return true;
        }
        Dom dom = Dom.unwrap(app);

        ServiceLocator locator = dom.getHabitat();
        if (locator == null) return true;

        ConfigBeansUtilities cbu = locator.getService(ConfigBeansUtilities.class);
        if (cbu == null) return true;

        Domain domain = cbu.getDomain();
        String appName = app.getName();

        String contextRoot = app.getContextRoot();

        if (contextRoot!=null && !contextRoot.startsWith("/")) {
            contextRoot = "/" + contextRoot;
        }

        boolean result = true;

        List<String> targets = domain.getAllReferencedTargetsForApplication(appName);
        for (String target : targets) {
            List<Server> servers = domain.getServersInTarget(target);
            for (Server server : servers) {
                ApplicationRef applicationRef = domain.getApplicationRefInServer(server.getName(), appName);

                if (applicationRef != null) {
                    for (Application application : domain.getApplications().getApplications()) {
                        if (isSameApp(appName, application.getName())) {
                            // skip the check if the validation is for different versions of the same application
                        } else if ((application.getContextRoot()!=null) && application.getContextRoot().equals(contextRoot)) {

                            String virtualServers = applicationRef.getVirtualServers();
                            List<String> vsList = Arrays.asList(virtualServers.split(","));

                            ApplicationRef thisAppRef = domain.getApplicationRefInServer(server.getName(), application.getName());

                            if (thisAppRef != null) {

                                virtualServers = thisAppRef.getVirtualServers();
                                List<String> thisVsList = Arrays.asList(virtualServers.split(","));

                                for (String vs : thisVsList) {
                                    if (vsList.contains(vs)) {
                                        result = false;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return result;

    }

    private static final boolean isSameApp(String name1, String name2) {
        try {
            if (name1.equals(name2)) {
                return true;
            } else if (name1.equals(getUntaggedName(name2))) {
                return true;
            } else if (name2.equals(getUntaggedName(name1))) {
                return true;
            } else if (getUntaggedName(name1).equals(getUntaggedName(name2))) {
                return true;
            }
        } catch (Exception ex) {
            // ignore
        }
        return false;
    }

    private static final String getUntaggedName(String appName) throws Exception {

        if(appName != null && !appName.isEmpty()){
            int colonIndex = appName.indexOf(":");
            // if the appname contains a EXPRESSION_SEPARATOR
            if (colonIndex >= 0){
                if (colonIndex == 0) {
                    // if appName is starting with a colon
                    throw new Exception("excepted application name before colon: " + appName);
                } else if (colonIndex == (appName.length() - 1)) {
                    // if appName is ending with a colon
                    throw new Exception("excepted version identifier after colon: " + appName);
                }
                // versioned
                return appName.substring(0, colonIndex);
            }
        }
        // not versioned
        return appName;
    }

}
