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

import java.util.Map;

@Deprecated
public interface ConnectorConnectionPool
        extends Description, NamedConfigElement, PropertiesAccess, ResourceRefReferent {


    public String getDescription();

    public void setDescription(String param1);

    public String getIsConnectionValidationRequired();

    public void setIsConnectionValidationRequired(String param1);

    public String getConnectionDefinitionName();

    public void setConnectionDefinitionName(String param1);

    public String getFailAllConnections();

    public void setFailAllConnections(String param1);

    public String getIdleTimeoutInSeconds();

    public void setIdleTimeoutInSeconds(String param1);

    public String getMaxPoolSize();

    public void setMaxPoolSize(String param1);

    public String getMaxWaitTimeInMillis();

    public void setMaxWaitTimeInMillis(String param1);

    public String getPoolResizeQuantity();

    public void setPoolResizeQuantity(String param1);

    public String getResourceAdapterName();

    public void setResourceAdapterName(String param1);

    public String getSteadyPoolSize();

    public void setSteadyPoolSize(String param1);

    public String getTransactionSupport();

    public void setTransactionSupport(String param1);

    /**
     * @return Map of all SecurityMap contained in this item.
     */
    public Map<String, SecurityMap> getSecurityMap();

    public String getConnectionLeakTimeoutInSeconds();

    public void setConnectionLeakTimeoutInSeconds(String param1);

    public String getConnectionLeakReclaim();

    public void setConnectionLeakReclaim(String param1);

    public String getConnectionCreationRetryAttempts();

    public void setConnectionCreationRetryAttempts(String param1);

    public String getConnectionCreationRetryIntervalInSeconds();

    public void setConnectionCreationRetryIntervalInSeconds(String param1);

    public String getValidateAtmostOncePeriodInSeconds();

    public void setValidateAtmostOncePeriodInSeconds(String param1);

    public String getLazyConnectionEnlistment();

    public void setLazyConnectionEnlistment(String param1);

    public String getLazyConnectionAssociation();

    public void setLazyConnectionAssociation(String param1);

    public String getAssociateWithThread();

    public void setAssociateWithThread(String param1);

    public String getMatchConnections();

    public void setMatchConnections(String param1);

    public String getMaxConnectionUsageCount();

    public void setMaxConnectionUsageCount(String param1);

    public String getPing();

    public void setPing(String param1);

    public String getPooling();

    public void setPooling(String param1);

}
