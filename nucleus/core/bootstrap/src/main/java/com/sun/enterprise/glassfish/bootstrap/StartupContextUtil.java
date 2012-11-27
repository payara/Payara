/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.glassfish.bootstrap;

import com.sun.enterprise.module.bootstrap.StartupContext;

import java.io.File;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;

/**
 * This encapsulates the behavior of the properties object that's part of
 * {@link com.sun.enterprise.module.bootstrap.StartupContext}.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public final class StartupContextUtil {

    // this contains utility methods only
    private StartupContextUtil() {
    }

    public static File getInstallRoot(Properties p) {
        return absolutize(new File(p.getProperty(Constants.INSTALL_ROOT_PROP_NAME)));

    }

    public static File getInstallRoot(StartupContext sc) {
        return getInstallRoot(sc.getArguments());
    }

    public static File getInstanceRoot(Properties p) {
        return absolutize(new File(p.getProperty(Constants.INSTANCE_ROOT_PROP_NAME)));
    }

    public static File getInstanceRoot(StartupContext sc) {
        return getInstanceRoot(sc.getArguments());
    }

    public static String[] getOriginalArguments(StartupContext sc) {
        Properties args = sc.getArguments();
        String s = args.getProperty(Constants.ORIGINAL_ARGS); // See how ASMain packages the arguments
        if (s == null) return new String[0];
        StringTokenizer st = new StringTokenizer(s, Constants.ARG_SEP, false);
        List<String> result = new ArrayList<String>();
        while (st.hasMoreTokens()) {
            result.add(st.nextToken());
        }
        return result.toArray(new String[result.size()]);

    }

    private static File absolutize(File f)
    {
        try
        {
            return f.getCanonicalFile();
        }
        catch(Exception e)
        {
            return f.getAbsoluteFile();
        }
    }

}
