/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2018] Payara Foundation and/or affiliates

package org.glassfish.flashlight.datatree.impl;

import org.glassfish.flashlight.datatree.MethodInvoker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author Harpreet Singh
 */
public class MethodInvokerImpl extends AbstractTreeNode implements MethodInvoker {
    Method method;
    Object methodInstance;

    @Override
    public void setMethod (Method m){
        method = m;
    }
    
    @Override
    public Method getMethod (){
        return method;
    }
    
    @Override
    public void setInstance (Object i){
        methodInstance = i;
    }
    
    @Override
    public Object getInstance (){
        return methodInstance;
    }
    
    @Override
    // TBD Put Logger calls.
    public Object getValue (){
        Object retValue = null;
        try {
            if (method == null){
                throw new RuntimeException ("Flashlight:MethodInvoker: method, is null - cannot be null.");
            }
            if (methodInstance == null) {
                 throw new RuntimeException ("Flashlight:MethodInvoker: object,  instance is null - cannot be null.");
            }
            if (super.isEnabled()) {
                retValue = method.invoke(methodInstance, null);
            }
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            Logger.getLogger(MethodInvokerImpl.class.getName()).log(Level.WARNING, null, ex);
        }
        return retValue;
    }

}
