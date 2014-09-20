/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

import org.testng.annotations.*;
import org.testng.Assert;

import javax.management.ObjectName;

import java.lang.Exception;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.*;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import org.glassfish.admin.amx.core.*;
import org.glassfish.admin.amx.base.*;
import org.glassfish.admin.amx.config.*;
import org.glassfish.admin.amx.monitoring.*;
import org.glassfish.admin.amx.util.CollectionUtil;
import org.glassfish.admin.amx.util.ExceptionUtil;
import org.glassfish.admin.amx.util.MapUtil;
import org.glassfish.admin.amx.logging.Logging;
import org.glassfish.admin.amx.annotation.*;

/** 
 */
//@Test(groups={"amx"}, description="AMXProxy tests", sequential=false, threadPoolSize=5)
@Test(
    sequential=false, threadPoolSize=10,
    groups =
    {
        "amx"
    },
    description = "test varioius miscellaneous AMX feature"
)
public final class AMXOtherTests extends AMXTestBase
{
    public AMXOtherTests()
    {
    }
    
    @Test
    public void testVariousGetters()
    {
        final RuntimeRoot runtimeRoot = getDomainRootProxy().getRuntime();
        final Map<String,ServerRuntime>  serverRuntimes = runtimeRoot.getServerRuntime();
    }
    
}




































