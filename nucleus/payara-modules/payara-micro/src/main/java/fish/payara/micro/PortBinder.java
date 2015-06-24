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
import java.net.ServerSocket;

/**
 *
 * @author Andrew Pielage
 */
public class PortBinder
{
    
    
    public int findAvailablePort(int port)
    {
        // Set the number of times to try and bind to new ports
        final int PORT_COUNT = 10;

        // Initialise a return variable equal to parameter passed in
        int returnPort = port;
        
        /**
         * Loop through, incrementing the port to bind to by 1 for each failure, until PORT_COUNT is reached
         */
        for (int i = 0; i < PORT_COUNT; i++)
        {        
            // Try to bind to the port                     
            try (ServerSocket serverSocket = new ServerSocket(port);)
            {
                // If no exception thrown, set returnPort to the open port and break out of loop
                returnPort = port;
                break;
            }

            catch (IOException ex)
            {
                // Increment port to try again on next port
                port++;
            }
        }
        
        // Return either the found port, or the original port passed in if no open port found
        return returnPort;
    }
}
