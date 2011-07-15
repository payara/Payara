/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.amx.impl.ext;

import org.glassfish.admin.amx.base.DomainRoot;
import org.glassfish.admin.amx.core.proxy.ProxyFactory;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;


import org.glassfish.admin.amx.base.Realms;
import org.glassfish.admin.amx.base.RuntimeRoot;
import org.glassfish.admin.amx.base.ConnectorRuntimeAPIProvider;
import org.glassfish.admin.amx.base.SystemStatus;
import org.glassfish.admin.amx.logging.Logging;
import org.glassfish.admin.amx.config.AMXConfigConstants;
import org.glassfish.admin.amx.impl.config.AMXExtStartupServiceMBean;
import org.glassfish.admin.amx.impl.mbean.AMXImplBase;
import org.glassfish.admin.amx.impl.util.InjectedValues;
import org.glassfish.admin.amx.impl.util.ObjectNameBuilder;
import org.glassfish.admin.amx.util.FeatureAvailability;

/**
Startup service that loads support for AMX config MBeans.  How this is to be
triggered is not yet clear.
 */
@Service
public final class AMXExtStartupService
        implements org.jvnet.hk2.component.PostConstruct,
        org.jvnet.hk2.component.PreDestroy,
        AMXExtStartupServiceMBean
{
    private static void debug(final String s)
    {
        System.out.println(s);
    }

    @Inject
    InjectedValues mInjectedValues;

    @Inject//(name=AppserverMBeanServerFactory.OFFICIAL_MBEANSERVER)
    private MBeanServer mMBeanServer;

    private volatile boolean mLoaded = false;

    public AMXExtStartupService()
    {
    }

    public void postConstruct()
    {
        try
        {
            final StandardMBean mbean = new StandardMBean(this, AMXExtStartupServiceMBean.class);
            mMBeanServer.registerMBean(mbean, OBJECT_NAME);
        }
        catch (JMException e)
        {
            throw new Error(e);
        }
    }

    public void preDestroy()
    {
        unloadAMXMBeans();
    }

    public DomainRoot getDomainRootProxy()
    {
        return ProxyFactory.getInstance(mMBeanServer).getDomainRootProxy(false);
    }

    public synchronized ObjectName loadAMXMBeans()
    {
        if (!mLoaded)
        {
            mLoaded = true;
            FeatureAvailability.getInstance().waitForFeature(FeatureAvailability.AMX_CORE_READY_FEATURE, "AMXExtStartupService.loadAMXMBeans");
            FeatureAvailability.getInstance().waitForFeature(AMXConfigConstants.AMX_CONFIG_READY_FEATURE, "AMXExtStartupService.loadAMXMBeans");

            AMXImplBase mbean;
            final MBeanServer s = mMBeanServer;
            final ObjectName domainRoot = getDomainRootProxy().objectName();
            
            // Register children of DomainRoot
            final ObjectName loggingObjectName = ObjectNameBuilder.buildChildObjectName( s, domainRoot, Logging.class );
            mbean = new LoggingImpl(domainRoot, "server");
            registerChild(mbean, loggingObjectName);

            final ObjectName runtimeObjectname = ObjectNameBuilder.buildChildObjectName( s, domainRoot, RuntimeRoot.class );
            mbean = new RuntimeRootImpl(domainRoot);
            registerChild(mbean, runtimeObjectname);


            // register all children of Ext

            ObjectName child;
            final ObjectName ext = getDomainRootProxy().getExt().objectName();
            final ObjectNameBuilder names = new ObjectNameBuilder(s, ext);
            
            child = names.buildChildObjectName(SystemStatus.class);
            mbean = new SystemStatusImpl(ext);
            registerChild(mbean, child);

            child = names.buildChildObjectName(Realms.class);
            mbean = new RealmsImpl(ext);
            registerChild(mbean, child);

            child = names.buildChildObjectName(ConnectorRuntimeAPIProvider.class);
            mbean = new ConnectorRuntimeAPIProviderImpl(ext, InjectedValues.getInstance().getHabitat());
            registerChild(mbean, child);

            //final GmbalMOM mom = new GmbalMOM( s, getDomainRootProxy().objectName() );
            //mom.registerChildren();
        }
        return null;
    }

    protected synchronized ObjectName registerChild(final Object mbean, final ObjectName childObjectName)
    {
        try
        {
            final ObjectName objectName = mMBeanServer.registerMBean(mbean, childObjectName).getObjectName();
            return objectName;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public synchronized void unloadAMXMBeans()
    {
        // final Set<ObjectName> children = MBeanTracker.getInstance();
    }

}












