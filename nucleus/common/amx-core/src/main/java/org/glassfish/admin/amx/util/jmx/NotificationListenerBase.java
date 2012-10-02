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

package org.glassfish.admin.amx.util.jmx;

import java.io.IOException;
import java.util.Collections;

import java.util.HashSet;
import java.util.Set;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import org.glassfish.admin.amx.util.SetUtil;

/**
Convenience base class for listening for Notifications
from one or more MBeans, which may be specified as
a specific MBean ObjectName, or an ObjectName pattern.
If the ObjectName is a pattern, the list of listenees
is dynamically maintained.
<p>
Caller should call {@link #cleanup} when done, because
a listener is maintained on the MBeanServer delegate.

 */
public abstract class NotificationListenerBase
        implements NotificationListener
{
    private final MBeanServerConnection mConn;

    /** actual MBean ObjectNames, not patterns */
    private final Set<ObjectName> mListenees;

    /** targets as specified by caller, may be a pattern or fixed ObjectName */
    private final ObjectName mPattern;

    private final NotificationFilter mFilter;

    private RegistrationListener mDelegateListener;

    private volatile boolean mSetupListening;

    /**
    Calls this( conn, listenTo, null, null ).
    <p><b>Instantiating code must call setupListening() in order to initiate
    listening</b>
     */
    protected NotificationListenerBase(
            final String name,
            final MBeanServerConnection conn,
            final ObjectName pattern)
            throws IOException
    {
        this(name, conn, pattern, null);
    }

    /**
    Listen to all MBean(s) which match the pattern 'listenTo'.
    <p><b>Instantiating code must call setupListening() in order to initiate
    listening</b>
    @param name arbitrary name of this listener
    @param conn the MBeanServerConnection or MBeanServer
    @param pattern an MBean ObjectName, or an ObjectName pattern
    @param filter optional NotificationFilter
     */
    protected NotificationListenerBase(
            final String name,
            final MBeanServerConnection conn,
            final ObjectName pattern,
            final NotificationFilter filter)
            throws IOException
    {
        mConn = conn;
        mPattern = pattern;
        mFilter = filter;
        mDelegateListener = null;
        mSetupListening = false;

        mListenees = Collections.synchronizedSet(new HashSet<ObjectName>());

        // test connection for validity
        if (!conn.isRegistered(JMXUtil.getMBeanServerDelegateObjectName()))
        {
            throw new IllegalArgumentException();
        }
    }

    /**
    Subclass should implement this routine.
     */
    @Override
    public abstract void handleNotification(final Notification notif, final Object handback);

    protected synchronized void listenToMBean(final ObjectName objectName)
            throws InstanceNotFoundException, IOException
    {
        if (!mListenees.contains(objectName))
        {
            mListenees.add(objectName);
            getMBeanServerConnection().addNotificationListener(
                    objectName, this, mFilter, null);
        }
    }

    public synchronized void startListening()
            throws InstanceNotFoundException, IOException
    {
        if (mSetupListening)
        {
            throw new IllegalStateException("setupListening() must be called exactly once");
        }

        if (mPattern.isPattern())
        {
            // it's crucial we listen for registration/unregistration events
            // so that any patterns are maintained.
            // do this BEFORE the code below, of we could
            // miss a registration.
            mDelegateListener = new RegistrationListener();
            JMXUtil.listenToMBeanServerDelegate(mConn,
                    mDelegateListener, null, null);
        }


        Set<ObjectName> s;

        if (mPattern.isPattern())
        {
            s = JMXUtil.queryNames(getConn(), mPattern, null);
        }
        else
        {
            s = SetUtil.newSet(mPattern);
        }
       
        for (final ObjectName objectName : s)
        {
            listenToMBean(objectName);
        }
        
        mSetupListening = true;
    }

    /**
    Get the filter originally specified when constructing this object.
     */
    public final NotificationFilter getNotificationFilter(final ObjectName objectName)
    {
        return mFilter;
    }

    protected synchronized void listenToIfMatch(final ObjectName objectName)
            throws IOException, InstanceNotFoundException
    {
        if (!mListenees.contains(objectName))
        {
            final String defaultDomain = getConn().getDefaultDomain();

            if (JMXUtil.matchesPattern(defaultDomain, mPattern, objectName))
            {
                listenToMBean(objectName);
            }
        }
    }

    /**
    tracks coming and going of MBeans being listened to which
    match our patterns.
     */
    private final class RegistrationListener implements NotificationListener
    {
        public RegistrationListener()
        {
        }

        @Override
        public void handleNotification(
                final Notification notifIn,
                final Object handback)
        {
            if (notifIn instanceof MBeanServerNotification)
            {
                final MBeanServerNotification notif = (MBeanServerNotification) notifIn;

                final ObjectName objectName = notif.getMBeanName();
                final String type = notif.getType();

                try
                {
                    if (type.equals(MBeanServerNotification.REGISTRATION_NOTIFICATION))
                    {
                        listenToIfMatch(objectName);
                    }
                    else if (type.equals(MBeanServerNotification.UNREGISTRATION_NOTIFICATION))
                    {
                        mListenees.remove(objectName);
                    }
                }
                catch (Exception e)
                {
                    // nothing can be done...
                }
            }
        }

    }

    /**
    Reset everything so that no listening is occuring and
    all lists are empty.
     */
    public synchronized void cleanup()
    {
        try
        {
            if (mDelegateListener != null)
            {
                // it's crucial we listen for registration/unregistration events
                // so that any patterns are maintained.
                getConn().removeNotificationListener(
                        JMXUtil.getMBeanServerDelegateObjectName(),
                        mDelegateListener, null, null);
                mDelegateListener = null;
            }

            for (final ObjectName objectName : mListenees)
            {
                getConn().removeNotificationListener(
                        objectName, this, mFilter, null);
            }
        }
        catch (JMException e)
        {
        }
        catch (IOException e)
        {
        }

        mListenees.clear();
    }

    /**
    @return a copy of the MBean currently being listened to.
     */
    public synchronized Set<ObjectName> getListenees()
    {
        final Set<ObjectName> objectNames = new HashSet<ObjectName>();

        synchronized (mListenees)
        {
            objectNames.addAll(mListenees);
        }

        return (objectNames);
    }

    /**
    @return the MBeanServerConnection in use.
    @throws an Exception if no longer alive ( isAlive() returns false).
     */
    public final MBeanServerConnection getMBeanServerConnection()
    {
        return getConn();
    }

    protected final MBeanServerConnection getConn()
    {
        return mConn;
    }

    protected final void checkAlive()
            throws IOException
    {
        if (!isAlive())
        {
            throw new IOException("MBeanServerConnection failed");
        }
    }

    /**
    @return true if still listening and the connection is still alive
     */
    public boolean isAlive()
    {
        boolean isAlive = true;

        if (!(mConn instanceof MBeanServer))
        {
            // remote, check if it is alive
            try
            {
                mConn.isRegistered(JMXUtil.getMBeanServerDelegateObjectName());
            }
            catch (Exception e)
            {
                isAlive = false;
            }
        }
        return isAlive;
    }

}






