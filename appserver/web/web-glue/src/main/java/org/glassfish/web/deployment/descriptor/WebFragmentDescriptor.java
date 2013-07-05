/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.web.deployment.descriptor;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.types.EjbReference;
import com.sun.enterprise.deployment.web.EnvironmentEntry;
import com.sun.enterprise.deployment.web.LoginConfiguration;
import com.sun.enterprise.deployment.web.MimeMapping;
import com.sun.enterprise.deployment.web.SecurityConstraint;
import com.sun.enterprise.deployment.web.ServletFilter;
import org.glassfish.deployment.common.JavaEEResourceType;

import java.util.HashSet;
import java.util.Set;

/**
 * I am an object that represents all the deployment information about
 * a web fragment.
 *
 * @author Shing Wai Chan
 */

public class WebFragmentDescriptor extends WebBundleDescriptorImpl
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
                webCompDesc.add(webComponentDescriptor, true, true);
            }
        } else {
            resultDesc = webComponentDescriptor;
            this.getWebComponentDescriptors().add(webComponentDescriptor);
        }

        return resultDesc;
    }

    @Override
    protected void combineMimeMappings(Set<MimeMapping> mimeMappings) {
        // do not call getMimeMappingsSet().addAll() as there is special overriding rule
        for (MimeMapping mimeMap : mimeMappings) {
            if (!mimeMap.getMimeType().equals(addMimeMapping(mimeMap))) {
                getConflictedMimeMappingExtensions().add(mimeMap.getExtension());
            }
        }
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
                combineInjectionTargets(envProp, (EnvironmentProperty)enve);
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
                combineInjectionTargets(ejbRefDesc, (EnvironmentProperty)ejbRef);
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
                combineInjectionTargets(sr, serviceRef);
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
                combineInjectionTargets(rrd, resRef);
            } else {
                addResourceReferenceDescriptor(resRef);
            }
        }
    }

    @Override
    protected void combineResourceEnvReferenceDescriptors(JndiNameEnvironment env) {
        for (Object ojdRef : env.getResourceEnvReferenceDescriptors()) {
            ResourceEnvReferenceDescriptor jdRef =
                (ResourceEnvReferenceDescriptor)ojdRef;
            ResourceEnvReferenceDescriptor jdr = _getResourceEnvReferenceByName(jdRef.getName());
            if (jdr != null) {
                if (jdr.isConflict((ResourceEnvReferenceDescriptor)jdRef)) {
                    conflictResourceEnvReference = true;
                }
                combineInjectionTargets(jdr, jdRef);   
            } else {
                addResourceEnvReferenceDescriptor(jdRef);
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
                combineInjectionTargets(mdr, mdRef);
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
                combineInjectionTargets(emr, emRef);
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
                combineInjectionTargets(emfr, emfRef);
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

    @Override
    protected void combineResourceDescriptors(JndiNameEnvironment env,JavaEEResourceType javaEEResourceType) {
        for (ResourceDescriptor ddd: env.getResourceDescriptors(javaEEResourceType)) {
            ResourceDescriptor descriptor = getResourceDescriptor(javaEEResourceType, ddd.getName());
            if (descriptor != null) {
                if (descriptor.getResourceType().equals(JavaEEResourceType.DSD) &&
                        ((DataSourceDefinitionDescriptor)descriptor).isConflict((DataSourceDefinitionDescriptor)ddd)) {
                    conflictDataSourceDefinition = true;
                } else if (descriptor.getResourceType().equals(JavaEEResourceType.MSD) &&
                        ((MailSessionDescriptor)descriptor).isConflict((MailSessionDescriptor)ddd)) {
                    conflictMailSessionDefinition = true;
                } else if (descriptor.getResourceType().equals(JavaEEResourceType.AODD) &&
                        ((AdministeredObjectDefinitionDescriptor)descriptor).isConflict((AdministeredObjectDefinitionDescriptor)ddd)) {
                    conflictAdminObjectDefinition = true;
                } else if (descriptor.getResourceType().equals(JavaEEResourceType.CFD) &&
                        ((ConnectionFactoryDefinitionDescriptor)descriptor).isConflict((ConnectionFactoryDefinitionDescriptor)ddd)) {
                    conflictConnectionFactoryDefinition = true;
                } else if (descriptor.getResourceType().equals(JavaEEResourceType.JMSCFDD) &&
                        ((JMSConnectionFactoryDefinitionDescriptor)descriptor).isConflict((JMSConnectionFactoryDefinitionDescriptor)ddd)) {
                    conflictJMSConnectionFactoryDefinition = true;
                } else if (descriptor.getResourceType().equals(JavaEEResourceType.JMSDD) &&
                        ((JMSDestinationDefinitionDescriptor)descriptor).isConflict((JMSDestinationDefinitionDescriptor)ddd)) {
                    conflictJMSDestinationDefinition = true;
                }
            } else {
                getResourceDescriptors(javaEEResourceType).add(ddd);
            }
        }
    }
}
