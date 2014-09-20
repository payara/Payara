/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.admin.util.cache;

import com.sun.enterprise.security.store.AsadminSecurityUtil;
import java.io.File;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author mmares
 */
public class AdminCacheWeakReferenceTest extends AdminCacheTstBase {
    
    public AdminCacheWeakReferenceTest() {
        super(AdminCacheWeakReference.getInstance());
    }
    
    @Test
    public void testWithFileDelete() {
        if (isSkipThisTest()) {
            System.out.println(this.getClass().getName() + ".testWithFileDelete(): Must skip this unit test, because something is wrong with file cache writing during build");
        } else {
            System.out.println(this.getClass().getName() + ".testWithFileDelete()");
        }
        String floyd1 = "Wish You Were Here";
        String floyd1Key = TEST_CACHE_COTEXT + "Pink.Floyd.1";
        getCache().put(floyd1Key, floyd1);
        String holder = getCache().get(floyd1Key, String.class); //To be shure that it stay in memory
        assertEquals(floyd1, holder);
        recursiveDelete(new File(AsadminSecurityUtil.getDefaultClientDir(), TEST_CACHE_COTEXT));
        assertEquals(floyd1, getCache().get(floyd1Key, String.class));
        System.out.println(this.getClass().getName() + ".testWithFileDelete(): Done");
    }
    
}
