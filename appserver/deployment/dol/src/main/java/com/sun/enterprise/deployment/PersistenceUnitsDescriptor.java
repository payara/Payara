/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.deployment;

import org.glassfish.api.deployment.archive.ArchiveType;
import org.glassfish.deployment.common.ModuleDescriptor;
import org.glassfish.deployment.common.RootDeploymentDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This descriptor represents contents for one persistence.xml file.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class PersistenceUnitsDescriptor extends RootDeploymentDescriptor {

    /** the parent descriptor that contains this descriptor */
    private RootDeploymentDescriptor parent;

    /**
     * The relative path from the parent {@link RootDeploymentDescriptor}
     * to the root of this persistence unit. e.g.
     * WEB-INF/classes -- if persistence.xml is in WEB-INF/classes/META-INF,
     * WEB-INF/lib/foo.jar -- if persistence.xml is in WEB-INF/lib/foo.jar/META-INF,
     * "" -- if persistence.xml is in some ejb.jar, or
     * util/bar.jar -- if persistence.xml is in a.ear/util/bar.jar
     */
    private String puRoot;

    List<PersistenceUnitDescriptor> persistenceUnitDescriptors =
            new ArrayList<PersistenceUnitDescriptor>();

    private static final String JPA_1_0 = "1.0";

    public PersistenceUnitsDescriptor() {
    }

    public RootDeploymentDescriptor getParent() {
        return parent;
    }

    public void setParent(RootDeploymentDescriptor parent) {
        this.parent = parent;
    }

    public String getPuRoot() {
        return puRoot;
    }

    public void setPuRoot(String puRoot) {
        this.puRoot = puRoot;
    }

    public String getDefaultSpecVersion() {
        return JPA_1_0;
    }

    public String getModuleID() {
        throw new RuntimeException();
    }

    public ArchiveType getModuleType() {
        throw new RuntimeException();
    }

    public ClassLoader getClassLoader() {
        return parent.getClassLoader();
    }

    public boolean isApplication() {
        return false;
    }

    /**
     * This method does not do any validation like checking for unique names
     * of PersistenceUnits.
     * @param pud the PersistenceUnitDescriptor to be added.
     */
    public void addPersistenceUnitDescriptor(PersistenceUnitDescriptor pud){
        persistenceUnitDescriptors.add(pud);
        pud.setParent(this);
    }

    /**
     * @return an unmodifiable list.
     */
    public List<PersistenceUnitDescriptor> getPersistenceUnitDescriptors() {
        return Collections.unmodifiableList(persistenceUnitDescriptors);
    }

    /**
     * This is a utility method which calculates the absolute path of the
     * root of a PU. Absolute path is not the path with regards to
     * root of file system. It is the path from the root of the Java EE
     * application this persistence unit belongs to.
     * Like {@link #getPuRoot()} returned path always uses '/' as path separator.
     * @return the absolute path of the root of this persistence unit
     * @see #getPuRoot()
     */
    public String getAbsolutePuRoot() {
        RootDeploymentDescriptor rootDD = getParent();
        if(rootDD.isApplication()){
            return getPuRoot();
        } else {
            ModuleDescriptor module = BundleDescriptor.class.cast(rootDD).
                    getModuleDescriptor();
            if(module.isStandalone()) {
                return getPuRoot();
            } else {
                final String moduleLocation = module.getArchiveUri();
                return moduleLocation + '/' + getPuRoot(); // see we always '/'
            }
        }
    }


    public boolean isEmpty() {
        return persistenceUnitDescriptors.isEmpty();
    }
}
