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

package org.glassfish.ejb.deployment.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.enterprise.deployment.EjbInterceptor;
import com.sun.enterprise.deployment.MethodDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl;
import org.glassfish.ejb.deployment.descriptor.InterceptorBindingDescriptor;
import org.glassfish.ejb.deployment.descriptor.InterceptorBindingDescriptor.BindingType;

public class InterceptorBindingTranslator {

    private List<InterceptorBindingDescriptor> interceptorBindings;
    private EjbBundleDescriptorImpl ejbBundle;

    private List<String> defaultInterceptorChain = 
        new LinkedList<String>();

    private List<String> classInterceptorChain =
        new LinkedList<String>();

    private boolean hasTotalClassLevelOrdering = false;
    private List<String> totalClassLevelOrdering =
        new LinkedList<String>();

    private Map<MethodDescriptor, LinkedList<String>> methodInterceptorsMap = 
        new HashMap<MethodDescriptor, LinkedList<String>>();

    // true if there are 0 bindings.  
    private boolean isEmpty;

    public InterceptorBindingTranslator(EjbBundleDescriptorImpl bundle) {

        ejbBundle = bundle;
        interceptorBindings = ejbBundle.getInterceptorBindings();

        if( interceptorBindings.isEmpty() ) {

            isEmpty = true;

        } else {

            validateInterceptors();

        }

        
    }

    public TranslationResults apply(String ejbName) {

        if( isEmpty ) {
            return new TranslationResults();
        }

        defaultInterceptorChain.clear();
        classInterceptorChain.clear();

        hasTotalClassLevelOrdering = false;
        totalClassLevelOrdering.clear();

        methodInterceptorsMap.clear();

        // Do a pass through default interceptor bindings.
        for(InterceptorBindingDescriptor binding : interceptorBindings) {

            if( binding.getBindingType() == BindingType.DEFAULT ) {
                defaultInterceptorChain.addAll
                    (binding.getInterceptorClasses());
            } 

        }

        // Do a pass through Class level bindings.
        for(InterceptorBindingDescriptor binding : interceptorBindings) {

            if( binding.getBindingType() == BindingType.CLASS ) {

                if( binding.getEjbName().equals(ejbName) ) {
                    processClassLevelBinding(binding);
                }
            } 

        }

        // Now do method-level bindings.

        Map<MethodDescriptor, List<InterceptorBindingDescriptor>> 
            methodBindings = new HashMap<MethodDescriptor, 
                                         List<InterceptorBindingDescriptor>>();

        // First build a map of all business methods for the current
        // ejb that have binding information, and their associated
        // bindings.
        for(InterceptorBindingDescriptor binding : interceptorBindings) {

            if( (binding.getEjbName().equals(ejbName)) &&
                (binding.getBindingType() == BindingType.METHOD) ) { 

                MethodDescriptor method = binding.getBusinessMethod();

                List<InterceptorBindingDescriptor> methodBindingDescs =
                    methodBindings.get(method);
                if( methodBindingDescs == null ) {
                    methodBindingDescs = 
                        new LinkedList<InterceptorBindingDescriptor>();
                }

                methodBindingDescs.add(binding);

                methodBindings.put(method, methodBindingDescs);
            }

        }

        for(Map.Entry<MethodDescriptor, List<InterceptorBindingDescriptor>> next
                : methodBindings.entrySet()) {
            processMethod(next.getKey(), next.getValue());
        }

        TranslationResults results = buildResults();
        
        return results;

    }

    private void processClassLevelBinding(InterceptorBindingDescriptor 
                                          binding) {

        if( binding.getExcludeDefaultInterceptors() ) {
            defaultInterceptorChain.clear();
        } 
            
        if( binding.getIsTotalOrdering() ) {

            hasTotalClassLevelOrdering = true;
            totalClassLevelOrdering.clear();
            totalClassLevelOrdering.addAll(binding.getInterceptorClasses());
            
            // totalClassLevelOrdering will take precedence, but keep 
            // classInterceptorChain updated to contain class-level, but not
            // default-level, interceptors.  These might be needed during
            // method-level exclude-class-interceptors processing.
            for(String next : binding.getInterceptorClasses()) {
                if( !defaultInterceptorChain.contains(next) ) {
                    if( !classInterceptorChain.contains(next) ) {
                        classInterceptorChain.add(next);
                    }
                }
            }
        } else {
            classInterceptorChain.addAll(binding.getInterceptorClasses());
        }

    }

    private void processMethod(MethodDescriptor businessMethod,
                               List<InterceptorBindingDescriptor> bindings) {

        LinkedList<String> tempDefaultInterceptorChain = 
            new LinkedList<String>();

        LinkedList<String> tempClassInterceptorChain =
            new LinkedList<String>();

        LinkedList<String> tempMethodInterceptorChain =
            new LinkedList<String>();

        if( hasTotalClassLevelOrdering ) {
            tempClassInterceptorChain.addAll(totalClassLevelOrdering);
        } else {                
            tempDefaultInterceptorChain.addAll(defaultInterceptorChain);
            tempClassInterceptorChain.addAll(classInterceptorChain);
        }

        for(InterceptorBindingDescriptor nextBinding : bindings) {

            if( nextBinding.getExcludeDefaultInterceptors() ) {
                if( hasTotalClassLevelOrdering ) {
                    tempClassInterceptorChain.removeAll
                        (defaultInterceptorChain);
                } else {
                    tempDefaultInterceptorChain.clear();
                }
            }

            if( nextBinding.getExcludeClassInterceptors() ) {
                if( hasTotalClassLevelOrdering ) {
                    tempClassInterceptorChain.removeAll
                        (classInterceptorChain);
                } else {
                    tempClassInterceptorChain.clear();
                }
            }
            
            if( nextBinding.getIsTotalOrdering() ) {
                tempDefaultInterceptorChain.clear();
                tempClassInterceptorChain.clear();
                tempMethodInterceptorChain.clear();
            }

            tempMethodInterceptorChain.addAll
                (nextBinding.getInterceptorClasses());

        }

        LinkedList<String> methodInterceptors = new LinkedList<String>();
        methodInterceptors.addAll(tempDefaultInterceptorChain);
        methodInterceptors.addAll(tempClassInterceptorChain);
        methodInterceptors.addAll(tempMethodInterceptorChain);

        methodInterceptorsMap.put(businessMethod, methodInterceptors);
        
    }

    private TranslationResults buildResults() {

        TranslationResults results = new TranslationResults();        

        if( hasTotalClassLevelOrdering ) {

            for(String next : totalClassLevelOrdering ) {
                EjbInterceptor interceptor = 
                    ejbBundle.getInterceptorByClassName(next);
                results.allInterceptorClasses.add(interceptor);
                results.classInterceptorChain.add(interceptor);
            }

        } else {

            for(String next : defaultInterceptorChain) {
                EjbInterceptor interceptor = 
                    ejbBundle.getInterceptorByClassName(next);

                results.allInterceptorClasses.add(interceptor);
                results.classInterceptorChain.add(interceptor);
            }

            for(String next : classInterceptorChain) {
                EjbInterceptor interceptor = 
                    ejbBundle.getInterceptorByClassName(next);

                results.allInterceptorClasses.add(interceptor);
                results.classInterceptorChain.add(interceptor);
            }
        }

        for(MethodDescriptor nextMethod : methodInterceptorsMap.keySet()) {

            List<String> interceptorClassChain = (List<String>)
                methodInterceptorsMap.get(nextMethod);
            
            List<EjbInterceptor> interceptorChain = 
                new LinkedList<EjbInterceptor>();
            
            for(String nextClass : interceptorClassChain) {
                EjbInterceptor interceptor = 
                    ejbBundle.getInterceptorByClassName(nextClass);

                results.allInterceptorClasses.add(interceptor);
                interceptorChain.add(interceptor);

            }
            
            results.methodInterceptorsMap.put(nextMethod, interceptorChain);

        }

        return results;
    }

    private void validateInterceptors() {

        // Make sure there's an interceptor defined for every interceptor
        // class name listed in the bindings.
        for(InterceptorBindingDescriptor binding : interceptorBindings) {

            for(String interceptor : binding.getInterceptorClasses()) {

                if(ejbBundle.getInterceptorByClassName(interceptor) == null) {
                    throw new IllegalStateException
                        ("Interceptor binding contains an interceptor class " +
                         " name = " + interceptor + 
                         " that is not defined as an interceptor");
                }
            }
        }

    }

    public static class TranslationResults {
        
        public Set<EjbInterceptor> allInterceptorClasses;

        public List<EjbInterceptor> classInterceptorChain;

        public Map<MethodDescriptor, List<EjbInterceptor>> 
            methodInterceptorsMap;

        public TranslationResults() {
            allInterceptorClasses = new HashSet<EjbInterceptor>();
            classInterceptorChain = new LinkedList<EjbInterceptor>();
            methodInterceptorsMap = 
                new HashMap<MethodDescriptor, List<EjbInterceptor>>();
        }

    }

}

