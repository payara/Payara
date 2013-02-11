
/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.registration.impl.environment;

// The Service Tags team maintains the latest version of the implementation
// for system environment data collection.  JDK will include a copy of
// the most recent released version for a JDK release.        We rename
// the package to com.sun.servicetag so that the Sun Connection
// product always uses the latest version from the com.sun.scn.servicetags
// package. JDK and users of the com.sun.servicetag API
// (e.g. NetBeans and SunStudio) will use the version in JDK.

import com.sun.enterprise.registration.impl.RegistrationLogger;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.io.*;
import java.util.Locale;
/**
 * Windows implementation of the SystemEnvironment class.
 */
class WindowsSystemEnvironment extends SystemEnvironment {
    private static final Logger logger = RegistrationLogger.getLogger();
    WindowsSystemEnvironment() {
        super();

        // run a call to make sure things are initialized
        // ignore the first call result as the system may 
        // give inconsistent data on the first invocation ever
        getWmicResult("computersystem", "get", "model"); 

        setSystemModel(getWmicResult("computersystem", "get", "model"));
        setSystemManufacturer(getWmicResult("computersystem", "get", "manufacturer"));
        setSerialNumber(getWmicResult("bios", "get", "serialnumber"));

        String cpuMfr = getWmicResult("cpu", "get", "manufacturer");
        // this isn't as good an option, but if we couldn't get anything
        // from wmic, try the processor_identifier
        if (cpuMfr.length() == 0) {
            String procId = System.getenv("processor_identifer");
            if (procId != null) {
                String[] s = procId.split(",");
                cpuMfr = s[s.length - 1].trim();
            }
        }
        setCpuManufacturer(cpuMfr);

        setSockets(getWindowsSockets());
        setCores(getWindowsCores());
        setVirtCpus(getWindowsVirtCpus());
        setPhysMem(getWindowsPhysMem());
        setCpuName(getWmicResult("cpu", "get", "Name"));
        setClockRate(getWmicResult("cpu", "get", "MaxClockSpeed"));

        // try to remove the temp file that gets created from running wmic cmds
        try {
            // look in the current working directory
            File f = new File("TempWmicBatchFile.bat");
            if (f.exists()) {
                boolean b = f.delete();
                if (!b)
                    logger.finest("Could not delete" + f.getAbsolutePath());
            }
        } catch (Exception e) {
            logger.finest(e.getMessage());
            // ignore the exception
        }
    }

    private String getWindowsVirtCpus() {
        String res = getWmicResult("cpu", "get", "NumberOfLogicalProcessors");
        if (res == null || res.equals("")) {
            res = "1";
        }
        return res;
    }

    private String getWindowsCores() {
        String res = getWmicResult("cpu", "get", "NumberOfCores");
        if (res == null || res.equals("")) {
            res = "1";
        }
        return res;
    }

    private String getWindowsSockets() {
        String res = getFullWmicResult("cpu", "get", "DeviceID");
        Set<String> set = new HashSet<String>();
        for (String line : res.split("\n")) {
            line = line.trim();
            if (line.equals("")) {
                continue;
            }
            set.add(line);
        }
        if (set.size() == 0) {
            return "1";
        }
        return "" + set.size();
    }

    private String getWindowsPhysMem() {
        String mem = getWmicResult("computersystem", "get", "TotalPhysicalMemory");
        if (mem == null || mem.trim().equals("")) {
            return "0";
        }
        try {
            long l = Long.parseLong(mem);
            return "" + ((long) (l / (1024*1024)));
        } catch (Exception e) {
            return "0";
        }
    }


    /**
     * This method invokes wmic outside of the normal environment
     * collection routines.
     *
     * An initial call to wmic can be costly in terms of time.  
     *
     * <code>
     * Details of why the first call is costly can be found at:
     *
     * http://support.microsoft.com/kb/290216/en-us
     *
     * "When you run the Wmic.exe utility for the first time, the utility
     * compiles its .mof files into the repository. To save time during
     * Windows installation, this operation takes place as necessary."
     * </code>
     */
    private String getWmicResult(String alias, String verb, String property) {
        String res = "";
        BufferedReader in = null;
        BufferedWriter bw = null; 
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd", "/C", "WMIC", alias, verb, property);
            Process p = pb.start();
            // need this for executing windows commands (at least
            // needed for executing wmic command)
            bw = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
            bw.write(13);
            
            p.waitFor();
            if (p.exitValue() == 0) {
                in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line = null;
                while ((line = in.readLine()) != null) {
                    line = line.trim();
                    if (line.length() == 0) {
                        continue;
                    }
                    res = line;
                }
                // return the *last* line read
                return res;
            }

        } catch (Exception e) {
            // ignore the exception
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            if (bw != null) {
                try {
                    bw.flush();
                } catch (Exception ex) {                    //ignore...
                }
                try {
                    bw.close();
                } catch (Exception ex) {                    //ignore...
                }     
            }
        }
        return res.trim();
    }

    private String getFullWmicResult(String alias, String verb, String property) {
        StringBuilder res = new StringBuilder();
        BufferedReader in = null;
        BufferedWriter bw = null;
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd", "/C", "WMIC", alias, verb, property);
            Process p = pb.start();
            // need this for executing windows commands (at least
            // needed for executing wmic command)
            bw = new BufferedWriter(
                new OutputStreamWriter(p.getOutputStream()));
            bw.write(13);
            bw.flush();

            p.waitFor();
            if (p.exitValue() == 0) {
                in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line = null;
                while ((line = in.readLine()) != null) {
                    line = line.trim();
                    if (line.length() == 0) {
                        continue;
                    }
                    if (line.toLowerCase(Locale.US).indexOf(property.toLowerCase(Locale.US)) != -1) {
                        continue;
                    }
                    res.append(line).append("\n");
                }
            }

        } catch (Exception e) {
            // ignore the exception
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            
        }
        return res.toString();
    }
}
