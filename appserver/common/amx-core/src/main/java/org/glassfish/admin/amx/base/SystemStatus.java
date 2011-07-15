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

package org.glassfish.admin.amx.base;

import java.util.Map;
import java.util.List;

import javax.management.MBeanOperationInfo;
import org.glassfish.admin.amx.annotation.ManagedAttribute;
import org.glassfish.admin.amx.annotation.ManagedOperation;
import org.glassfish.external.arc.Stability;
import org.glassfish.external.arc.Taxonomy;
import org.glassfish.admin.amx.util.ExceptionUtil;
import org.glassfish.admin.amx.core.AMXProxy;
import org.glassfish.admin.amx.core.AMXMBeanMetadata;

/**
Provides various routines for system status.
<p>
<b>WARNING: some of these routines may be relocated.</b>
@see SystemInfo
 */
@Taxonomy(stability = Stability.UNCOMMITTED)
@AMXMBeanMetadata(singleton = true, globalSingleton = true, leaf = true)
public interface SystemStatus extends AMXProxy, Utility, Singleton
{
    /** Key into Map returned by various methods including {@link #pingJDBCConnectionPool} */
    public static final String PING_SUCCEEDED_KEY = "PingSucceededKey";

    /** @deprecated use ExceptionUtil.MESSAGE_KEY */
    public static final String REASON_FAILED_KEY = ExceptionUtil.MESSAGE_KEY;

    /**
    Ping the JDBCConnectionPool and return status.  Various values can be found in the
    resulting Map.
    @see #PING_SUCCEEDED_KEY
    @see #REASON_FAILED_KEY
    @see org.glassfish.admin.amx.util.ExceptionUtil#toMap
     */
    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    public Map<String, Object> pingJdbcConnectionPool(final String poolName);

    /**
    <em>Note: this API is highly volatile and subject to change<em>.
    <p>
    Query configuration changes that require restart.
    <p>
    Changes are listed oldest first.  The system may drop older changes in order to limit the size
    of the list and/or merge related changes to the most recent.
    <p>
    Over the wire transmission of 'UnprocessedConfigChange' would require the client to have its class;
    as delivered the Object[] contains only standard JDK types. Actual over-the-wire type is List<Object[].
    See the Javadoc for {@link UnprocessedConfigChange} for the order of values in the Object[].
     */
    @ManagedAttribute
    @Taxonomy(stability = Stability.UNCOMMITTED)
    public List<Object[]> getRestartRequiredChanges();

    /** helper class, in particular to convert results from {@link #getRestartRequiredChanges} */
    public final class Helper
    {
        private Helper() {}
        public static List<UnprocessedConfigChange> toUnprocessedConfigChange(final List<Object[]> items)
        {
            final List<UnprocessedConfigChange> l = new java.util.ArrayList<UnprocessedConfigChange>();
            for (final Object[] a : items)
            {
                l.add(new UnprocessedConfigChange(a));
            }
            return l;
        }
    }
}











