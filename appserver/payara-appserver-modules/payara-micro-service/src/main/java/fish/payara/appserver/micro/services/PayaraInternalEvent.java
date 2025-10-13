/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2016 Payara Foundation. All rights reserved.

 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://github.com/payara/Payara/blob/master/LICENSE.txt
 See the License for the specific
 language governing permissions and limitations under the License.

 When distributing the software, include this License Header Notice in each
 file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 */
package fish.payara.appserver.micro.services;

import fish.payara.micro.data.InstanceDescriptor;

import java.io.Serializable;

/**
 *
 * @author steve
 */
public class PayaraInternalEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    
    private final MESSAGE messageType;
    private final InstanceDescriptor id;

    
    public PayaraInternalEvent(MESSAGE message, InstanceDescriptor id) {
        this.messageType = message;
        this.id = id;
    }

    public MESSAGE getMessageType() {
        return messageType;
    }
    
    public InstanceDescriptor getId() {
        return id;
    }

    public enum MESSAGE {

        ADDED, REMOVED
    }
    
}
