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

import com.sun.enterprise.deployment.util.DOLUtils;

/**
 * I am an object representing a dependency on a resource environment
 * @author Kenneth Saks
 */

public class ResourceEnvReferenceDescriptor extends EnvironmentProperty implements NamedDescriptor, ResourceEnvReference { 

    static private final int NULL_HASH_CODE = Integer.valueOf(1).hashCode();

    private String refType;

    private boolean isManagedBean = false;
    private ManagedBeanDescriptor managedBeanDesc;

    private static final String SESSION_CTX_TYPE = "javax.ejb.SessionContext";
    private static final String MDB_CTX_TYPE ="javax.ejb.MessageDrivenContext";
    private static final String EJB_CTX_TYPE ="javax.ejb.EJBContext";
    private static final String EJB_TIMER_SERVICE_TYPE
        = "javax.ejb.TimerService";
    private static final String VALIDATION_VALIDATOR ="javax.validation.Validator";
    private static final String VALIDATION_VALIDATOR_FACTORY ="javax.validation.ValidatorFactory";

    private static final String CDI_BEAN_MANAGER_TYPE = "javax.enterprise.inject.spi.BeanManager";

    public ResourceEnvReferenceDescriptor() {
    }
    
    public ResourceEnvReferenceDescriptor(String name, String description, String refType) {
        super(name, "", description);
        this.refType = refType;
    }

    public void setRefType(String refType) {
        this.refType = refType;

    }

    public String getRefType() {
        return this.refType;
    }

    public String getInjectResourceType() {
        return getRefType();
    }

    public void setInjectResourceType(String refType) {
        setRefType(refType);
    }

    public void setIsManagedBean(boolean flag) {
        isManagedBean = flag;
    }

    public boolean isManagedBean() {
        return isManagedBean;
    }

    public void setManagedBeanDescriptor(ManagedBeanDescriptor desc) {
        managedBeanDesc = desc;
    }

    public ManagedBeanDescriptor getManagedBeanDescriptor() {
        return managedBeanDesc;
    }
    
   /**
    * Return the jndi name of the destination to which I refer.
    */
    public String getJndiName() {
        String jndiName = this.getValue();
        if (jndiName != null  && ! jndiName.equals("")) {
            return jndiName;
        }
        if (mappedName != null && ! mappedName.equals("")) {
            return mappedName;
        }
        return lookupName;
    }

   /**
    * Sets the jndi name of the destination to which I refer
    */
    public void setJndiName(String jndiName) {
        this.setValue(jndiName);
    }

    public boolean isEJBContext() {
        return (getRefType().equals(SESSION_CTX_TYPE) ||
                getRefType().equals(MDB_CTX_TYPE) ||
                getRefType().equals(EJB_CTX_TYPE) ||
                getRefType().equals(EJB_TIMER_SERVICE_TYPE));
    }

    public boolean isValidator() {
        return (getRefType().equals(VALIDATION_VALIDATOR));
    }

    public boolean isValidatorFactory() {
        return (getRefType().equals(VALIDATION_VALIDATOR_FACTORY));
    }

    public boolean isCDIBeanManager() {
        return (getRefType().equals(CDI_BEAN_MANAGER_TYPE));
    }

    public boolean isConflict(ResourceReferenceDescriptor other) {
        return (getName().equals(other.getName())) &&
            (!DOLUtils.equals(getType(), other.getType())
            || isConflictResourceGroup(other));
    }

    /* Equality on name. */
    public boolean equals(Object object) {
        if (object instanceof ResourceEnvReference) {
            ResourceEnvReference destReference = (ResourceEnvReference) object;
            return destReference.getName().equals(this.getName());
        }
        return false;
    }

    public int hashCode() {
        int result = NULL_HASH_CODE;
        String name = getName();
        if (name != null) {
            result += name.hashCode();
        }
        return result;
    }
    /**
     * Performs the same check as in ResourceReferenceDescriptor
     */
    public void checkType() {
        if (refType == null) {
            if (isBoundsChecking()) {
                throw new IllegalArgumentException(localStrings.getLocalString(
                        "enterprise.deployment.exceptiontypenotallowedpropertytype",
                        "{0} is not an allowed property value type",
                        new Object[]{"null"}));
            }
        }
        if (refType != null) {
            try {
                Class.forName(refType, true, Thread.currentThread().getContextClassLoader());
            } catch (Throwable t) {
                if (isBoundsChecking()) {
                    throw new IllegalArgumentException(localStrings.getLocalString(
                            "enterprise.deployment.exceptiontypenotallowedpropertytype",
                            "{0} is not an allowed property value type",
                            new Object[]{refType}));
                } else {
                    return;
                }
            }
        }
    }
}
