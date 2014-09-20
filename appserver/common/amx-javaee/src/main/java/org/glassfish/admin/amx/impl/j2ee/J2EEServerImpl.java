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

import java.util.Set;

import org.glassfish.admin.amx.j2ee.J2EEServer;
import org.glassfish.admin.amx.util.SetUtil;
import org.glassfish.admin.amx.impl.util.Issues;

import javax.management.ObjectName;


import static org.glassfish.admin.amx.j2ee.J2EETypes.*;

/**
JSR 77 extension representing an Appserver standalone server (non-clustered).

Note that this class has a subclass:  DASJ2EEServerImpl.
 */
public class J2EEServerImpl extends J2EELogicalServerImplBase
{
    public static final Class<J2EEServer> INTF = J2EEServer.class;

    private volatile RegistrationSupport mRegistrationSupport = null;

    public J2EEServerImpl(final ObjectName parentObjectName, final Metadata meta)
    {
        super(parentObjectName, meta, INTF);
    }
    /* The vendor information for this server. */

    private static final String serverVendor = "Oracle Corporation";

    public String[] getjavaVMs()
    {
        final ObjectName child = child(JVM);

        return child == null ? new String[0] : new String[]
                {
                    child.toString()
                };
    }

    public static final Set<String> J2EE_RESOURCE_TYPES = SetUtil.newUnmodifiableStringSet(
        JDBC_RESOURCE,
        JAVA_MAIL_RESOURCE,
        JCA_RESOURCE,
        JMS_RESOURCE,
        JNDI_RESOURCE,
        JTA_RESOURCE,
        RMI_IIOP_RESOURCE,
        URL_RESOURCE
    );

    public String[] getresources()
    {
        return getChildrenAsStrings( J2EE_RESOURCE_TYPES );
    }

    public String getserverVersion()
    {
        Issues.getAMXIssues().notDone("How to get the server version");
        return "Glassfish V3.1";
    }

    public String getserverVendor()
    {
        return serverVendor;
    }

    public String getjvm()
    {
        return "" + getAncestorByType(JVM);
    }

    @Override
    protected void registerChildren()
    {
        super.registerChildren();
        
        final J2EEServer selfProxy = getSelf(J2EEServer.class);
        mRegistrationSupport = new RegistrationSupport( selfProxy );
        mRegistrationSupport.start();
    }

    @Override
    protected void unregisterChildren()
    {
        if (mRegistrationSupport != null) {
            mRegistrationSupport.cleanup();
        }
        super.unregisterChildren();
    }

}





















