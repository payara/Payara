/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.ejb.deployment.archive;

import org.glassfish.internal.deployment.GenericSniffer;
import com.sun.enterprise.module.Module;
import com.sun.enterprise.module.ModuleDefinition;
import com.sun.enterprise.module.common_impl.DirectoryBasedRepository;
import com.sun.enterprise.module.common_impl.AbstractModulesRegistryImpl;

import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.archive.ArchiveType;


import org.jvnet.hk2.annotations.Service;
import javax.inject.Inject;
import javax.enterprise.deploy.shared.ModuleType;
import javax.inject.Singleton;

import java.io.IOException;
import java.io.File;
import java.util.logging.Logger;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.lang.annotation.Annotation;

/**
 * Implementation of the Sniffer for the Ejb container.
 * 
 * @author Mahesh Kannan
 */
@Service(name="Ejb")
@Singleton
public class EjbSniffer  extends GenericSniffer {

    @Inject EjbType ejbType;

    private static final Class[]  ejbAnnotations = new Class[] {
            javax.ejb.Stateless.class, javax.ejb.Stateful.class,
            javax.ejb.MessageDriven.class, javax.ejb.Singleton.class };

    public EjbSniffer() {
        this("ejb", "META-INF/ejb-jar.xml", null);
    }
    
    public EjbSniffer(String containerName, String appStigma, String urlPattern) {
        super(containerName, appStigma, urlPattern);
    }    

    final String[] containers = {
            "org.glassfish.ejb.startup.EjbContainerStarter",
    };
        
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
        boolean result = super.handles(location);    //Check ejb-jar.xml

        if (result == false) {
            try {
                result = location.exists("META-INF/sun-ejb-jar.xml") || 
                    location.exists("META-INF/glassfish-ejb-jar.xml"); 
            } catch (IOException ioe) {
                // Ignore
            }
        }

        if (result == false) {
            try {
                result = location.exists("WEB-INF/ejb-jar.xml");
            } catch (IOException ioEx) {
                //TODO
            }
        }

        return result;
    }

    @Override
    public Class<? extends Annotation>[] getAnnotationTypes() {
        return ejbAnnotations;
    }

    /**
     * @return whether this sniffer should be visible to user
     *
     */
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
    public String[] getIncompatibleSnifferTypes() {
        return new String[] {"connector"};
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
        if (archiveType.equals(ejbType) ||
            archiveType.toString().equals(ModuleType.WAR.toString())) {
            return true;
        }
        return false;
    }

    private static final List<String> deploymentConfigurationPaths =
            initDeploymentConfigurationPaths();

    private static List<String> initDeploymentConfigurationPaths() {
        final List<String> result = new ArrayList<String>();
        result.add("META-INF/ejb-jar.xml");
        result.add("META-INF/sun-ejb-jar.xml");
        result.add("META-INF/glassfish-ejb-jar.xml");
        result.add("META-INF/weblogic-ejb-jar.xml");
        return result;
    }

    /**
     * Returns the descriptor paths that might exist at an ejb app.
     *
     * @return list of the deployment descriptor paths
     */
    @Override
    protected List<String> getDeploymentConfigurationPaths() {
        return deploymentConfigurationPaths;
    }

}
