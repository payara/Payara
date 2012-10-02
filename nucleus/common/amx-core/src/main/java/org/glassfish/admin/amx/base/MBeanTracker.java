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
package org.glassfish.admin.amx.base;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.management.*;
import org.glassfish.admin.amx.core.AMXMBeanMetadata;
import org.glassfish.admin.amx.core.Util;
import org.glassfish.admin.amx.util.jmx.JMXUtil;
import org.glassfish.external.amx.AMX;
import org.glassfish.external.arc.Stability;
import org.glassfish.external.arc.Taxonomy;

/**
 * Tracks the entire MBean parent/child hierarachy so that individual MBeans
 * need not do so. Can supply parents and children of any MBean, used by all AMX
 * implementations.
 */
@Taxonomy(stability = Stability.NOT_AN_INTERFACE)
@AMXMBeanMetadata(singleton = true, globalSingleton = true, leaf = true)
public final class MBeanTracker implements NotificationListener, MBeanRegistration, MBeanTrackerMBean {

    private static void debug(final Object o) {
        System.out.println("" + o);
    }
    /**
     * maps a parent ObjectName to a Set of children
     */
    final ConcurrentMap<ObjectName, Set<ObjectName>> mParentChildren;
    /**
     * maps a child to its parent, needed because when unregistered we can't
     * obtain parent
     */
    final ConcurrentMap<ObjectName, ObjectName> mChildParent;
    private volatile MBeanServer mServer;
    private volatile ObjectName mObjectName;
    private final String mDomain;
    private volatile boolean mEmitMBeanStatus;

    public MBeanTracker(final String jmxDomain) {
        mParentChildren = new ConcurrentHashMap<ObjectName, Set<ObjectName>>();
        mChildParent = new ConcurrentHashMap<ObjectName, ObjectName>();

        mDomain = jmxDomain;

        mEmitMBeanStatus = false;
    }

    @Override
    public boolean getEmitMBeanStatus() {
        return mEmitMBeanStatus;
    }

    @Override
    public void setEmitMBeanStatus(final boolean emit) {
        mEmitMBeanStatus = emit;
    }

    @Override
    public void handleNotification(final Notification notifIn, final Object handback) {
        if (notifIn instanceof MBeanServerNotification) {
            final MBeanServerNotification notif = (MBeanServerNotification) notifIn;

            final String type = notif.getType();
            final ObjectName objectName = notif.getMBeanName();

            if (isRelevantMBean(objectName)) {
                // what happens if an MBean is removed before we can add it
                // eg the MBeanServer uses more than one thread to deliver notifications
                // to use? Even if we synchronize this method, the remove could still arrive
                // first and there's nothing we could do about it.
                if (type.equals(MBeanServerNotification.REGISTRATION_NOTIFICATION)) {
                    if (mEmitMBeanStatus) {
                        System.out.println("AMX MBean registered: " + objectName);
                    }
                    addChild(objectName);
                } else if (type.equals(MBeanServerNotification.UNREGISTRATION_NOTIFICATION)) {
                    if (mEmitMBeanStatus) {
                        System.out.println("AMX MBean UNregistered: " + objectName);
                    }
                    removeChild(objectName);
                }
            }
        }
    }

    @Override
    public ObjectName preRegister(
            final MBeanServer server,
            final ObjectName nameIn)
            throws Exception {
        mServer = server;
        mObjectName = nameIn;
        return (nameIn);
    }

    @Override
    public final void postRegister(final Boolean registrationSucceeded) {
        if (mServer == null) {
            return;
        }
        if (registrationSucceeded.booleanValue()) {
            try {
                mServer.addNotificationListener(JMXUtil.getMBeanServerDelegateObjectName(), this, null, null);
            } catch (Exception e) {
                throw new RuntimeException("Could not register with MBeanServerDelegate", e);
            }
            //debug( "MBeanTracker: registered as " + mObjectName );
        }
        // populate our list
        final ObjectName pattern = Util.newObjectNamePattern(mDomain, "");
        final Set<ObjectName> names = JMXUtil.queryNames(mServer, pattern, null);
        //debug( "MBeanTracker: found MBeans: " + names.size() );
        for (final ObjectName o : names) {
            addChild(o);
        }
    }

    @Override
    public final void preDeregister() throws Exception {
        if (mServer != null) {
            mServer.removeNotificationListener(mObjectName, this);
        }
    }

    @Override
    public final void postDeregister() {
    }

    private boolean isRelevantMBean(final ObjectName child) {
        return child != null && mDomain.equals(child.getDomain());
    }

    private void addChild(final ObjectName child) {
        if (mServer == null) {
            return;
        }
        ObjectName parent = null;
        try {
            parent = (ObjectName) mServer.getAttribute(child, AMX.ATTR_PARENT);
        } catch (final Exception e) {
            // nothing to be done, MBean gone missing, badly implemented, etc.
            //System.out.println( "No Parent for: " + child );
        }

        if (parent != null) {
            synchronized (this) {
                mChildParent.put(child, parent);
                Set<ObjectName> children = mParentChildren.get(parent);
                if (children == null) {
                    children = new HashSet<ObjectName>();
                    mParentChildren.put(parent, children);
                }
                children.add(child);
                //debug( "MBeanTracker: ADDED " + child + " with parent " + parent );
            }
        }
    }

    /**
     * Must be 'synchronized' because we're working on two different Maps.
     */
    private synchronized ObjectName removeChild(final ObjectName child) {
        final ObjectName parent = mChildParent.remove(child);
        if (parent != null) {
            final Set<ObjectName> children = mParentChildren.get(parent);
            if (children != null) {
                children.remove(child);
                if (children.isEmpty()) {
                    mParentChildren.remove(parent);
                    //debug( "MBeanTracker: REMOVED " + child + " from parent " + parent );
                }
            }
        }
        return parent;
    }

    @Override
    public ObjectName getParentOf(final ObjectName child) {
        return mChildParent.get(child);
    }

    @Override
    public synchronized Set<ObjectName> getChildrenOf(final ObjectName parent) {
        final Set<ObjectName> children = mParentChildren.get(parent);
        if (children == null) {
            return Collections.emptySet();
        }

        return new HashSet<ObjectName>(children);
    }
}
