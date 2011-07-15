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

package amxtest;

import javax.management.ObjectName;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;

import org.testng.annotations.*;
import org.testng.Assert;


import java.util.Set;
import java.util.Map;
import java.util.List;

import  org.glassfish.admin.amx.core.AMXProxy;
import org.glassfish.admin.amx.base.DomainRoot;
import org.glassfish.admin.amx.core.Util;
import org.glassfish.admin.amx.util.jmx.JMXUtil;
import org.glassfish.admin.amx.util.ExceptionUtil;

/** 
Basic AMXProxy tests that verify connectivity and ability to
traverse the AMXProxy hierarchy and fetch all attributes.
 */
//@Test(groups={"amx"}, description="AMXProxy tests", sequential=false, threadPoolSize=5)
@Test(groups =
{
    "amx"
}, description = "Core AMX tests")
public final class AMXCoreTests extends AMXTestBase
{
    public AMXCoreTests()
    {
    }

    //@Test(timeOut=15000)
    public void bootAMX() throws Exception
    {
        final DomainRoot domainRoot = getDomainRootProxy();

        // one basic call to prove it's there...
        domainRoot.getAppserverDomainName();
    }

    /** IF THIS TEST FAILS, DO NOT REMOVE, just comment it out if the need mandates, but only temporarily */
    @Test(dependsOnMethods = "bootAMX")
    public void iterateAllSanityCheck() throws Exception
    {
        try
        {
            final Set<AMXProxy> all = getAllAMX();
            assert all.size() > 20 : "Expected at least 20 AMX MBeans, got: " + all.size();
            for (final AMXProxy amx : all)
            {
                try
                {
                    if ( ! amx.valid() )
                    {
                        continue;   // could have been unregistered
                    }
                    final Set<AMXProxy> children = amx.childrenSet();
                    assert children != null;
                }
                catch( final Exception e )
                {
                    if ( ExceptionUtil.getRootCause(e) instanceof InstanceNotFoundException )
                    {
                        continue;
                    }
                    if ( ! amx.valid() )
                    {
                        warning( "MBean valid()=false during testing, ignoring: " + amx.objectName() );
                    }
                    
                    throw e;
                }
            }
        }
        catch( final Throwable t )
        {
           System.out.println( "Test iterateAllSanityCheck() IGNORED, see issue #9355" );
           t.printStackTrace();
        }
    }
    
    /** IF THIS TEST FAILS, DO NOT REMOVE, just comment it out if the need mandates, but only temporarily */
    @Test
    public void testAMXComplianceMonitorFailureCount()
    {
        try
        {
        final Map<ObjectName, List<String>> failures = getDomainRootProxy().getComplianceFailures();
        
        assert failures.size() == 0 :
            "Server indicates that there are non-compliant AMX MBean validator failures, failure count = " + failures.size() + "\n" + failures;
        }
        catch( final Throwable t )
        {
            System.out.println( "\n******* Test testAMXComplianceMonitorFailureCount() IGNORED, see issue #10096 ******* \n" );
            t.printStackTrace();
        }
     }
     
    @Test
    public void testDemo()
    {
        Demo.runDemo( false, new String[] { mHost, "" + mPort, mAdminUser, mAdminPassword });
    }
}





























