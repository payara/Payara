/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.util.*;

import com.sun.enterprise.universal.io.SmartFile;
import com.sun.enterprise.util.OS;

import static com.sun.enterprise.util.StringUtils.ok;

/**
 * Run a native process with jps
 * -- get the pid for a running JVM
 * note:  dropping in an implementation for jps is not hard.
 * @author bnevins
 */
public class Jps {
    public static void main(String[] args) {
        Set<Map.Entry<Integer, String>> set = getProcessTable().entrySet();
        System.out.println("** Got " + set.size() + " process entries");
        for (Map.Entry<Integer, String> e : set) {
            System.out.printf("%d %s%n", e.getKey(), e.getValue());
        }
        if(args.length > 0) {
            System.out.printf("Jps.isPid(%s) ==> %b%n", args[0], Jps.isPid(Integer.parseInt(args[0])));
        }
    }

    static public Map<Integer, String> getProcessTable() {
        return new Jps().pidMap;
    }

    /**
     * return the platform-specific process-id of a JVM
     * @param mainClassName The main class - this is how we identify the right JVM.
     * You can pass in a fully-qualified name or just the classname.
     * E.g. com.sun.enterprise.glassfish.bootstrap.ASMain and ASMain work the same.
     * @return the process id if possible otherwise 0
     */
    static public List<Integer> getPid(String mainClassName) {
        if (mainClassName == null)
            return Collections.emptyList();

        String plainName = plainClassName(mainClassName);
        Jps jps = new Jps();
        List<Integer> ints = new LinkedList<Integer>();
        Set<Map.Entry<Integer, String>> set = jps.pidMap.entrySet();
        Iterator<Map.Entry<Integer, String>> it = set.iterator();

        while (it.hasNext()) {
            Map.Entry<Integer, String> entry = it.next();
            String valueFull = entry.getValue();
            String valuePlain = plainClassName(valueFull);

            if (mainClassName.equals(valueFull) || plainName.equals(valuePlain))
                // got a match!
                ints.add(entry.getKey());
        }
        return ints;
    }

    /**
     * Is this pid owned by a process?
     * @param apid the pid of interest
     * @return whether there is a process running with that id
     */
    final static public boolean isPid(int apid) {
        return new Jps().pidMap.containsKey(apid);
    }

    private Jps() {
        try {
            if (ProcessUtils.jpsExe == null) {
                return;
            }
            ProcessManager pm = new ProcessManager(ProcessUtils.jpsExe.getPath(), "-l");
            pm.setEcho(false);
            pm.execute();
            String jpsOutput = pm.getStdout();

            // get each line
            String[] ss = jpsOutput.split("[\n\r]");

            for (String line : ss) {
                if (line == null || line.length() <= 0) {
                    continue;
                }

                String[] sublines = line.split(" ");
                if (sublines.length != 2) {
                    continue;
                }

                int aPid = 0;
                try {
                    aPid = Integer.parseInt(sublines[0]);
                }
                catch (Exception e) {
                    continue;
                }
                if (!isJps(sublines[1])) {
                    pidMap.put(aPid, sublines[1]);
                }
            }
        }
        catch (Exception e) {
        }
    }

    private boolean isJps(String id) {
        if (!ok(id)) {
            return false;
        }

        if (id.equals(getClass().getName())) {
            return true;
        }

        if (id.equals("sun.tools.jps.Jps")) {
            return true;
        }

        return false;
    }

    /**
     * This is a bit tricky.  "jps -l" will return a FQ classname
     * But it also might return a path if you start with "java -jar"
     * E.g.
     <pre>
     2524 sun.tools.jps.Jps
     5324 com.sun.enterprise.glassfish.bootstrap.ASMain
     4120 D:\glassfish4\glassfish\bin\..\modules\admin-cli.jar
     </pre>
     * If there is a path -- then there is no classname and vice-versa
     * @param s
     * @return
     */
    private static String plainClassName(String s) {
        if(s == null)
            return null;

        if(hasPath(s))
            return stripPath(s);

        if (!s.contains(".") || s.endsWith("."))
            return s;


        // we handled a/b/c/foo.jar
        // now let's handle foo.jar
        if(s.endsWith(".jar"))
            return s;

        return s.substring(s.lastIndexOf('.') + 1);
    }

    private static boolean hasPath(String s) {
        if(s.indexOf('/') >= 0)
            return true;
        if(s.indexOf('\\') >= 0)
            return true;
        return false;
    }

    /**
     * return whatever comes after the last file separator
     */
    private static String stripPath(String s) {
        // Don't bother with the annoying back vs. forward
        s = s.replace('\\', '/');
        int index = s.lastIndexOf('/');

        if(index < 0)
            return s;

        // don't forget about handling a name that ends in a slash!
        // should not happen!!  But if it does return the original
        if(s.length() - 1 <= index)
            return s;

        // we are GUARANTEED to have at least one char past the final slash...
        return s.substring(index + 1);
    }

    private Map<Integer, String> pidMap = new HashMap<Integer, String>();
}
