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

package org.glassfish.admin.amx.impl.ext;

import com.sun.appserv.connectors.internal.api.ConnectorRuntime;
import org.glassfish.resource.common.PoolInfo;
import org.glassfish.admin.amx.intf.config.JdbcConnectionPool;
import org.glassfish.admin.amx.util.ExceptionUtil;
import org.jvnet.hk2.component.ComponentException;
import org.jvnet.hk2.component.Habitat;

import javax.management.ObjectName;
import javax.resource.ResourceException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.beans.PropertyChangeEvent;


import org.glassfish.admin.amx.intf.config.Resources;
import org.glassfish.admin.amx.base.SystemStatus;
import static org.glassfish.admin.amx.base.SystemStatus.*;
import org.glassfish.admin.amx.base.UnprocessedConfigChange;
import org.glassfish.admin.amx.impl.mbean.AMXImplBase;
import org.glassfish.admin.amx.intf.config.Domain;
import org.jvnet.hk2.config.*;

import org.glassfish.internal.config.UnprocessedConfigListener;

/**

 */
public final class SystemStatusImpl extends AMXImplBase
// implements SystemStatus
{
    public SystemStatusImpl(final ObjectName parent)
    {
        super(parent, SystemStatus.class);
    }

    private Habitat getHabitat()
    {
        return org.glassfish.internal.api.Globals.getDefaultHabitat();
    }

    public Map<String, Object> pingJdbcConnectionPool(final String poolName)
    {
        final Map<String, Object> result = new HashMap<String, Object>();
        final Habitat habitat = getHabitat();
        ConnectorRuntime connRuntime = null;

        result.put(PING_SUCCEEDED_KEY, false);
        if (habitat == null)
        {
            result.put(REASON_FAILED_KEY, "Habitat is null");
            return result;
        }

        // check pool name
        final Resources resources = getDomainRootProxy().child(Domain.class).getResources();

        final Map<String, JdbcConnectionPool> pools = resources.childrenMap(JdbcConnectionPool.class);
        final JdbcConnectionPool cfg = pools.get(poolName);
        if (cfg == null)
        {
            result.put(REASON_FAILED_KEY, "The JdbcConnectionPool \"" + poolName + "\" does not exist");
            return result;
        }

        // get connector runtime
        try
        {
            connRuntime = habitat.getComponent(ConnectorRuntime.class, null);
        }
        catch (final ComponentException e)
        {
            result.putAll(ExceptionUtil.toMap(e));
            result.put(REASON_FAILED_KEY, ExceptionUtil.toString(e));
            return result;
        }

        // do the ping
        try
        {
            PoolInfo poolInfo = new PoolInfo(poolName);
            final boolean pingable = connRuntime.pingConnectionPool(poolInfo);
            result.put(PING_SUCCEEDED_KEY, pingable);
        }
        catch (final ResourceException e)
        {
            result.putAll(ExceptionUtil.toMap(e));
            assert REASON_FAILED_KEY.equals(ExceptionUtil.MESSAGE_KEY);
            return result;
        }

        return result;
    }

//-------------------------------------
    private static void xdebug(final String s)
    {
        System.out.println("### " + s);
    }

    private static String str(final Object o)
    {
        return o == null ? null : ("" + o);
    }

    private ObjectName sourceToObjectName(final Object source)
    {
        ObjectName objectName = null;

        if (source instanceof ConfigBean)
        {
            objectName = ((ConfigBean) source).getObjectName();
        }
        else if (source instanceof ConfigBeanProxy)
        {
            objectName = ((ConfigBean) Dom.unwrap((ConfigBeanProxy) source)).getObjectName();
        }
        else
        {
            xdebug("UnprocessedConfigChange.sourceToObjectName: source is something else");
        }

        return objectName;
    }

    public List<Object[]> getRestartRequiredChanges()
    {
        final UnprocessedConfigListener unp = getHabitat().getComponent(UnprocessedConfigListener.class);

        final List<UnprocessedChangeEvents> items = unp.getUnprocessedChangeEvents();

        final List<Object[]> changesObjects = new ArrayList<Object[]>();

        //xdebug( "SystemStatusImpl: UnprocessedConfigChange: processing events: " + items.size() );
        for (final UnprocessedChangeEvents events : items)
        {
            for (final UnprocessedChangeEvent event : events.getUnprocessed())
            {
                //xdebug( "SystemStatusImpl: UnprocessedConfigChange: event: " + event );
                final String reason = event.getReason();
                final PropertyChangeEvent pce = event.getEvent();
                final long when = event.getWhen();

                final ObjectName objectName = sourceToObjectName(pce.getSource());

                final UnprocessedConfigChange ucc = new UnprocessedConfigChange(
                        pce.getPropertyName(),
                        str(pce.getOldValue()),
                        str(pce.getNewValue()),
                        objectName,
                        reason);
                //xdebug("SystemStatusImpl: UnprocessedConfigChange: " + ucc);
                changesObjects.add(ucc.toArray());
            }
        }

        return changesObjects;
    }

}








