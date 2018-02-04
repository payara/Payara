/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.jvnet.hk2.config;

import org.glassfish.hk2.api.Descriptor;
import org.glassfish.hk2.api.DescriptorType;
import org.glassfish.hk2.api.DescriptorVisibility;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.utilities.AbstractActiveDescriptor;
import org.jvnet.hk2.config.provider.internal.Creator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: makannan
 * Date: 4/28/12
 * Time: 3:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class DomDescriptor<T>
    extends AbstractActiveDescriptor<T> {
    /**
     * For serialization
     */
    //private static final long serialVersionUID = -9196390718074767455L;

    private Dom theDom;

    private Creator<T> creator;
    
    private T theOne;

    /**
     * For serializable
     */
    public DomDescriptor() {
        super();
    }

    /**
     * Creates the constant descriptor
     *
     * @param theDom May not be null
     * @param advertisedContracts
     * @param scope
     * @param name
     * @param qualifiers
     */
    public DomDescriptor(Dom theDom, Set<Type> advertisedContracts,
                        Class<? extends Annotation> scope, String name,
                        Set<Annotation> qualifiers) {
        super(advertisedContracts,
                scope,
                name,
                qualifiers,
                DescriptorType.CLASS,
                DescriptorVisibility.NORMAL,
                0,
                null,
                null,
                null,
                null);
        super.addAdvertisedContract(ConfigBeanProxy.class.getName());
        if (theDom == null) throw new IllegalArgumentException();

        this.theDom = theDom;
        setImplementation(theDom.getClass().getName());
    }

    /* (non-Javadoc)
     * @see org.glassfish.hk2.api.Descriptor#getImplementation()
     */
    @Override
    public String getImplementation() {
        return theDom.getClass().getName();
    }

    /* (non-Javadoc)
     * @see org.glassfish.hk2.api.ActiveDescriptor#getImplementationClass()
     */
    @Override
    public Class<?> getImplementationClass() {
        return theDom.getClass();
    }

    @Override
    public Type getImplementationType() {
        return theDom.getClass();
    }

    @Override
    public void setImplementationType(Type t) {
        throw new AssertionError("May not set type of DomDescriptor");
    }

    /* (non-Javadoc)
     * @see org.glassfish.hk2.api.ActiveDescriptor#create(org.glassfish.hk2.api.ServiceHandle)
     */
    @Override
    public T create(ServiceHandle<?> root) {
        initTheOne();
        return theOne;
    }

    private void initTheOne() {
        if (theOne == null) {
            Class c = theDom.getImplementationClass();
            creator = (ConfigBeanProxy.class.isAssignableFrom(c)
                    ? new DomProxyCreator(c, theDom)
                    : new ConfiguredCreator(theDom.createCreator(c), theDom));
        }

        theOne = creator.create();
    }

    @Override
    public boolean equals(Object a) {
        if (a instanceof DomDescriptor && super.equals(a)) {
            DomDescriptor other = (DomDescriptor) a;
            return theDom.equals(other.theDom);
            
        }
        return false;
    }

    public int hashCode() {
        return System.identityHashCode(this);
    }
}
