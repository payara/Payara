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

import javax.management.MBeanOperationInfo;
import org.glassfish.admin.amx.core.AMXProxy;
import org.glassfish.admin.amx.annotation.*;
import org.glassfish.admin.amx.core.AMXMBeanMetadata;
import org.glassfish.external.arc.Stability;
import org.glassfish.external.arc.Taxonomy;



/**
    @since GlassFish V3
 */
@Taxonomy(stability = Stability.UNCOMMITTED)
@AMXMBeanMetadata(singleton=true, globalSingleton=true, leaf=true)
public interface Realms extends AMXProxy, Utility, Singleton
{
    /** get the names of all realms */
    @ManagedAttribute
    public String[] getRealmNames();
    @ManagedAttribute
    public String[] getPredefinedAuthRealmClassNames();
    
    @ManagedAttribute
    public String getDefaultRealmName();
    @ManagedAttribute
    public void   setDefaultRealmName(String realmName);

    @ManagedOperation(impact=MBeanOperationInfo.ACTION)
    public void addUser( String realm, String user, String password, String[] groupList );
    @ManagedOperation(impact=MBeanOperationInfo.ACTION)
    public void updateUser( String realm, String user, String newUser, String password, String[] groupList );
    @ManagedOperation(impact=MBeanOperationInfo.ACTION)
    public void removeUser(String realm, String user);

    @ManagedOperation(impact=MBeanOperationInfo.INFO)
    public String[] getUserNames(String realm);
    @ManagedOperation(impact=MBeanOperationInfo.INFO)
    public String[] getGroupNames(String realm);

    @ManagedOperation(impact=MBeanOperationInfo.INFO)
    public Map<String,Object> getUserAttributes(final String realm, final String user);

    @ManagedOperation(impact=MBeanOperationInfo.INFO)
    public String[] getGroupNames(String realm, String user);

    /** @return true if the realm implementation support User Management (add,remove,update user) */
    @ManagedOperation(impact=MBeanOperationInfo.INFO)
    public boolean supportsUserManagement(final String realmName);
    
    /** @return the username of any user that uses an empty password */
    @ManagedAttribute
    public String getAnonymousUser();
}










