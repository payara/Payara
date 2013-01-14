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

package com.sun.enterprise.deployment;

import com.sun.enterprise.deployment.types.EntityManagerReference;
import com.sun.enterprise.deployment.util.DOLUtils;

import javax.persistence.PersistenceContextType;
import javax.persistence.SynchronizationType;
import java.util.HashMap;
import java.util.Map;

/**
 * An object representing an component environment reference 
 * to an EntityManager
 *
*/
public class EntityManagerReferenceDescriptor extends 
    EnvironmentProperty implements EntityManagerReference {

    private String unitName = null;
    private PersistenceContextType contextType = PersistenceContextType.TRANSACTION;
    private SynchronizationType synchronizationType = SynchronizationType.SYNCHRONIZED;
    private BundleDescriptor referringBundle;

    private Map<String, String> properties = new HashMap<String,String>();

    public EntityManagerReferenceDescriptor(String name, 
                                            String unitName,
                                            PersistenceContextType type) {
        super(name, "", "");

        this.unitName = unitName;
        this.contextType = type;
    }

    public EntityManagerReferenceDescriptor() {}

    public void setUnitName(String unitName) {
        
        this.unitName = unitName;
    }

    public String getUnitName() {

        return unitName;

    }

    public String getInjectResourceType() {
        return "javax.persistence.EntityManager";
    }

    public void setInjectResourceType(String resourceType) {
    }

    public void setPersistenceContextType(PersistenceContextType type) {

        contextType = type;

    }

    public PersistenceContextType getPersistenceContextType() {

        return contextType;

    }


    public SynchronizationType getSynchronizationType() {
        return synchronizationType;
    }


    public void setSynchronizationType(SynchronizationType synchronizationType) {
        this.synchronizationType = synchronizationType;
    }

    public void addProperty(String name, String value) {
        properties.put(name, value);
    }

    public Map<String,String> getProperties() {
        return new HashMap<String,String>(properties);
    }

    public void setReferringBundleDescriptor(BundleDescriptor referringBundle)
    {
	this.referringBundle = referringBundle;
    }

    public BundleDescriptor getReferringBundleDescriptor()
    {
	return referringBundle;
    }  

    public boolean isConflict(EntityManagerReferenceDescriptor other) {
        return getName().equals(other.getName()) &&
            (!(
                DOLUtils.equals(getUnitName(), other.getUnitName()) &&
                DOLUtils.equals(getPersistenceContextType(), other.getPersistenceContextType()) &&
                properties.equals(other.properties)
                ) ||
            isConflictResourceGroup(other));
    }
}

