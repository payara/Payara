/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2018-2021] Payara Foundation and/or affiliates

package org.glassfish.admin.mbeanserver;

import java.lang.management.ManagementFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.StandardMBean;
import javax.management.ObjectName;
import javax.management.remote.JMXServiceURL;

import org.glassfish.external.amx.BootAMXMBean;
import org.glassfish.external.amx.AMXGlassfish;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.jvnet.hk2.annotations.Service;

/**
 * The MBean implementation for BootAMXMBean.
 *
 * Public API is the name of the booter MBean eg {@link BootAMXMBean.OBJECT_NAME}
 */
@Service
public final class BootAMX implements BootAMXMBean {

    private static final Logger JMX_LOGGER = Util.JMX_LOGGER;

    @Inject
    private ServiceLocator mHabitat;    
    private final MBeanServer mMBeanServer;
    private final ObjectName mObjectName;
    private ObjectName mDomainRootObjectName;
    
    public BootAMX(){
        mMBeanServer = ManagementFactory.getPlatformMBeanServer();
        mObjectName = getBootAMXMBeanObjectName();
        mDomainRootObjectName = null;
        if (mMBeanServer.isRegistered(mObjectName)) {
            throw new IllegalStateException("AMX Booter MBean is already registered: " + mObjectName);
        }
    }
    
    @PostConstruct
    public void postConstuct(){
        try {
            final StandardMBean mbean = new StandardMBean(this, BootAMXMBean.class);

            if (!mMBeanServer.registerMBean(mbean, mObjectName).getObjectName().equals(mObjectName)) {
                throw new IllegalStateException();
            }
        } catch (JMException e) {
            JMX_LOGGER.log(Level.SEVERE, null, e);
            throw new IllegalStateException(e);
        }
    }

    public static ObjectName getBootAMXMBeanObjectName() {
        return AMXGlassfish.DEFAULT.getBootAMXMBeanObjectName();
    }

    AMXStartupServiceMBean getLoader() {
        try {
            return mHabitat.getService(AMXStartupServiceMBean.class);
        } catch (Throwable t) {
            JMX_LOGGER.log(Level.SEVERE, null, t);
            throw new RuntimeException(t);
        }
    }

    /**
     * We need to dynamically load the AMX module. HOW? we can't depend on the amx-impl module.
     *
     * For now though, assume that a well-known MBean is available through other means via the amx-impl module.
     * @return 
     */
    @Override
    public synchronized ObjectName bootAMX() {
        if (mDomainRootObjectName == null) {
            getLoader();
            final ObjectName startupON = AMXStartupServiceMBean.OBJECT_NAME;
            if (!mMBeanServer.isRegistered(startupON)) {
                JMX_LOGGER.log(Level.FINEST, "Booter.bootAMX(): AMX MBean not yet available: {0}", startupON);
                throw new IllegalStateException("AMX MBean not yet available: " + startupON);
            }

            try {
                JMX_LOGGER.log(Level.FINEST, "Booter.bootAMX: invoking loadAMXMBeans() on {0}", startupON);
                mDomainRootObjectName = (ObjectName) mMBeanServer.invoke(startupON, "loadAMXMBeans", null, null);
                JMX_LOGGER.log(Level.FINEST, "Booter.bootAMX: domainRoot = {0}", mDomainRootObjectName);
            } catch (final JMException e) {
                JMX_LOGGER.log(Level.SEVERE, null, e);
                throw new RuntimeException(e);
            }
        }

        return mDomainRootObjectName;
    }

    /**
     * Return the JMXServiceURLs for all connectors we've loaded.
     * @return 
     */
    @Override
    public JMXServiceURL[] getJMXServiceURLs() {
        return JMXStartupService.getJMXServiceURLs(mMBeanServer);
    }

    @LogMessageInfo(
            message = "Error while shutting down AMX",
            level = "WARNING")
    static final String errorDuringShutdown = Util.LOG_PREFIX + "-00008";

    @PreDestroy
    public void shutdown() {
        try {
            if (mMBeanServer.isRegistered(getBootAMXMBeanObjectName())) {
                mMBeanServer.unregisterMBean(getBootAMXMBeanObjectName());
            }
        } catch (final Exception e) {
            Util.getLogger().log(Level.WARNING, errorDuringShutdown, e);
        }
    }
}
