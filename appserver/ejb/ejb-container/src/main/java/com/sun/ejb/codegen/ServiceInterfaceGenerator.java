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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2025] [Payara Foundation and/or its affiliates]

package com.sun.ejb.codegen;


import jakarta.jws.WebMethod;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.glassfish.pfl.dynamic.codegen.spi.Type;

import static java.lang.reflect.Modifier.ABSTRACT;
import static java.lang.reflect.Modifier.PUBLIC;
import static org.glassfish.pfl.dynamic.codegen.spi.Wrapper._arg;
import static org.glassfish.pfl.dynamic.codegen.spi.Wrapper._end;
import static org.glassfish.pfl.dynamic.codegen.spi.Wrapper._interface;
import static org.glassfish.pfl.dynamic.codegen.spi.Wrapper._method;
import static org.glassfish.pfl.dynamic.codegen.spi.Wrapper._t;

/**
 * This class is responsible for generating the SEI when it is not packaged 
 * by the application. 
 *
 * @author Jerome Dochez
 */
public class ServiceInterfaceGenerator extends Generator {
    
    private final Class<?> ejbClass;
    private final String packageName;
    private final String serviceIntfName;
    private final String serviceIntfSimpleName;
    private final Method[] intfMethods;

    /**
     * Construct the Wrapper generator with the specified deployment
     * descriptor and class loader.
     *
     * @throws GeneratorException
     */
    public ServiceInterfaceGenerator(final ClassLoader classLoader, final Class<?> ejbClass) {
        super(classLoader);
        this.ejbClass = ejbClass;
        packageName = getPackageName(ejbClass.getName());
        serviceIntfSimpleName = getServiceIntfName(ejbClass);
        serviceIntfName = (packageName == null ? "" : packageName + ".") + serviceIntfSimpleName;
        intfMethods = calculateMethods(ejbClass, removeRedundantMethods(ejbClass.getMethods()));

        // NOTE : no need to remove ejb object methods because EJBObject
        // is only visible through the RemoteHome view.
    }

    @Override
    protected String getPackageName() {
        return this.packageName;
    }

    /**
     * Get the fully qualified name of the generated class.
     * Note: the remote/local implementation class is in the same package 
     * as the bean class, NOT the remote/local interface.
     * @return the name of the generated class.
     */
    @Override
    public String getGeneratedClassName() {
        return serviceIntfName;
    }

    private static String getServiceIntfName(Class<?> ejbClass) {
        String serviceIntfSimpleName = ejbClass.getSimpleName();
        if (serviceIntfSimpleName.endsWith("EJB")) {
            return serviceIntfSimpleName.substring(0, serviceIntfSimpleName.length() - 3) + "_GeneratedSEI";
        }
        return serviceIntfSimpleName + "_GeneratedSEI";
    }
    
    private Method[] calculateMethods(Class sib, Method[] initialList) {
        // we start by assuming the @WebMethod was NOT used on this class
        boolean webMethodAnnotationUsed = false;
        List<Method> list = new ArrayList<>();
        
        for (Method m : initialList) {
            WebMethod wm = m.getAnnotation(WebMethod.class);
            if (wm != null && !webMethodAnnotationUsed) {
                webMethodAnnotationUsed=true;
                // reset the list, this is the first annotated method we find
                list.clear();
            }
            if (wm!=null) {
                list.add(m);
            } else {
                if (!webMethodAnnotationUsed && !m.getDeclaringClass().equals(java.lang.Object.class)) {
                    list.add(m);
                }
            }
        }
        return list.toArray(new Method[list.size()]);
    }

    @Override
    public Class<?> getAnchorClass() {
        return ejbClass;
    }

    @Override
    protected void defineClassBody() {
        _interface(PUBLIC, serviceIntfSimpleName);

        for (Method intfMethod : intfMethods) {
            printMethod(intfMethod);
        }

        _end();
    }

    private void printMethod(Method m) {
        boolean throwsRemoteException = false;
        List<Type> exceptionList = new LinkedList<>();
        for (Class<?> exception : m.getExceptionTypes()) {
            exceptionList.add(Type.type(exception));
            if (exception.getName().equals(RemoteException.class.getName())) {
                throwsRemoteException = true;
            }
        }
        if (!throwsRemoteException) {
            exceptionList.add(_t(RemoteException.class.getName()));
        }

        _method(PUBLIC | ABSTRACT, Type.type(m.getReturnType()), m.getName(), exceptionList);

        int i = 0;

        for (Class<?> param : m.getParameterTypes()) {
            _arg(Type.type(param), "param" + i);
            i++;
        }
        _end();
    }
}
