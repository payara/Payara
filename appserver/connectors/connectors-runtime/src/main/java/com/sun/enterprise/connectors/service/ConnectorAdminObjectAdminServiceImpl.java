/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.connectors.service;

import com.sun.appserv.connectors.internal.api.ConnectorRuntimeException;
import com.sun.enterprise.connectors.ActiveOutboundResourceAdapter;
import com.sun.enterprise.connectors.ActiveResourceAdapter;
import org.glassfish.resourcebase.resources.api.ResourceInfo;
import org.glassfish.resourcebase.resources.naming.ResourceNamingService;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import java.util.Properties;
import java.util.logging.Level;

/**
 * AdminObject administration service. It performs the functionality of
 * creating and deleting the Admin Objects
 *
 * @author Binod P.G and Srikanth P
 */


public class ConnectorAdminObjectAdminServiceImpl extends
        ConnectorService {


    public ConnectorAdminObjectAdminServiceImpl() {
        super();
    }

    public void addAdminObject(
            String appName,
            String connectorName,
            ResourceInfo resourceInfo,
            String adminObjectType,
            String adminObjectClassName, 
            Properties props)
            throws ConnectorRuntimeException {
        ActiveResourceAdapter ar =
                _registry.getActiveResourceAdapter(connectorName);
        if (ar == null) {
            ifSystemRarLoad(connectorName);
            ar = _registry.getActiveResourceAdapter(connectorName);
        }
        if (ar instanceof ActiveOutboundResourceAdapter) {
            ActiveOutboundResourceAdapter aor =
                    (ActiveOutboundResourceAdapter) ar;
            aor.addAdminObject(appName, connectorName, resourceInfo,
                    adminObjectType, adminObjectClassName, props);
        } else {
            ConnectorRuntimeException cre = new ConnectorRuntimeException(
                    "This adapter is not 1.5 compliant");
            _logger.log(Level.SEVERE,
                    "rardeployment.non_1.5_compliant_rar", resourceInfo);
            throw cre;
        }
    }

    public void deleteAdminObject(ResourceInfo resourceInfo)
            throws ConnectorRuntimeException {

        ResourceNamingService namingService = _runtime.getResourceNamingService();
        try {
            namingService.unpublishObject(resourceInfo, resourceInfo.getName());
        }
        catch (NamingException ne) {
            /* TODO V3 JMS RA ?
            ResourcesUtil resutil = ResourcesUtil.createInstance();
            if (resutil.adminObjectBelongsToSystemRar(jndiName)) {
                return;
            }
            */
            if (ne instanceof NameNotFoundException) {
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE,
                            "rardeployment.admin_object_delete_failure", resourceInfo);
                    _logger.log(Level.FINE, "", ne);
                }
                return;
            }
            ConnectorRuntimeException cre = new ConnectorRuntimeException(
                    "Failed to delete admin object from jndi");
            cre.initCause(ne);
            _logger.log(Level.SEVERE,
                    "rardeployment.admin_object_delete_failure", resourceInfo);
            _logger.log(Level.SEVERE, "", cre);
            throw cre;
        }
    }
}
