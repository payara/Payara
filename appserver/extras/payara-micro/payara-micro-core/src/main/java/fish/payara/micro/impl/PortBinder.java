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

package fish.payara.micro.impl;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;

/**
 *
 * @author Andrew Pielage
 */
public class PortBinder {
    
    /**
     * Searches for and returns a bindable port number.
     * @param port The port to start searching from.
     * @param autoBindRange The maximum range of ports to check for availability.
     * @return A bindable port number.
     * @throws BindException 
     */
    public int findAvailablePort(int port, int autoBindRange) throws BindException {
        int returnPort = port;
        boolean foundAvailablePort = false;

        for (int i = 0; i <= autoBindRange; i++) {   
            try (ServerSocket serverSocket = new ServerSocket(port);) {
                returnPort = port;
                foundAvailablePort = true;
                break;
            } catch (IOException ex) {
                port++;
            }
        }
        
        if (foundAvailablePort == false) {
            throw new BindException();
        }
        
        return returnPort;
    }
}
