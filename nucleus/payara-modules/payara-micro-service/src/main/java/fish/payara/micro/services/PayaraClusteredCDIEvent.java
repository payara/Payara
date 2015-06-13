/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2015 C2B2 Consulting Limited. All rights reserved.

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
package fish.payara.micro.services;

import fish.payara.micro.services.data.InstanceDescriptor;
import java.io.Serializable;

/**
 *
 * @author steve
 */
public class PayaraClusteredCDIEvent implements Serializable {

    private static final long serialVersionUID = 1L;
    private InstanceDescriptor id;
    private boolean loopBack = false;

    public PayaraClusteredCDIEvent() {
        id = null;
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

    public boolean isLoopBack() {
        return loopBack;
    }

    public void setLoopBack(boolean loopBack) {
        this.loopBack = loopBack;
    }
    
    
}
