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
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.ServerSocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Andrew Pielage
 */
public class PortBinder
{
    
    
    public void findAvailablePort(int port)
    {
        if (port == Integer.MIN_VALUE)
        {
            
        }
        
        try
        {
            final int PORT_COUNT = 10;
            ServerSocket serverSocket = null;
            ServerSocketChannel serverSocketChannel = null;
            InetSocketAddress inetSocketAddress;


            for (int i = 0; i < PORT_COUNT; i++)
            {
                serverSocketChannel = ServerSocketChannel.open();
                serverSocket = serverSocketChannel.socket();
                serverSocket.setSoTimeout(1000);
                
                try
                {
                    inetSocketAddress = new InetSocketAddress(port);
                    
                    serverSocket.bind(inetSocketAddress, 100);
                    
                    break;
                }
                
                catch (final Exception e) 
                {
                    serverSocket.close();
                    serverSocketChannel.close();
                    
                    port++;
                }  
            }
            
            serverSocketChannel.configureBlocking(false);
        }
          
        catch (IOException ex)
        {
            Logger.getLogger(PortBinder.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
}
