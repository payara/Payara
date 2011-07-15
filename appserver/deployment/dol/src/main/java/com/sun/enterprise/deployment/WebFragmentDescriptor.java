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

package com.sun.enterprise.deployment;

import java.util.List;
import java.util.Set;

import com.sun.enterprise.deployment.runtime.web.SunWebApp;
import com.sun.enterprise.deployment.web.EnvironmentEntry;
import com.sun.enterprise.deployment.web.LoginConfiguration;
import com.sun.enterprise.deployment.web.SecurityConstraint;
import com.sun.enterprise.deployment.web.ServletFilter;
import com.sun.enterprise.deployment.types.EjbReference;

/**
 * I am an object that represents all the deployment information about
 * a web fragment.
 *
 * @author Shing Wai Chan
 */

public class WebFragmentDescriptor extends WebBundleDescriptor
{
    private String jarName = null;
    private OrderingDescriptor ordering = null;

    /**
     * Constrct an empty web app [{0}].
     */
    public WebFragmentDescriptor() {
        super();
    }

    public String getJarName() {
        return jarName;
    }

    public void setJarName(String jarName) {
        this.jarName = jarName;
    }

    public OrderingDescriptor getOrderingDescriptor() {
        return ordering;
    }

    public void setOrderingDescriptor(OrderingDescriptor ordering) {
        this.ordering = ordering;
    }

    @Override
    protected WebComponentDescriptor combineWebComponentDescriptor(
            WebComponentDescriptor webComponentDescriptor) {

        WebComponentDescriptor resultDesc = null;
        String name = webComponentDescriptor.getCanonicalName();
        WebComponentDescriptor webCompDesc = getWebComponentByCanonicalName(name);

        if (webCompDesc != null) {
            resultDesc = webCompDesc;
            if (webCompDesc.isConflict(webComponentDescriptor, false)) {
                webCompDesc.setConflict(true);
            } else {
                // combine the contents of the given one to this one
                webCompDesc.add(webComponentDescriptor);
            }
        } else {
            resultDesc = webComponentDescriptor;
            this.getWebComponentDescriptors().add(webComponentDescriptor);
        }

        return resultDesc;
    }

    @Override
    protected void combineServletFilters(WebBundleDescriptor webBundleDescriptor) {
        for (ServletFilter servletFilter : webBundleDescriptor.getServletFilters()) {
            ServletFilterDescriptor servletFilterDesc = (ServletFilterDescriptor)servletFilter;
            String name = servletFilter.getName();
            ServletFilterDescriptor aServletFilterDesc = null;
            for (ServletFilter sf : getServletFilters()) {
                if (name.equals(sf.getName())) {
                    aServletFilterDesc = (ServletFilterDescriptor)sf;
                    break;
                }
            }

            if (aServletFilterDesc != null) {
                if (aServletFilterDesc.isConflict(servletFilterDesc)) {
                    aServletFilterDesc.setConflict(true);
                }
            } else {
                getServletFilters().add(servletFilterDesc);
            }
        }
    }

    @Override
    protected void combineServletFilterMappings(WebBundleDescriptor webBundleDescriptor) {
        getServletFilterMappings().addAll(webBundleDescriptor.getServletFilterMappings());
    }

    @Override
    protected void combineSecurityConstraints(Set<SecurityConstraint> firstScSet,
           Set<SecurityConstraint>secondScSet) {
        firstScSet.addAll(secondScSet);
    }

    @Override
    protected void combineLoginConfiguration(WebBundleDescriptor webBundleDescriptor) {
        if (getLoginConfiguration() == null) {
            setLoginConfiguration(webBundleDescriptor.getLoginConfiguration());
        } else {
            LoginConfiguration lgConf = webBundleDescriptor.getLoginConfiguration();
            if (lgConf != null && (!lgConf.equals(getLoginConfiguration()))) {
                conflictLoginConfig = true;
            }
        }
    }

    @Override
    protected void combineEnvironmentEntries(JndiNameEnvironment env) {
        for (Object oenve : env.getEnvironmentProperties()) {
            EnvironmentEntry enve = (EnvironmentEntry)oenve;
            EnvironmentProperty envProp = _getEnvironmentPropertyByName(enve.getName());
            if (envProp != null) {
                if (envProp.isConflict((EnvironmentProperty)enve)) {
                    conflictEnvironmentEntry = true;
                }
                unionInjectionTargets(envProp, (EnvironmentProperty)enve);
            } else {
                addEnvironmentEntry(enve);
            }
        }
    }

    @Override
    protected void combineEjbReferenceDescriptors(JndiNameEnvironment env) {
        for (Object oejbRef : env.getEjbReferenceDescriptors()) {
            EjbReference ejbRef = (EjbReference)oejbRef;
            EjbReferenceDescriptor ejbRefDesc =
                    (EjbReferenceDescriptor)_getEjbReference(ejbRef.getName());
            if (ejbRefDesc != null) {
                if (ejbRefDesc.isConflict((EjbReferenceDescriptor)ejbRef)) {
                    conflictEjbReference = true;
                }
                unionInjectionTargets(ejbRefDesc, (EnvironmentProperty)ejbRef);
            } else {
                addEjbReferenceDescriptor(ejbRef);
            }
        }
    }

    @Override
    protected void combineServiceReferenceDescriptors(JndiNameEnvironment env) {
        for (Object oserviceRef : env.getServiceReferenceDescriptors()) {
            ServiceReferenceDescriptor serviceRef =
                (ServiceReferenceDescriptor)oserviceRef;
            ServiceReferenceDescriptor sr = _getServiceReferenceByName(serviceRef.getName());
            if (sr != null) {
                if (sr.isConflict((ServiceReferenceDescriptor)serviceRef)) {
                    conflictServiceReference = true;
                }
                unionInjectionTargets(sr, serviceRef);
            } else {
                addServiceReferenceDescriptor(serviceRef);
            }
        }
    }

    @Override
    protected void combineResourceReferenceDescriptors(JndiNameEnvironment env) {
        for (Object oresRef : env.getResourceReferenceDescriptors()) {
            ResourceReferenceDescriptor resRef =
                (ResourceReferenceDescriptor)oresRef;
            ResourceReferenceDescriptor rrd = _getResourceReferenceByName(resRef.getName());
            if (rrd != null) {
                if (resRef.isConflict(rrd)) {
                    conflictResourceReference = true;
                }
                unionInjectionTargets(rrd, resRef);
            } else {
                addResourceReferenceDescriptor(resRef);
            }
        }
    }

    @Override
    protected void combineJmsDestinationReferenceDescriptors(JndiNameEnvironment env) {
        for (Object ojdRef : env.getJmsDestinationReferenceDescriptors()) {
            JmsDestinationReferenceDescriptor jdRef =
                (JmsDestinationReferenceDescriptor)ojdRef;
            JmsDestinationReferenceDescriptor jdr = _getJmsDestinationReferenceByName(jdRef.getName());
            if (jdr != null) {
                if (jdr.isConflict((JmsDestinationReferenceDescriptor)jdRef)) {
                    conflictJmsDestinationReference = true;
                }
                unionInjectionTargets(jdr, jdRef);   
            } else {
                addJmsDestinationReferenceDescriptor(jdRef);
            }
        }
    }

    @Override
    protected void combineMessageDestinationReferenceDescriptors(JndiNameEnvironment env) {
        for (Object omdRef : env.getMessageDestinationReferenceDescriptors()) {
            MessageDestinationReferenceDescriptor mdRef =
                (MessageDestinationReferenceDescriptor)omdRef;
            MessageDestinationReferenceDescriptor mdr =
                _getMessageDestinationReferenceByName(mdRef.getName());
            if (mdr != null) {
                if (mdr.isConflict(mdRef)) {
                    conflictMessageDestinationReference = true;
                }
                unionInjectionTargets(mdr, mdRef);
            } else {
                addMessageDestinationReferenceDescriptor(mdRef);
            }
        }
    }

    @Override
    protected void combineEntityManagerReferenceDescriptors(JndiNameEnvironment env) {
        for (EntityManagerReferenceDescriptor emRef :
                env.getEntityManagerReferenceDescriptors()) {
            EntityManagerReferenceDescriptor emr =
                _getEntityManagerReferenceByName(emRef.getName());
            if (emr != null) {
                if (emr.isConflict(emRef)) {
                    conflictEntityManagerReference = true;
                }
                unionInjectionTargets(emr, emRef);
            } else {
                addEntityManagerReferenceDescriptor(emRef);
            }
        }
    }

    @Override
     protected void combineEntityManagerFactoryReferenceDescriptors(JndiNameEnvironment env) {
        for (EntityManagerFactoryReferenceDescriptor emfRef :
                env.getEntityManagerFactoryReferenceDescriptors()) {
            EntityManagerFactoryReferenceDescriptor emfr =
                _getEntityManagerFactoryReferenceByName(emfRef.getName());
            if (emfr != null) {
                if (emfr.isConflict(emfRef)) {
                    conflictEntityManagerFactoryReference = true;
                }
                unionInjectionTargets(emfr, emfRef);
            } else {
                addEntityManagerFactoryReferenceDescriptor(emfRef);
            }
        }
    }

    @Override
    protected void combinePostConstructDescriptors(WebBundleDescriptor webBundleDescriptor) {
        getPostConstructDescriptors().addAll(webBundleDescriptor.getPostConstructDescriptors());
    }

    @Override
    protected void combinePreDestroyDescriptors(WebBundleDescriptor webBundleDescriptor) {
        getPreDestroyDescriptors().addAll(webBundleDescriptor.getPreDestroyDescriptors());
    }

    @Override
    protected void combineDataSourceDefinitionDescriptors(JndiNameEnvironment env) {
        for (DataSourceDefinitionDescriptor ddd: env.getDataSourceDefinitionDescriptors()) {
            DataSourceDefinitionDescriptor ddDesc = getDataSourceDefinitionDescriptor(ddd.getName());
            if (ddDesc != null) {
                if (ddDesc.isConflict(ddd)) {
                    conflictDataSourceDefinition = true;
                }
            } else {
                getDataSourceDefinitionDescriptors().add(ddd);
            }
        }
    }

    /**
     * Copy all injection targets from env2 to env1.
     *
     * @param env1
     * @param env2
     */
    private void unionInjectionTargets(EnvironmentProperty env1, EnvironmentProperty env2) {
        for (InjectionTarget injTarget: env2.getInjectionTargets()) {
            env1.addInjectionTarget(injTarget);
        }
    }

    /**
     * Return a formatted version as a String.
     */
    public void print(StringBuffer toStringBuffer) {
        toStringBuffer.append("\nWeb Fragment descriptor");
        toStringBuffer.append("\n");
        printCommon(toStringBuffer);
        if (jarName != null) {
            toStringBuffer.append("\njar name " + jarName);
        }
        if (ordering != null) {
            toStringBuffer.append("\nordering " + ordering);
        }
    }
}
