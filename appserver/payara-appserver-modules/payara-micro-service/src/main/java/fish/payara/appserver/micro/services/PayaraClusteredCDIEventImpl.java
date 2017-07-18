/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2017 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.appserver.micro.services;

import com.sun.enterprise.util.Utility;
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
 * @author Steve Millidge (Payara Services Limited)
 */
public class PayaraClusteredCDIEventImpl implements PayaraClusteredCDIEvent {

    private static final long serialVersionUID = 1L;
    private InstanceDescriptor id;
    private boolean loopBack = false;
    private PayaraValueHolder payload;
    private Properties props;
    private PayaraValueHolder<Set<InvocationHandler>> qualifiersPayload;
    private transient Set<InvocationHandler> qualifiers;

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
    public Set<Annotation> getQualifiers() {
        if (qualifiers == null) {
            try {
                qualifiers = qualifiersPayload.getValue();
            } catch (IOException | ClassNotFoundException ex) {
                Logger.getLogger(PayaraClusteredCDIEventImpl.class.getName()).log(Level.INFO, "Unable to deserialize qualifiers received on the event ignoring...", ex);
            }
        }
        if (qualifiers != null) {
            Set<Annotation> result = new HashSet<>(qualifiers.size());
            for (InvocationHandler qualifier : qualifiers) {
                // ok we have the invocation handlers that have been serialized
                // we now need to create a Proxy for each
                
                // First find the type
                Method methods[] = Annotation.class.getMethods();
                Class<?> annotationClazz = null;
                for (Method method : methods) {
                    if (method.getName().equals("annotationType")) {
                        try {
                            annotationClazz = (Class<?>) qualifier.invoke(null, method, new Object[0]);
                            // then create a proxy for the annotation type from the serialized Invocation Handler
                            result.add((Annotation) Proxy.newProxyInstance(Utility.getClassLoader(), new Class[]{annotationClazz}, qualifier));
                        } catch (Throwable ex) {
                            Logger.getLogger(PayaraClusteredCDIEventImpl.class.getName()).log(Level.INFO, "Problem determining the qualifier type of an Event ignoring", ex);
                        }
                    }
                }
                
            }
            return result;
        }else {
            return Collections.EMPTY_SET;
        }
    } 

    @Override
    public void addQualifiers(Set<Annotation> add) {
        if (qualifiers == null) {
            qualifiers = new HashSet<>(add.size());
        }
        
        // we can't serialize the Proxies as we will receive a CNFE
        // However we can serialize the Invocation Handlers and then recreate the Proxies
        for (Annotation annotation : add) {
            if (Proxy.isProxyClass(annotation.getClass())) {
                InvocationHandler handler = Proxy.getInvocationHandler(annotation);
                qualifiers.add(handler);
            }
        }
        try {
            // We need to use a Payara Value Holder to prevent Hazelcast Serialization Errors
            qualifiersPayload = new PayaraValueHolder(qualifiers);
        } catch (IOException ex) {
        }
    }    
    
    
    
}
