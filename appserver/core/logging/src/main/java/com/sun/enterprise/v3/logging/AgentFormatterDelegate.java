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

package com.sun.enterprise.v3.logging;

import com.sun.enterprise.server.logging.FormatterDelegate;
import com.sun.enterprise.admin.monitor.callflow.Agent;
import com.sun.enterprise.admin.monitor.callflow.ThreadLocalData;

import static com.sun.enterprise.server.logging.UniformLogFormatter.*;

import java.util.logging.Level;

/**
 * Created by IntelliJ IDEA.
 * User: dochez
 * Date: May 29, 2007
 * Time: 4:13:03 PM
 * To change this template use File | Settings | File Templates.
 */

public class AgentFormatterDelegate implements FormatterDelegate {

    Agent agent;

    public AgentFormatterDelegate(Agent agent) {
        this.agent = agent;
    }

    public void format(StringBuilder buf, Level level) {


        ThreadLocalData tld = agent.getThreadLocalData();
        if (tld==null) {
            return;
        }

        if (level.equals(Level.INFO) || level.equals(Level.CONFIG)) {

            if (tld.getApplicationName() != null) {
                buf.append("_ApplicationName").append(NV_SEPARATOR).
                        append(tld.getApplicationName()).
                        append(NVPAIR_SEPARATOR);
            }

        } else {

            if (tld.getRequestId() != null) {
                buf.append("_RequestID").append(NV_SEPARATOR).
                        append(tld.getRequestId()).append(NVPAIR_SEPARATOR);
            }

            if (tld.getApplicationName() != null) {
                buf.append("_ApplicationName").append(NV_SEPARATOR).
                        append(tld.getApplicationName()).
                        append(NVPAIR_SEPARATOR);
            }

            if (tld.getModuleName() != null) {
                buf.append("_ModuleName").append(NV_SEPARATOR).
                        append(tld.getModuleName()).append(NVPAIR_SEPARATOR);
            }

            if (tld.getComponentName() != null) {
                buf.append("_ComponentName").append(NV_SEPARATOR).
                        append(tld.getComponentName()).append(NVPAIR_SEPARATOR);
            }

            if (tld.getComponentType() != null) {
                buf.append("_ComponentType").append(NV_SEPARATOR).
                        append(tld.getComponentType()).append(NVPAIR_SEPARATOR);
            }

            if (tld.getMethodName() != null) {
                buf.append("_MethodName").append(NV_SEPARATOR).
                        append(tld.getMethodName()).append(NVPAIR_SEPARATOR);
            }

            if (tld.getTransactionId() != null) {
                buf.append("_TransactionId").append(NV_SEPARATOR).
                        append(tld.getTransactionId()).append(NVPAIR_SEPARATOR);
            }

            if (tld.getSecurityId() != null) {
                buf.append("_CallerId").append(NV_SEPARATOR).
                        append(tld.getSecurityId()).append(NVPAIR_SEPARATOR);
            }
        }
    }    
}
