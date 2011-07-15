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

import com.sun.appserv.management.base.AMX;
import com.sun.appserv.management.base.Util;
import com.sun.appserv.management.config.Libraries;
import com.sun.appserv.management.util.misc.GSetUtil;
import org.glassfish.admin.amxtest.AMXTestBase;

import javax.management.ObjectName;
import java.util.Set;


/**
 */
public final class LibrariesTest
        extends AMXTestBase {
    public LibrariesTest() {
    }

    private final Set<String> READ_ONLY_LIBRARIES =
            GSetUtil.newUnmodifiableStringSet(
                    "MEjbApp", "__ejb_container_timer_app", "__JWSappclients");

    // see bug#6323557 "admin GUI becomes non-responsive after adding a library"
    private final Set<String> DONT_TEST_LIBRARIES =
            GSetUtil.newUnmodifiableStringSet("admingui");

    private static final String TEST_LIBS = "/foo:/bar";

    /**
     public void
     testGUIHang()
     {
     final String[]   TEST_LIBS   = new String[] { "/foo", "/bar" };
     final ObjectName    objectName  = Util.newObjectName( "amx:j2eeType=X-WebModuleConfig,name=admingui" );
     final WebModuleConfig   cfg = getProxyFactory().getProxy( objectName );
     <p/>
     final String[]  saveLibs    = cfg.getLibraries();
     assert( saveLibs != null );
     <p/>
     final String[]  testLibs    = ArrayUtil.newArray( saveLibs, TEST_LIBS );
     try
     {
     cfg.setLibraries( testLibs );
     }
     finally
     {
     cfg.setLibraries( saveLibs );
     }
     }
     */

    public void
    testLibraries() {
        final Set<Libraries> all = getTestUtil().getAllAMX(Libraries.class);

        for (final Libraries l : all) {
            final AMX amx = (AMX) l;

            if (DONT_TEST_LIBRARIES.contains(amx.getName())) {
                continue;
            }

            final ObjectName objectName = Util.getObjectName(amx);

            final String saveLibs = l.getLibraries();
            assert (saveLibs != null);

            final String testLibs = TEST_LIBS;
            try {
                l.setLibraries(testLibs);
            }
            catch (Exception e) {
                if (!READ_ONLY_LIBRARIES.contains(((AMX) l).getName())) {
                    warning("Couldn't change Libraries Attribute for " + objectName +
                            " (probably read-only, though not advertised as such)");
                }
            }
            finally {
                l.setLibraries( saveLibs );
		    }
		    
		}
	}
 
}


