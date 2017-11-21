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
package com.sun.enterprise.connectors.jms.system;

/**
 * Exception class thrown when there is a problem initialising the broker
 * @author steve
 */
public class JmsInitialisationException extends Exception {
    private static final long serialVersionUID = -2763569052869563797L;

    public JmsInitialisationException() {
    }

    public JmsInitialisationException(String message) {
        super(message);
    }

    public JmsInitialisationException(Throwable cause) {
        super(cause);
    }

    public JmsInitialisationException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
