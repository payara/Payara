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

package org.glassfish.admin.amxtest.config;

import com.sun.appserv.management.base.Util;
import com.sun.appserv.management.config.DomainConfig;
import com.sun.appserv.management.config.ResourceConfig;
import com.sun.appserv.management.config.ResourceRefConfig;
import com.sun.appserv.management.config.ResourceRefConfigCR;
import com.sun.appserv.management.helper.RefHelper;
import org.glassfish.admin.amxtest.AMXTestBase;

import java.util.Map;
import java.util.Set;


/**
 This test should normally be run before the generic tests
 so that it can set up default items for many of the config elements
 so that the generic tests will actually test them. Otherwise,
 when the generic tests are run, they won't see any instances
 of many of the AMXConfig MBeans.
 <p/>
 If there are errors doing this, disable this test in amxtest.classes,
 fix the error in the specific place it's occurring, then re-enabled
 this test.
 */
public final class DanglingRefsTest
        extends AMXTestBase {
    public DanglingRefsTest() {
    }

    public void
    testAllDangling()
            throws ClassNotFoundException {
        _testDanglingResourceRefConfigs();
        //_testDanglingDeployedItemRefConfigs();
    }

    private void
    _testDanglingResourceRefConfigs()
            throws ClassNotFoundException {
        final DomainConfig domainConfig = getDomainConfig();
        final Set<ResourceConfig> resourcesSet =
                getQueryMgr().queryInterfaceSet(ResourceConfig.class.getName(), null);

        final Set<ResourceRefConfig>
                refs = RefHelper.findAllResourceRefConfigs(getQueryMgr());

        final Map<String, ResourceConfig> resourcesMap = Util.createNameMap(resourcesSet);

        for (final ResourceRefConfig ref : refs) {
            final String name = ref.getName();

            final ResourceConfig resourceConfig = resourcesMap.get(name);
            if (resourceConfig == null) {
                String msg =
                        "Resource reference '" +
                                Util.getObjectName(ref) + "' refers to a non-existent resource";

                boolean removedOK = false;
                try {
                    final ResourceRefConfigCR container =
                            (ResourceRefConfigCR) ref.getContainer();

                    container.removeResourceRefConfig(name);
                    removedOK = true;
                }
                catch (Exception e) {
                    msg = msg + ", and trying to remove it throws an Exception " +
                            "(remove it manually from domain.xml)" +
                            ", see bug #6298512";
                }

                if (!removedOK) {
                    warning(msg);
                }
            } else {
                //printVerbose( "ResourceRefConfig '" + name + "' is OK" );
            }
        }
    }

    /*
        private void
     _testDanglingDeployedItemRefConfigs()
         throws ClassNotFoundException
     {
         final DomainConfig  domainConfig    = getDomainConfig();

         final Set<DeployedItemRefConfig> s    = (Set<DeployedItemRefConfig>)
             getQueryMgr().queryInterfaceSet( DeployedItemRefConfig.class.getName(), null );

         final Set<DeployedItemRefConfig>
             refs = RefHelper.findAllDeployedItemRefConfigRefs( getQueryMgr() );

         final Map<String,DeployedItem> deployedItemsMap    = Util.createNameMap( s );

         for( final DeployedItemRefConfig ref : refs )
         {
             final String    name    = ref.getName();

             final DeployedItem    deployedItem  = deployedItemsMap.get( name );
             assert( deployedItem != null ) :
                 "Deployed item reference '" + Util.getObjectName( ref ) +
                     "' refers to a non-existent item";
         }
     }
     */
}

















