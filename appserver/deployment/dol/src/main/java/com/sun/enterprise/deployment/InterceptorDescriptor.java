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

import static com.sun.enterprise.deployment.LifecycleCallbackDescriptor.CallbackType;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.util.LocalStringManagerImpl;

import java.util.*;
import java.util.logging.Logger;

/**
 * Contains information about 1 Java EE interceptor.
 */ 

public class InterceptorDescriptor extends JndiEnvironmentRefsGroupDescriptor
{
    private static LocalStringManagerImpl localStrings =
	    new LocalStringManagerImpl(InterceptorDescriptor.class);

    private static final Logger _logger = DOLUtils.getDefaultLogger();

    private Set<LifecycleCallbackDescriptor> aroundInvokeDescriptors;
    private Set<LifecycleCallbackDescriptor> aroundTimeoutDescriptors;
    private String interceptorClassName;

    // Should ONLY be used for system-level interceptors whose class
    // is loaded by something other than the application class-loader
    private Class interceptorClass;


    // true if the AroundInvoke/AroundTimeout/Callback methods for this 
    // descriptor were defined on the bean class itself (or one of its
    // super-classes).  false if the methods are defined
    // on a separate interceptor class (or one of its super-classes).  
    private boolean fromBeanClass = false;

    public String getInterceptorClassName() {
        return interceptorClassName;
    }

    public void setInterceptorClassName(String className) {
        interceptorClassName = className;
    }


    public Class getInterceptorClass() {
        return interceptorClass;
    }

    // Should ONLY be used for system-level interceptors whose class
    // is loaded by something other than the application class-loader
    public void setInterceptorClass(Class c) {
        interceptorClass = c;
        setInterceptorClassName(c.getName());
    }


    public Set<LifecycleCallbackDescriptor> getAroundInvokeDescriptors() {
        if (aroundInvokeDescriptors == null) {
            aroundInvokeDescriptors =
                new HashSet<LifecycleCallbackDescriptor>(); 
        }
        return aroundInvokeDescriptors;
    }

    /**
     * Some clients need the AroundInvoke methods for this inheritance
     * hierarchy in the spec-defined "least derived --> most derived" order.
     */
    public List<LifecycleCallbackDescriptor> getOrderedAroundInvokeDescriptors
        (ClassLoader loader) throws Exception {

        return orderDescriptors(getAroundInvokeDescriptors(), loader);

    }

    public Set<LifecycleCallbackDescriptor> getAroundTimeoutDescriptors() {
        if (aroundTimeoutDescriptors == null) {
            aroundTimeoutDescriptors =
                new HashSet<LifecycleCallbackDescriptor>(); 
        }
        return aroundTimeoutDescriptors;
    }

    /**
     * Some clients need the AroundTimeout methods for this inheritance
     * hierarchy in the spec-defined "least derived --> most derived" order.
     */
    public List<LifecycleCallbackDescriptor> getOrderedAroundTimeoutDescriptors
        (ClassLoader loader) throws Exception {

        return orderDescriptors(getAroundTimeoutDescriptors(), loader);

    }

    public void setFromBeanClass(boolean flag) {
        fromBeanClass = flag;
    }

    public boolean getFromBeanClass() {
        return fromBeanClass;
    }

    public void addAroundInvokeDescriptor(LifecycleCallbackDescriptor aroundInvokeDesc) {
        Set<LifecycleCallbackDescriptor> aroundInvokeDescs =
            getAroundInvokeDescriptors();

        if (!knownLifecycleCallbackDescriptor(aroundInvokeDesc, aroundInvokeDescs)) {
            aroundInvokeDescs.add(aroundInvokeDesc);
        }
    }

    public void addAroundInvokeDescriptors(
        Set<LifecycleCallbackDescriptor> aroundInvokes) {
        for (LifecycleCallbackDescriptor ai : aroundInvokes) {
            addAroundInvokeDescriptor(ai);
        }
    }

    public boolean hasAroundInvokeDescriptor() {
        return (getAroundInvokeDescriptors().size() > 0);
    }

    public void addAroundTimeoutDescriptor(LifecycleCallbackDescriptor aroundTimeoutDesc) {
        Set<LifecycleCallbackDescriptor> aroundTimeoutDescs =
            getAroundTimeoutDescriptors();

        if (!knownLifecycleCallbackDescriptor(aroundTimeoutDesc, aroundTimeoutDescs)) {
            aroundTimeoutDescs.add(aroundTimeoutDesc);
        }
    }

    private boolean knownLifecycleCallbackDescriptor(
            LifecycleCallbackDescriptor desc, Set<LifecycleCallbackDescriptor> descs) {
        boolean found = false;

        for (LifecycleCallbackDescriptor ai : descs) {
            if ((desc.getLifecycleCallbackClass() != null) &&
                    desc.getLifecycleCallbackClass().equals(
                    ai.getLifecycleCallbackClass())) {
                found = true;
            }
        }

        return found;
    }

    public void addAroundTimeoutDescriptors(
        Set<LifecycleCallbackDescriptor> aroundTimeouts) {
        for (LifecycleCallbackDescriptor ai : aroundTimeouts) {
            addAroundTimeoutDescriptor(ai);
        }
    }

    public boolean hasAroundTimeoutDescriptor() {
        return (getAroundTimeoutDescriptors().size() > 0);
    }

    /**
     * Some clients need the Callback methods for this inheritance
     * hierarchy in the spec-defined "least derived --> most derived" order.
     */
    public List<LifecycleCallbackDescriptor> getOrderedCallbackDescriptors
        (CallbackType type, ClassLoader loader) throws Exception {

        return orderDescriptors(getCallbackDescriptors(type), loader);
    }

    public void addAroundConstructDescriptor(LifecycleCallbackDescriptor lcDesc) {
        addCallbackDescriptor(CallbackType.AROUND_CONSTRUCT, lcDesc);
    }

    public void addPostActivateDescriptor(LifecycleCallbackDescriptor lcDesc) {
        addCallbackDescriptor(CallbackType.POST_ACTIVATE, lcDesc);
    }

    public void addPrePassivateDescriptor(LifecycleCallbackDescriptor lcDesc) {
        addCallbackDescriptor(CallbackType.PRE_PASSIVATE, lcDesc);
    }

    /**
     * Order a set of lifecycle method descriptors for a particular
     * inheritance hierarchy with highest precedence assigned to the
     * least derived class. 
     */
    private List<LifecycleCallbackDescriptor> orderDescriptors
        (Set<LifecycleCallbackDescriptor> lcds, ClassLoader loader) 
        throws Exception 
    {

        LinkedList<LifecycleCallbackDescriptor> orderedDescs =
            new LinkedList<LifecycleCallbackDescriptor>();

        Map<String, LifecycleCallbackDescriptor> map =
            new HashMap<String, LifecycleCallbackDescriptor>();

        for(LifecycleCallbackDescriptor next : lcds) {
            map.put(next.getLifecycleCallbackClass(), next);
        }

        Class nextClass = loader.loadClass(getInterceptorClassName());

        while((nextClass != Object.class) && (nextClass != null)) {
            String nextClassName = nextClass.getName();
            if( map.containsKey(nextClassName) ) {
                LifecycleCallbackDescriptor lcd = map.get(nextClassName);
                orderedDescs.addFirst(lcd);
            }

            nextClass = nextClass.getSuperclass();
        }


        return orderedDescs;

    }

    public String toString() {
        return "InterceptorDescriptor class = " + getInterceptorClassName();
    }
}
