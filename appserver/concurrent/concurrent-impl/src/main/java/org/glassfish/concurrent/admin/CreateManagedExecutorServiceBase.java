/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.concurrent.admin;

import com.sun.enterprise.config.serverbeans.ServerTags;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.resources.admin.cli.ResourceConstants;
import org.glassfish.resourcebase.resources.api.ResourceStatus;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Properties;


/**
 * Base command for creating managed executor service and managed 
 * scheduled executor service
 *
 */
public class CreateManagedExecutorServiceBase {

    final protected static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(CreateManagedExecutorServiceBase.class);

    @Param(name="jndi_name", primary=true)
    protected String jndiName;

    @Param(optional=true, defaultValue="true")
    protected Boolean enabled;

    @Param(name="contextinfoenabled", alias="contextInfoEnabled", defaultValue="true", optional=true)
    private Boolean contextinfoenabled;

    @Param(name="contextinfo", alias="contextInfo", defaultValue=ResourceConstants.CONTEXT_INFO_DEFAULT_VALUE, optional=true)
    protected String contextinfo;

    @Param(name="threadpriority", alias="threadPriority", defaultValue=""+Thread.NORM_PRIORITY, optional=true)
    protected Integer threadpriority;

    @Param(name="longrunningtasks", alias="longRunningTasks", defaultValue="false", optional=true)
    protected Boolean longrunningtasks;

    @Param(name="hungafterseconds", alias="hungAfterSeconds", defaultValue="0", optional=true)
    protected Integer hungafterseconds;

    @Param(name="corepoolsize", alias="corePoolSize", defaultValue="0", optional=true)
    protected Integer corepoolsize;

    @Param(name="keepaliveseconds", alias="keepAliveSeconds", defaultValue="60", optional=true)
    protected Integer keepaliveseconds;

    @Param(name="threadlifetimeseconds", alias="threadLifetimeSeconds", defaultValue="0", optional=true)
    protected Integer threadlifetimeseconds;

    @Param(optional=true)
    protected String description;

    @Param(name="property", optional=true, separator=':')
    protected Properties properties;

    @Param(optional=true)
    protected String target = SystemPropertyConstants.DAS_SERVER_NAME;

    protected void setAttributeList(HashMap attrList) {
        attrList.put(ResourceConstants.JNDI_NAME, jndiName);
        attrList.put(ResourceConstants.CONTEXT_INFO_ENABLED, contextinfoenabled.toString());
        attrList.put(ResourceConstants.CONTEXT_INFO, contextinfo);
        attrList.put(ResourceConstants.THREAD_PRIORITY, 
            threadpriority.toString());
        attrList.put(ResourceConstants.LONG_RUNNING_TASKS, 
            longrunningtasks.toString());
        attrList.put(ResourceConstants.HUNG_AFTER_SECONDS, 
            hungafterseconds.toString());
        attrList.put(ResourceConstants.CORE_POOL_SIZE, 
            corepoolsize.toString());
        attrList.put(ResourceConstants.KEEP_ALIVE_SECONDS, 
            keepaliveseconds.toString());
        attrList.put(ResourceConstants.THREAD_LIFETIME_SECONDS, 
            threadlifetimeseconds.toString());
        attrList.put(ServerTags.DESCRIPTION, description);
        attrList.put(ResourceConstants.ENABLED, enabled.toString());
    }
}
