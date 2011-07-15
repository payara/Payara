/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.amx.intf.config;

import org.glassfish.admin.amx.base.Singleton;

import java.util.Map;

@Deprecated
public interface ModuleLogLevels
        extends Singleton, ConfigElement, PropertiesAccess {


    public String getServer();

    public String getWebContainer();

    public String getEjbContainer();

    public String getMdbContainer();

    public String getGroupManagementService();

    public String getRoot();

    public void setRoot(String param1);

    public void setServer(String param1);

    public void setEjbContainer(String param1);

    public String getCmpContainer();

    public void setCmpContainer(String param1);

    public void setMdbContainer(String param1);

    public void setWebContainer(String param1);

    public String getClassloader();

    public void setClassloader(String param1);

    public String getConfiguration();

    public void setConfiguration(String param1);

    public String getNaming();

    public void setNaming(String param1);

    public String getSecurity();

    public void setSecurity(String param1);

    public String getJts();

    public void setJts(String param1);

    public String getJta();

    public void setJta(String param1);

    public String getAdmin();

    public void setAdmin(String param1);

    public String getDeployment();

    public void setDeployment(String param1);

    public String getVerifier();

    public void setVerifier(String param1);

    public String getJaxr();

    public void setJaxr(String param1);

    public String getJaxrpc();

    public void setJaxrpc(String param1);

    public String getSaaj();

    public void setSaaj(String param1);

    public String getCorba();

    public void setCorba(String param1);

    public String getJavamail();

    public void setJavamail(String param1);

    public String getJms();

    public void setJms(String param1);

    public String getConnector();

    public void setConnector(String param1);

    public String getJdo();

    public void setJdo(String param1);

    public String getCmp();

    public void setCmp(String param1);

    public String getUtil();

    public void setUtil(String param1);

    public String getResourceAdapter();

    public void setResourceAdapter(String param1);

    public String getSynchronization();

    public void setSynchronization(String param1);

    public String getNodeAgent();

    public void setNodeAgent(String param1);

    public String getSelfManagement();

    public void setSelfManagement(String param1);

    public void setGroupManagementService(String param1);

    public String getManagementEvent();

    public void setManagementEvent(String param1);

    public Map getAllLogLevels();

}
