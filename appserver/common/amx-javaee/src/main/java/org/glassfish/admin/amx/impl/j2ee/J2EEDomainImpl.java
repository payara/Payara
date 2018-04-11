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

package org.glassfish.admin.amx.impl.j2ee;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;

import org.glassfish.admin.amx.core.Util;
import org.glassfish.admin.amx.impl.util.Issues;
import org.glassfish.admin.amx.impl.util.ObjectNameBuilder;
import org.glassfish.admin.amx.impl.config.ConfigBeanRegistry;
import org.glassfish.admin.amx.j2ee.J2EEDomain;
import org.glassfish.admin.amx.j2ee.J2EEManagedObject;
import org.glassfish.admin.amx.j2ee.J2EETypes;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.util.List;

/**
 * Base implementation for the J2EEDomain for DAS and non-DAS server instances.
 */
public class J2EEDomainImpl extends J2EEManagedObjectImplBase {

    public static final Class<? extends J2EEManagedObject> INTF = J2EEDomain.class;

    private String serverName = "server";

    public J2EEDomainImpl(final ObjectName parentObjectName, final Metadata meta) {
        super(parentObjectName, meta, INTF);
        Issues.getAMXIssues().notDone("J2EEDomainImpl needs to account for DAS/non-DAS");
    }

    /**
     * JSR 77 impl
     *
     * @return String representation of the ObjectName
     */
    public String[] getservers() {

        return getChildrenAsStrings(J2EETypes.J2EE_SERVER);
    }

    @Override
    protected String getExtraObjectNameProps(final MBeanServer server, final ObjectName nameIn) {
        // SPECIAL CASE per JSR 77 spec:
        // add the 'name' property even though this is a singleton
        String props = super.getExtraObjectNameProps(server, nameIn);
        final String nameProp = Util.makeNameProp(nameIn.getDomain());
        props = Util.concatenateProps(props, nameProp);

        return props;
    }

    @Override
    protected void
    registerChildren() {

        final ObjectNameBuilder builder = getObjectNames();

        final MetadataImpl meta = defaultChildMetadata();
        List<Server> servers = getDomain().getServers().getServer();
        for (Server server : servers) {

            meta.setCorrespondingConfig(ConfigBeanRegistry.getInstance().getObjectNameForProxy(server));
            final DASJ2EEServerImpl impl = new DASJ2EEServerImpl(getObjectName(), meta);
            ObjectName serverObjectName = builder.buildChildObjectName(J2EETypes.J2EE_SERVER, server.getName());
            registerChild(impl, serverObjectName);
        }
        //ImplUtil.getLogger().info( "Registered J2EEDomain as " + getObjectName() + " with J2EEServer of " + serverObjectName);
    }

    @Override
    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }
}







