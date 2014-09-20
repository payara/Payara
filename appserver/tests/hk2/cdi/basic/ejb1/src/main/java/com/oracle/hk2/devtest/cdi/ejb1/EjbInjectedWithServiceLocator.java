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

import java.util.List;
import java.util.Map;

import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.DynamicConfigurationService;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.BuilderHelper;

import com.oracle.hk2.devtest.cdi.ejb1.ppp.ApplicationPopulatorPostProcessor;
import com.oracle.hk2.devtest.cdi.ejb1.scoped.CountingApplicationScopedCDIService;
import com.oracle.hk2.devtest.cdi.ejb1.scoped.CustomScopedEjb;
import com.oracle.hk2.devtest.cdi.ejb1.scoped.HK2PerLookupInjectedWithCDIApplicationScoped;
import com.oracle.hk2.devtest.cdi.ejb1.scoped.HK2Service;
import com.oracle.hk2.devtest.cdi.extension.HK2ExtensionVerifier;
import com.oracle.hk2.devtest.cdi.jit.CDIServiceInjectedWithHK2Service;
import com.oracle.hk2.devtest.cdi.locator.BasicService;

/**
 * Simple EJB created by CDI that injects a HK2 ServiceLocator!
 * 
 * @author jwells
 */
@Stateless
@Remote(BasicEjb.class)
public class EjbInjectedWithServiceLocator implements BasicEjb {
    @Inject
    private BeanManager beanManager;
    
    @Inject
    private ServiceLocator locator;
    
    @Inject
    private CustomScopedEjb customScopedEjb;
    
    @Inject
    private CDIServiceInjectedWithHK2Service cdiInjectedWithHK2Service;

    @Override
    public boolean cdiManagerInjected() {
        return (beanManager != null);
    }

    @Override
    public boolean serviceLocatorInjected() {
        return (locator != null);
    }

    @Override
    public void installHK2Service() {
        DynamicConfigurationService dcs = locator.getService(DynamicConfigurationService.class);
        DynamicConfiguration config = dcs.createDynamicConfiguration();
        
        config.addActiveDescriptor(BasicService.class);
        
        config.commit(); 
    }

    @Override
    public boolean hk2ServiceInjectedWithEjb() {
        BasicService bs = locator.getService(BasicService.class);
        if (bs == null) {
            throw new RuntimeException("Could not find BasicService in locator " + locator);
        }
        
        boolean retVal = bs.gotInjectedWithBeanManager();
        return retVal;
    }

    @Override
    public void isServiceLocatorAvailableInAllCDIExtensionEvents() {
        HK2ExtensionVerifier verifier = locator.getService(HK2ExtensionVerifier.class);
        
        verifier.validate();
    }

    @Override
    public void isEJBWithCustomHK2ScopeProperlyInjected() {
        customScopedEjb.checkMe();
        
    }

    @Override
    public void doesApplicationDefinedPopulatorPostProcessorRun() {
        ActiveDescriptor<?> descriptor = locator.getBestDescriptor(BuilderHelper.createContractFilter(HK2Service.class.getName()));
        Map<String, List<String>> metadata = descriptor.getMetadata();  // An NPE means we don't have the descriptor
        
        List<String> values = metadata.get(ApplicationPopulatorPostProcessor.KEY);
        if (!values.get(0).equals(ApplicationPopulatorPostProcessor.VALUE)) {
            throw new AssertionError("Incorrect value 0: " + values.get(0));
        }
    }

    @Override
    public void isServiceAddedWithJITResolverAdded() {
        if (!cdiInjectedWithHK2Service.hasHK2Service()) {
            throw new AssertionError("cdiInjectedWithHK2Service is not valid: " + cdiInjectedWithHK2Service.hasHK2Service());
        }
    }

    @Override
    public void checkApplicationScopedServiceInjectedIntoHk2Service() {
        HK2PerLookupInjectedWithCDIApplicationScoped hk2Service1 = locator.getService(
                HK2PerLookupInjectedWithCDIApplicationScoped.class);
        HK2PerLookupInjectedWithCDIApplicationScoped hk2Service2 = locator.getService(
                HK2PerLookupInjectedWithCDIApplicationScoped.class);
        HK2PerLookupInjectedWithCDIApplicationScoped hk2Service3 = locator.getService(
                HK2PerLookupInjectedWithCDIApplicationScoped.class);
        
        CountingApplicationScopedCDIService cdiService1 = hk2Service1.getCountingCDIService();
        CountingApplicationScopedCDIService cdiService2 = hk2Service2.getCountingCDIService();
        CountingApplicationScopedCDIService cdiService3 = hk2Service3.getCountingCDIService();
        
        if (1 != cdiService1.getNumberOfTimesMethodCalled()) {
            throw new AssertionError("Did not get 1 for first call");
        }
        
        if (2 != cdiService2.getNumberOfTimesMethodCalled()) {
            throw new AssertionError("Did not get 2 for second call (not the same instance) " + cdiService1 + "/" + cdiService2 + "/" + cdiService3);
        }

        if (3 != cdiService3.getNumberOfTimesMethodCalled()) {
            throw new AssertionError("Did not get 3 for second call (not the same instance) " + cdiService1 + "/" + cdiService2 + "/" + cdiService3);
        }
        
        int constructorCount1 = cdiService1.getConstructedCount();
        if (constructorCount1 > 2) {  // One for the proxy, one for the true object
            throw new AssertionError("counstructorCount1=" + constructorCount1 + " it should less than 2");
        }
        
        int constructorCount2 = cdiService2.getConstructedCount();
        if (constructorCount1 > 2) {  // One for the proxy, one for the true object
            throw new AssertionError("counstructorCount2=" + constructorCount2 + " it should be less than 2");
        }
        
        int constructorCount3 = cdiService3.getConstructedCount();
        if (constructorCount1 > 2) {  // One for the proxy, one for the true object
            throw new AssertionError("counstructorCount3=" + constructorCount3 + " it should be less than 2");
        }
    }    
}
