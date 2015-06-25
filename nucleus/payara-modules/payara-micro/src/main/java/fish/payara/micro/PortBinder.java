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

package fish.payara.micro;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;

/**
 *
 * @author Andrew Pielage
 */
public class PortBinder
{
    public int findAvailablePort(int port, int autoBindRange) throws BindException
    {
        // Initialise a return variable equal to parameter passed in
        int returnPort = port;
        
        // Initialise a flag for throwing a custom error if no available ports within range
        boolean foundAvailablePort = false;
        
        /**
         * Loop through, incrementing the port to bind to by 1 for each failure, until PORT_COUNT is reached
         */
        for (int i = 0; i <= autoBindRange; i++)
        {        
            // Try to bind to the port                     
            try (ServerSocket serverSocket = new ServerSocket(port);)
            {
                // If no exception thrown, set returnPort to the open port, set the "found" flag to true, and break out of loop
                returnPort = port;
                foundAvailablePort = true;
                break;
            }

            catch (IOException ex)
            {
                // Increment port to try again on next port
                port++;
            }
        }
        
        // Check if an available port has been found
        if (foundAvailablePort == false)
        {
            // If a port hasn't been found, throw a BindException
            throw new BindException();
        }
        
        // Return the available port, or the original port passed in if no available port found
        return returnPort;
    }
}
