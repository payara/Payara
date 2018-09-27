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

// Portions Copyright [2016-2017] [Payara Foundation and/or its affiliates]

package org.glassfish.weld;

import com.sun.enterprise.deployment.EjbDescriptor;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.weld.connector.WeldUtils;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.BeansXml;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * A root BDA represents the root of a module where a module is a war, ejb, rar, ear lib
 * A root BDA of each module follows accessibility of the module (can only see BDAs, including root ones,
 * in accessible modules).  A root BDA contains no bean classes.  All bdas of the module are visible to the root bda.
 * And the root bda is visible to all bdas of the module.
 *
 * (Alternatively creating one root BDA per deployment has the disadvantage that you need to be careful about accessibility rules.
 * If you allow every BDA to see the root BDA - return it from BDA.getBeanDeploymentArchives() - and allow the root BDA
 * to see all other BDAs - return all other BDAs from root BDA.getDeployemtArchive(). Due to transitivity you make
 * any BDA accessible to any other BDA and break the accessibility rules.  One way is to only allow the root BDA to
 * see all the other BDAs (but not vice versa). This may work for the InjectionTarget case but may be a
 * limitation elsewhere.)
 *
 * @author <a href="mailto:j.j.snyder@oracle.com">JJ Snyder</a>
 */
public class RootBeanDeploymentArchive extends BeanDeploymentArchiveImpl {
    private BeanDeploymentArchiveImpl moduleBda;

    public RootBeanDeploymentArchive(ReadableArchive archive,
                                     Collection<EjbDescriptor> ejbs,
                                     DeploymentContext deploymentContext) {
        this(archive, ejbs, deploymentContext, null);
    }

    public RootBeanDeploymentArchive(ReadableArchive archive,
                                     Collection<EjbDescriptor> ejbs,
                                     DeploymentContext deploymentContext,
                                     String moduleBdaID) {
        super("root_" + (moduleBdaID != null? moduleBdaID : archive.getName()),
              new ArrayList<Class<?>>(),
              new ArrayList<URL>(),
              new ArrayList<EjbDescriptor>(),
              deploymentContext);
        createModuleBda(archive, ejbs, deploymentContext, moduleBdaID);
    }

    private void createModuleBda(ReadableArchive archive,
                                 Collection<EjbDescriptor> ejbs,
                                 DeploymentContext deploymentContext,
                                 String bdaId) {
        moduleBda = new BeanDeploymentArchiveImpl(archive, ejbs, deploymentContext, bdaId);

        // set the bda visibility for the root
        Collection<BeanDeploymentArchive> bdas = moduleBda.getBeanDeploymentArchives();
        for ( BeanDeploymentArchive oneBda : bdas ) {
            oneBda.getBeanDeploymentArchives().add( this );
            getBeanDeploymentArchives().add( oneBda );
        }

        moduleBda.getBeanDeploymentArchives().add( this );
        getBeanDeploymentArchives().add( moduleBda);
    }

    @Override
    public Collection<String> getBeanClasses() {
        return Collections.emptyList();
    }

    @Override
    public Collection<Class<?>> getBeanClassObjects() {
        return Collections.emptyList();
    }

    @Override
    public Collection<String> getModuleBeanClasses() {
        return Collections.emptyList();
    }

    @Override
    public Collection<Class<?>> getModuleBeanClassObjects() {
        return Collections.emptyList();
    }

    @Override
    public BeansXml getBeansXml() {
        return null;
    }

    @Override
    public WeldUtils.BDAType getBDAType() {
        //todo: this should return a root type
        return WeldUtils.BDAType.UNKNOWN;
    }

    @Override
    public ClassLoader getModuleClassLoaderForBDA() {
        return moduleBda.getModuleClassLoaderForBDA();
    }

    public BeanDeploymentArchive getModuleBda() {
        return moduleBda;
    }

    public WeldUtils.BDAType getModuleBDAType() {
        return moduleBda.getBDAType();
    }
}
