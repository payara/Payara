/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2016 Payara Foundation. All rights reserved.

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

import fish.payara.cdi.jsr107.impl.PayaraValueHolder;
import fish.payara.appserver.micro.services.data.InstanceDescriptor;
import java.io.IOException;
import java.io.Serializable;
import java.util.Properties;

/**
 *
 * @author steve
 */
public class PayaraClusteredCDIEvent implements Serializable {

    private static final long serialVersionUID = 1L;
    private InstanceDescriptor id;
    private boolean loopBack = false;
    private PayaraValueHolder payload;
    private Properties props;

    public PayaraClusteredCDIEvent(InstanceDescriptor id, Serializable payload) throws IOException {
        this.id = id;
        this.payload = new PayaraValueHolder(payload);
    }

    public PayaraClusteredCDIEvent(InstanceDescriptor id) {
        this.id = id;
    }

    public InstanceDescriptor getInstanceDescriptor() {
        return id;
    }

    void setInstanceDescriptor(InstanceDescriptor localDescriptor) {
        id = localDescriptor;
    }

    public InstanceDescriptor getId() {
        return id;
    }

    public void setId(InstanceDescriptor id) {
        this.id = id;
    }
    
    public Serializable getPayload() throws IOException, ClassNotFoundException {
        if (payload != null) {
            return (Serializable) payload.getValue();
        }
        return null;
    }

    public boolean isLoopBack() {
        return loopBack;
    }

    public void setLoopBack(boolean loopBack) {
        this.loopBack = loopBack;
    }

    public Properties getProperties() {
        return props;
    }
    
    public void setProperty(String name, String value) {
        if (props == null) {
            props = new Properties();
        }
        props.setProperty(name, value);
    }
    
    public String getProperty(String name) {
        String result = null;
        if (props!= null) {
            result = props.getProperty(name);
        }
        return result;
    }

    public String getProperty(String name, String defaultValue) {
        String result = null;
        if (props!= null) {
            result = props.getProperty(name, defaultValue);
        }
        return result;
    }
     
    
    
    
}
