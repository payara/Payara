/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.config.serverbeans.customvalidators;

import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.logging.LogDomains;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.UnexpectedTypeException;

import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Dom;

/**
 * @author Martin Mares
 */
public class ReferenceValidator implements ConstraintValidator<ReferenceConstraint, ConfigBeanProxy> {
    
    static class RemoteKeyInfo {
        final Method method;
        final ReferenceConstraint.RemoteKey annotation;
        public RemoteKeyInfo(Method method, ReferenceConstraint.RemoteKey annotation) {
            this.method = method;
            this.annotation = annotation;
        }
    }

    static final LocalStringManagerImpl localStrings = new LocalStringManagerImpl(ReferenceValidator.class);
    
    private ReferenceConstraint rc;
    
    @Override
    public void initialize(ReferenceConstraint rc) {
        this.rc = rc;
    }

    @Override
    public boolean isValid(ConfigBeanProxy config, ConstraintValidatorContext cvc) throws UnexpectedTypeException {
        if (config == null) {
            return true;
        }
        Dom dom = Dom.unwrap(config);
        if (rc.skipDuringCreation() && dom.getKey() == null) {
            return true; //During creation the coresponding DOM is not fully loaded.
        }
        Collection<RemoteKeyInfo> remoteKeys = findRemoteKeys(config);
        if (remoteKeys != null && !remoteKeys.isEmpty()) {
            ServiceLocator habitat = dom.getHabitat();
            boolean result = true;
            boolean disableGlobalMessage = true;
            for (RemoteKeyInfo remoteKeyInfo : remoteKeys) {
                if (remoteKeyInfo.method.getParameterTypes().length > 0) {
                    throw new UnexpectedTypeException(localStrings.getLocalString("referenceValidator.not.getter", 
                            "The RemoteKey annotation must be on a getter method."));
                }
                try {
                    Object value = remoteKeyInfo.method.invoke(config);
                    if (value instanceof String) {
                        String key = (String) value;
                        ConfigBeanProxy component = habitat.getService(remoteKeyInfo.annotation.type(), key);
                        if (component == null) {
                            result = false;
                            if (remoteKeyInfo.annotation.message().isEmpty()) {
                                disableGlobalMessage = false;
                            } else {
                                cvc.buildConstraintViolationWithTemplate(remoteKeyInfo.annotation.message())
                                        .addNode(Dom.convertName(remoteKeyInfo.method.getName()))
                                        .addConstraintViolation();
                            }
                        }
                    } else {
                        throw new UnexpectedTypeException(localStrings.getLocalString("referenceValidator.not.string", 
                            "The RemoteKey annotation must identify a method that returns a String."));
                    }
                } catch (Exception ex) {
                    return false;
                }
            }
            if (!result && disableGlobalMessage) {
                cvc.disableDefaultConstraintViolation();
            }
            return result;
        }
        return true;
    }
    
    private Collection<RemoteKeyInfo> findRemoteKeys(Object o) {
        Collection<RemoteKeyInfo> result = new ArrayList<RemoteKeyInfo>();
        if (o == null) {
            return result;
        }
        findRemoteKeys(o.getClass(), result);
        return result;
    }
    
    private void findRemoteKeys(Class c, Collection<RemoteKeyInfo> result) {
        Method[] methods = c.getMethods();
        for (Method method : methods) {
            ReferenceConstraint.RemoteKey annotation = method.getAnnotation(ReferenceConstraint.RemoteKey.class);
            if (annotation != null) {
                result.add(new RemoteKeyInfo(method, annotation));
            }
        }
        Class superclass = c.getSuperclass();
        if (superclass != null) {
            findRemoteKeys(superclass, result);
        }
        Class[] interfaces = c.getInterfaces();
        for (Class iface : interfaces) {
            findRemoteKeys(iface, result);
        }
    }
    
}
