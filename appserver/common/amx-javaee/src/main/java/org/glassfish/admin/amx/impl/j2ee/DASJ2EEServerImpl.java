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

package org.glassfish.admin.amx.impl.j2ee;

import java.lang.reflect.Constructor;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import org.glassfish.admin.amx.core.Util;
import org.glassfish.admin.amx.impl.util.Issues;
import org.glassfish.admin.amx.impl.util.ObjectNameBuilder;
import org.glassfish.admin.amx.j2ee.*;
import static org.glassfish.admin.amx.j2ee.StateManageable.*;

/**
JSR 77 extension representing an Appserver standalone server (non-clustered)

Server MBean which will reside on DAS
for enabling state management including start() and stop()
 */
public class DASJ2EEServerImpl extends J2EEServerImpl
        implements NotificationListener {

    public DASJ2EEServerImpl(final ObjectName parentObjectName, final Metadata meta) {
        super(parentObjectName, meta);

        Issues.getAMXIssues().notDone("DASJ2EEServer needs to account for DAS/non-DAS");
    }


    @Override
        protected void
    registerChildren()
    {
        super.registerChildren();
        
        final ObjectNameBuilder builder = getObjectNames();

        final JVMImpl jvm = new JVMImpl( getObjectName(), defaultChildMetadata() );
        final ObjectName jvmObjectName = builder.buildChildObjectName( J2EETypes.JVM, null);
        registerChild( jvm, jvmObjectName );
    }
    
    /*
    static private final Class[]	DOMAIN_STATUS_INTERFACES	=
    new Class[] { DomainStatusMBean.class };

    protected DomainStatusMBean
    getDomainStatus()
    {
    DomainStatusMBean	domainStatus	= null;
    try {
    final MBeanServer	mbeanServer = getMBeanServer();
    final Set<ObjectName>	candidates	= QueryMgrImpl.queryPatternObjectNameSet(
    mbeanServer, JMXUtil.newObjectNamePattern(
    "*", DomainStatusMBean.DOMAIN_STATUS_PROPS ) );
    final ObjectName on = SetUtil.getSingleton( candidates );
    domainStatus = (DomainStatusMBean)MBeanServerInvocationHandler.
    newProxyInstance( mbeanServer, on, DomainStatusMBean.class, false );
    } catch (Exception e) {
    final Throwable rootCause = ExceptionUtil.getRootCause( e );
    getMBeanLogger().warning( rootCause.toString() + "\n" +
    ExceptionUtil.getStackTrace( rootCause ) );
    }
    return( domainStatus );
    }

     */

    private boolean remoteServerIsStartable() {
        final int cState = getstate();

        return (STATE_STOPPED == cState) ||
                (STATE_FAILED == cState);
    }

    private boolean remoteServerIsStoppable() {
        int cState = getstate();

        if ((STATE_STARTING == cState) ||
                (STATE_RUNNING == cState) ||
                (STATE_FAILED == cState)) {
            return true;
        } else {
            return false;
        }
    }

    public void handleNotification(final Notification notif, final Object ignore) {
    }

    protected String getServerName() {
        return Util.getNameProp(getObjectName());
    }

    public boolean isstateManageable() {
        return false;
    }

    /*
    final RuntimeStatus
    getRuntimeStatus(final String serverName )
    {
    final MBeanServer mbeanServer = getMBeanServer();

    final OldServersMBean oldServers =
    OldConfigProxies.getInstance( mbeanServer ).getOldServersMBean( );

    final RuntimeStatus status = oldServers.getRuntimeStatus( serverName );

    return status;
    }
     */
    /**
    Convert an internal status code to JSR 77 StateManageable state.
     *
    private static int
    serverStatusCodeToStateManageableState( final int statusCode )
    {
    int state = STATE_FAILED;
    switch( statusCode )
    {
    default: throw new IllegalArgumentException( "Uknown status code: " + statusCode );

    case Status.kInstanceStartingCode: state = STATE_STARTING; break;
    case Status.kInstanceRunningCode: state = STATE_RUNNING; break;
    case Status.kInstanceStoppingCode: state = STATE_STOPPING; break;
    case Status.kInstanceNotRunningCode: state = STATE_STOPPED; break;
    }

    return state;
    }
     */
    public int getstate() {
        int state = STATE_STOPPED;
        try {
            Issues.getAMXIssues().notDone("DASJ2EEServerImpl.getRuntimeStatus: getRuntimeStatus");
            //final int internalStatus = getRuntimeStatus(getServerName()).getStatus().getStatusCode();
            //state = serverStatusCodeToStateManageableState( internalStatus );
            state = STATE_RUNNING;
        } catch (final Exception e) {
            // not available, must not be running
        }

        return state;
    }

    public void start() {
        if (remoteServerIsStartable()) {
            startRemoteServer();
        } else {
            throw new RuntimeException("server is not in a startable state");
        }
    }

    public void startRecursive() {
        start();
    }
    /** The DAS is always named "server", or so inquiries suggest */
    static final String DAS_SERVER_NAME = "server";

    /**
    Does this particular J2EEServer represent the DAS?
     */
    private boolean isDASJ2EEServer() {
        return DAS_SERVER_NAME.equals(getName());
    }

    public void stop() {
        if (isDASJ2EEServer()) {
            //getDelegate().invoke( "stop", (Object[])null, (String[])null);
        } else if (remoteServerIsStoppable()) {
            //stopRemoteServer();
        } else {
            throw new RuntimeException("server is not in a stoppable state");
        }
    }

    private void startRemoteServer() {
        Issues.getAMXIssues().notDone("DASJ2EEServerImpl.startRemoteServer");
    }

}





