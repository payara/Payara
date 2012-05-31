/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2012 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.corba.ee.spi.folb.ClusterInstanceInfo;
import com.sun.corba.ee.spi.folb.GroupInfoService;
import com.sun.corba.ee.spi.folb.GroupInfoServiceObserver;
import org.glassfish.logging.annotation.LogMessageInfo;

import java.util.List;
import java.util.logging.Level;

import static org.glassfish.enterprise.iiop.impl.NamingClusterInfoImpl.logger;

/**
 * Called when the GroupInfoService that you register with
 * has a change.  You should call the GroupInfoService
 * <code>getClusterInstanceInfo</code> method to get
 * updated info.
 * @author Ken Cavanaugh
 * @author Sheetal Vartak
 */
public class GroupInfoServiceObserverImpl implements GroupInfoServiceObserver {
    @LogMessageInfo(message = "Problem with membership change notification. Exception occurred : {0}",
    cause = "check server.log for details",
    action = "check network configuration and cluster setup")
    public static final String GROUPINFOSERVICE_MEMBERSHIP_NOTIFICATION_PROBLEM = "AS-ORB-00003";

    private GroupInfoService gis;
    private RoundRobinPolicy rr ;

    public GroupInfoServiceObserverImpl(GroupInfoService gis,
        RoundRobinPolicy rr ) {

	this.gis = gis;
        this.rr = rr ;
    }

    // This method is called for internally forced updates: 
    // see SerialInitContextFactory.getInitialContext.
    public void forceMembershipChange() {
        doMembershipChange();
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "GroupInfoServiceObserverImpl.forceMembershipChange called");
        }
    }

    @Override
    // This method is called when the client is informed about a cluster
    // membership change through ClientGroupManager.receive_star.
    public void membershipChange() {
        doMembershipChange();
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "GroupInfoServiceObserverImpl.membershipChange called");
        }
    }

    private void doMembershipChange() {
        try {
            List<ClusterInstanceInfo> instanceInfoList =
                    gis.getClusterInstanceInfo(null, rr.getHostPortList());
            if (instanceInfoList != null && instanceInfoList.size() > 0) {
                rr.setClusterInstanceInfo(instanceInfoList);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, GROUPINFOSERVICE_MEMBERSHIP_NOTIFICATION_PROBLEM, e);
            logger.log(Level.SEVERE, "", e);
        }
    }
}
