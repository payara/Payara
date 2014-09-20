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

package com.sun.enterprise.tools.verifier.tests.ejb;

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Local interface should not be exposed through remote interface
 *
 * @author  Sheetal Vartak
 * @version 
 */
public class LocalInterfaceExposed extends EjbTest implements EjbCheck { 
    
    /**  
     * Bean interface type test.  
     * The bean provider must provide either Local or Remote or Both interfaces
     *   
     * @param descriptor the Enterprise Java Bean deployment descriptor   
     *
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {
        
        Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        
        if (!(descriptor instanceof EjbSessionDescriptor) &&
                !(descriptor instanceof EjbEntityDescriptor)) {
            addNaDetails(result, compName);
            result.notApplicable(smh.getLocalString
                            (getClass().getName()+".notApplicable1",
                            "Test apply only to session or entity beans."));
            return result;
        }
        
        EjbBundleDescriptorImpl bundle = descriptor.getEjbBundleDescriptor();
        Iterator<EjbDescriptor> iterator = (bundle.getEjbs()).iterator();
        Set<String> localInterfaces = new HashSet<String>();
        while(iterator.hasNext()) {
            EjbDescriptor entity = iterator.next();
            if (entity.getLocalClassName() != null) 
                localInterfaces.add(entity.getLocalClassName());
            localInterfaces.addAll(entity.getLocalBusinessClassNames());
        }
        ClassLoader jcl = getVerifierContext().getClassLoader();
        try { 
            Set<String> remoteInterfaces = new HashSet<String>();
            if(descriptor.getRemoteClassName()!=null)
                remoteInterfaces.add(descriptor.getRemoteClassName());
            remoteInterfaces.addAll(descriptor.getRemoteBusinessClassNames());
            
            for (String intf : remoteInterfaces) {
                Class c = Class.forName(intf, false, getVerifierContext().getClassLoader());
                Method[] methods = c.getDeclaredMethods();
                for(int i=0; i<methods.length; i++) {
                    //check all the local interfaces in the ejb bundle
                    for(Iterator itr = localInterfaces.iterator();itr.hasNext();) {
                        String localIntf = (String) itr.next();
                        Class returnType = methods[i].getReturnType();
                        if((getBaseComponentType(returnType).getName()).equals(localIntf) ||
                                (contains(methods[i].getParameterTypes(), localIntf))) {
                            addErrorDetails(result, compName);
                            result.failed(smh.getLocalString
                                    (getClass().getName() + ".failed",
                                    "Error : Local Interface [ {0} ] has been " +
                                    "exposed in remote interface [ {1} ]",
                                    new Object[] {localIntf, c.getName()}));
                            return result;
                        }
                    }
                } 
            }
            addGoodDetails(result, compName);
            result.passed(smh.getLocalString
                            (getClass().getName() + ".passed",
                            "Valid Remote interface."));
        } catch (ClassNotFoundException e) {
            Verifier.debug(e);
            addErrorDetails(result, compName);
            result.failed(smh.getLocalString
                            (getClass().getName() + ".failedException",
                            "Error: [ {0} ] class not found.",
                            new Object[] {descriptor.getRemoteClassName()}));
        }
        return result;
    }
    /** returns true if intf is contained in this args array */
    private boolean contains(Class[] args, String intf) {
        for (int i = 0; i < args.length; i++)
            if(getBaseComponentType(args[i]).getName().equals(intf))
                return true;
        
        return false;
    }

    /** This api recursively looks for class.getComponentType. This handles 
     *  cases where array of arrays are used. */
    private Class getBaseComponentType(Class cls) {
        if(!cls.isArray())
            return cls;
        return getBaseComponentType(cls.getComponentType());
    }
}

