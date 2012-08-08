/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.appclient.server.connector;

import java.util.jar.Manifest;
import org.glassfish.internal.deployment.GenericSniffer;
import javax.inject.Inject;

import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.archive.ArchiveType;


import org.jvnet.hk2.annotations.Service;
import javax.inject.Singleton;

import java.io.IOException;
import java.util.jar.Attributes;
import java.util.List;
import java.util.ArrayList;

@Service(name = "AppClient")
@Singleton
public class AppClientSniffer extends GenericSniffer {
    private static final String[] stigmas = {
        "META-INF/application-client.xml", "META-INF/sun-application-client.xml", "META-INF/glassfish-application-client.xml"
    };

    private static final String[] containers = {"appclient"};

    @Inject CarType carType;

    public AppClientSniffer() {
        this(containers[0], stigmas[0], null);
    }

    public AppClientSniffer(String containerName, String appStigma, String urlPattern) {
        super(containerName, appStigma, urlPattern);
    }

    @Override
    public String[] getContainersNames() {
        return containers;
    }

    /**
     * Returns true if the passed file or directory is recognized by this
     * instance.                   
     *
     * @param location the file or directory to explore
     * @return true if this sniffer handles this application type
     */
    @Override
    public boolean handles(ReadableArchive location) {
        for (String s : stigmas) {
            try {
                if (location.exists(s)) {
                    return true;
                }
            } catch (IOException ignore) {
            }
        }

        try {
            Manifest manifest = location.getManifest();
            if (manifest != null && 
                manifest.getMainAttributes().containsKey(Attributes.Name.MAIN_CLASS)) {
                return true;
            }
        } catch (IOException ignore) {
        }
        return false;
    }

    /**
     * @return whether this sniffer should be visible to user
     */
    @Override
    public boolean isUserVisible() {
        return true;
    }

    /**
     * @return whether this sniffer represents a Java EE container type
     *
     */
    public boolean isJavaEE() {
        return true;
    }

    /**
     * @return the set of the sniffers that should not co-exist for the
     * same module. For example, ejb and connector sniffers should not
     * be returned in the sniffer list for a certain module.
     * This method will be used to validate and filter the retrieved sniffer
     * lists for a certain module
     *
     */
    @Override
    public String[] getIncompatibleSnifferTypes() {
        return new String[] {};
    }

    /**
     *
     * This API is used to help determine if the sniffer should recognize
     * the current archive.
     * If the sniffer does not support the archive type associated with
     * the current deployment, the sniffer should not recognize the archive.
     *
     * @param archiveType the archive type to check
     * @return whether the sniffer supports the archive type
     *
     */
    public boolean supportsArchiveType(ArchiveType archiveType) {
        if (archiveType.equals(carType)) {
            return true;
        }
        return false;
    }

    private static final List<String> deploymentConfigurationPaths =
            initDeploymentConfigurationPaths();

    private static List<String> initDeploymentConfigurationPaths() {
        final List<String> result = new ArrayList<String>();
        result.add("META-INF/application-client.xml");
        result.add("META-INF/sun-application-client.xml");
        result.add("META-INF/glassfish-application-client.xml");
        result.add("META-INF/weblogic-application-client.xml");
        return result;
    }

    /**
     * Returns the descriptor paths that might exist in an appclient app.
     *
     * @return list of the deployment descriptor paths
     */
    @Override
    protected List<String> getDeploymentConfigurationPaths() {
        return deploymentConfigurationPaths;
    }
}
