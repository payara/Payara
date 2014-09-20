/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.iiop.security;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

//import org.omg.IOP.TAG_ALTERNATE_IIOP_ADDRESS ;

import com.sun.corba.ee.spi.ior.IOR;
import com.sun.corba.ee.spi.ior.iiop.AlternateIIOPAddressComponent;
//import com.sun.enterprise.iiop.AlternateIIOPAddressComponent;
import com.sun.corba.ee.spi.ior.iiop.IIOPProfileTemplate ;
import com.sun.corba.ee.spi.ior.iiop.IIOPAddress ;
import com.sun.corba.ee.spi.transport.IORToSocketInfo;
import com.sun.corba.ee.spi.transport.SocketInfo;

import com.sun.logging.LogDomains;
import org.glassfish.internal.api.Globals;

/**
 * This implements IORToSocketInfo for ORB.
 * Part of logic is from previous version of IIOPSSLSocketFactory.
 * @author Shing Wai Chan
 */
public class IORToSocketInfoImpl implements IORToSocketInfo {
    private static Logger _logger = null;
    static {
        _logger = LogDomains.getLogger(IORToSocketInfoImpl.class,LogDomains.CORBA_LOGGER);
    }

    private final String baseMsg = IORToSocketInfoImpl.class.getName();

    // Maps primary address to list of alternate addresses.
    // Used to compare new IOR/primaries with ones already seen for
    // that primary.  We expect them to be equal.
    private Map primaryToAddresses = new HashMap();

    // Maps primary to randomized list of alternate addresses.
    private Map primaryToRandomizedAddresses = new HashMap();

    //----- implements com.sun.corba.ee.spi.transport.IORToSocketInfo -----
    private SecurityMechanismSelector selector;
    
    public IORToSocketInfoImpl() {
        selector = Globals.get(SecurityMechanismSelector.class);
    }
    
    @Override
    public List getSocketInfo(IOR ior, List previous) 
    {
        try {
	    if (_logger.isLoggable(Level.FINE)) {
		_logger.log(Level.FINE, baseMsg + ".getSocketInfo->:");
	    }
            List result = new ArrayList();
            
            IIOPProfileTemplate iiopProfileTemplate = (IIOPProfileTemplate)ior.
                                 getProfile().getTaggedProfileTemplate();
            IIOPAddress primary = iiopProfileTemplate.getPrimaryAddress() ;
            Locale loc = Locale.getDefault();
            String host = primary.getHost().toLowerCase(loc);

            String type = null;
            int port = 0;
            ConnectionContext ctx = new ConnectionContext();
            SocketInfo socketInfo = selector.getSSLPort(ior, ctx);
            selector.setClientConnectionContext(ctx);
            if (socketInfo == null) {
                type = SocketInfo.IIOP_CLEAR_TEXT;
                port = primary.getPort();
		if (_logger.isLoggable(Level.FINE)) {
		    _logger.log(Level.FINE, baseMsg
				+ ".getSocketInfo: did not find SSL SocketInfo");
		}
            } else {
                type = socketInfo.getType();
                port = socketInfo.getPort();
		if (_logger.isLoggable(Level.FINE)) {
		    _logger.log(Level.FINE, baseMsg
				+ ".getSocketInfo: found SSL socketInfo");
		}
            }
        
	    if (_logger.isLoggable(Level.FINE)) {
		_logger.log(Level.FINE, baseMsg 
			    + ".getSocketInfo: Connection Context:" + ctx);
		_logger.log(Level.FINE, baseMsg
			    + ".getSocketInfo: ENDPOINT INFO:type=" + type + ",host=" +host + ", port=" + port);
	    }

            // for SSL
            if (socketInfo != null ) {
                result.add(socketInfo);
		if (_logger.isLoggable(Level.FINE)) {
		    _logger.log(Level.FINE, baseMsg 
				+ ".getSocketInfo: returning SSL socketInfo:"
				+ " " + socketInfo.getType()
				+ " " + socketInfo.getHost()
				+ " " + socketInfo.getPort());
		}
		// REVISIT: should call IIOPPrimaryToContactInfo.reset
		// right here to invalidate sticky for this result.
		// However, SSL and IIOP-FO is not a supported feature.
                return result;
            }

	    ////////////////////////////////////////////////////
	    //
	    // The remainder of this code is non-SSL.
	    // Author: Harold Carr
	    // Please contact author is changes needed.
	    //

            // for non-SSL
	    if (_logger.isLoggable(Level.FINE)) {
		_logger.log(Level.FINE, baseMsg 
			    + ".getSocketInfo: returning non SSL socketInfo");
	    }

	    if (! previous.isEmpty()) {
		if (_logger.isLoggable(Level.FINE)) {
		    _logger.log(Level.FINE, baseMsg 
				+ ".getSocketInfo: returning previous socketInfo: "
				+ previous);
		}
		return previous;
	    }

	    //
	    // Save and add primary address
	    //

	    SocketInfo primarySocketInfo = 
		createSocketInfo("primary", type, host, port);
	    result.add(primarySocketInfo);

	    //
	    // List alternate addresses.
	    //

	    Iterator iterator = iiopProfileTemplate.iteratorById(
	        org.omg.IOP.TAG_ALTERNATE_IIOP_ADDRESS.value
		);

	    while (iterator.hasNext()) {
		AlternateIIOPAddressComponent alternate =
		    (AlternateIIOPAddressComponent) iterator.next();
		
		host = alternate.getAddress().getHost().toLowerCase(loc);
		port = alternate.getAddress().getPort();
		
		result.add(createSocketInfo(
		    "AlternateIIOPAddressComponent",
		    SocketInfo.IIOP_CLEAR_TEXT, host, port));
	    }

	    synchronized (this) {
		List existing = (List) 
		    primaryToAddresses.get(primarySocketInfo);
		if ( existing == null ) {
		    // First time we've seen this primary.
		    // Save unrandomized list with primary at head.
		    primaryToAddresses.put(primarySocketInfo, result);
		    result.remove(0); // do not randomize primary
		    // Randomized the alternates.
		    java.util.Collections.shuffle(result); 
		    result.add(0, primarySocketInfo); // put primary at head
		    // Save for subsequent use.
		    primaryToRandomizedAddresses.put(primarySocketInfo,result);
		    if (_logger.isLoggable(Level.FINE)) {
			_logger.log(Level.FINE, baseMsg 
				    + ".getSocketInfo: initial randomized result: "
				    + result);
		    }
		    return result;
		} else {
		    if ( result.equals(existing) ) {
			// The are the same so return the randomized version.
			result = (List)
			    primaryToRandomizedAddresses.get(primarySocketInfo);
			if (_logger.isLoggable(Level.FINE)) {
			    _logger.log(Level.FINE, baseMsg 
					+ ".getSocketInfo: existing randomized result: "
					+ result);
			}
			return result;
		    } else {
			// The lists should be the same.
			// If not log a warning and return the 
			// non-randomized current list since it is different.
			_logger.log(Level.FINE, 
				    baseMsg + ".getSocketInfo:"
				    + " Address lists do not match: primary: "
				    + primarySocketInfo
				    + "; returning current: " + result
				    + "; existing is: " + existing);
			return result;
		    }
		}
	    }
        } catch ( Exception ex ) {
	    _logger.log(Level.WARNING, "Exception getting SocketInfo",ex);
	    RuntimeException rte = new RuntimeException(ex);
	    rte.initCause(ex);
            throw rte;
        } finally {
	    if (_logger.isLoggable(Level.FINE)) {
		_logger.log(Level.FINE, baseMsg + ".getSocketInfo<-:");
	    }
	}
    }

    //----- END implements com.sun.corba.ee.spi.transport.IORToSocketInfo -----

    public static SocketInfo createSocketInfo(String msg,
					      final String type,
					      final String host,
					      final int port) 
    {
	if (_logger.isLoggable(Level.FINE)) {
	    _logger.log(Level.FINE,
			"Address from: "
			+ msg
			+ "; type/address/port: "
			+ type + "/" + host + "/" + port);
	}

        return new SocketInfo() {
                public String getType() {
                    return type;
                }

                public String getHost() {
                    return host;
                }

                public int getPort() {
                    return port;
                }

		public boolean equals(Object o) {
		    if (o == null) {
			return false;
		    }
		    if (! (o instanceof SocketInfo)) {
			return false;
		    }
		    SocketInfo other = (SocketInfo)o;
		    if (other.getPort() != port) {
			return false;
		    }
		    if (! other.getHost().equals(host)) {
			return false;
		    }
		    if (! other.getType().equals(type)) {
			return false;
		    }
		    return true;
		}

		public int hashCode() {
		    return type.hashCode() ^ host.hashCode() ^ port;
		}

		public String toString() {
		    return "SocketInfo[" + type + " " + host + " " + port +"]";
		}
            };
    }
}

// End of file.

