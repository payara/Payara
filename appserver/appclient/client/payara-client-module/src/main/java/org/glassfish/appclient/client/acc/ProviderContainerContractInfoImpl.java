/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.appclient.client.acc;

import com.sun.appserv.connectors.internal.api.ConnectorRuntime;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.HashSet;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.ClassTransformer;
import javax.validation.ValidatorFactory;

import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.deployment.common.RootDeploymentDescriptor;
import org.glassfish.persistence.jpa.ProviderContainerContractInfoBase;

/**
 * Implements the internal GlassFish interface which all persistence provider
 * containers must.
 *
 * @author tjquinn
 */
public class ProviderContainerContractInfoImpl extends ProviderContainerContractInfoBase {

    private final ACCClassLoader classLoader;
    private final Instrumentation inst;
    private final String applicationLocation;

    private final Collection<EntityManagerFactory> emfs = new HashSet<EntityManagerFactory>();
    /**
     * Creates a new instance of the ACC's implementation of the contract.
     * The ACC uses its agent to register a VM transformer which can then
     * delegate to transformers registered with this class by the
     * persistence logic.
     *
     * @param classLoader ACC's class loader
     * @param inst VM's instrumentation object
     */
    public ProviderContainerContractInfoImpl(
            final ACCClassLoader classLoader,
            final Instrumentation inst,
            final String applicationLocation,
            final ConnectorRuntime connectorRuntime) {
        super(connectorRuntime);
        this.classLoader = classLoader;
        this.inst = inst;
        this.applicationLocation = applicationLocation;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public ClassLoader getTempClassloader() {
        return AccessController.doPrivileged(new PrivilegedAction<URLClassLoader>() {

                @Override
                public URLClassLoader run() {
                    return new URLClassLoader(classLoader.getURLs());
                }

            });
    }

    public void addTransformer(ClassTransformer transformer) {
        final TransformerWrapper tw = new TransformerWrapper(transformer, classLoader);
        if (inst != null) {
            inst.addTransformer(tw);
        } else {
            classLoader.addTransformer(tw);
        }
    }

    public String getApplicationLocation() {
        return applicationLocation;
    }

    public ValidatorFactory getValidatorFactory() {
        // TODO: Need to implement this correctly.
        return null;
    }

    // TODO: remove after persistence is refactored.
    public DeploymentContext getDeploymentContext() {
        return null;
    }

    public boolean isJava2DBRequired() {
        // Returns whether Java2DB is required or not. For an AppClient it is always false
        return false;
    }

    public void registerEMF(String unitName, String persistenceRootUri, RootDeploymentDescriptor containingBundle, EntityManagerFactory emf) {
        emfs.add(emf);
    }

    public String getJTADataSourceOverride() {
        // Returns whether JTA datasource is overridden. For an appclient it is never the case.
        return null;
    }

    public Collection<EntityManagerFactory> emfs() {
        return emfs;
    }

    /**
     * Wraps a persistence transformer in a java.lang.instrumentation.ClassFileTransformer
     * suitable for addition as a transformer to the JVM-provided instrumentation
     * class.
     */
    public static class TransformerWrapper implements ClassFileTransformer {

        private final ClassTransformer persistenceTransformer;
        private final ClassLoader classLoader;

        TransformerWrapper(final ClassTransformer persistenceTransformer,
                final ClassLoader classLoader) {
            this.persistenceTransformer = persistenceTransformer;
            this.classLoader = classLoader;
        }

        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
            /*
             * Do not even bother running the transformer unless the loader
             * loading the class is the ACC's class loader.
             */
            return (loader.equals(classLoader) ?
                persistenceTransformer.transform(loader, className,
                    classBeingRedefined, protectionDomain, classfileBuffer)
                : null);
        }
    }

}
