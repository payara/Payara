/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.enterprise.iiop.impl;

import com.sun.enterprise.ee.cms.core.GroupManagementService;
import org.glassfish.enterprise.iiop.api.GlassFishORBFactory;
import org.glassfish.enterprise.iiop.util.IIOPUtils;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.ServiceLocator;
import org.omg.CORBA.ORB;
import org.omg.PortableInterceptor.ServerRequestInfo;

import java.util.Properties;
import org.glassfish.gms.bootstrap.GMSAdapterService;

/**
 * @author Mahesh Kannan
 *         Date: Jan 15, 2009
 */
@Service
public class GlassFishORBFactoryImpl
        implements GlassFishORBFactory, PostConstruct {

    @Inject
    private ServiceLocator habitat;

    @Inject
    private IIOPUtils iiopUtils;

    private GlassFishORBManager gfORBManager = null;

    @Override
    public void postConstruct() {
        gfORBManager = new GlassFishORBManager(habitat);

        IIOPUtils.setInstance(iiopUtils);
        //iiopUtils.setGlassFishORBManager(gfORBManager);
    }

    @Override
    public int getOTSPolicyType() {
        return POARemoteReferenceFactory.OTS_POLICY_TYPE;
    }

    @Override
    public int getCSIv2PolicyType() {
        return POARemoteReferenceFactory.CSIv2_POLICY_TYPE;
    }

    @Override
    public ORB createORB(Properties props) {
        // TODO change this to a create call
       return gfORBManager.getORB(props);
    }

    @Override
    public Properties getCSIv2Props() {
        return gfORBManager.getCSIv2Props();
    }

    @Override
    public void setCSIv2Prop(String name, String value) {
        gfORBManager.setCSIv2Prop(name, value);
    }

    @Override
    public int getORBInitialPort() {
        return gfORBManager.getORBInitialPort();
    }

    @Override
    public String getORBHost(ORB orb) {
        return ((com.sun.corba.ee.spi.orb.ORB) orb).getORBData().getORBInitialHost();
    }

    @Override
    public int getORBPort(ORB orb) {
        return ((com.sun.corba.ee.spi.orb.ORB) orb).getORBData().getORBInitialPort();
    }

    /**
     * Returns true, if the incoming call is a EJB method call.
     * This checks for is_a calls and ignores those calls. In callflow analysis
     * when a component looks up another component, this lookup should be
     * considered part of the same call coming in.
     * Since a lookup triggers the iiop codebase, it will fire a new request start.
     * With this check, we consider the calls that are only new incoming ejb
     * method calls as new request starts.
     */
    @Override
    public boolean isEjbCall (ServerRequestInfo sri) {
        return (gfORBManager.isEjbAdapterName(sri.adapter_name()) &&
                (!gfORBManager.isIsACall(sri.operation())));
    }

    @Override
    public String getIIOPEndpoints() {
        return gfORBManager.getIIOPEndpoints() ;
    }
}
