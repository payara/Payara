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

package com.sun.ejb.base.container.util;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;

import com.sun.logging.LogDomains;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.ejb.config.EjbContainer;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;
import org.glassfish.ejb.deployment.descriptor.runtime.BeanCacheDescriptor;
import org.glassfish.ejb.deployment.descriptor.runtime.IASEjbExtraDescriptors;
import org.glassfish.hk2.api.PostConstruct;
import com.sun.enterprise.config.serverbeans.Config;
import org.jvnet.hk2.annotations.Service;

/**
 * A util class to read the bean cache related entries from
 * domain.xml and sun-ejb-jar.xml
 *
 * @author Mahesh Kannan
 */
@Service
public class CacheProperties implements PostConstruct {

    protected static final Logger _logger =
            LogDomains.getLogger(CacheProperties.class, LogDomains.EJB_LOGGER);

    private int maxCacheSize;
    private int numberOfVictimsToSelect;
    private int cacheIdleTimeoutInSeconds;
    private int removalTimeoutInSeconds;

    private String victimSelectionPolicy;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private Config serverConfig;

    EjbContainer ejbContainer;

    public CacheProperties() {
    }

    public void postConstruct() {
        ejbContainer = serverConfig.getExtensionByType(EjbContainer.class);
    }

    public void init(EjbDescriptor desc) {

        BeanCacheDescriptor beanCacheDes = null;

        IASEjbExtraDescriptors iased = desc.getIASEjbExtraDescriptors();
        if (iased != null) {
            beanCacheDes = iased.getBeanCache();
        }

        loadProperties(ejbContainer, desc, beanCacheDes);
        //container.setMonitorOn(ejbContainer.isMonitoringEnabled());

    }

    public int getMaxCacheSize() {
        return this.maxCacheSize;
    }

    public int getNumberOfVictimsToSelect() {
        return this.numberOfVictimsToSelect;
    }

    public int getCacheIdleTimeoutInSeconds() {
        return this.cacheIdleTimeoutInSeconds;
    }

    public int getRemovalTimeoutInSeconds() {
        return this.removalTimeoutInSeconds;
    }

    public String getVictimSelectionPolicy() {
        return this.victimSelectionPolicy;
    }

    public String getPassivationStorePath() {
        return ejbContainer.getSessionStore();
    }

    public String toString() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append("maxSize: ").append(maxCacheSize)
                .append("; victims: ").append(numberOfVictimsToSelect)
                .append("; idleTimeout: ").append(cacheIdleTimeoutInSeconds)
                .append("; removalTimeout: ").append(removalTimeoutInSeconds)
                .append("; policy: ").append(victimSelectionPolicy);

        return sbuf.toString();
    }

    private void loadProperties(EjbContainer ejbContainer,
                                EjbDescriptor ejbDesc,
                                BeanCacheDescriptor beanCacheDes) {
        numberOfVictimsToSelect =
                Integer.parseInt(ejbContainer.getCacheResizeQuantity());

        maxCacheSize =
                Integer.parseInt(ejbContainer.getMaxCacheSize());

        cacheIdleTimeoutInSeconds = Integer.parseInt(
                ejbContainer.getCacheIdleTimeoutInSeconds());

        removalTimeoutInSeconds =
                Integer.parseInt(ejbContainer.getRemovalTimeoutInSeconds());

        victimSelectionPolicy = ejbContainer.getVictimSelectionPolicy();

        // If portable @StatefulTimeout is specified, it takes precedence over
        // any default value in domain.xml.  However, if a removal timeout is
        // specified in sun-ejb-jar.xml, that has highest precedence. 
        if( ejbDesc instanceof EjbSessionDescriptor ) {

            EjbSessionDescriptor sessionDesc = (EjbSessionDescriptor) ejbDesc;
            if( sessionDesc.hasStatefulTimeout() ) {

                long value = sessionDesc.getStatefulTimeoutValue();
                TimeUnit unit = sessionDesc.getStatefulTimeoutUnit();

                value = TimeUnit.SECONDS.convert(value, unit);
		if (value < 0) {
                    this.removalTimeoutInSeconds = -1;
                    this.cacheIdleTimeoutInSeconds = -1;
		} else if (value == 0) {
                    this.removalTimeoutInSeconds = 1;
                    this.cacheIdleTimeoutInSeconds = 2;
		} else {
                    this.removalTimeoutInSeconds = (int) value;
                    this.cacheIdleTimeoutInSeconds = (int) (value + 1);
		}

            }

            /* lifespan of an idle sfsb is time-in-cache + time-on-disk, with cacheIdleTimeoutInSeconds setting
               for the 1st interval and removalTimeoutInSeconds for the 2nd. So if you add them, the sfsb will stay
               in the cache to the max possible interval, and because it'll be never written to disk,
               there will be nothing to remove from there.
               
               set cacheIdleTimeoutInSeconds and maxCacheSize will cause cache never overflow, and sfsb just be
               removed from cache when cacheIdleTimeoutInSeconds arrives */
            if (sessionDesc.isStateful() && !sessionDesc.isPassivationCapable()) {
                cacheIdleTimeoutInSeconds = cacheIdleTimeoutInSeconds + removalTimeoutInSeconds;
                maxCacheSize = -1;                
            }
        }

        if (beanCacheDes != null) {
            int temp = 0;
            if ((temp = beanCacheDes.getResizeQuantity()) != -1) {
                this.numberOfVictimsToSelect = temp;
            }
            if ((temp = beanCacheDes.getMaxCacheSize()) != -1) {
                this.maxCacheSize = temp;
            }
            if ((temp = beanCacheDes.getCacheIdleTimeoutInSeconds()) != -1) {
                this.cacheIdleTimeoutInSeconds = temp;
            }
            if ((temp = beanCacheDes.getRemovalTimeoutInSeconds()) != -1) {
                this.removalTimeoutInSeconds = temp;
            }
            if ((beanCacheDes.getVictimSelectionPolicy()) != null) {
                this.victimSelectionPolicy =
                        beanCacheDes.getVictimSelectionPolicy();
            }
        }

    }

}
   
