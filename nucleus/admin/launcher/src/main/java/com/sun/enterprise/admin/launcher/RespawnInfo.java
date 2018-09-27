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
//Portions Copyright [2018] Payara Foundation and/or affiliates

package com.sun.enterprise.admin.launcher;

import java.util.*;

/**
 *
 * @author bnevins
 */
class RespawnInfo {
    
    private final String      classname;
    private final String      classpath;
    private final String[]    args;

    private static final String PREFIX = "-asadmin-";
    private static final String SEPARATOR = ",,,";
    
    RespawnInfo(String cn, String cp, String[] a) {
        classname = cn;
        classpath = cp;

        if(a == null)
            a = new String[0];

        args = a;
    }

    void put(Map<String, String> map) throws GFLauncherException {
        validate();
        map.put(PREFIX + "classname", classname);
        map.put(PREFIX + "classpath", classpath);
        putArgs(map);
    }

    private void validate() throws GFLauncherException {
        if(!ok(classname))
            throw new GFLauncherException("respawninfo.empty", "classname");
        if(!ok(classpath))
            throw new GFLauncherException("respawninfo.empty", "classpath");
        // args are idiot-proof
    }

    private void putArgs(Map<String, String> map) throws GFLauncherException {
        int numArgs = args.length;
        StringBuilder argLine = new StringBuilder();

        for(int i = 0; i < numArgs; i++) {
            String arg = args[i];

            if(i != 0)
                argLine.append(SEPARATOR);

            if(arg.contains(SEPARATOR)) {
                // this should not happen.  Only the ultra-paranoid programmer would
                // bother checking for it.  I guess that's me!
                throw new GFLauncherException("respawninfo.illegalToken", arg, SEPARATOR);
            }
            argLine.append(args[i]);
        }

        map.put(PREFIX + "args", argLine.toString());
    }
    
    private boolean ok(String s) {
        return s != null && s.length() > 0;
    }

}
