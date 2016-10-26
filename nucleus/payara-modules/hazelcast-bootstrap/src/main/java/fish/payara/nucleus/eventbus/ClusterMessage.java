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
package fish.payara.nucleus.eventbus;

import java.io.Serializable;

/**
 *
 * @author steve
 * @param <T> The type of the message payload
 */
public class ClusterMessage<T extends Serializable> implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private T body;
    
    public ClusterMessage(T payload) {
        body = payload;
    }
    
    public T getPayload() {
        return body;
    }
    
}
