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

//
// Created       : 2005 Jul 29 (Fri) 08:23:33 by Harold Carr.
// Last Modified : 2005 Aug 31 (Wed) 19:57:12 by Harold Carr.
//

package org.glassfish.enterprise.iiop.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.IOP.TaggedComponent;
import org.omg.PortableInterceptor.IORInfo;

import com.sun.corba.ee.spi.folb.ClusterInstanceInfo;
import com.sun.corba.ee.impl.folb.CSIv2SSLTaggedComponentHandler;
import com.sun.corba.ee.spi.ior.IOR;
import com.sun.corba.ee.spi.orb.DataCollector;
import com.sun.corba.ee.spi.orb.ORB;
import com.sun.corba.ee.spi.orb.ORBConfigurator;
import com.sun.corba.ee.spi.transport.SocketInfo;

import com.sun.corba.ee.spi.misc.ORBConstants;
import com.sun.corba.ee.spi.ior.iiop.IIOPProfileTemplate ;
import com.sun.corba.ee.spi.ior.iiop.IIOPAddress ;

// END imports for getSocketInfo code

import com.sun.logging.LogDomains;
//
import org.glassfish.enterprise.iiop.api.IIOPSSLUtil;
import org.glassfish.internal.api.Globals;

/**
 * @author Harold Carr
 */
public class CSIv2SSLTaggedComponentHandlerImpl
    extends org.omg.CORBA.LocalObject
    implements CSIv2SSLTaggedComponentHandler,
	       ORBConfigurator
{
    private static final Logger _logger = LogDomains.getLogger(
        CSIv2SSLTaggedComponentHandlerImpl.class, LogDomains.CORBA_LOGGER);

    private final String baseMsg = 
	CSIv2SSLTaggedComponentHandlerImpl.class.getName();

    private ORB orb;

    ////////////////////////////////////////////////////
    //
    // CSIv2SSLTaggedComponentHandler
    //

    @Override
    public TaggedComponent insert(IORInfo iorInfo, 
 				  List<ClusterInstanceInfo> clusterInstanceInfo)
    {
	TaggedComponent result = null;
	try {
	    if (_logger.isLoggable(Level.FINE)) {
		_logger.log(Level.FINE, "{0}.insert->:", baseMsg);
	    }

            List<com.sun.corba.ee.spi.folb.SocketInfo> socketInfos =
                new ArrayList<com.sun.corba.ee.spi.folb.SocketInfo>();
            for(ClusterInstanceInfo clInstInfo : clusterInstanceInfo){
                for (com.sun.corba.ee.spi.folb.SocketInfo sinfo :
                    clInstInfo.endpoints()) {
                    if (sinfo.type().equals("SSL")
                       || sinfo.type().equals("SSL_MUTUALAUTH")){
                       socketInfos.add(sinfo);
                    }
                }                
            }
            IIOPSSLUtil sslUtil = null;
            if (Globals.getDefaultHabitat() != null) {
                sslUtil =
                    Globals.getDefaultHabitat().getService(IIOPSSLUtil.class);
                return sslUtil.createSSLTaggedComponent(iorInfo, socketInfos);
            } else {
                return null;
            }
           
	} finally {
	    if (_logger.isLoggable(Level.FINE)) {
		_logger.log(Level.FINE, "{0}.insert<-: {1}",
                    new Object[]{baseMsg, result});
	    }
	}
    }

    @Override
    public List<SocketInfo> extract(IOR ior)
    {
	List<SocketInfo> socketInfo = null;
        try {
	    if (_logger.isLoggable(Level.FINE)) {
		_logger.log(Level.FINE, "{0}.extract->:", baseMsg);
	    }

            // IIOPProfileTemplate iiopProfileTemplate = (IIOPProfileTemplate)ior.getProfile().getTaggedProfileTemplate();
            // IIOPAddress primary = iiopProfileTemplate.getPrimaryAddress() ;
            // String host = primary.getHost().toLowerCase(Locale.ENGLISH);

            IIOPSSLUtil sslUtil = null;
            if (Globals.getDefaultHabitat() != null) {
                sslUtil = Globals.getDefaultHabitat().getService(
                    IIOPSSLUtil.class);
                socketInfo = (List<SocketInfo>)sslUtil.getSSLPortsAsSocketInfo(
                    ior);
            }

            if (socketInfo == null) {
                if (_logger.isLoggable(Level.FINE)) {
		    _logger.log(Level.FINE, 
                        "{0}.extract: did not find SSL SocketInfo", baseMsg);
		}
            } else {
                if (_logger.isLoggable(Level.FINE)) {
		    _logger.log(Level.FINE, 
                        "{0}.extract: found SSL socketInfo", baseMsg);
		}
            }        
	    if (_logger.isLoggable(Level.FINE)) {
		_logger.log(Level.FINE, 
                    "{0}.extract: Connection Context", baseMsg);
	    }
        } catch ( Exception ex ) {
	    _logger.log(Level.WARNING, "Exception getting SocketInfo", ex);
        } finally {
	    if (_logger.isLoggable(Level.FINE)) {
		_logger.log(Level.FINE, 
                    "{0}.extract<-: {1}", new Object[]{baseMsg, socketInfo});
	    }
	}
	return socketInfo;
    }

    ////////////////////////////////////////////////////
    //
    // ORBConfigurator
    //

    @Override
    public void configure(DataCollector collector, ORB orb) 
    {
	if (_logger.isLoggable(Level.FINE)) {
	    _logger.log(Level.FINE, ".configure->:");
	}

	this.orb = orb;
	try {
	    orb.register_initial_reference(
	        ORBConstants.CSI_V2_SSL_TAGGED_COMPONENT_HANDLER,
	        this);
	} catch (InvalidName e) {
	    _logger.log(Level.WARNING, ".configure: ", e);
	}

	if (_logger.isLoggable(Level.FINE)) {
	    _logger.log(Level.FINE, ".configure<-:");
	}
    }
}

// End of file.


