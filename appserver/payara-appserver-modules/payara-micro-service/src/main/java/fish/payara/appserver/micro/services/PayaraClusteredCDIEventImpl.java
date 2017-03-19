/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2016-2017 Payara Foundation. All rights reserved.

 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.

 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.appserver.micro.services;

import fish.payara.micro.event.PayaraClusteredCDIEvent;
import fish.payara.cdi.jsr107.implementation.PayaraValueHolder;
import fish.payara.micro.data.InstanceDescriptor;
import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author steve
 */
public class PayaraClusteredCDIEventImpl implements PayaraClusteredCDIEvent {

    private static final long serialVersionUID = 1L;
    private InstanceDescriptor id;
    private boolean loopBack = false;
    private PayaraValueHolder payload;
    private Properties props;
    private Set<String> qualifiers;

    public PayaraClusteredCDIEventImpl(InstanceDescriptor id, Serializable payload) throws IOException {
        this.id = id;
        this.payload = new PayaraValueHolder(payload);
    }

    public PayaraClusteredCDIEventImpl(InstanceDescriptor id) {
        this.id = id;
    }

    @Override
    public InstanceDescriptor getInstanceDescriptor() {
        return id;
    }

    void setInstanceDescriptor(InstanceDescriptor localDescriptor) {
        id = localDescriptor;
    }

    @Override
    public InstanceDescriptor getId() {
        return id;
    }

    @Override
    public void setId(InstanceDescriptor id) {
        this.id = id;
    }
    
    @Override
    public Serializable getPayload() throws IOException, ClassNotFoundException {
        if (payload != null) {
            return (Serializable) payload.getValue();
        }
        return null;
    }

    @Override
    public boolean isLoopBack() {
        return loopBack;
    }

    @Override
    public void setLoopBack(boolean loopBack) {
        this.loopBack = loopBack;
    }

    @Override
    public Properties getProperties() {
        return props;
    }
    
    @Override
    public void setProperty(String name, String value) {
        if (props == null) {
            props = new Properties();
        }
        props.setProperty(name, value);
    }
    
    @Override
    public String getProperty(String name) {
        String result = null;
        if (props!= null) {
            result = props.getProperty(name);
        }
        return result;
    }

    @Override
    public String getProperty(String name, String defaultValue) {
        String result = null;
        if (props!= null) {
            result = props.getProperty(name, defaultValue);
        }
        return result;
    }
    
    @Override
    public Set<String> getQualifierClassNames() {
        if (qualifiers != null) {
            return qualifiers;
        }else {
            return Collections.EMPTY_SET;
        }
    } 

    @Override
    public void addQualifiers(Set<Annotation> add) {
        if (qualifiers == null) {
            qualifiers = new HashSet<>(add.size());
        }
        for (Annotation annotation : add) {
            if (Proxy.isProxyClass(annotation.getClass())) {
                InvocationHandler handler = Proxy.getInvocationHandler(annotation);
                Method methods[] = Annotation.class.getMethods();
                Method annotationType = null;
                for (Method method : methods) {
                    if (method.getName().equals("annotationType")) {
                        annotationType = method;
                    }
                }
                if (annotationType != null) {
                    try {        
                            Class<? extends Annotation> annotationClazz = (Class<? extends Annotation>) handler.invoke(add, annotationType, new Object[0]);
                            if (!annotationClazz.getCanonicalName().equals("fish.payara.micro.cdi.Outbound")) {
                                qualifiers.add(annotationClazz.getCanonicalName());
                            }
                        } catch (Throwable ex) {
                    }
                }
            }
        }
    }    
    
    
    
}
