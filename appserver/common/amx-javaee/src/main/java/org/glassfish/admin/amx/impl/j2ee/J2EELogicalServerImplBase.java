/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.amx.impl.j2ee;

import org.glassfish.admin.amx.j2ee.J2EELogicalServer;

import javax.management.AttributeNotFoundException;
import javax.management.ObjectName;
import java.io.Serializable;
import java.util.Map;

/**
Base interface only (for cluster and standalone server)
 */
public class J2EELogicalServerImplBase
        extends J2EEManagedObjectImplBase {

    public J2EELogicalServerImplBase(
            final ObjectName parentObjectName,
            final Metadata meta,
            final Class<? extends J2EELogicalServer> intf) {
        super(parentObjectName, meta, intf);
    }

    public int getstate() {
        throw new RuntimeException(new AttributeNotFoundException("state"));
    }

    public void start() {
        throw new RuntimeException("can't start");
    }

    public void startRecursive() {
        throw new RuntimeException("can't startRecursive");
    }

    public void stop() {
        throw new RuntimeException("can't stop");
    }

    /**
     * starts the app
     */
    public void startApp(String appID, Map<String, Serializable> optional) {
        /*
        final OldApplicationsConfigMBean oldApplicationsMBean =
        getOldConfigProxies().getOldApplicationsConfigMBean();

        final Map<String,Serializable> m = TypeCast.asMap(
        oldApplicationsMBean.startAndReturnStatusAsMap( appID, getSelfName(), optional ) );
        checkDeploymentStatusForExceptions( m );
         */
    }

    /**
     * stops the app
     */
    public void stopApp(String appID, Map<String, Serializable> optional) {
        /*
        final OldApplicationsConfigMBean oldApplicationsMBean =
        getOldConfigProxies().getOldApplicationsConfigMBean();

        final Map<String,Serializable>    m = TypeCast.asMap(
        oldApplicationsMBean.stopAndReturnStatusAsMap( appID, getSelfName(), optional ) );

        checkDeploymentStatusForExceptions( m );
         */
    }
    /**
     * Checks the DeploymentStatus and all substages.
     *
     * Can't depend on SUCCESS or FAILURE as the backend.DeploymentStatus sets
     * the stageStatus to its own codes. Cannot import backend.DeploymentStatus
     * to translate the codes.
    private void
    checkDeploymentStatusForExceptions( Map<String,Serializable > m )
    {
    DeploymentStatus status = DeploymentSupport.mapToDeploymentStatus( m );

    Throwable t = status.getStageThrowable();

    final Iterator<DeploymentStatus> it = status.getSubStagesList().iterator();
    while ( ( t == null ) && ( it.hasNext() ) )
    {
    final DeploymentStatus m1 = it.next();
    t = status.getThrowable();
    }
    if ( null != t )
    {
    throw new RuntimeException( status.getStageStatusMessage() );
    }
    }
     */
}

