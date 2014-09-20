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

import javax.inject.Singleton;

/**
 * This is a service that has fields indicating whether or not
 * the various CDI extension points were properly reached
 * 
 * @author jwells
 *
 */
@Singleton
public class HK2ExtensionVerifier {
    private boolean afterBeanDiscoveryCalled = false;
    private boolean afterDeploymentValidationCalled = false;
    private boolean processAnnotatedTypeCalled = false;
    private boolean processInjectionTargetCalled = false;
    private boolean processProducerCalled = false;
    private boolean processManagedBeanCalled = false;
    private boolean processSessionBeanCalled = false;
    private boolean processProducerMethodCalled = false;
    private boolean processProducerFieldCalled = false;
    private boolean processObserverMethodCalled = false;
    
    public void afterBeanDiscoveryCalled() {
        afterBeanDiscoveryCalled = true;
    }
    
    public void afterDeploymentValidationCalled() {
        afterDeploymentValidationCalled = true;
    }
    
    public void processAnnotatedTypeCalled() {
        processAnnotatedTypeCalled = true;
    }
    
    public void processInjectionTargetCalled() {
        processInjectionTargetCalled = true;
    }
    
    public void processProducerCalled() {
        processProducerCalled = true;
    }
    
    public void processManagedBeanCalled() {
        processManagedBeanCalled = true;
    }
    
    public void processSessionBeanCalled() {
        processSessionBeanCalled = true;
    }
    
    public void processProducerMethodCalled() {
        processProducerMethodCalled = true;
    }
    
    public void processProducerFieldCalled() {
        processProducerFieldCalled = true;
    }
    
    public void processObserverMethodCalled() {
        processObserverMethodCalled = true;
    }
    
    public void validate() {
        if (!afterBeanDiscoveryCalled) {
            throw new AssertionError("AfterBeanDiscovery was not able to get the ServiceLocator");
        }
        
        if (!afterDeploymentValidationCalled) {
            throw new AssertionError("AfterDeploymentValidation was not able to get the ServiceLocator");
        }
        
        if (!processAnnotatedTypeCalled) {
            throw new AssertionError("ProcessAnnotatedType was not able to get the ServiceLocator");
        }
        
        if (!processInjectionTargetCalled) {
            throw new AssertionError("ProcessInjectionTarget was not able to get the ServiceLocator");
        }
        
        if (!processProducerCalled) {
            throw new AssertionError("ProcessProducer was not able to get the ServiceLocator");
        }
        
        if (!processManagedBeanCalled) {
            throw new AssertionError("ProcessManagedBean was not able to get the ServiceLocator");
        }
        
        if (!processSessionBeanCalled) {
            throw new AssertionError("ProcessSessionBean was not able to get the ServiceLocator");
        }
        
        if (!processProducerMethodCalled) {
            throw new AssertionError("ProcessProducerMethod was not able to get the ServiceLocator");
        }
        
        if (!processProducerFieldCalled) {
            throw new AssertionError("ProcessProducerField was not able to get the ServiceLocator");
        }
        
        if (!processObserverMethodCalled) {
            throw new AssertionError("ProcessObserverMethod was not able to get the ServiceLocator");
        }
    }
}
