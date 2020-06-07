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
// Portions Copyright [2018] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.iiop.security;

import org.glassfish.orb.admin.config.IiopListener;
import java.util.logging.*;

import static java.util.logging.Level.FINE;

import java.util.List;

import org.omg.IOP.Codec;
import org.omg.IOP.TaggedComponent;
import org.omg.PortableInterceptor.IORInfo;

import com.sun.logging.*;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.Servers;
import com.sun.enterprise.deployment.EjbDescriptor;
import fish.payara.nucleus.cluster.PayaraCluster;

import org.glassfish.enterprise.iiop.api.GlassFishORBFactory;
import org.glassfish.enterprise.iiop.impl.GlassFishORBManager;
import org.glassfish.enterprise.iiop.util.IIOPUtils;
import org.omg.CORBA.ORB;

public class SecIORInterceptor extends org.omg.CORBA.LocalObject implements org.omg.PortableInterceptor.IORInterceptor {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private static Logger _logger = null;

    static {
        _logger = LogDomains.getLogger(SecIORInterceptor.class, LogDomains.SECURITY_LOGGER);
    }

    private Codec codec;
    private GlassFishORBFactory orbFactory;

    // private GlassFishORBHelper helper = null;
    private ORB orb;

    public SecIORInterceptor(Codec c, ORB orb) {
        codec = c;
        this.orb = orb;
        
        orbFactory = Lookups.getGlassFishORBFactory();	   
    }

    @Override
    public void destroy() {
    }

    @Override
    public String name() {
        return "SecIORInterceptor";
    }

    // Note: this is called for all remote refs created from this ORB,
    // including EJBs and COSNaming objects.
    @Override
    public void establish_components(IORInfo iorInfo) {
        try {
            _logger.log(Level.FINE, "SecIORInterceptor.establish_components->:");
            // EjbDescriptor desc = CSIV2TaggedComponentInfo.getEjbDescriptor( iorInfo ) ;
            addCSIv2Components(iorInfo);
        } catch (Exception e) {
            _logger.log(Level.WARNING, "Exception in establish_components", e);
        } finally {
            _logger.log(Level.FINE, "SecIORInterceptor.establish_components<-:");
        }
    }

    private void addCSIv2Components(IORInfo iorInfo) {
        EjbDescriptor desc = null;
        
        try {
            if (_logger.isLoggable(FINE)) {
                _logger.log(FINE, ".addCSIv2Components->: " + " " + iorInfo);
            }

            if (orbFactory != null && orbFactory.isClusterActive()) {

                // If this app server instance is part of a dynamic cluster (that is, one 
            	// that supports RMI-IIOP fail-over and load balancing, or a deployment
            	// group exists (hazelcast cluster with more than one member, then DO NOT
            	// create the CSIv2 components here. 
            	// 
            	// Instead, handle this in the ORB's ServerGroupManager, in conjunctions with the
                // CSIv2SSLTaggedComponentHandler.
            	// See org.glassfish.enterprise.iiop.impl.GlassFishORBManager.setFOLBProperties(Properties)
                
            	return;
            }

            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, ".addCSIv2Components ");
            }

            int sslMutualAuthPort = getServerPort("SSL_MUTUALAUTH");

            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, ".addCSIv2Components: sslMutualAuthPort: " + sslMutualAuthPort);
            }

            CSIV2TaggedComponentInfo ctc = new CSIV2TaggedComponentInfo(orb, sslMutualAuthPort);
            desc = ctc.getEjbDescriptor(iorInfo);

            // Create CSIv2 tagged component
            int sslport = getServerPort("SSL");
            
            if (_logger.isLoggable(FINE)) {
                _logger.log(FINE, ".addCSIv2Components: sslport: " + sslport);
            }

            TaggedComponent csiv2Comp = null;
            if (desc != null) {
                csiv2Comp = ctc.createSecurityTaggedComponent(sslport, desc);
            } else {
                // this is not an EJB object, must be a non-EJB CORBA object
                csiv2Comp = ctc.createSecurityTaggedComponent(sslport);
            }

            iorInfo.add_ior_component(csiv2Comp);

        } finally {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, ".addCSIv2Components<-: " + " " + iorInfo + " " + desc);
            }
        }
    }

    private int getServerPort(String mech) {

        List<IiopListener> listenersList = IIOPUtils.getInstance().getIiopService().getIiopListener();
        IiopListener[] iiopListenerBeans = listenersList.toArray(new IiopListener[listenersList.size()]);

        for (IiopListener ilisten : iiopListenerBeans) {
            if (mech.equalsIgnoreCase("SSL")) {
                if (ilisten.getSecurityEnabled().equalsIgnoreCase("true") && ilisten.getSsl() != null
                        && !ilisten.getSsl().getClientAuthEnabled().equalsIgnoreCase("true")) {
                    return Integer.parseInt(ilisten.getPort());
                }
            } else if (mech.equalsIgnoreCase("SSL_MUTUALAUTH")) {
                if (ilisten.getSecurityEnabled().equalsIgnoreCase("true") && ilisten.getSsl() != null
                        && ilisten.getSsl().getClientAuthEnabled().equalsIgnoreCase("true")) {
                    return Integer.parseInt(ilisten.getPort());
                }
            } else if (!ilisten.getSecurityEnabled().equalsIgnoreCase("true")) {
                return Integer.parseInt(ilisten.getPort());
            }
        }
        return -1;
    }
}

// End of file.
