/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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

/*
 * @(#)MQRMIClientSocketFactory.java	1.5 06/29/07
 */

/*
 *  IMPORTANT NOTE: Please do not modify this file directly. This source code is owned and shipped as a part of MQ but has only been included here
 *  since it is required for certain JMX operations especially when MQ is running in the HA mode. Please refer to GF issue 13602 for more details.
 */

package com.sun.messaging.jmq.management;

import java.io.IOException;
import java.net.Socket;

import java.rmi.server.RMISocketFactory;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.rmi.ssl.SslRMIClientSocketFactory;

public class MQRMIClientSocketFactory extends SslRMIClientSocketFactory {
    boolean debug = false;
    boolean isBrokerHostTrusted = true;
    boolean useSSL = false;
    String hostname = null;

    public MQRMIClientSocketFactory(String hostname, boolean isBrokerHostTrusted,
					boolean useSSL)  {
	this.isBrokerHostTrusted = isBrokerHostTrusted;
	this.hostname = hostname;
	this.useSSL = useSSL;
    }

    public Socket createSocket(String host, int port) throws IOException {
	Socket s = null;
	String socketHost = hostname;

	/*
	 * If the factory is not configured for any specific host, use whatever
	 * is passed in to createSocket.
	 *
	 * The wildcard "*" here is something that could be set on the server
	 * side. It is the constant jmsserver.Globals.HOSTNAME_ALL but we don't
	 * want to introduce any server side compile time dependencies here.
	 * Remember that this factory is created by the server.
	 */
	if ((socketHost == null) || (socketHost.equals("*")))  {
	    socketHost = host;
	}

	try  {
	    if (useSSL)  {
	        s = (Socket)makeSSLSocket(socketHost, port);
	    } else  {
	        s = RMISocketFactory.getDefaultSocketFactory().
			createSocket(socketHost, port);
	    }
	} catch (Exception e)  {
	    throw new IOException(e.toString());
	}

	return (s);
    }

    public String toString()  {
        return ("hostname="
		+ hostname 
		+ ",isBrokerHostTrusted=" 
		+ isBrokerHostTrusted 
		+ ",useSSL=" 
		+ useSSL);
    }

    public boolean equals(Object obj)  {
        if (!(obj instanceof MQRMIClientSocketFactory))  {
            return (false);
        }
    
        MQRMIClientSocketFactory that = (MQRMIClientSocketFactory)obj;

        if (this.hostname != null)  {
            if ((that.hostname == null) || !that.hostname.equals(this.hostname))  {
                return (false);
            }
        } else  {
            if (that.hostname != null)  {
                return (false);
            }
        }

        if (this.isBrokerHostTrusted != that.isBrokerHostTrusted)  {
            return (false);
        }

        if (this.useSSL != that.useSSL)  {
            return (false);
        }

        return (true);
    }

    public int hashCode()  {
        return toString().hashCode();
    }

    private SSLSocket makeSSLSocket(String host, int port) throws Exception {
        SSLSocketFactory sslFactory;

        if (isBrokerHostTrusted) {
            sslFactory = getTrustSocketFactory();

            if ( debug ) {
                System.err.println("Broker is trusted ...");
            }
        } else {
            sslFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        }

        //This is here for QA to verify that SSL is used ...
        if ( debug ) {
            System.err.println ("Create connection using SSL protocol ...");
            System.err.println ("Broker Host: " + host);
            System.err.println ("Broker Port: " + port);
        }

        Object socket = sslFactory.createSocket (host, port);
        SSLSocket sslSocket = null;
        if (socket instanceof SSLSocket) {
            sslSocket = (SSLSocket) socket;

            //tcp no delay flag
            boolean tcpNoDelay = true;
            String prop = System.getProperty("imqTcpNoDelay", "true");
            if ( prop.equals("false") ) {
                tcpNoDelay = false;
            } else {
                sslSocket.setTcpNoDelay(tcpNoDelay);
            }
        }
        return sslSocket;
    }


    private SSLSocketFactory getTrustSocketFactory() throws Exception {
        SSLSocketFactory factory = null;

        SSLContext ctx;
        ctx = SSLContext.getInstance("TLS");
        TrustManager[] tm = new TrustManager [1];
        tm[0] = new DefaultTrustManager();

        ctx.init(null, tm, null);
        factory = ctx.getSocketFactory();

        return factory;
    }

}
