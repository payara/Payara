/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.
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
import com.sun.enterprise.deployment.runtime.web.SessionManager;
import com.sun.enterprise.web.BasePersistenceStrategyBuilder;
import com.sun.enterprise.web.ServerConfigLookup;
import org.apache.catalina.Context;
import org.apache.catalina.core.StandardContext;
import org.glassfish.ha.store.util.SimpleMetadata;
import org.glassfish.web.ha.session.management.*;
import org.glassfish.web.valve.GlassFishValve;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

import java.util.HashMap;

/**
 * @author Rajiv Mordani
 */

@Service(name="replicated")
@Scoped(PerLookup.class)
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
        ReplicationStore store = null;
        HashMap<String, Object> vendorMap = new HashMap<String, Object>();
        boolean asyncReplicationValue = serverConfigLookup.getAsyncReplicationFromConfig((WebModule)ctx);
        boolean disableJreplica = serverConfigLookup.getDisableJreplicaFromConfig();
        vendorMap.put("async.replication", asyncReplicationValue);
        vendorMap.put("broadcast.remove.expired", false);
        vendorMap.put("value.class.is.thread.safe", true);
        if (this.getPersistenceScope().equals("session")) {
            rwepMgr.setSessionFactory(new FullSessionFactory());
            store = new ReplicationStore(serverConfigLookup, ioUtils);
            rwepMgr.createBackingStore(this.getPassedInPersistenceType(), ctx.getPath(), SimpleMetadata.class, vendorMap);
        } else if (this.getPersistenceScope().equals("modified-session")) {
            rwepMgr.setSessionFactory(new ModifiedSessionFactory());
            store = new ReplicationStore(serverConfigLookup, ioUtils);
            rwepMgr.createBackingStore(this.getPassedInPersistenceType(), ctx.getPath(), SimpleMetadata.class, vendorMap);
        } else if (this.getPersistenceScope().equals("modified-attribute")) {
            rwepMgr.setSessionFactory(new ModifiedAttributeSessionFactory());
            store = new ReplicationAttributeStore(serverConfigLookup, ioUtils);
            rwepMgr.createBackingStore(this.getPassedInPersistenceType(), ctx.getPath(), CompositeMetadata.class, vendorMap);
        }


        rwepMgr.setMaxActiveSessions(maxSessions);
        rwepMgr.setMaxIdleBackup(0);
        rwepMgr.setRelaxCacheVersionSemantics(relaxCacheVersionSemantics);
        rwepMgr.setStore(store);
        rwepMgr.setDisableJreplica(disableJreplica);
        
        ctx.setManager(rwepMgr);
        if(!((StandardContext)ctx).isSessionTimeoutOveridden()) {
            rwepMgr.setMaxInactiveInterval(sessionMaxInactiveInterval);
        }


        HASessionStoreValve haValve = new HASessionStoreValve();
        StandardContext stdCtx = (StandardContext) ctx;
        stdCtx.addValve((GlassFishValve)haValve);

    }

}
