/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.hk2.devtest.cdi.ejb1;

import org.jvnet.hk2.annotations.Contract;

/**
 * This is a simple EJB interface that has methods used by the client
 * to ensure that each test has passed
 * 
 * @author jwells
 *
 */
@Contract
public interface BasicEjb {
    /**
     * Returns true if the CDI manager was properly injected
     * into the EJB that implements this interface
     * 
     * @return true if the CDI bean manager was injected into this EJB
     */
    public boolean cdiManagerInjected();
    
    /**
     * Returns true if an HK2 serviceLocator was properly injected
     * into the EJB that implements this interface
     * <p>
     * This demonstrates that HK2 services are being injected into
     * CDI created beans
     * 
     * @return true if the EJB was injected with an HK2 service locator
     */
    public boolean serviceLocatorInjected();
    
    /**
     * This uses the HK2 ServiceLocator to install the
     * BasicService descriptor into the injected HK2
     * ServiceLocator
     */
    public void installHK2Service();
    
    /**
     * This method ensures that the HK2 service installed with
     * {@link #installHK2Service()} can be injected with
     * CDI bean instances
     * <p>
     * This demonstrates that services created with HK2 can be
     * injected with beans created with CDI
     * 
     * @return true if the BasicService HK2 service was injected
     * with a CDI bean instance
     */
    public boolean hk2ServiceInjectedWithEjb();
    
    /**
     * Returns without throwing an error if all of the CDI extension
     * events had proper access to the ServiceLocator in JNDI
     * 
     * @throws AssertionError if any of the extension points did not have
     * access to the ServiceLocator
     */
    public void isServiceLocatorAvailableInAllCDIExtensionEvents();
    
    /**
     * This method ensures that the CustomScopedEJB gets properly injected
     * with the HK2 service
     */
    public void isEJBWithCustomHK2ScopeProperlyInjected();
    
    /**
     * Tests that an implementation of PopulatorPostProcessor put into
     * META-INF/services runs properly
     */
    public void doesApplicationDefinedPopulatorPostProcessorRun();
    
    /**
     * Tests that a service added via an HK2 {@link JustInTimeResolver}
     * is properly added to the CDI bridge
     */
    public void isServiceAddedWithJITResolverAdded();
    
    /**
     * Checks that an ApplicationScoped CDI service can be injected into
     * an HK2 service
     */
    public void checkApplicationScopedServiceInjectedIntoHk2Service();
    

}
