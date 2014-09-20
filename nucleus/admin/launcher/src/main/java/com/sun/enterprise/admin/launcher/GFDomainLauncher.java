/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.launcher;

import com.sun.enterprise.universal.io.SmartFile;
import java.io.File;
import java.util.*;
import static com.sun.enterprise.util.SystemPropertyConstants.*;

/**
 * GFDomainLauncher
 * This class is a package-private subclass of GFLauncher designed for
 * domain launching
 * @author bnevins
 */
class GFDomainLauncher extends GFLauncher {

    GFDomainLauncher(GFLauncherInfo info) {
        super(info);
    }

    void internalLaunch() throws GFLauncherException {
        try {
            launchInstance();
        }
        catch (GFLauncherException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new GFLauncherException(ex);
        }
    }

    List<File> getMainClasspath() throws GFLauncherException {
        List<File> list = new ArrayList<File>();
        File dir = new File(getEnvProps().get(INSTALL_ROOT_PROPERTY),"modules");

        File bootjar = new File(dir, BOOTSTRAP_JAR);
        if (!bootjar.exists() && !isFakeLaunch())
            throw new GFLauncherException("nobootjar", dir.getPath());

        if(bootjar.exists())
            list.add(SmartFile.sanitize(bootjar));

        return list;
    }

    String getMainClass() throws GFLauncherException {
        return MAIN_CLASS;
    }
    private static final String MAIN_CLASS = "com.sun.enterprise.glassfish.bootstrap.ASMain";
    private static final String BOOTSTRAP_JAR = "glassfish.jar";
}

/* sample profiler config
 * 
       <java-config classpath-suffix="" debug-enabled="false" debug-options="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=9009" env-classpath-ignored="true" java-home="${com.sun.aas.javaRoot}" javac-options="-g" rmic-options="-iiop -poa -alwaysgenerate -keepgenerated -g" system-classpath="">
        <profiler classpath="c:/dev/elf/dist/elf.jar" enabled="false" name="MyProfiler" native-library-path="c:/bin">
          <jvm-options>-Dprofiler3=foo3</jvm-options>
          <jvm-options>-Dprofiler2=foo2</jvm-options>
          <jvm-options>-Dprofiler1=foof</jvm-options>
        </profiler>
 */
