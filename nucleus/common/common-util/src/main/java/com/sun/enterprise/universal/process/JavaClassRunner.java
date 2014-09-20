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

package com.sun.enterprise.universal.process;

import com.sun.enterprise.universal.io.SmartFile;
import com.sun.enterprise.util.OS;
import java.io.*;
import java.util.*;

/**
 * Very simple initial implementation
 * If it is useful there are plenty of improvements that can be made...
 * @author bnevins
 */
public class JavaClassRunner {
    public JavaClassRunner(String classpath, String[] sysprops, String classname, String[] args) throws IOException{
        if(javaExe == null)
            throw new IOException("Can not find a jvm");

        if(!ok(classname))
            throw new IllegalArgumentException("classname was null");

        List<String> cmdline = new LinkedList<String>();
        cmdline.add(javaExe.getPath());

        if(ok(classpath)) {
            cmdline.add("-cp");
            cmdline.add(classpath);
        }

        if(sysprops != null)
            for(String sysprop : sysprops) 
                cmdline.add(sysprop);

        cmdline.add(classname);

        if(args != null)
            for(String arg : args)
                cmdline.add(arg);

        ProcessBuilder pb = new ProcessBuilder(cmdline);
        Process p = pb.start();
        ProcessStreamDrainer.drain(classname, p);
    }

    private boolean ok(String s) {
        return s != null && s.length() > 0;
    }

    private static final File javaExe;

    static{
        String javaName = "java";

        if(OS.isWindows())
            javaName = "java.exe";

        final String    javaroot    = System.getProperty("java.home");
        final String    relpath     = "/bin/" + javaName;
        final File      fhere       = new File(javaroot + relpath);
        File            fthere      = new File(javaroot + "/.." + relpath);

        if(fhere.isFile())
            javaExe = SmartFile.sanitize(fhere);
        else if(fthere.isFile())
            javaExe = SmartFile.sanitize(fthere);
        else
            javaExe = null;
    }
}
