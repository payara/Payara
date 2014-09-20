/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.web.ha.strategy.builder;

import com.sun.enterprise.web.WebModule;
import com.sun.enterprise.container.common.spi.util.JavaEEIOUtils;
import com.sun.enterprise.web.BasePersistenceStrategyBuilder;
import com.sun.enterprise.web.ServerConfigLookup;
import org.apache.catalina.Context;
import org.apache.catalina.core.StandardContext;
import org.glassfish.web.deployment.runtime.SessionManager;
import org.glassfish.ha.store.api.Storeable;
import org.glassfish.ha.store.util.SimpleMetadata;
import org.glassfish.web.ha.session.management.*;
import org.glassfish.web.valve.GlassFishValve;
import javax.inject.Inject;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Rajiv Mordani
 */

@Service(name="replicated")
@PerLookup
public class ReplicatedWebMethodSessionStrategyBuilder extends BasePersistenceStrategyBuilder {
    @Inject
    private ReplicationWebEventPersistentManager rwepMgr;

    @Inject
    private JavaEEIOUtils ioUtils;

    public ReplicatedWebMethodSessionStrategyBuilder() {
        super();
    }

    public void initializePersistenceStrategy(
            Context ctx,
            SessionManager smBean,
            ServerConfigLookup serverConfigLookup)
    {

        super.initializePersistenceStrategy(ctx, smBean, serverConfigLookup);
        //super.setPassedInPersistenceType("replicated");
        if (this.getPersistenceScope().equals("session")) {
            setupReplicationWebEventPersistentManager(SimpleMetadata.class,
                    new FullSessionFactory(),
                    new ReplicationStore(ioUtils),
                    ctx, serverConfigLookup);
        } else if (this.getPersistenceScope().equals("modified-session")) {
            setupReplicationWebEventPersistentManager(SimpleMetadata.class,
                    new ModifiedSessionFactory(),
                    new ReplicationStore(ioUtils),
                    ctx, serverConfigLookup);
        } else if (this.getPersistenceScope().equals("modified-attribute")) {
            setupReplicationWebEventPersistentManager(CompositeMetadata.class,
                    new ModifiedAttributeSessionFactory(),
                    new ReplicationAttributeStore(ioUtils),
                    ctx, serverConfigLookup);
        } else {
            throw new IllegalArgumentException(this.getPersistenceScope());
        }

        HASessionStoreValve haValve = new HASessionStoreValve();
        StandardContext stdCtx = (StandardContext) ctx;
        stdCtx.addValve((GlassFishValve)haValve);

    }

    private <T extends Storeable> void setupReplicationWebEventPersistentManager(
            Class<T> metadataClass, SessionFactory sessionFactory, ReplicationStore store,
            Context ctx, ServerConfigLookup serverConfigLookup) {

        Map<String, Object> vendorMap = new HashMap<String, Object>();
        boolean asyncReplicationValue = serverConfigLookup.getAsyncReplicationFromConfig((WebModule)ctx);
        vendorMap.put("async.replication", asyncReplicationValue);
        vendorMap.put("broadcast.remove.expired", false);
        vendorMap.put("value.class.is.thread.safe", true);
        ReplicationWebEventPersistentManager<T> rwepMgr = getReplicationWebEventPersistentManager();
        rwepMgr.setSessionFactory(sessionFactory);
        rwepMgr.createBackingStore(this.getPassedInPersistenceType(), ctx.getPath(), metadataClass, vendorMap);

        boolean disableJreplica = serverConfigLookup.getDisableJreplicaFromConfig();
        rwepMgr.setMaxActiveSessions(maxSessions);
        rwepMgr.setMaxIdleBackup(0);
        rwepMgr.setRelaxCacheVersionSemantics(relaxCacheVersionSemantics);
        rwepMgr.setStore(store);
        rwepMgr.setDisableJreplica(disableJreplica);

        ctx.setManager(rwepMgr);
        if(!((StandardContext)ctx).isSessionTimeoutOveridden()) {
            rwepMgr.setMaxInactiveInterval(sessionMaxInactiveInterval);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Storeable>  ReplicationWebEventPersistentManager<T> getReplicationWebEventPersistentManager() {
 
        return rwepMgr;
    }
}
