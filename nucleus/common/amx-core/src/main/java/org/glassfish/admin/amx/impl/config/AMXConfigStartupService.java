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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
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
// Portions Copyright [2019-2021] Payara Foundation and/or affiliates

package org.glassfish.admin.amx.impl.config;

import com.sun.enterprise.config.serverbeans.Domain;
import java.util.logging.Level;

import org.glassfish.admin.amx.base.DomainRoot;
import org.glassfish.admin.amx.core.AMXProxy;
import org.glassfish.admin.amx.core.proxy.ProxyFactory;
import org.glassfish.admin.amx.util.TimingDelta;
import org.jvnet.hk2.annotations.Service;

import jakarta.inject.Inject;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.glassfish.external.arc.Stability;
import org.glassfish.external.arc.Taxonomy;
import org.glassfish.admin.amx.impl.util.ImplUtil;
import org.glassfish.admin.amx.impl.util.InjectedValues;
import org.glassfish.admin.amx.util.AMXLoggerInfo;

import org.glassfish.admin.mbeanserver.PendingConfigBeans;
import org.glassfish.api.amx.AMXLoader;
import org.jvnet.hk2.config.Transactions;

/**
Startup service that loads support for AMX config MBeans.  How this is to be
triggered is not yet clear.
 */
@Taxonomy(stability = Stability.NOT_AN_INTERFACE)
@Service
public final class AMXConfigStartupService
        implements org.glassfish.hk2.api.PostConstruct,
        org.glassfish.hk2.api.PreDestroy,
        AMXLoader {

    @Inject
    InjectedValues mInjectedValues;
    @Inject//(name=AppserverMBeanServerFactory.OFFICIAL_MBEANSERVER)
    private MBeanServer mMBeanServer;
    @Inject
    private volatile PendingConfigBeans mPendingConfigBeans;
    @Inject
    private Transactions mTransactions;
    private volatile AMXConfigLoader mLoader;
    private volatile PendingConfigBeans mPendingConfigBeansBackup;

    @Override
    public void postConstruct() {
        final TimingDelta delta = new TimingDelta();

        if (mMBeanServer == null) {
            throw new Error("AMXStartup: null MBeanServer");
        }
        if (mPendingConfigBeans == null) {
            throw new Error("AMXStartup: null mPendingConfigBeans");
        }

        mPendingConfigBeansBackup = mPendingConfigBeans;
        AMXLoggerInfo.getLogger().log(Level.FINE, "Initialized AMXConfig Startup service in {0} ms", delta.elapsedMillis());
    }

    @Override
    public void preDestroy() {
        AMXLoggerInfo.getLogger().info(AMXLoggerInfo.stoppingAMX);
        unloadAMXMBeans();
    }

    public DomainRoot getDomainRoot() {
        return ProxyFactory.getInstance(mMBeanServer).getDomainRootProxy(false);
    }

    public ObjectName getDomainConfig() {
    	return ConfigBeanRegistry.getInstance().getObjectNameForProxy(getDomain());
    }

    public Domain getDomain() {
    	return InjectedValues.getInstance().getHabitat().getService(Domain.class);
    }

    public AMXProxy getDomainConfigProxy() {
        return ProxyFactory.getInstance(mMBeanServer).getProxy(getDomainConfig(), AMXProxy.class);
    }
    
    @Override
    public synchronized ObjectName loadAMXMBeans() {
        if (mLoader == null) {
            if(mPendingConfigBeans.size() == 0)  {
                mPendingConfigBeans = mPendingConfigBeansBackup;
            }
            mLoader = new AMXConfigLoader(mMBeanServer, mPendingConfigBeans, mTransactions);
            mLoader.start();
            // asynchronous start, caller must wait for 
        }
        return getDomainConfig();
    }

    @Override
    public synchronized void unloadAMXMBeans() {
        final AMXProxy domainConfigProxy = getDomainConfigProxy();
        if (domainConfigProxy != null) {
        	ImplUtil.unregisterAMXMBeans(domainConfigProxy);
        }
        if (mLoader != null) {
            mLoader.stop();
        }
        mLoader = null;
    }
}
