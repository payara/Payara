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
package com.oracle.hk2.devtest.cdi.extension;

import java.io.File;
import java.io.IOException;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.enterprise.inject.spi.ProcessManagedBean;
import javax.enterprise.inject.spi.ProcessObserverMethod;
import javax.enterprise.inject.spi.ProcessProducer;
import javax.enterprise.inject.spi.ProcessProducerField;
import javax.enterprise.inject.spi.ProcessProducerMethod;
import javax.enterprise.inject.spi.ProcessSessionBean;
import javax.inject.Singleton;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.glassfish.hk2.api.Descriptor;
import org.glassfish.hk2.api.HK2Loader;
import org.glassfish.hk2.api.MultiException;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;

/**
 * This extension is used to ensure that the ServiceLocator
 * is availble via JNDI in all of the extension callbacks
 * 
 * @author jwells
 *
 */
public class CDIExtension implements Extension {
    private final static String FILE_PREFIX = "destroyed-";
    private final static String FILE_POSTFIX = ".txt";
    private final static String JNDI_APP_NAME = "java:app/AppName";
    private final static String JNDI_LOCATOR_NAME = "java:app/hk2/ServiceLocator";
    
    private File createDestructionFileObject() {
        try {
            Context context = new InitialContext();
            
            String appName = (String) context.lookup(JNDI_APP_NAME);
            
            return new File(FILE_PREFIX + appName + FILE_POSTFIX);
        }
        catch (NamingException ne) {
            return null;
        }
    }
    
    private ServiceLocator getServiceLocator() {
        try {
            Context context = new InitialContext();
            
            return (ServiceLocator) context.lookup(JNDI_LOCATOR_NAME);
        }
        catch (NamingException ne) {
            return null;
        }
        
    }
    
    /**
     * This method will ensure that the file which indicates that the
     * application has shut down properly has been removed and then
     * adds the HK2 service to the system
     * 
     * @param beforeBeanDiscovery
     */
    @SuppressWarnings("unused")
    private void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscovery) {
        File destructoFile = createDestructionFileObject();
        if (destructoFile == null) return;
        
        if (destructoFile.exists()) {
            if (destructoFile.delete() == false) return;
        }
        
        ServiceLocator locator = getServiceLocator();
        if (locator == null) return;
        
        Descriptor d = BuilderHelper.link(HK2ExtensionVerifier.class).
                in(Singleton.class.getName()).
                andLoadWith(new HK2LoaderImpl()).
                build();
        
        // Just having the service present is enough for the first callback
        ServiceLocatorUtilities.addOneDescriptor(locator, d);
    }
    
    /**
     * This method will ensure that the file which indicates that the
     * application has shut down properly has been removed and then
     * adds the HK2 service to the system
     * 
     * @param beforeBeanDiscovery
     */
    @SuppressWarnings("unused")
    private void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscovery) {
        ServiceLocator locator = getServiceLocator();
        if (locator == null) return;
        
        HK2ExtensionVerifier verifier = locator.getService(HK2ExtensionVerifier.class);
        verifier.afterBeanDiscoveryCalled();
    }
    
    @SuppressWarnings("unused")
    private void afterDeploymentValidation(@Observes AfterDeploymentValidation afterDeploymentValidation) {
        ServiceLocator locator = getServiceLocator();
        if (locator == null) return;
        
        HK2ExtensionVerifier verifier = locator.getService(HK2ExtensionVerifier.class);
        verifier.afterDeploymentValidationCalled();
    }
    
    /**
     * This one is a little different, as it cannot use the application to
     * communicate success or failure.  Instead it writes out a file that
     * the test will look for after the application has been undeployed
     * 
     * @param beforeShutdown
     */
    @SuppressWarnings("unused")
    private void beforeShutdown(@Observes BeforeShutdown beforeShutdown) {
        ServiceLocator locator = getServiceLocator();
        if (locator == null) return;
        
        File destructoFile = createDestructionFileObject();
        try {
            destructoFile.createNewFile();
        }
        catch (IOException ioe) {
            System.err.println("ERROR:  Failed to create file " + destructoFile.getAbsolutePath());
            ioe.printStackTrace();
        }
    }
    
    @SuppressWarnings("unused")
    private <T> void processAnnotatedType(@Observes ProcessAnnotatedType<T> processAnnotatedType) {
        ServiceLocator locator = getServiceLocator();
        if (locator == null) return;
        
        HK2ExtensionVerifier verifier = locator.getService(HK2ExtensionVerifier.class);
        verifier.processAnnotatedTypeCalled();
    }
    
    @SuppressWarnings("unused")
    private <T> void processInjectionTarget(@Observes ProcessInjectionTarget<T> processInjectionTarget) {
        ServiceLocator locator = getServiceLocator();
        if (locator == null) return;
        
        HK2ExtensionVerifier verifier = locator.getService(HK2ExtensionVerifier.class);
        verifier.processInjectionTargetCalled();
    }
    
    @SuppressWarnings("unused")
    private <T, X> void processProducer(@Observes ProcessProducer<T, X> processProducer) {
        ServiceLocator locator = getServiceLocator();
        if (locator == null) return;
        
        HK2ExtensionVerifier verifier = locator.getService(HK2ExtensionVerifier.class);
        verifier.processProducerCalled();
    }
    
    @SuppressWarnings("unused")
    private <T> void processManagedBean(@Observes ProcessManagedBean<T> processManagedBean) {
        ServiceLocator locator = getServiceLocator();
        if (locator == null) return;
        
        HK2ExtensionVerifier verifier = locator.getService(HK2ExtensionVerifier.class);
        verifier.processManagedBeanCalled();
    }
    
    @SuppressWarnings("unused")
    private <T> void processSessionBean(@Observes ProcessSessionBean<T> processSessionBean) {
        ServiceLocator locator = getServiceLocator();
        if (locator == null) return;
        
        HK2ExtensionVerifier verifier = locator.getService(HK2ExtensionVerifier.class);
        verifier.processSessionBeanCalled();
    }
    
    @SuppressWarnings("unused")
    private <T, X> void processProducerMethod(@Observes ProcessProducerMethod<T, X> processProducerMethod) {
        ServiceLocator locator = getServiceLocator();
        if (locator == null) return;
        
        HK2ExtensionVerifier verifier = locator.getService(HK2ExtensionVerifier.class);
        verifier.processProducerMethodCalled();
    }
    
    @SuppressWarnings("unused")
    private <T, X> void processProducerField(@Observes ProcessProducerField<T, X> processProducerField) {
        ServiceLocator locator = getServiceLocator();
        if (locator == null) return;
        
        HK2ExtensionVerifier verifier = locator.getService(HK2ExtensionVerifier.class);
        verifier.processProducerFieldCalled();
    }
    
    @SuppressWarnings("unused")
    private <T, X> void processObserverMethod(@Observes ProcessObserverMethod<T, X> processObserverMethod) {
        ServiceLocator locator = getServiceLocator();
        if (locator == null) return;
        
        HK2ExtensionVerifier verifier = locator.getService(HK2ExtensionVerifier.class);
        verifier.processObserverMethodCalled();
    }
    
    private class HK2LoaderImpl implements HK2Loader {
        private final ClassLoader loader = getClass().getClassLoader();

        @Override
        public Class<?> loadClass(String className) throws MultiException {
            try {
                return loader.loadClass(className);
            }
            catch (Throwable th) {
                throw new MultiException(th);
            }
        }
        
    }

}
