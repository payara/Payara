/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.persistence.jpa;

import com.sun.appserv.connectors.internal.api.ConnectorRuntime;
import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.BundleDescriptor;
import org.glassfish.deployment.common.RootDeploymentDescriptor;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.InstrumentableClassLoader;
import org.glassfish.api.deployment.OpsParams;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.persistence.common.DatabaseConstants;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.ClassTransformer;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/**
 * Implementation of ProviderContainerContractInfo while running on server.
 * @author Mitesh Meswani
 */
public class ServerProviderContainerContractInfo extends ProviderContainerContractInfoBase {

       private final DeploymentContext deploymentContext;
       private final ClassLoader finalClassLoader;
       private ValidatorFactory validatorFactory;
       boolean isDas;

       public ServerProviderContainerContractInfo(DeploymentContext deploymentContext, ConnectorRuntime connectorRuntime, boolean isDas) {
           super(connectorRuntime, deploymentContext);
           this.deploymentContext = deploymentContext;
           // Cache finalClassLoader as deploymentContext.getFinalClassLoader() is expected to be called only once during deployment.
           this.finalClassLoader = deploymentContext.getFinalClassLoader();
           this.isDas = isDas;
       }

      @Override
      public ClassLoader getClassLoader() {
           return finalClassLoader;
       }

       @Override
       public ClassLoader getTempClassloader() {
           return ( (InstrumentableClassLoader)deploymentContext.getClassLoader() ).copy();
       }

       @Override
       public void addTransformer(final ClassTransformer transformer) {
           // Bridge between java.lang.instrument.ClassFileTransformer that DeploymentContext accepts
           // and javax.persistence.spi.ClassTransformer that JPA supplies.
           deploymentContext.addTransformer(new ClassFileTransformer() {
               public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                       ProtectionDomain protectionDomain, byte[] classfileBuffer)
                       throws IllegalClassFormatException {
                   return transformer.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
               }
           });
       }

        @Override
        public String getApplicationLocation() {
           // Get source for current bundle. If it has not parent, it is the top level application
           // else continue traversing up till we find one with not parent.
           ReadableArchive archive = deploymentContext.getSource();
           boolean appRootFound = false;
           while (!appRootFound) {
               ReadableArchive parentArchive = archive.getParentArchive();
               if(parentArchive != null) {
                   archive = parentArchive;
               } else {
                   appRootFound = true;
               }
           }
           return archive.getURI().getPath();
       }

       @Override
       public ValidatorFactory getValidatorFactory() {
           // TODO Once discussion about BeanValidation in JavaEE is done, ValidatorFactory should be available from deployment context
           // We only create one validator factory per bundle.
           if (validatorFactory == null) {
               validatorFactory = Validation.buildDefaultValidatorFactory();
           }

           return validatorFactory;
       }

       @Override
       public boolean isJava2DBRequired() {
           OpsParams params = deploymentContext.getCommandParameters(OpsParams.class);
           // We only do java2db while being deployed on DAS. We do not do java2DB on load of an application or being deployed on an instance of a cluster
           return params.origin.isDeploy() && isDas;
       }

       @Override
       public DeploymentContext getDeploymentContext() {
           return deploymentContext;
       }

       @Override
       public void registerEMF(String unitName, String persistenceRootUri, RootDeploymentDescriptor containingBundle, EntityManagerFactory emf) {
           // We register the EMF into the bundle that declared the corresponding PU. This limits visibility of the emf
           // to containing module.
           // See EMFWrapper.lookupEntityManagerFactory() for corresponding look up logic
           if (containingBundle.isApplication()) {
               // ear level pu
               assert containingBundle instanceof Application;
               Application.class.cast(containingBundle).addEntityManagerFactory(
                       unitName, persistenceRootUri, emf);
           } else {
               assert containingBundle instanceof BundleDescriptor;
               BundleDescriptor.class.cast(containingBundle).addEntityManagerFactory(
                       unitName, emf);
           }
       }

       @Override
       public String getJTADataSourceOverride() {
           return deploymentContext.getTransientAppMetaData(DatabaseConstants.JTA_DATASOURCE_JNDI_NAME_OVERRIDE, String.class);
       }
}

