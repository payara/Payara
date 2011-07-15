/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.universal.glassfish.GFLauncherUtils;
import com.sun.enterprise.universal.io.SmartFile;
import java.io.File;
import java.util.*;
import static com.sun.enterprise.admin.launcher.GFLauncherConstants.*;

/**
 * The platform-specific code is ugly.  That's why it is concentrated here.
 * @author bnevins
 */
class GFLauncherNativeHelper {
    GFLauncherNativeHelper(GFLauncherInfo info_, JavaConfig javaConfig_, JvmOptions jvmOptions_, Profiler profiler_) {
        info = info_;
        javaConfig = javaConfig_;
        jvmOptions = jvmOptions_;
        profiler = profiler_;

        if(info == null || jvmOptions == null || profiler == null)
            throw new NullPointerException(
           "Null argument(s) to GFLauncherNativeHelper.GFLauncherNativeHelper");

        installDir = SmartFile.sanitize(info.getInstallDir());
        libDir = new File(installDir, LIBDIR);
    }

    List<String> getCommands() {
        List<String> list = new ArrayList<String>();

        String stockNativePathsString   = getStockNativePathString();
        String prefixFileString         = getPrefixString();
        String suffixFileString         = getSuffixString();
        String profilerFileString       = getProfilerString();
        String libFileString            = libDir.getPath();
        String lib64FileString          = getLib64String();

        // bnevins: Very simple to change the order right here in the future!
        // don't worry about extra PS's --> no problem-o
        // don't worry about duplicates -- SmartFile will get rid of them...
        StringBuilder sb = new StringBuilder();
        sb.append(prefixFileString).append(PS);
        sb.append(libFileString).append(PS);
        sb.append(lib64FileString).append(PS);
        sb.append(stockNativePathsString).append(PS);
        sb.append(profilerFileString).append(PS);
        sb.append(suffixFileString);

        // this looks dumb but there is a lot of potential cleaning going on here
        // * all duplicate directories are removed
        // * junk is removed, e.g. ":xxx::yy::::::" goes to "xxx:yy"

        String finalPathString = GFLauncherUtils.fileListToPathString(GFLauncherUtils.stringToFiles(sb.toString()));
        String nativeCommand = "-D" + JAVA_NATIVE_SYSPROP_NAME + "=" + finalPathString;
        list.add(nativeCommand);
        return list;
    }

    private String getStockNativePathString() {
        // return the path that is setup by the JVM
        String s = System.getProperty(JAVA_NATIVE_SYSPROP_NAME);

        if(!GFLauncherUtils.ok(s))
            s = "";

        return s;
    }

    private String getPrefixString() {
        return javaConfig.getNativeLibraryPrefix();
    }

    private String getSuffixString() {
        return javaConfig.getNativeLibrarySuffix();
    }

    private String getProfilerString() {
        // if not enabled -- fagetaboutit
        if(!profiler.isEnabled())
            return "";

        List<File> ff = profiler.getNativePath();
        return GFLauncherUtils.fileListToPathString(ff);
    }

    private String getLib64String() {
        // <i-r>/lib/sparcv9  has 64-bit SPARC natives
        // <i-r>/lib/amd64    has 64-bit x86 natives

        String osArch = System.getProperty("os.arch");
        File f64 = null;

        if(osArch.equals(SPARC))
            f64 = new File(libDir, SPARCV9);
        else if(osArch.equals(X86))
            f64 = new File(libDir, AMD64);

        if(f64 != null && f64.isDirectory())
            return f64.getPath();
        
        return "";
    }

    private final GFLauncherInfo    info;
    private final JvmOptions        jvmOptions;
    private final Profiler          profiler;
    private final File              installDir;
    private final File              libDir;
    private final JavaConfig        javaConfig;
}
