/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.uberjar.builder.installroot;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author bhavanishankar@dev.java.net
 */

public class InstallRootBuilder implements BundleActivator {

    private static final Logger logger = Logger.getLogger("embedded-glassfish");
    private static String resourceroot = "glassfish4/glassfish/";

    public void start(BundleContext context) throws Exception {
        String installRoot = context.getProperty("com.sun.aas.installRoot");
        buildInstallRoot(context.getBundle(), installRoot);
    }

    public void stop(BundleContext context) throws Exception {
        logger.fine("InstallRootBuilder stopped");
    }

    public void buildInstallRoot(Bundle bundle, String installRoot) throws Exception {
        List<String> resources = getResources(bundle, resourceroot + "lib/");
        for (String resource : resources) {
            InstallRootBuilderUtil.copy(bundle.getResource(resource).openConnection().getInputStream(),
                    installRoot, resource.substring(resourceroot.length()));
        }
    }

    private List<String> getResources(Bundle b, String... subpaths) {
        List<String> resources = new ArrayList();
        if (subpaths == null || subpaths.length == 0) {
            subpaths = new String[]{"/"};
        }
        for (String subpath : subpaths) {
            for (Enumeration e = b.getEntryPaths(subpath); e != null && e.hasMoreElements();) {
                String entryPath = (String) e.nextElement();
                if (entryPath.endsWith("/")) {
                    resources.addAll(getResources(b, entryPath));
                } else {
                    resources.add(entryPath);
                }
            }
        }
        return resources;
    }

}
